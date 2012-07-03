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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.subsonic.service.PlaylistService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.support.RequestContextUtils;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.InternetRadio;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.MusicIndex;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.MediaScannerService;
import net.sourceforge.subsonic.service.MusicIndexService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Controller for the left index frame.
 *
 * @author Sindre Mehus
 */
public class LeftController extends ParameterizableViewController implements LastModified {

    private static final Logger LOG = Logger.getLogger(LeftController.class);

    // Update this time if you want to force a refresh in clients.
    private static final Calendar LAST_COMPATIBILITY_TIME = Calendar.getInstance();
    static {
        LAST_COMPATIBILITY_TIME.set(2012, Calendar.MARCH, 6, 0, 0, 0);
        LAST_COMPATIBILITY_TIME.set(Calendar.MILLISECOND, 0);
    }

    private MediaScannerService mediaScannerService;
    private SettingsService settingsService;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private MusicIndexService musicIndexService;
    private PlayerService playerService;
    private PlaylistService playlistService;

    public long getLastModified(HttpServletRequest request) {
        saveSelectedMusicFolder(request);

        if (mediaScannerService.isScanning()) {
            return -1L;
        }

        long lastModified = LAST_COMPATIBILITY_TIME.getTimeInMillis();
        String username = securityService.getCurrentUsername(request);

        // When was settings last changed?
        lastModified = Math.max(lastModified, settingsService.getSettingsChanged());

        // When was music folder(s) on disk last changed?
        List<MusicFolder> allMusicFolders = settingsService.getAllMusicFolders();
        MusicFolder selectedMusicFolder = getSelectedMusicFolder(request);
        if (selectedMusicFolder != null) {
            File file = selectedMusicFolder.getPath();
            lastModified = Math.max(lastModified, FileUtil.lastModified(file));
        } else {
            for (MusicFolder musicFolder : allMusicFolders) {
                File file = musicFolder.getPath();
                lastModified = Math.max(lastModified, FileUtil.lastModified(file));
            }
        }

        // When was music folder table last changed?
        for (MusicFolder musicFolder : allMusicFolders) {
            lastModified = Math.max(lastModified, musicFolder.getChanged().getTime());
        }

        // When was internet radio table last changed?
        for (InternetRadio internetRadio : settingsService.getAllInternetRadios()) {
            lastModified = Math.max(lastModified, internetRadio.getChanged().getTime());
        }

        // When was user settings last changed?
        UserSettings userSettings = settingsService.getUserSettings(username);
        lastModified = Math.max(lastModified, userSettings.getChanged().getTime());

        return lastModified;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        saveSelectedMusicFolder(request);
        Map<String, Object> map = new HashMap<String, Object>();

        MediaLibraryStatistics statistics = mediaScannerService.getStatistics();
        Locale locale = RequestContextUtils.getLocale(request);

        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> allMusicFolders = settingsService.getAllMusicFolders();
        MusicFolder selectedMusicFolder = getSelectedMusicFolder(request);
        List<MusicFolder> musicFoldersToUse = selectedMusicFolder == null ? allMusicFolders : Arrays.asList(selectedMusicFolder);
        String[] shortcuts = settingsService.getShortcutsAsArray();
        UserSettings userSettings = settingsService.getUserSettings(username);

        MusicFolderContent musicFolderContent = getMusicFolderContent(musicFoldersToUse);

        map.put("player", playerService.getPlayer(request, response));
        map.put("scanning", mediaScannerService.isScanning());
        map.put("musicFolders", allMusicFolders);
        map.put("selectedMusicFolder", selectedMusicFolder);
        map.put("radios", settingsService.getAllInternetRadios());
        map.put("shortcuts", getShortcuts(musicFoldersToUse, shortcuts));
        map.put("captionCutoff", userSettings.getMainVisibility().getCaptionCutoff());
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("organizeByFolderStructure", settingsService.isOrganizeByFolderStructure());

        if (statistics != null) {
            map.put("statistics", statistics);
            long bytes = statistics.getTotalLengthInBytes();
            long hours = statistics.getTotalDurationInSeconds() / 3600L;
            map.put("hours", hours);
            map.put("bytes", StringUtil.formatBytes(bytes, locale));
        }

        map.put("indexedArtists", musicFolderContent.getIndexedArtists());
        map.put("singleSongs", musicFolderContent.getSingleSongs());
        map.put("indexes", musicFolderContent.getIndexedArtists().keySet());
        map.put("user", securityService.getCurrentUser(request));

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private void saveSelectedMusicFolder(HttpServletRequest request) {
        if (request.getParameter("musicFolderId") == null) {
            return;
        }
        int musicFolderId = Integer.parseInt(request.getParameter("musicFolderId"));

        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the left frame.
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);
    }

    /**
     * Returns the selected music folder, or <code>null</code> if all music folders should be displayed.
     */
    private MusicFolder getSelectedMusicFolder(HttpServletRequest request) {
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        int musicFolderId = settings.getSelectedMusicFolderId();

        return settingsService.getMusicFolderById(musicFolderId);
    }

    protected List<MediaFile> getSingleSongs(List<MusicFolder> folders) throws IOException {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MusicFolder folder : folders) {
            MediaFile parent = mediaFileService.getMediaFile(folder.getPath(), true);
            result.addAll(mediaFileService.getChildrenOf(parent, true, false, true, true));
        }
        return result;
    }

    public List<MediaFile> getShortcuts(List<MusicFolder> musicFoldersToUse, String[] shortcuts) {
        List<MediaFile> result = new ArrayList<MediaFile>();

        for (String shortcut : shortcuts) {
            for (MusicFolder musicFolder : musicFoldersToUse) {
                File file = new File(musicFolder.getPath(), shortcut);
                if (FileUtil.exists(file)) {
                    result.add(mediaFileService.getMediaFile(file, true));
                }
            }
        }

        return result;
    }

    public MusicFolderContent getMusicFolderContent(List<MusicFolder> musicFoldersToUse) throws Exception {
        SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists = musicIndexService.getIndexedArtists(musicFoldersToUse);
        List<MediaFile> singleSongs = getSingleSongs(musicFoldersToUse);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    public void setMediaScannerService(MediaScannerService mediaScannerService) {
        this.mediaScannerService = mediaScannerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMusicIndexService(MusicIndexService musicIndexService) {
        this.musicIndexService = musicIndexService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public static class MusicFolderContent {

        private final SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists;
        private final List<MediaFile> singleSongs;

        public MusicFolderContent(SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists, List<MediaFile> singleSongs) {
            this.indexedArtists = indexedArtists;
            this.singleSongs = singleSongs;
        }

        public SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> getIndexedArtists() {
            return indexedArtists;
        }

        public List<MediaFile> getSingleSongs() {
            return singleSongs;
        }

    }
}
