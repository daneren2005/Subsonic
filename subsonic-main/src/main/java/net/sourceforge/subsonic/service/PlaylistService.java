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
package net.sourceforge.subsonic.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.util.Pair;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.dao.PlaylistDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * @see net.sourceforge.subsonic.domain.PlayQueue
 */
public class PlaylistService {

    private static final Logger LOG = Logger.getLogger(PlaylistService.class);
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private PlaylistDao playlistDao;
    private SecurityService securityService;
    private SettingsService settingsService;

    public void init() {
        try {
            importPlaylists();
        } catch (Throwable x) {
            LOG.warn("Failed to import playlists: " + x, x);
        }
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {
        return playlistDao.getReadablePlaylistsForUser(username);
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {

        // Admin users are allowed to modify all playlists that are visible to them.
        if (securityService.isAdmin(username)) {
            return getReadablePlaylistsForUser(username);
        }

        return playlistDao.getWritablePlaylistsForUser(username);
    }

    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return playlistDao.getPlaylistUsers(playlistId);
    }

    public List<MediaFile> getFilesInPlaylist(int id) {
        return mediaFileDao.getFilesInPlaylist(id);
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        playlistDao.setFilesInPlaylist(id, files);
    }

    public void createPlaylist(Playlist playlist) {
        playlistDao.createPlaylist(playlist);
    }

    public void addPlaylistUser(int playlistId, String username) {
        playlistDao.addPlaylistUser(playlistId, username);
    }

    public void deletePlaylistUser(int playlistId, String username) {
        playlistDao.deletePlaylistUser(playlistId, username);
    }

    public boolean isReadAllowed(Playlist playlist, String username) {
        if (username == null) {
            return false;
        }
        if (username.equals(playlist.getUsername()) || playlist.isPublic()) {
            return true;
        }
        return playlistDao.getPlaylistUsers(playlist.getId()).contains(username);
    }

    public boolean isWriteAllowed(Playlist playlist, String username) {
        return username != null && username.equals(playlist.getUsername());
    }

    public void deletePlaylist(int id) {
        playlistDao.deletePlaylist(id);
    }

    public void updatePlaylist(Playlist playlist) {
        playlistDao.updatePlaylist(playlist);
    }

    public Playlist importPlaylist(String username, String playlistName, String fileName, String format, InputStream inputStream) throws Exception {
        PlaylistFormat playlistFormat = PlaylistFormat.getPlaylistFormat(format);
        if (playlistFormat == null) {
            throw new Exception("Unsupported playlist format: " + format);
        }

        Pair<List<MediaFile>, List<String>> result = parseFiles(IOUtils.toByteArray(inputStream), playlistFormat);
        if (result.getFirst().isEmpty() && !result.getSecond().isEmpty()) {
            throw new Exception("No songs in the playlist were found.");
        }

        for (String error : result.getSecond()) {
            LOG.warn("File in playlist '" + fileName + "' not found: " + error);
        }

        Date now = new Date();
        Playlist playlist = new Playlist();
        playlist.setUsername(username);
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setPublic(true);
        playlist.setName(playlistName);
        playlist.setImportedFrom(fileName);

        createPlaylist(playlist);
        setFilesInPlaylist(playlist.getId(), result.getFirst());

        return playlist;
    }

    private Pair<List<MediaFile>, List<String>> parseFiles(byte[] playlist, PlaylistFormat playlistFormat) throws IOException {
        Pair<List<MediaFile>, List<String>> result = null;

        // Try with multiple encodings; use the one that finds the most files.
        String[] encodings = {StringUtil.ENCODING_LATIN, StringUtil.ENCODING_UTF8, Charset.defaultCharset().name()};
        for (String encoding : encodings) {
            Pair<List<MediaFile>, List<String>> files = parseFilesWithEncoding(playlist, playlistFormat, encoding);
            if (result == null || result.getFirst().size() < files.getFirst().size()) {
                result = files;
            }
        }
        return result;
    }

    private Pair<List<MediaFile>, List<String>> parseFilesWithEncoding(byte[] playlist, PlaylistFormat playlistFormat, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(playlist), encoding));
        return playlistFormat.parse(reader, mediaFileService);
    }

    public void exportPlaylist(int id, OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StringUtil.ENCODING_UTF8));
        new M3UFormat().format(getFilesInPlaylist(id), writer);
    }

    /**
     * Implementation of M3U playlist format.
     */
    private void importPlaylists() throws Exception {
        String playlistFolderPath = settingsService.getPlaylistFolder();
        if (playlistFolderPath == null) {
            return;
        }
        File playlistFolder = new File(playlistFolderPath);
        if (!playlistFolder.exists()) {
            return;
        }

        List<Playlist> allPlaylists = playlistDao.getAllPlaylists();
        for (File file : playlistFolder.listFiles()) {
            try {
                importPlaylistIfNotExisting(file, allPlaylists);
            } catch (Exception x) {
                LOG.warn("Failed to auto-import playlist " + file + ". " + x.getMessage());
            }
        }
    }

    private void importPlaylistIfNotExisting(File file, List<Playlist> allPlaylists) throws Exception {
        String format = FilenameUtils.getExtension(file.getPath());
        if (PlaylistFormat.getPlaylistFormat(format) == null) {
            return;
        }

        String fileName = file.getName();
        for (Playlist playlist : allPlaylists) {
            if (fileName.equals(playlist.getImportedFrom())) {
                return; // Already imported.
            }
        }
        InputStream in = new FileInputStream(file);
        try {
            importPlaylist(User.USERNAME_ADMIN, FilenameUtils.getBaseName(fileName), fileName, format, in);
            LOG.info("Auto-imported playlist " + file);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void setPlaylistDao(PlaylistDao playlistDao) {
        this.playlistDao = playlistDao;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
    /**
     * Abstract superclass for playlist formats.
     */

    private abstract static class PlaylistFormat {
        public abstract Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException;

        public abstract void format(List<MediaFile> files, PrintWriter writer) throws IOException;

        public static PlaylistFormat getPlaylistFormat(String format) {
            if (format == null) {
                return null;
            }
            if (format.equalsIgnoreCase("m3u") || format.equalsIgnoreCase("m3u8")) {
                return new M3UFormat();
            }
            if (format.equalsIgnoreCase("pls")) {
                return new PLSFormat();
            }
            if (format.equalsIgnoreCase("xspf")) {
                return new XSPFFormat();
            }
            return null;
        }

        protected MediaFile getMediaFile(MediaFileService mediaFileService, String path) {
            try {
                MediaFile file = mediaFileService.getMediaFile(path);
                if (file != null && file.exists()) {
                    return file;
                }
            } catch (SecurityException x) {
                // Ignored
            }
            return null;
        }
    }

    private static class M3UFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    MediaFile file = getMediaFile(mediaFileService, line);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(line);
                    }
                }
                line = reader.readLine();
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("#EXTM3U");
            for (MediaFile file : files) {
                writer.println(file.getPath());
            }
            if (writer.checkError()) {
                throw new IOException("Error when writing playlist");
            }
        }
    }

    /**
     * Implementation of PLS playlist format.
     */
    private static class PLSFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();

            Pattern pattern = Pattern.compile("^File\\d+=(.*)$");
            String line = reader.readLine();
            while (line != null) {

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String path = matcher.group(1);
                    MediaFile file = getMediaFile(mediaFileService, path);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(path);
                    }
                }
                line = reader.readLine();
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("[playlist]");
            int counter = 0;

            for (MediaFile file : files) {
                counter++;
                writer.println("File" + counter + '=' + file.getPath());
            }
            writer.println("NumberOfEntries=" + counter);
            writer.println("Version=2");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }

    /**
     * Implementation of XSPF (http://www.xspf.org/) playlist format.
     */
    private static class XSPFFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();

            SAXBuilder builder = new SAXBuilder();
            Document document;
            try {
                document = builder.build(reader);
            } catch (JDOMException x) {
                LOG.warn("Failed to parse XSPF playlist.", x);
                throw new IOException("Failed to parse XSPF playlist.");
            }

            Element root = document.getRootElement();
            Namespace ns = root.getNamespace();
            Element trackList = root.getChild("trackList", ns);
            List<?> tracks = trackList.getChildren("track", ns);

            for (Object obj : tracks) {
                Element track = (Element) obj;
                String location = track.getChildText("location", ns);
                if (location != null && location.startsWith("file://")) {
                    location = location.replaceFirst("file://", "");
                    MediaFile file = getMediaFile(mediaFileService, location);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(location);
                    }
                }
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">");
            writer.println("    <trackList>");

            for (MediaFile file : files) {
                writer.println("        <track><location>file://" + StringEscapeUtils.escapeXml(file.getPath()) + "</location></track>");
            }
            writer.println("    </trackList>");
            writer.println("</playlist>");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }
}
