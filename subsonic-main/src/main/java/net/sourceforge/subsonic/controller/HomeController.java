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
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.MediaScannerService;
import net.sourceforge.subsonic.service.RatingService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the home page.
 *
 * @author Sindre Mehus
 */
public class HomeController extends ParameterizableViewController {

    private static final Logger LOG = Logger.getLogger(HomeController.class);

    private static final int DEFAULT_LIST_SIZE = 10;
    private static final int MAX_LIST_SIZE = 500;
    private static final int DEFAULT_LIST_OFFSET = 0;
    private static final int MAX_LIST_OFFSET = 5000;

    private SettingsService settingsService;
    private MediaScannerService mediaScannerService;
    private RatingService ratingService;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private SearchService searchService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        if (user.isAdminRole() && settingsService.isGettingStartedEnabled()) {
            return new ModelAndView(new RedirectView("gettingStarted.view"));
        }

        int listSize = DEFAULT_LIST_SIZE;
        int listOffset = DEFAULT_LIST_OFFSET;
        if (request.getParameter("listSize") != null) {
            listSize = Math.max(0, Math.min(Integer.parseInt(request.getParameter("listSize")), MAX_LIST_SIZE));
        }
        if (request.getParameter("listOffset") != null) {
            listOffset = Math.max(0, Math.min(Integer.parseInt(request.getParameter("listOffset")), MAX_LIST_OFFSET));
        }

        String listType = request.getParameter("listType");
        if (listType == null) {
            listType = "random";
        }

        List<Album> albums;
        if ("highest".equals(listType)) {
            albums = getHighestRated(listOffset, listSize);
        } else if ("frequent".equals(listType)) {
            albums = getMostFrequent(listOffset, listSize);
        } else if ("recent".equals(listType)) {
            albums = getMostRecent(listOffset, listSize);
        } else if ("newest".equals(listType)) {
            albums = getNewest(listOffset, listSize);
        } else if ("starred".equals(listType)) {
            albums = getStarred(listOffset, listSize, user.getUsername());
        } else if ("random".equals(listType)) {
            albums = getRandom(listSize);
        } else if ("alphabetical".equals(listType)) {
            albums = getAlphabetical(listOffset, listSize, true);
        } else {
            albums = Collections.emptyList();
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("albums", albums);
        map.put("welcomeTitle", settingsService.getWelcomeTitle());
        map.put("welcomeSubtitle", settingsService.getWelcomeSubtitle());
        map.put("welcomeMessage", settingsService.getWelcomeMessage());
        map.put("isIndexBeingCreated", mediaScannerService.isScanning());
        map.put("listType", listType);
        map.put("listSize", listSize);
        map.put("listOffset", listOffset);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    List<Album> getHighestRated(int offset, int count) {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile mediaFile : ratingService.getHighestRated(offset, count)) {
            Album album = createAlbum(mediaFile);
            if (album != null) {
                album.setRating((int) Math.round(ratingService.getAverageRating(mediaFile) * 10.0D));
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getMostFrequent(int offset, int count) {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile mediaFile : mediaFileService.getMostFrequentlyPlayedAlbums(offset, count)) {
            Album album = createAlbum(mediaFile);
            if (album != null) {
                album.setPlayCount(mediaFile.getPlayCount());
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getMostRecent(int offset, int count) {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile mediaFile : mediaFileService.getMostRecentlyPlayedAlbums(offset, count)) {
            Album album = createAlbum(mediaFile);
            if (album != null) {
                album.setLastPlayed(mediaFile.getLastPlayed());
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getNewest(int offset, int count) throws IOException {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile file : mediaFileService.getNewestAlbums(offset, count)) {
            Album album = createAlbum(file);
            if (album != null) {
                Date created = file.getCreated();
                if (created == null) {
                    created = file.getChanged();
                }
                album.setCreated(created);
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getStarred(int offset, int count, String username) throws IOException {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile file : mediaFileService.getStarredAlbums(offset, count, username)) {
            Album album = createAlbum(file);
            if (album != null) {
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getRandom(int count) throws IOException {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile file : searchService.getRandomAlbums(count)) {
            Album album = createAlbum(file);
            if (album != null) {
                result.add(album);
            }
        }
        return result;
    }

    List<Album> getAlphabetical(int offset, int count, boolean byArtist) throws IOException {
        List<Album> result = new ArrayList<Album>();
        for (MediaFile file : mediaFileService.getAlphabetialAlbums(offset, count, byArtist)) {
            Album album = createAlbum(file);
            if (album != null) {
                result.add(album);
            }
        }
        return result;
    }

    private Album createAlbum(MediaFile file) {
        Album album = new Album();
        album.setId(file.getId());
        album.setPath(file.getPath());
        try {
            resolveArtistAndAlbumTitle(album, file);
            resolveCoverArt(album, file);
        } catch (Exception x) {
            LOG.warn("Failed to create albumTitle list entry for " + file.getPath(), x);
            return null;
        }
        return album;
    }

    private void resolveArtistAndAlbumTitle(Album album, MediaFile file) throws IOException {
        album.setArtist(file.getArtist());
        album.setAlbumTitle(file.getAlbumName());
    }

    private void resolveCoverArt(Album album, MediaFile file) {
        album.setCoverArtPath(file.getCoverArtPath());
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaScannerService(MediaScannerService mediaScannerService) {
        this.mediaScannerService = mediaScannerService;
    }

    public void setRatingService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Contains info for a single album.
     */
    @Deprecated
    public static class Album {
        private String path;
        private String coverArtPath;
        private String artist;
        private String albumTitle;
        private Date created;
        private Date lastPlayed;
        private Integer playCount;
        private Integer rating;
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCoverArtPath() {
            return coverArtPath;
        }

        public void setCoverArtPath(String coverArtPath) {
            this.coverArtPath = coverArtPath;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbumTitle() {
            return albumTitle;
        }

        public void setAlbumTitle(String albumTitle) {
            this.albumTitle = albumTitle;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public Date getLastPlayed() {
            return lastPlayed;
        }

        public void setLastPlayed(Date lastPlayed) {
            this.lastPlayed = lastPlayed;
        }

        public Integer getPlayCount() {
            return playCount;
        }

        public void setPlayCount(Integer playCount) {
            this.playCount = playCount;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }
    }
}
