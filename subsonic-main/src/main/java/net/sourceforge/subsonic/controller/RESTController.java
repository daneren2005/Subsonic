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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.subsonic.ajax.PlayQueueService;
import net.sourceforge.subsonic.domain.Playlist;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.ajax.ChatService;
import net.sourceforge.subsonic.ajax.LyricsInfo;
import net.sourceforge.subsonic.ajax.LyricsService;
import net.sourceforge.subsonic.command.UserSettingsCommand;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.ArtistDao;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.Artist;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.MusicIndex;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayerTechnology;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.PodcastChannel;
import net.sourceforge.subsonic.domain.PodcastEpisode;
import net.sourceforge.subsonic.domain.RandomSearchCriteria;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.domain.Share;
import net.sourceforge.subsonic.domain.TranscodeScheme;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.AudioScrobblerService;
import net.sourceforge.subsonic.service.JukeboxService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.PlaylistService;
import net.sourceforge.subsonic.service.PodcastService;
import net.sourceforge.subsonic.service.RatingService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.ShareService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.util.StringUtil;
import net.sourceforge.subsonic.util.XMLBuilder;

import static net.sourceforge.subsonic.security.RESTRequestParameterProcessingFilter.decrypt;
import static net.sourceforge.subsonic.util.XMLBuilder.Attribute;
import static net.sourceforge.subsonic.util.XMLBuilder.AttributeSet;

/**
 * Multi-controller used for the REST API.
 * <p/>
 * For documentation, please refer to api.jsp.
 *
 * @author Sindre Mehus
 */
public class RESTController extends MultiActionController {

    private static final Logger LOG = Logger.getLogger(RESTController.class);

    private SettingsService settingsService;
    private SecurityService securityService;
    private PlayerService playerService;
    private MediaFileService mediaFileService;
    private TranscodingService transcodingService;
    private DownloadController downloadController;
    private CoverArtController coverArtController;
    private AvatarController avatarController;
    private UserSettingsController userSettingsController;
    private LeftController leftController;
    private HomeController homeController;
    private StatusService statusService;
    private StreamController streamController;
    private ShareService shareService;
    private PlaylistService playlistService;
    private ChatService chatService;
    private LyricsService lyricsService;
    private PlayQueueService playQueueService;
    private JukeboxService jukeboxService;
    private AudioScrobblerService audioScrobblerService;
    private PodcastService podcastService;
    private RatingService ratingService;
    private SearchService searchService;
    private MediaFileDao mediaFileDao;
    private ArtistDao artistDao;
    private AlbumDao albumDao;

    public void ping(HttpServletRequest request, HttpServletResponse response) throws Exception {
        XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
        response.getWriter().print(builder);
    }

    public void getLicense(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        String email = settingsService.getLicenseEmail();
        String key = settingsService.getLicenseCode();
        Date date = settingsService.getLicenseDate();
        boolean valid = settingsService.isLicenseValid();

        AttributeSet attributes = new AttributeSet();
        attributes.add("valid", valid);
        if (valid) {
            attributes.add("email", email);
            attributes.add("key", key);
            attributes.add("date", StringUtil.toISO8601(date));
        }

        builder.add("license", attributes, true);
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getMusicFolders(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("musicFolders", false);

        for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
            AttributeSet attributes = new AttributeSet();
            attributes.add("id", musicFolder.getId());
            attributes.add("name", musicFolder.getName());
            builder.add("musicFolder", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getIndexes(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        long ifModifiedSince = ServletRequestUtils.getLongParameter(request, "ifModifiedSince", 0L);
        long lastModified = leftController.getLastModified(request);

        if (lastModified <= ifModifiedSince) {
            builder.endAll();
            response.getWriter().print(builder);
            return;
        }

        builder.add("indexes", "lastModified", lastModified, false);

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders();
        Integer musicFolderId = ServletRequestUtils.getIntParameter(request, "musicFolderId");
        if (musicFolderId != null) {
            for (MusicFolder musicFolder : musicFolders) {
                if (musicFolderId.equals(musicFolder.getId())) {
                    musicFolders = Arrays.asList(musicFolder);
                    break;
                }
            }
        }

        List<MediaFile> shortcuts = leftController.getShortcuts(musicFolders, settingsService.getShortcutsAsArray());
        for (MediaFile shortcut : shortcuts) {
            builder.add("shortcut", true,
                    new Attribute("name", shortcut.getName()),
                    new Attribute("id", shortcut.getId()));
        }

        SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists = leftController.getMusicFolderContent(musicFolders).getIndexedArtists();

        for (Map.Entry<MusicIndex, SortedSet<MusicIndex.Artist>> entry : indexedArtists.entrySet()) {
            builder.add("index", "name", entry.getKey().getIndex(), false);

            for (MusicIndex.Artist artist : entry.getValue()) {
                for (MediaFile mediaFile : artist.getMediaFiles()) {
                    if (mediaFile.isDirectory()) {
                        builder.add("artist", true,
                                new Attribute("name", artist.getName()),
                                new Attribute("id", mediaFile.getId()));
                    }
                }
            }
            builder.end();
        }

        // Add children
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        List<MediaFile> singleSongs = leftController.getSingleSongs(musicFolders);

        for (MediaFile singleSong : singleSongs) {
            builder.add("child", createAttributesForMediaFile(player, singleSong, username), true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getArtists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        String username = securityService.getCurrentUsername(request);

        builder.add("artists", false);

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE);
        for (Artist artist : artists) {
            AttributeSet attributes = createAttributesForArtist(artist, username);
            builder.add("artist", attributes, true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    private AttributeSet createAttributesForArtist(Artist artist, String username) {
        AttributeSet attributes = new AttributeSet();
        attributes.add("id", artist.getId());
        attributes.add("name", artist.getName());
        if (artist.getCoverArtPath() != null) {
            attributes.add("coverArt", CoverArtController.ARTIST_COVERART_PREFIX + artist.getId());
        }
        attributes.add("albumCount", artist.getAlbumCount());
        attributes.add("starred", StringUtil.toISO8601(artistDao.getArtistStarredDate(artist.getId(), username)));
        return attributes;
    }

    public void getArtist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        String username = securityService.getCurrentUsername(request);
        Artist artist;
        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            artist = artistDao.getArtist(id);
            if (artist == null) {
                throw new Exception();
            }
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        builder.add("artist", createAttributesForArtist(artist, username), false);
        for (Album album : albumDao.getAlbumsForArtist(artist.getName())) {
            builder.add("album", createAttributesForAlbum(album, username), true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    private AttributeSet createAttributesForAlbum(Album album, String username) {
        AttributeSet attributes;
        attributes = new AttributeSet();
        attributes.add("id", album.getId());
        attributes.add("name", album.getName());
        attributes.add("artist", album.getArtist());
        if (album.getArtist() != null) {
            Artist artist = artistDao.getArtist(album.getArtist());
            if (artist != null) {
                attributes.add("artistId", artist.getId());
            }
        }
        if (album.getCoverArtPath() != null) {
            attributes.add("coverArt", CoverArtController.ALBUM_COVERART_PREFIX + album.getId());
        }
        attributes.add("songCount", album.getSongCount());
        attributes.add("duration", album.getDurationSeconds());
        attributes.add("created", StringUtil.toISO8601(album.getCreated()));
        attributes.add("starred", StringUtil.toISO8601(albumDao.getAlbumStarredDate(album.getId(), username)));

        return attributes;
    }

    private AttributeSet createAttributesForPlaylist(Playlist playlist) {
        AttributeSet attributes;
        attributes = new AttributeSet();
        attributes.add("id", playlist.getId());
        attributes.add("name", playlist.getName());
        attributes.add("comment", playlist.getComment());
        attributes.add("owner", playlist.getUsername());
        attributes.add("public", playlist.isPublic());
        attributes.add("songCount", playlist.getFileCount());
        attributes.add("duration", playlist.getDurationSeconds());
        attributes.add("created", StringUtil.toISO8601(playlist.getCreated()));
        return attributes;
    }

    public void getAlbum(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        Album album;
        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            album = albumDao.getAlbum(id);
            if (album == null) {
                throw new Exception();
            }
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.NOT_FOUND, "Album not found.");
            return;
        }

        builder.add("album", createAttributesForAlbum(album, username), false);
        for (MediaFile mediaFile : mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName())) {
            builder.add("song", createAttributesForMediaFile(player, mediaFile, username) , true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getSong(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        MediaFile song;
        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            song = mediaFileDao.getMediaFile(id);
            if (song == null || song.isDirectory()) {
                throw new Exception();
            }
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.NOT_FOUND, "Song not found.");
            return;
        }

        builder.add("song", createAttributesForMediaFile(player, song, username), true);

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getMusicDirectory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        MediaFile dir;
        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            dir = mediaFileService.getMediaFile(id);
            if (dir == null) {
                throw new Exception();
            }
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.NOT_FOUND, "Directory not found");
            return;
        }

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("directory", false,
                new Attribute("id", dir.getId()),
                new Attribute("name", dir.getName()));

        for (MediaFile child : mediaFileService.getChildrenOf(dir, true, true, true)) {
            AttributeSet attributes = createAttributesForMediaFile(player, child, username);
            builder.add("child", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    @Deprecated
    public void search(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        String any = request.getParameter("any");
        String artist = request.getParameter("artist");
        String album = request.getParameter("album");
        String title = request.getParameter("title");

        StringBuilder query = new StringBuilder();
        if (any != null) {
            query.append(any).append(" ");
        }
        if (artist != null) {
            query.append(artist).append(" ");
        }
        if (album != null) {
            query.append(album).append(" ");
        }
        if (title != null) {
            query.append(title);
        }

        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query.toString().trim());
        criteria.setCount(ServletRequestUtils.getIntParameter(request, "count", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "offset", 0));

        SearchResult result = searchService.search(criteria, SearchService.IndexType.SONG);
        builder.add("searchResult", false,
                new Attribute("offset", result.getOffset()),
                new Attribute("totalHits", result.getTotalHits()));

        for (MediaFile mediaFile : result.getMediaFiles()) {
            AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
            builder.add("match", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void search2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        builder.add("searchResult2", false);

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(ServletRequestUtils.getIntParameter(request, "artistCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "artistOffset", 0));
        SearchResult artists = searchService.search(criteria, SearchService.IndexType.ARTIST);
        for (MediaFile mediaFile : artists.getMediaFiles()) {
            builder.add("artist", true,
                    new Attribute("name", mediaFile.getName()),
                    new Attribute("id", mediaFile.getId()));
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "albumCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "albumOffset", 0));
        SearchResult albums = searchService.search(criteria, SearchService.IndexType.ALBUM);
        for (MediaFile mediaFile : albums.getMediaFiles()) {
            AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
            builder.add("album", attributes, true);
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "songCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "songOffset", 0));
        SearchResult songs = searchService.search(criteria, SearchService.IndexType.SONG);
        for (MediaFile mediaFile : songs.getMediaFiles()) {
            AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
            builder.add("song", attributes, true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }
    
    public void search3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        builder.add("searchResult3", false);

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(ServletRequestUtils.getIntParameter(request, "artistCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "artistOffset", 0));
        SearchResult searchResult = searchService.search(criteria, SearchService.IndexType.ARTIST_ID3);
        for (Artist artist : searchResult.getArtists()) {
            builder.add("artist", createAttributesForArtist(artist, username), true);
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "albumCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "albumOffset", 0));
        searchResult = searchService.search(criteria, SearchService.IndexType.ALBUM_ID3);
        for (Album album : searchResult.getAlbums()) {
            builder.add("album", createAttributesForAlbum(album, username), true);
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "songCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "songOffset", 0));
        searchResult = searchService.search(criteria, SearchService.IndexType.SONG);
        for (MediaFile song : searchResult.getMediaFiles()) {
            builder.add("song", createAttributesForMediaFile(player, song, username), true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPlaylists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        User user = securityService.getCurrentUser(request);
        String authenticatedUsername = user.getUsername();
        String requestedUsername = request.getParameter("username");

        if (requestedUsername == null) {
            requestedUsername = authenticatedUsername;
        } else if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, authenticatedUsername + " is not authorized to get playlists for " + requestedUsername);
            return;
        }

        builder.add("playlists", false);

        for (Playlist playlist : playlistService.getReadablePlaylistsForUser(requestedUsername)) {
            List<String> sharedUsers = playlistService.getPlaylistUsers(playlist.getId());
            builder.add("playlist", createAttributesForPlaylist(playlist), sharedUsers.isEmpty());
            if (!sharedUsers.isEmpty()) {
                for (String username : sharedUsers) {
                    builder.add("allowedUser", (Iterable<Attribute>) null, username, true);
                }
                builder.end();
            }
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);

        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

            Playlist playlist = playlistService.getPlaylist(id);
            if (playlist == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
                return;
            }
            if (!playlistService.isReadAllowed(playlist, username)) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
                return;
            }
            builder.add("playlist", createAttributesForPlaylist(playlist), false);
            for (String allowedUser : playlistService.getPlaylistUsers(playlist.getId())) {
                builder.add("allowedUser", (Iterable<Attribute>) null, allowedUser, true);
            }
            for (MediaFile mediaFile : playlistService.getFilesInPlaylist(id)) {
                AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                builder.add("entry", attributes, true);
            }

            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void jukeboxControl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        User user = securityService.getCurrentUser(request);
        if (!user.isJukeboxRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to use jukebox.");
            return;
        }

        try {
            boolean returnPlaylist = false;
            String action = ServletRequestUtils.getRequiredStringParameter(request, "action");
            if ("start".equals(action)) {
                playQueueService.doStart(request, response);
            } else if ("stop".equals(action)) {
                playQueueService.doStop(request, response);
            } else if ("skip".equals(action)) {
                int index = ServletRequestUtils.getRequiredIntParameter(request, "index");
                int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);
                playQueueService.doSkip(request, response, index, offset);
            } else if ("add".equals(action)) {
                int[] ids = ServletRequestUtils.getIntParameters(request, "id");
                playQueueService.doAdd(request, response, ids);
            } else if ("set".equals(action)) {
                int[] ids = ServletRequestUtils.getIntParameters(request, "id");
                playQueueService.doSet(request, response, ids);
            } else if ("clear".equals(action)) {
                playQueueService.doClear(request, response);
            } else if ("remove".equals(action)) {
                int index = ServletRequestUtils.getRequiredIntParameter(request, "index");
                playQueueService.doRemove(request, response, index);
            } else if ("shuffle".equals(action)) {
                playQueueService.doShuffle(request, response);
            } else if ("setGain".equals(action)) {
                float gain = ServletRequestUtils.getRequiredFloatParameter(request, "gain");
                jukeboxService.setGain(gain);
            } else if ("get".equals(action)) {
                returnPlaylist = true;
            } else if ("status".equals(action)) {
                // No action necessary.
            } else {
                throw new Exception("Unknown jukebox action: '" + action + "'.");
            }

            XMLBuilder builder = createXMLBuilder(request, response, true);

            Player player = playerService.getPlayer(request, response);
            String username = securityService.getCurrentUsername(request);
            Player jukeboxPlayer = jukeboxService.getPlayer();
            boolean controlsJukebox = jukeboxPlayer != null && jukeboxPlayer.getId().equals(player.getId());
            PlayQueue playQueue = player.getPlayQueue();

            List<Attribute> attrs = new ArrayList<Attribute>(Arrays.asList(
                    new Attribute("currentIndex", controlsJukebox && !playQueue.isEmpty() ? playQueue.getIndex() : -1),
                    new Attribute("playing", controlsJukebox && !playQueue.isEmpty() && playQueue.getStatus() == PlayQueue.Status.PLAYING),
                    new Attribute("gain", jukeboxService.getGain()),
                    new Attribute("position", controlsJukebox && !playQueue.isEmpty() ? jukeboxService.getPosition() : 0)));

            if (returnPlaylist) {
                builder.add("jukeboxPlaylist", attrs, false);
                List<MediaFile> result;
                synchronized (playQueue) {
                    result = playQueue.getFiles();
                }
                for (MediaFile mediaFile : result) {
                    AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                    builder.add("entry", attributes, true);
                }
            } else {
                builder.add("jukeboxStatus", attrs, false);
            }

            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void createPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        try {

            Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlistId");
            String name = request.getParameter("name");
            if (playlistId == null && name == null) {
                error(request, response, ErrorCode.MISSING_PARAMETER, "Playlist ID or name must be specified.");
                return;
            }

            Playlist playlist;
            if (playlistId != null) {
                playlist = playlistService.getPlaylist(playlistId);
                if (playlist == null) {
                    error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + playlistId);
                    return;
                }
                if (!playlistService.isWriteAllowed(playlist, username)) {
                    error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + playlistId);
                    return;
                }
            } else {
                playlist = new Playlist();
                playlist.setName(name);
                playlist.setCreated(new Date());
                playlist.setChanged(new Date());
                playlist.setPublic(false);
                playlist.setUsername(username);
                playlistService.createPlaylist(playlist);
            }

            List<MediaFile> songs = new ArrayList<MediaFile>();
            for (int id : ServletRequestUtils.getIntParameters(request, "songId")) {
                MediaFile song = mediaFileService.getMediaFile(id);
                if (song != null) {
                    songs.add(song);
                }
            }
            playlistService.setFilesInPlaylist(playlist.getId(), songs);

            XMLBuilder builder = createXMLBuilder(request, response, true);
            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void updatePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "playlistId");
            Playlist playlist = playlistService.getPlaylist(id);
            if (playlist == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
                return;
            }
            if (!playlistService.isWriteAllowed(playlist, username)) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
                return;
            }

            String name = request.getParameter("name");
            if (name != null) {
                playlist.setName(name);
            }
            String comment = request.getParameter("comment");
            if (comment != null) {
                playlist.setComment(comment);
            }
            Boolean isPublic = ServletRequestUtils.getBooleanParameter(request, "public");
            if (isPublic != null) {
                playlist.setPublic(isPublic);
            }
            playlistService.updatePlaylist(playlist);

            // TODO: Add later
//            for (String usernameToAdd : ServletRequestUtils.getStringParameters(request, "usernameToAdd")) {
//                if (securityService.getUserByName(usernameToAdd) != null) {
//                    playlistService.addPlaylistUser(id, usernameToAdd);
//                }
//            }
//            for (String usernameToRemove : ServletRequestUtils.getStringParameters(request, "usernameToRemove")) {
//                if (securityService.getUserByName(usernameToRemove) != null) {
//                    playlistService.deletePlaylistUser(id, usernameToRemove);
//                }
//            }
            List<MediaFile> songs = playlistService.getFilesInPlaylist(id);
            boolean songsChanged = false;

            SortedSet<Integer> tmp = new TreeSet<Integer>();
            for (int songIndexToRemove : ServletRequestUtils.getIntParameters(request, "songIndexToRemove")) {
                tmp.add(songIndexToRemove);
            }
            List<Integer> songIndexesToRemove = new ArrayList<Integer>(tmp);
            Collections.reverse(songIndexesToRemove);
            for (Integer songIndexToRemove : songIndexesToRemove) {
                songs.remove(songIndexToRemove.intValue());
                songsChanged = true;
            }
            for (int songToAdd : ServletRequestUtils.getIntParameters(request, "songIdToAdd")) {
                MediaFile song = mediaFileService.getMediaFile(songToAdd);
                if (song != null) {
                    songs.add(song);
                    songsChanged = true;
                }
            }
            if (songsChanged) {
                playlistService.setFilesInPlaylist(id, songs);
            }

            XMLBuilder builder = createXMLBuilder(request, response, true);
            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }
    
    public void deletePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            Playlist playlist = playlistService.getPlaylist(id);
            if (playlist == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
                return;
            }
            if (!playlistService.isWriteAllowed(playlist, username)) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
                return;
            }
            playlistService.deletePlaylist(id);

            XMLBuilder builder = createXMLBuilder(request, response, true);
            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getAlbumList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("albumList", false);

        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", 10);
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);
            size = Math.max(0, Math.min(size, 500));
            String type = ServletRequestUtils.getRequiredStringParameter(request, "type");

            List<HomeController.Album> albums;
            if ("highest".equals(type)) {
                albums = homeController.getHighestRated(offset, size);
            } else if ("frequent".equals(type)) {
                albums = homeController.getMostFrequent(offset, size);
            } else if ("recent".equals(type)) {
                albums = homeController.getMostRecent(offset, size);
            } else if ("newest".equals(type)) {
                albums = homeController.getNewest(offset, size);
            } else if ("starred".equals(type)) {
                albums = homeController.getStarred(offset, size, username);
            } else if ("alphabeticalByArtist".equals(type)) {
                albums = homeController.getAlphabetical(offset, size, true);
            } else if ("alphabeticalByName".equals(type)) {
                albums = homeController.getAlphabetical(offset, size, false);
            } else if ("random".equals(type)) {
                albums = homeController.getRandom(size);
            } else {
                throw new Exception("Invalid list type: " + type);
            }

            for (HomeController.Album album : albums) {
                MediaFile mediaFile = mediaFileService.getMediaFile(album.getPath());
                AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                builder.add("album", attributes, true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getAlbumList2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("albumList2", false);

        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", 10);
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);
            size = Math.max(0, Math.min(size, 500));
            String type = ServletRequestUtils.getRequiredStringParameter(request, "type");
            String username = securityService.getCurrentUsername(request);

            List<Album> albums;
            if ("frequent".equals(type)) {
                albums = albumDao.getMostFrequentlyPlayedAlbums(offset, size);
            } else if ("recent".equals(type)) {
                albums = albumDao.getMostRecentlyPlayedAlbums(offset, size);
            } else if ("newest".equals(type)) {
                albums = albumDao.getNewestAlbums(offset, size);
            } else if ("alphabeticalByArtist".equals(type)) {
                albums = albumDao.getAlphabetialAlbums(offset, size, true);
            } else if ("alphabeticalByName".equals(type)) {
                albums = albumDao.getAlphabetialAlbums(offset, size, false);
            } else if ("starred".equals(type)) {
                albums = albumDao.getStarredAlbums(offset, size, securityService.getCurrentUser(request).getUsername());
            } else if ("random".equals(type)) {
                albums = searchService.getRandomAlbumsId3(size);
            } else {
                throw new Exception("Invalid list type: " + type);
            }
            for (Album album : albums) {
                builder.add("album", createAttributesForAlbum(album, username), true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getRandomSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("randomSongs", false);

        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", 10);
            size = Math.max(0, Math.min(size, 500));
            String genre = ServletRequestUtils.getStringParameter(request, "genre");
            Integer fromYear = ServletRequestUtils.getIntParameter(request, "fromYear");
            Integer toYear = ServletRequestUtils.getIntParameter(request, "toYear");
            Integer musicFolderId = ServletRequestUtils.getIntParameter(request, "musicFolderId");
            RandomSearchCriteria criteria = new RandomSearchCriteria(size, genre, fromYear, toYear, musicFolderId);

            for (MediaFile mediaFile : searchService.getRandomSongs(criteria)) {
                AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                builder.add("song", attributes, true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }
    
    public void getVideos(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("videos", false);
        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", Integer.MAX_VALUE);
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);

            for (MediaFile mediaFile : mediaFileDao.getVideos(size, offset)) {
                builder.add("video", createAttributesForMediaFile(player, mediaFile, username), true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getNowPlaying(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("nowPlaying", false);

        for (TransferStatus status : statusService.getAllStreamStatuses()) {

            Player player = status.getPlayer();
            File file = status.getFile();
            if (player != null && player.getUsername() != null && file != null) {

                String username = player.getUsername();
                UserSettings userSettings = settingsService.getUserSettings(username);
                if (!userSettings.isNowPlayingAllowed()) {
                    continue;
                }

                MediaFile mediaFile = mediaFileService.getMediaFile(file);

                long minutesAgo = status.getMillisSinceLastUpdate() / 1000L / 60L;
                if (minutesAgo < 60) {
                    AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                    attributes.add("username", username);
                    attributes.add("playerId", player.getId());
                    attributes.add("playerName", player.getName());
                    attributes.add("minutesAgo", minutesAgo);
                    builder.add("entry", attributes, true);
                }
            }
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    private AttributeSet createAttributesForMediaFile(Player player, MediaFile mediaFile, String username) {
        MediaFile parent = mediaFileService.getParentOf(mediaFile);
        AttributeSet attributes = new AttributeSet();
        attributes.add("id", mediaFile.getId());
        try {
            if (!mediaFileService.isRoot(parent)) {
                attributes.add("parent", parent.getId());
            }
        } catch (SecurityException x) {
            // Ignored.
        }
        attributes.add("title", mediaFile.getName());
        attributes.add("album", mediaFile.getAlbumName());
        attributes.add("artist", mediaFile.getArtist());
        attributes.add("isDir", mediaFile.isDirectory());
        attributes.add("coverArt", findCoverArt(mediaFile, parent));
        attributes.add("created", StringUtil.toISO8601(mediaFile.getCreated()));
        attributes.add("starred", StringUtil.toISO8601(mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username)));
        attributes.add("userRating", ratingService.getRatingForUser(username, mediaFile));
        attributes.add("averageRating", ratingService.getAverageRating(mediaFile));

        if (mediaFile.isFile()) {
            attributes.add("duration", mediaFile.getDurationSeconds());
            attributes.add("bitRate", mediaFile.getBitRate());
            attributes.add("track", mediaFile.getTrackNumber());
            attributes.add("discNumber", mediaFile.getDiscNumber());
            attributes.add("year", mediaFile.getYear());
            attributes.add("genre", mediaFile.getGenre());
            attributes.add("size", mediaFile.getFileSize());
            String suffix = mediaFile.getFormat();
            attributes.add("suffix", suffix);
            attributes.add("contentType", StringUtil.getMimeType(suffix));
            attributes.add("isVideo", mediaFile.isVideo());
            attributes.add("path", getRelativePath(mediaFile));

            if (mediaFile.getArtist() != null && mediaFile.getAlbumName() != null) {
                Album album = albumDao.getAlbum(mediaFile.getAlbumArtist(), mediaFile.getAlbumName());
                if (album != null) {
                    attributes.add("albumId", album.getId());
                }
            }
            if (mediaFile.getArtist() != null) {
                Artist artist = artistDao.getArtist(mediaFile.getArtist());
                if (artist != null) {
                    attributes.add("artistId", artist.getId());
                }
            }
            switch (mediaFile.getMediaType()) {
                case MUSIC:
                    attributes.add("type", "music");
                    break;
                case PODCAST:
                    attributes.add("type", "podcast");
                    break;
                case AUDIOBOOK:
                    attributes.add("type", "audiobook");
                    break;
                default:
                    break;
            }

            if (transcodingService.isTranscodingRequired(mediaFile, player)) {
                String transcodedSuffix = transcodingService.getSuffix(player, mediaFile, null);
                attributes.add("transcodedSuffix", transcodedSuffix);
                attributes.add("transcodedContentType", StringUtil.getMimeType(transcodedSuffix));
            }
        }
        return attributes;
    }

    private Integer findCoverArt(MediaFile mediaFile, MediaFile parent) {
        MediaFile dir = mediaFile.isDirectory() ? mediaFile : parent;
        if (dir != null && dir.getCoverArtPath() != null) {
            return dir.getId();
        }
        return null;
    }

    private String getRelativePath(MediaFile musicFile) {

        String filePath = musicFile.getPath();

        // Convert slashes.
        filePath = filePath.replace('\\', '/');

        String filePathLower = filePath.toLowerCase();

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders(false, true);
        for (MusicFolder musicFolder : musicFolders) {
            String folderPath = musicFolder.getPath().getPath();
            folderPath = folderPath.replace('\\', '/');
            String folderPathLower = folderPath.toLowerCase();

            if (filePathLower.startsWith(folderPathLower)) {
                String relativePath = filePath.substring(folderPath.length());
                return relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            }
        }

        return null;
    }

    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isDownloadRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to download files.");
            return null;
        }

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        long lastModified = downloadController.getLastModified(request);

        if (ifModifiedSince != -1 && lastModified != -1 && lastModified <= ifModifiedSince) {
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
        }

        if (lastModified != -1) {
            response.setDateHeader("Last-Modified", lastModified);
        }

        return downloadController.handleRequest(request, response);
    }

    public ModelAndView stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return null;
        }

        streamController.handleRequest(request, response);
        return null;
    }

    public void scrobble(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        Player player = playerService.getPlayer(request, response);

        if (!settingsService.getUserSettings(player.getUsername()).isLastFmEnabled()) {
            error(request, response, ErrorCode.GENERIC, "Scrobbling is not enabled for " + player.getUsername() + ".");
            return;
        }

        try {
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            MediaFile file = mediaFileService.getMediaFile(id);
            if (file == null) {
                error(request, response, ErrorCode.NOT_FOUND, "File not found: " + id);
                return;
            }
            boolean submission = ServletRequestUtils.getBooleanParameter(request, "submission", true);
            audioScrobblerService.register(file, player.getUsername(), submission);
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
            return;
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void star(HttpServletRequest request, HttpServletResponse response) throws Exception {
        starOrUnstar(request, response, true);
    }

    public void unstar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        starOrUnstar(request, response, false);
    }

    private void starOrUnstar(HttpServletRequest request, HttpServletResponse response, boolean star) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        try {
            String username = securityService.getCurrentUser(request).getUsername();
            for (int id : ServletRequestUtils.getIntParameters(request, "id")) {
                MediaFile mediaFile = mediaFileDao.getMediaFile(id);
                if (mediaFile == null) {
                    error(request, response, ErrorCode.NOT_FOUND, "Media file not found: " + id);
                    return;
                }
                if (star) {
                    mediaFileDao.starMediaFile(id, username);
                } else {
                    mediaFileDao.unstarMediaFile(id, username);
                }
            }
            for (int albumId : ServletRequestUtils.getIntParameters(request, "albumId")) {
                Album album = albumDao.getAlbum(albumId);
                if (album == null) {
                    error(request, response, ErrorCode.NOT_FOUND, "Album not found: " + albumId);
                    return;
                }
                if (star) {
                    albumDao.starAlbum(albumId, username);
                } else {
                    albumDao.unstarAlbum(albumId, username);
                }
            }
            for (int artistId : ServletRequestUtils.getIntParameters(request, "artistId")) {
                Artist artist = artistDao.getArtist(artistId);
                if (artist == null) {
                    error(request, response, ErrorCode.NOT_FOUND, "Artist not found: " + artistId);
                    return;
                }
                if (star) {
                    artistDao.starArtist(artistId, username);
                } else {
                    artistDao.unstarArtist(artistId, username);
                }
            }
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
            return;
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getStarred(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("starred", false);
        for (MediaFile artist : mediaFileDao.getStarredDirectories(0, Integer.MAX_VALUE, username)) {
            builder.add("artist", true,
                    new Attribute("name", artist.getName()),
                    new Attribute("id", artist.getId()));
        }
        for (MediaFile album : mediaFileDao.getStarredAlbums(0, Integer.MAX_VALUE, username)) {
            builder.add("album", createAttributesForMediaFile(player, album, username), true);
        }
        for (MediaFile song : mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username)) {
            builder.add("song", createAttributesForMediaFile(player, song, username), true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getStarred2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("starred2", false);
        for (Artist artist : artistDao.getStarredArtists(0, Integer.MAX_VALUE, username)) {
            builder.add("artist", createAttributesForArtist(artist, username), true);
        }
        for (Album album : albumDao.getStarredAlbums(0, Integer.MAX_VALUE, username)) {
            builder.add("album", createAttributesForAlbum(album, username), true);
        }
        for (MediaFile song : mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username)) {
            builder.add("song", createAttributesForMediaFile(player, song, username), true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("podcasts", false);

        for (PodcastChannel channel : podcastService.getAllChannels()) {
            AttributeSet channelAttrs = new AttributeSet();
            channelAttrs.add("id", channel.getId());
            channelAttrs.add("url", channel.getUrl());
            channelAttrs.add("status", channel.getStatus().toString().toLowerCase());
            channelAttrs.add("title", channel.getTitle());
            channelAttrs.add("description", channel.getDescription());
            channelAttrs.add("errorMessage", channel.getErrorMessage());
            builder.add("channel", channelAttrs, false);

            List<PodcastEpisode> episodes = podcastService.getEpisodes(channel.getId(), false);
            for (PodcastEpisode episode : episodes) {
                AttributeSet episodeAttrs = new AttributeSet();

                String path = episode.getPath();
                if (path != null) {
                    MediaFile mediaFile = mediaFileService.getMediaFile(path);
                    episodeAttrs.addAll(createAttributesForMediaFile(player, mediaFile, username));
                    episodeAttrs.add("streamId", mediaFile.getId());
                }

                episodeAttrs.add("id", episode.getId());  // Overwrites the previous "id" attribute.
                episodeAttrs.add("status", episode.getStatus().toString().toLowerCase());
                episodeAttrs.add("title", episode.getTitle());
                episodeAttrs.add("description", episode.getDescription());
                episodeAttrs.add("publishDate", episode.getPublishDate());

                builder.add("episode", episodeAttrs, true);
            }

            builder.end(); // <channel>
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getShares(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        User user = securityService.getCurrentUser(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        builder.add("shares", false);
        for (Share share : shareService.getSharesForUser(user)) {
            builder.add("share", createAttributesForShare(share), false);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId())) {
                AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                builder.add("entry", attributes, true);
            }

            builder.end();
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        User user = securityService.getCurrentUser(request);
        if (!user.isShareRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to share media.");
            return;
        }

        if (!settingsService.isUrlRedirectionEnabled()) {
            error(request, response, ErrorCode.GENERIC, "Sharing is only supported for *.subsonic.org domain names.");
            return;
        }

        XMLBuilder builder = createXMLBuilder(request, response, true);

        try {

            List<MediaFile> files = new ArrayList<MediaFile>();
            for (int id : ServletRequestUtils.getRequiredIntParameters(request, "id")) {
                files.add(mediaFileService.getMediaFile(id));
            }

            // TODO: Update api.jsp

            Share share = shareService.createShare(request, files);
            share.setDescription(request.getParameter("description"));
            long expires = ServletRequestUtils.getLongParameter(request, "expires", 0L);
            if (expires != 0) {
                share.setExpires(new Date(expires));
            }
            shareService.updateShare(share);

            builder.add("shares", false);
            builder.add("share", createAttributesForShare(share), false);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId())) {
                AttributeSet attributes = createAttributesForMediaFile(player, mediaFile, username);
                builder.add("entry", attributes, true);
            }

            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void deleteShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            request = wrapRequest(request);
            User user = securityService.getCurrentUser(request);
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

            Share share = shareService.getShareById(id);
            if (share == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
                return;
            }
            if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to delete shared media.");
                return;
            }

            shareService.deleteShare(id);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void updateShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            request = wrapRequest(request);
            User user = securityService.getCurrentUser(request);
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

            Share share = shareService.getShareById(id);
            if (share == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
                return;
            }
            if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to modify shared media.");
                return;
            }

            share.setDescription(request.getParameter("description"));
            String expiresString = request.getParameter("expires");
            if (expiresString != null) {
                long expires = Long.parseLong(expiresString);
                share.setExpires(expires == 0L ? null : new Date(expires));
            }
            shareService.updateShare(share);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    private List<Attribute> createAttributesForShare(Share share) {
        List<Attribute> attributes = new ArrayList<Attribute>();

        attributes.add(new Attribute("id", share.getId()));
        attributes.add(new Attribute("url", shareService.getShareUrl(share)));
        attributes.add(new Attribute("username", share.getUsername()));
        attributes.add(new Attribute("created", StringUtil.toISO8601(share.getCreated())));
        attributes.add(new Attribute("visitCount", share.getVisitCount()));
        attributes.add(new Attribute("description", share.getDescription()));
        attributes.add(new Attribute("expires", StringUtil.toISO8601(share.getExpires())));
        attributes.add(new Attribute("lastVisited", StringUtil.toISO8601(share.getLastVisited())));

        return attributes;
    }

    public ModelAndView videoPlayer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        Map<String, Object> map = new HashMap<String, Object>();
        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        MediaFile file = mediaFileService.getMediaFile(id);

        int timeOffset = ServletRequestUtils.getIntParameter(request, "timeOffset", 0);
        timeOffset = Math.max(0, timeOffset);
        Integer duration = file.getDurationSeconds();
        if (duration != null) {
            map.put("skipOffsets", VideoPlayerController.createSkipOffsets(duration));
            timeOffset = Math.min(duration, timeOffset);
            duration -= timeOffset;
        }

        map.put("id", request.getParameter("id"));
        map.put("u", request.getParameter("u"));
        map.put("p", request.getParameter("p"));
        map.put("c", request.getParameter("c"));
        map.put("v", request.getParameter("v"));
        map.put("video", file);
        map.put("maxBitRate", ServletRequestUtils.getIntParameter(request, "maxBitRate", VideoPlayerController.DEFAULT_BIT_RATE));
        map.put("duration", duration);
        map.put("timeOffset", timeOffset);
        map.put("bitRates", VideoPlayerController.BIT_RATES);
        map.put("autoplay", ServletRequestUtils.getBooleanParameter(request, "autoplay", true));

        ModelAndView result = new ModelAndView("rest/videoPlayer");
        result.addObject("model", map);
        return result;
    }

    public ModelAndView getCoverArt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        return coverArtController.handleRequest(request, response);
    }

    public ModelAndView getAvatar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        return avatarController.handleRequest(request, response);
    }

    public void changePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {

            String username = ServletRequestUtils.getRequiredStringParameter(request, "username");
            String password = decrypt(ServletRequestUtils.getRequiredStringParameter(request, "password"));

            User authUser = securityService.getCurrentUser(request);
            if (!authUser.isAdminRole() && !username.equals(authUser.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, authUser.getUsername() + " is not authorized to change password for " + username);
                return;
            }

            User user = securityService.getUserByName(username);
            user.setPassword(password);
            securityService.updateUser(user);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username;
        try {
            username = ServletRequestUtils.getRequiredStringParameter(request, "username");
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
            return;
        }

        User currentUser = securityService.getCurrentUser(request);
        if (!username.equals(currentUser.getUsername()) && !currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        User requestedUser = securityService.getUserByName(username);
        if (requestedUser == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        List<Attribute> attributes = Arrays.asList(
                new Attribute("username", requestedUser.getUsername()),
                new Attribute("email", requestedUser.getEmail()),
                new Attribute("scrobblingEnabled", userSettings.isLastFmEnabled()),
                new Attribute("adminRole", requestedUser.isAdminRole()),
                new Attribute("settingsRole", requestedUser.isSettingsRole()),
                new Attribute("downloadRole", requestedUser.isDownloadRole()),
                new Attribute("uploadRole", requestedUser.isUploadRole()),
                new Attribute("playlistRole", true),  // Since 1.8.0
                new Attribute("coverArtRole", requestedUser.isCoverArtRole()),
                new Attribute("commentRole", requestedUser.isCommentRole()),
                new Attribute("podcastRole", requestedUser.isPodcastRole()),
                new Attribute("streamRole", requestedUser.isStreamRole()),
                new Attribute("jukeboxRole", requestedUser.isJukeboxRole()),
                new Attribute("shareRole", requestedUser.isShareRole())
        );

        builder.add("user", attributes, true);
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void createUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to create new users.");
            return;
        }

        try {
            UserSettingsCommand command = new UserSettingsCommand();
            command.setUsername(ServletRequestUtils.getRequiredStringParameter(request, "username"));
            command.setPassword(decrypt(ServletRequestUtils.getRequiredStringParameter(request, "password")));
            command.setEmail(ServletRequestUtils.getRequiredStringParameter(request, "email"));
            command.setLdapAuthenticated(ServletRequestUtils.getBooleanParameter(request, "ldapAuthenticated", false));
            command.setAdminRole(ServletRequestUtils.getBooleanParameter(request, "adminRole", false));
            command.setCommentRole(ServletRequestUtils.getBooleanParameter(request, "commentRole", false));
            command.setCoverArtRole(ServletRequestUtils.getBooleanParameter(request, "coverArtRole", false));
            command.setDownloadRole(ServletRequestUtils.getBooleanParameter(request, "downloadRole", false));
            command.setStreamRole(ServletRequestUtils.getBooleanParameter(request, "streamRole", true));
            command.setUploadRole(ServletRequestUtils.getBooleanParameter(request, "uploadRole", false));
            command.setJukeboxRole(ServletRequestUtils.getBooleanParameter(request, "jukeboxRole", false));
            command.setPodcastRole(ServletRequestUtils.getBooleanParameter(request, "podcastRole", false));
            command.setSettingsRole(ServletRequestUtils.getBooleanParameter(request, "settingsRole", true));
            command.setTranscodeSchemeName(ServletRequestUtils.getStringParameter(request, "transcodeScheme", TranscodeScheme.OFF.name()));
            command.setShareRole(ServletRequestUtils.getBooleanParameter(request, "shareRole", false));

            userSettingsController.createUser(command);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void deleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to delete users.");
            return;
        }

        try {
            String username = ServletRequestUtils.getRequiredStringParameter(request, "username");
            securityService.deleteUser(username);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getChatMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        long since = ServletRequestUtils.getLongParameter(request, "since", 0L);

        builder.add("chatMessages", false);

        for (ChatService.Message message : chatService.getMessages(0L).getMessages()) {
            long time = message.getDate().getTime();
            if (time > since) {
                builder.add("chatMessage", true, new Attribute("username", message.getUsername()),
                        new Attribute("time", time), new Attribute("message", message.getContent()));
            }
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void addChatMessage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {
            chatService.doAddMessage(ServletRequestUtils.getRequiredStringParameter(request, "message"), request);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        }
    }

    public void getLyrics(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String artist = request.getParameter("artist");
        String title = request.getParameter("title");
        LyricsInfo lyrics = lyricsService.getLyrics(artist, title);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        AttributeSet attributes = new AttributeSet();
        attributes.add("artist", lyrics.getArtist());
        attributes.add("title", lyrics.getTitle());
        builder.add("lyrics", attributes, lyrics.getLyrics(), true);

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void setRating(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {
            Integer rating = ServletRequestUtils.getRequiredIntParameter(request, "rating");
            if (rating == 0) {
                rating = null;
            }

            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (mediaFile == null) {
                error(request, response, ErrorCode.NOT_FOUND, "File not found: " + id);
                return;
            }

            String username = securityService.getCurrentUsername(request);
            ratingService.setRatingForUser(username, mediaFile, rating);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        }
    }

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        return wrapRequest(request, false);
    }

    private HttpServletRequest wrapRequest(final HttpServletRequest request, boolean jukebox) {
        final String playerId = createPlayerIfNecessary(request, jukebox);
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                // Returns the correct player to be used in PlayerService.getPlayer()
                if ("player".equals(name)) {
                    return playerId;
                }

                // Support old style ID parameters.
                if ("id".equals(name)) {
                    return mapId(request.getParameter("id"));
                }

                return super.getParameter(name);
            }
        };
    }

    private String mapId(String id) {
        if (id == null || id.startsWith(CoverArtController.ALBUM_COVERART_PREFIX) ||
                id.startsWith(CoverArtController.ARTIST_COVERART_PREFIX) || StringUtils.isNumeric(id)) {
            return id;
        }

        try {
            String path = StringUtil.utf8HexDecode(id);
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            return String.valueOf(mediaFile.getId());
        } catch (Exception x) {
            return id;
        }
    }

    private String getErrorMessage(Exception x) {
        if (x.getMessage() != null) {
            return x.getMessage();
        }
        return x.getClass().getSimpleName();
    }

    private void error(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message) throws IOException {
        XMLBuilder builder = createXMLBuilder(request, response, false);
        builder.add("error", true,
                new XMLBuilder.Attribute("code", code.getCode()),
                new XMLBuilder.Attribute("message", message));
        builder.end();
        response.getWriter().print(builder);
    }

    private XMLBuilder createXMLBuilder(HttpServletRequest request, HttpServletResponse response, boolean ok) throws IOException {
        String format = ServletRequestUtils.getStringParameter(request, "f", "xml");
        boolean json = "json".equals(format);
        boolean jsonp = "jsonp".equals(format);
        XMLBuilder builder;

        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);

        if (json) {
            builder = XMLBuilder.createJSONBuilder();
            response.setContentType("application/json");
        } else if (jsonp) {
            builder = XMLBuilder.createJSONPBuilder(request.getParameter("callback"));
            response.setContentType("text/javascript");
        } else {
            builder = XMLBuilder.createXMLBuilder();
            response.setContentType("text/xml");
        }

        builder.preamble(StringUtil.ENCODING_UTF8);
        builder.add("subsonic-response", false,
                new Attribute("xmlns", "http://subsonic.org/restapi"),
                new Attribute("status", ok ? "ok" : "failed"),
                new Attribute("version", StringUtil.getRESTProtocolVersion()));
        return builder;
    }

    private String createPlayerIfNecessary(HttpServletRequest request, boolean jukebox) {
        String username = request.getRemoteUser();
        String clientId = request.getParameter("c");
        if (jukebox) {
            clientId += "-jukebox";
        }

        List<Player> players = playerService.getPlayersForUserAndClientId(username, clientId);

        // If not found, create it.
        if (players.isEmpty()) {
            Player player = new Player();
            player.setIpAddress(request.getRemoteAddr());
            player.setUsername(username);
            player.setClientId(clientId);
            player.setName(clientId);
            player.setTechnology(jukebox ? PlayerTechnology.JUKEBOX : PlayerTechnology.EXTERNAL_WITH_PLAYLIST);
            playerService.createPlayer(player);
            players = playerService.getPlayersForUserAndClientId(username, clientId);
        }

        // Return the player ID.
        return !players.isEmpty() ? players.get(0).getId() : null;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setDownloadController(DownloadController downloadController) {
        this.downloadController = downloadController;
    }

    public void setCoverArtController(CoverArtController coverArtController) {
        this.coverArtController = coverArtController;
    }

    public void setUserSettingsController(UserSettingsController userSettingsController) {
        this.userSettingsController = userSettingsController;
    }

    public void setLeftController(LeftController leftController) {
        this.leftController = leftController;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setStreamController(StreamController streamController) {
        this.streamController = streamController;
    }

    public void setChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    public void setLyricsService(LyricsService lyricsService) {
        this.lyricsService = lyricsService;
    }

    public void setPlayQueueService(PlayQueueService playQueueService) {
        this.playQueueService = playQueueService;
    }

    public void setJukeboxService(JukeboxService jukeboxService) {
        this.jukeboxService = jukeboxService;
    }

    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    public void setRatingService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setAvatarController(AvatarController avatarController) {
        this.avatarController = avatarController;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public static enum ErrorCode {

        GENERIC(0, "A generic error."),
        MISSING_PARAMETER(10, "Required parameter is missing."),
        PROTOCOL_MISMATCH_CLIENT_TOO_OLD(20, "Incompatible Subsonic REST protocol version. Client must upgrade."),
        PROTOCOL_MISMATCH_SERVER_TOO_OLD(30, "Incompatible Subsonic REST protocol version. Server must upgrade."),
        NOT_AUTHENTICATED(40, "Wrong username or password."),
        NOT_AUTHORIZED(50, "User is not authorized for the given operation."),
        NOT_LICENSED(60, "The trial period for the Subsonic server is over. Please donate to get a license key. Visit subsonic.org for details."),
        NOT_FOUND(70, "Requested data was not found.");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
