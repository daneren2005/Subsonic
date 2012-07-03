/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.ArtistDao;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.Artist;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Controller which produces cover art images.
 *
 * @author Sindre Mehus
 */
public class CoverArtController implements Controller, LastModified {

    public static final String ALBUM_COVERART_PREFIX = "al-";
    public static final String ARTIST_COVERART_PREFIX = "ar-";

    private static final Logger LOG = Logger.getLogger(CoverArtController.class);

    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private ArtistDao artistDao;
    private AlbumDao albumDao;

    public long getLastModified(HttpServletRequest request) {
        try {
            File file = getImageFile(request);
            if (file == null) {
                return 0;  // Request for the default image.
            }
            if (!FileUtil.exists(file)) {
                return -1;
            }

            return FileUtil.lastModified(file);
        } catch (Exception e) {
            return  -1;
        }
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        File file = getImageFile(request);

        if (file != null && !FileUtil.exists(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // Check access.
        if (file != null && !securityService.isReadAllowed(file)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        // Send default image if no path is given. (No need to cache it, since it will be cached in browser.)
        Integer size = ServletRequestUtils.getIntParameter(request, "size");
        if (file == null) {
            sendDefault(size, response);
            return null;
        }

        // Optimize if no scaling is required.
        if (size == null) {
            sendUnscaled(file, response);
            return null;
        }

        // Send cached image, creating it if necessary.
        try {
            File cachedImage = getCachedImage(file, size);
            sendImage(cachedImage, response);
        } catch (IOException e) {
            sendDefault(size, response);
        }

        return null;
    }

    private File getImageFile(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id != null) {
            if (id.startsWith(ALBUM_COVERART_PREFIX)) {
                return getAlbumImage(Integer.valueOf(id.replace(ALBUM_COVERART_PREFIX, "")));
            }
            if (id.startsWith(ARTIST_COVERART_PREFIX)) {
                return getArtistImage(Integer.valueOf(id.replace(ARTIST_COVERART_PREFIX, "")));
            }
            return getMediaFileImage(Integer.valueOf(id));
        }

        String path = StringUtils.trimToNull(request.getParameter("path"));
        return path != null ? new File(path) : null;
    }

    private File getArtistImage(int id) {
        Artist artist = artistDao.getArtist(id);
        return artist == null || artist.getCoverArtPath() == null ? null : new File(artist.getCoverArtPath());
    }

    private File getAlbumImage(int id) {
        Album album = albumDao.getAlbum(id);
        return album == null || album.getCoverArtPath() == null ? null : new File(album.getCoverArtPath());
    }

    private File getMediaFileImage(int id) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        return mediaFile == null ? null : mediaFileService.getCoverArt(mediaFile);
    }

    private void sendImage(File file, HttpServletResponse response) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void sendDefault(Integer size, HttpServletResponse response) throws IOException {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("default_cover.jpg");
            BufferedImage image = ImageIO.read(in);
            if (size != null) {
                image = scale(image, size, size);
            }
            ImageIO.write(image, "jpeg", response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void sendUnscaled(File file, HttpServletResponse response) throws IOException {
        InputStream in = null;
        try {
            in = getImageInputStream(file);
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private File getCachedImage(File file, int size) throws IOException {
        String md5 = DigestUtils.md5Hex(file.getPath());
        File cachedImage = new File(getImageCacheDirectory(size), md5 + ".jpeg");

        // Is cache missing or obsolete?
        if (!cachedImage.exists() || FileUtil.lastModified(file) > cachedImage.lastModified()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = getImageInputStream(file);
                out = new FileOutputStream(cachedImage);
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new Exception("Unable to decode image.");
                }

                image = scale(image, size, size);
                ImageIO.write(image, "jpeg", out);

            } catch (Throwable x) {
                // Delete corrupt (probably empty) thumbnail cache.
                LOG.warn("Failed to create thumbnail for " + file, x);
                IOUtils.closeQuietly(out);
                cachedImage.delete();
                throw new IOException("Failed to create thumbnail for " + file + ". " + x.getMessage());

            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        }
        return cachedImage;
    }

    /**
     * Returns an input stream to the image in the given file.  If the file is an audio file,
     * the embedded album art is returned.
     */
    private InputStream getImageInputStream(File file) throws IOException {
        JaudiotaggerParser parser = new JaudiotaggerParser();
        if (parser.isApplicable(file)) {
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            return new ByteArrayInputStream(parser.getImageData(mediaFile));
        } else {
            return new FileInputStream(file);
        }
    }

    private synchronized File getImageCacheDirectory(int size) {
        File dir = new File(SettingsService.getSubsonicHome(), "thumbs");
        dir = new File(dir, String.valueOf(size));
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                LOG.info("Created thumbnail cache " + dir);
            } else {
                LOG.error("Failed to create thumbnail cache " + dir);
            }
        }

        return dir;
    }

    public static BufferedImage scale(BufferedImage image, int width, int height) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage thumb = image;

        // For optimal results, use step by step bilinear resampling - halfing the size at each step.
        do {
            w /= 2;
            h /= 2;
            if (w < width) {
                w = width;
            }
            if (h < height) {
                h = height;
            }

            BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(thumb, 0, 0, temp.getWidth(), temp.getHeight(), null);
            g2.dispose();

            thumb = temp;
        } while (w != width);

        return thumb;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }
}
