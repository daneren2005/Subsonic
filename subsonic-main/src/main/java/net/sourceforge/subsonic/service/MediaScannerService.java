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

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.ArtistDao;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.Artist;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.lang.ObjectUtils;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
public class MediaScannerService {

    private static final int INDEX_VERSION = 15;
    private static final Logger LOG = Logger.getLogger(MediaScannerService.class);

    private MediaLibraryStatistics statistics;

    private boolean scanning;
    private Timer timer;
    private SettingsService settingsService;
    private SearchService searchService;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private ArtistDao artistDao;
    private AlbumDao albumDao;
    private int scanCount;

    public void init() {
        deleteOldIndexFiles();
        statistics = mediaFileDao.getStatistics();
        schedule();
    }

    /**
     * Schedule background execution of media library scanning.
     */
    public synchronized void schedule() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                scanLibrary();
            }
        };

        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic media scanning disabled.");
            return;
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTime().before(now)) {
            cal.add(Calendar.DATE, 1);
        }

        Date firstTime = cal.getTime();
        long period = daysBetween * 24L * 3600L * 1000L;
        timer.schedule(task, firstTime, period);

        LOG.info("Automatic media library scanning scheduled to run every " + daysBetween + " day(s), starting at " + firstTime);

        // In addition, create index immediately if it doesn't exist on disk.
        if (settingsService.getLastScanned() == null) {
            LOG.info("Media library never scanned. Doing it now.");
            scanLibrary();
        }
    }

    /**
     * Returns whether the media library is currently being scanned.
     */
    public synchronized boolean isScanning() {
        return scanning;
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount;
    }

    /**
     * Scans the media library.
     * The scanning is done asynchronously, i.e., this method returns immediately.
     */
    public synchronized void scanLibrary() {
        if (isScanning()) {
            return;
        }
        scanning = true;

        Thread thread = new Thread("MediaLibraryScanner") {
            @Override
            public void run() {
                doScanLibrary();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void doScanLibrary() {
        LOG.info("Starting to scan media library.");

        try {
            Date lastScanned = new Date();
            Map<String, Integer> albumCount = new HashMap<String, Integer>();
            scanCount = 0;

            searchService.startIndexing();

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                MediaFile root = mediaFileService.getMediaFile(musicFolder.getPath(), false);
                scanFile(root, musicFolder, lastScanned, albumCount);
            }
            mediaFileDao.markNonPresent(lastScanned);
            artistDao.markNonPresent(lastScanned);
            albumDao.markNonPresent(lastScanned);

            // Update statistics
            statistics = mediaFileDao.getStatistics();

            settingsService.setLastScanned(lastScanned);
            settingsService.save(false);
            LOG.info("Scanned media library with " + scanCount + " entries.");

        } catch (Throwable x) {
            LOG.error("Failed to scan media library.", x);
        } finally {
            scanning = false;
            searchService.stopIndexing();
        }
    }

    private void scanFile(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount) {
        scanCount++;
        if (scanCount % 250 == 0) {
            LOG.info("Scanned media library with " + scanCount + " entries.");
        }

        searchService.index(file);

        // Update the root folder if it has changed.
        if (!musicFolder.getPath().getPath().equals(file.getFolder())) {
            file.setFolder(musicFolder.getPath().getPath());
            mediaFileDao.createOrUpdateMediaFile(file);
        }

        if (file.isDirectory()) {
            for (MediaFile child : mediaFileService.getChildrenOf(file, true, false, false, false)) {
                scanFile(child, musicFolder, lastScanned, albumCount);
            }
            for (MediaFile child : mediaFileService.getChildrenOf(file, false, true, false, false)) {
                scanFile(child, musicFolder, lastScanned, albumCount);
            }
        } else {
            updateAlbum(file, lastScanned, albumCount);
            updateArtist(file, lastScanned, albumCount);
        }

        mediaFileDao.markPresent(file.getPath(), lastScanned);
        artistDao.markPresent(file.getArtist(), lastScanned);
    }

    private void updateAlbum(MediaFile file, Date lastScanned, Map<String, Integer> albumCount) {
        if (file.getAlbumName() == null || file.getArtist() == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        Album album = albumDao.getAlbumForFile(file);
        if (album == null) {
            album = new Album();
            album.setPath(file.getParentPath());
            album.setName(file.getAlbumName());
            album.setArtist(file.getArtist());
            album.setCreated(file.getChanged());
        }
        if (album.getCoverArtPath() == null) {
            MediaFile parent = mediaFileService.getParentOf(file);
            if (parent != null) {
                album.setCoverArtPath(parent.getCoverArtPath());
            }
        }
        boolean firstEncounter = !lastScanned.equals(album.getLastScanned());
        if (firstEncounter) {
            album.setDurationSeconds(0);
            album.setSongCount(0);
            Integer n = albumCount.get(file.getArtist());
            albumCount.put(file.getArtist(), n == null ? 1 : n + 1);
        }
        if (file.getDurationSeconds() != null) {
            album.setDurationSeconds(album.getDurationSeconds() + file.getDurationSeconds());
        }
        if (file.isAudio()) {
            album.setSongCount(album.getSongCount() + 1);
        }

        album.setLastScanned(lastScanned);
        album.setPresent(true);
        albumDao.createOrUpdateAlbum(album);
        if (firstEncounter) {
            searchService.index(album);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
             mediaFileDao.createOrUpdateMediaFile(file);
        }
    }

    private void updateArtist(MediaFile file, Date lastScanned, Map<String, Integer> albumCount) {
        if (file.getArtist() == null || !file.isAudio()) {
            return;
        }

        Artist artist = artistDao.getArtist(file.getArtist());
        if (artist == null) {
            artist = new Artist();
            artist.setName(file.getArtist());
        }
        if (artist.getCoverArtPath() == null) {
            MediaFile parent = mediaFileService.getParentOf(file);
            if (parent != null) {
                artist.setCoverArtPath(parent.getCoverArtPath());
            }
        }
        boolean firstEncounter = !lastScanned.equals(artist.getLastScanned());

        Integer n = albumCount.get(artist.getName());
        artist.setAlbumCount(n == null ? 0 : n);

        artist.setLastScanned(lastScanned);
        artist.setPresent(true);
        artistDao.createOrUpdateArtist(artist);

        if (firstEncounter) {
            searchService.index(artist);
        }
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        return statistics;
    }

    /**
     * Deletes old versions of the index file.
     */
    private void deleteOldIndexFiles() {
        for (int i = 2; i < INDEX_VERSION; i++) {
            File file = getIndexFile(i);
            try {
                if (FileUtil.exists(file)) {
                    if (file.delete()) {
                        LOG.info("Deleted old index file: " + file.getPath());
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to delete old index file: " + file.getPath(), x);
            }
        }
    }

    /**
     * Returns the index file for the given index version.
     *
     * @param version The index version.
     * @return The index file for the given index version.
     */
    private File getIndexFile(int version) {
        File home = SettingsService.getSubsonicHome();
        return new File(home, "subsonic" + version + ".index");
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }
}
