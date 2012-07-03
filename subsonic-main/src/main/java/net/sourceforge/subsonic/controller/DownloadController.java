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
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.io.RangeOutputStream;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.PlaylistService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.StringUtil;
import net.sourceforge.subsonic.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.LongRange;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * A controller used for downloading files to a remote client. If the requested path refers to a file, the
 * given file is downloaded.  If the requested path refers to a directory, the entire directory (including
 * sub-directories) are downloaded as an uncompressed zip-file.
 *
 * @author Sindre Mehus
 */
public class DownloadController implements Controller, LastModified {

    private static final Logger LOG = Logger.getLogger(DownloadController.class);

    private PlayerService playerService;
    private StatusService statusService;
    private SecurityService securityService;
    private PlaylistService playlistService;
    private SettingsService settingsService;
    private MediaFileService mediaFileService;

    public long getLastModified(HttpServletRequest request) {
        try {
            MediaFile mediaFile = getSingleFile(request);
            if (mediaFile == null || mediaFile.isDirectory() || mediaFile.getChanged() == null) {
                return -1;
            }
            return mediaFile.getChanged().getTime();
        } catch (ServletRequestBindingException e) {
            return -1;
        }
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        TransferStatus status = null;
        try {

            status = statusService.createDownloadStatus(playerService.getPlayer(request, response, false, false));

            MediaFile mediaFile = getSingleFile(request);
            String dir = request.getParameter("dir");
            Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");
            String playerId = request.getParameter("player");
            int[] indexes = ServletRequestUtils.getIntParameters(request, "i");

            if (mediaFile != null) {
                response.setIntHeader("ETag", mediaFile.getId());
                response.setHeader("Accept-Ranges", "bytes");
            }

            LongRange range = StringUtil.parseRange(request.getHeader("Range"));
            if (range != null) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                LOG.info("Got range: " + range);
            }

            if (mediaFile != null) {
                File file = mediaFile.getFile();
                if (!securityService.isReadAllowed(file)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return null;
                }

                if (file.isFile()) {
                    downloadFile(response, status, file, range);
                } else {
                    downloadDirectory(response, status, file, range);
                }
            } else if (dir != null) {
                File file = new File(dir);
                if (!securityService.isReadAllowed(file)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return null;
                }
                downloadFiles(response, status, file, indexes);

            } else if (playlistId != null) {
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                downloadFiles(response, status, songs, null, range);

            } else if (playerId != null) {
                Player player = playerService.getPlayerById(playerId);
                PlayQueue playQueue = player.getPlayQueue();
                playQueue.setName("Playlist");
                downloadFiles(response, status, playQueue.getFiles(), indexes.length == 0 ? null : indexes, range);
            }


        } finally {
            if (status != null) {
                statusService.removeDownloadStatus(status);
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, status.getBytesTransfered(), 0L);
            }
        }

        return null;
    }

    private MediaFile getSingleFile(HttpServletRequest request) throws ServletRequestBindingException {
        String path = request.getParameter("path");
        if (path != null) {
            return mediaFileService.getMediaFile(path);
        }
        Integer id = ServletRequestUtils.getIntParameter(request, "id");
        if (id != null) {
            return mediaFileService.getMediaFile(id);
        }
        return null;
    }

    /**
     * Downloads a single file.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param file     The file to download.
     * @param range    The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFile(HttpServletResponse response, TransferStatus status, File file, LongRange range) throws IOException {
        LOG.info("Starting to download '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
        status.setFile(file);

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + '\"');
        if (range == null) {
            Util.setContentLength(response, file.length());
        }

        copyFileToStream(file, RangeOutputStream.wrap(response.getOutputStream(), range), status, range);
        LOG.info("Downloaded '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
    }

    /**
     * Downloads a collection of files within a directory.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param dir      The directory.
     * @param indexes  Only download files with these indexes within the directory.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFiles(HttpServletResponse response, TransferStatus status, File dir, int[] indexes) throws IOException {
        String zipFileName = dir.getName() + ".zip";
        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        status.setFile(dir);

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        List<MediaFile> allChildren = mediaFileService.getChildrenOf(dir, true, true, true);
        List<MediaFile> mediaFiles = new ArrayList<MediaFile>();
        for (int index : indexes) {
            mediaFiles.add(allChildren.get(index));
        }

        for (MediaFile mediaFile : mediaFiles) {
            zip(out, mediaFile.getParentFile(), mediaFile.getFile(), status, null);
        }

        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Downloads all files in a directory (including sub-directories). The files are packed together in an
     * uncompressed zip-file.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param file     The file to download.
     * @param range    The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadDirectory(HttpServletResponse response, TransferStatus status, File file, LongRange range) throws IOException {
        String zipFileName = file.getName() + ".zip";
        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + '"');

        ZipOutputStream out = new ZipOutputStream(RangeOutputStream.wrap(response.getOutputStream(), range));
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        zip(out, file.getParentFile(), file, status, range);
        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Downloads the given files.  The files are packed together in an
     * uncompressed zip-file.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param files    The files to download.
     * @param indexes  Only download songs at these indexes. May be <code>null</code>.
     * @param range    The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFiles(HttpServletResponse response, TransferStatus status, List<MediaFile> files, int[] indexes, LongRange range) throws IOException {
        if (indexes != null && indexes.length == 1) {
            downloadFile(response, status, files.get(indexes[0]).getFile(), range);
            return;
        }

        String zipFileName = "download.zip";
        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + '"');

        ZipOutputStream out = new ZipOutputStream(RangeOutputStream.wrap(response.getOutputStream(), range));
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        List<MediaFile> filesToDownload = new ArrayList<MediaFile>();
        if (indexes == null) {
            filesToDownload.addAll(files);
        } else {
            for (int index : indexes) {
                try {
                    filesToDownload.add(files.get(index));
                } catch (IndexOutOfBoundsException x) { /* Ignored */}
            }
        }

        for (MediaFile mediaFile : filesToDownload) {
            zip(out, mediaFile.getParentFile(), mediaFile.getFile(), status, range);
        }

        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Utility method for writing the content of a given file to a given output stream.
     *
     * @param file   The file to copy.
     * @param out    The output stream to write to.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void copyFileToStream(File file, OutputStream out, TransferStatus status, LongRange range) throws IOException {
        LOG.info("Downloading '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());

        final int bufferSize = 16 * 1024; // 16 Kbit
        InputStream in = new BufferedInputStream(new FileInputStream(file), bufferSize);

        try {
            byte[] buf = new byte[bufferSize];
            long bitrateLimit = 0;
            long lastLimitCheck = 0;

            while (true) {
                long before = System.currentTimeMillis();
                int n = in.read(buf);
                if (n == -1) {
                    break;
                }
                out.write(buf, 0, n);

                // Don't sleep if outside range.
                if (range != null && !range.containsLong(status.getBytesSkipped() + status.getBytesTransfered())) {
                    status.addBytesSkipped(n);
                    continue;
                }

                status.addBytesTransfered(n);
                long after = System.currentTimeMillis();

                // Calculate bitrate limit every 5 seconds.
                if (after - lastLimitCheck > 5000) {
                    bitrateLimit = 1024L * settingsService.getDownloadBitrateLimit() /
                            Math.max(1, statusService.getAllDownloadStatuses().size());
                    lastLimitCheck = after;
                }

                // Sleep for a while to throttle bitrate.
                if (bitrateLimit != 0) {
                    long sleepTime = 8L * 1000 * bufferSize / bitrateLimit - (after - before);
                    if (sleepTime > 0L) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (Exception x) {
                            LOG.warn("Failed to sleep.", x);
                        }
                    }
                }
            }
        } finally {
            out.flush();
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Writes a file or a directory structure to a zip output stream. File entries in the zip file are relative
     * to the given root.
     *
     * @param out    The zip output stream.
     * @param root   The root of the directory structure.  Used to create path information in the zip file.
     * @param file   The file or directory to zip.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void zip(ZipOutputStream out, File root, File file, TransferStatus status, LongRange range) throws IOException {

        // Exclude all hidden files starting with a "."
        if (file.getName().startsWith(".")) {
            return;
        }

        String zipName = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);

        if (file.isFile()) {
            status.setFile(file);

            ZipEntry zipEntry = new ZipEntry(zipName);
            zipEntry.setSize(file.length());
            zipEntry.setCompressedSize(file.length());
            zipEntry.setCrc(computeCrc(file));

            out.putNextEntry(zipEntry);
            copyFileToStream(file, out, status, range);
            out.closeEntry();

        } else {
            ZipEntry zipEntry = new ZipEntry(zipName + '/');
            zipEntry.setSize(0);
            zipEntry.setCompressedSize(0);
            zipEntry.setCrc(0);

            out.putNextEntry(zipEntry);
            out.closeEntry();

            File[] children = FileUtil.listFiles(file);
            for (File child : children) {
                zip(out, root, child, status, range);
            }
        }
    }

    /**
     * Computes the CRC checksum for the given file.
     *
     * @param file The file to compute checksum for.
     * @return A CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    private long computeCrc(File file) throws IOException {
        CRC32 crc = new CRC32();
        InputStream in = new FileInputStream(file);

        try {

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n != -1) {
                crc.update(buf, 0, n);
                n = in.read(buf);
            }

        } finally {
            in.close();
        }

        return crc.getValue();
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
