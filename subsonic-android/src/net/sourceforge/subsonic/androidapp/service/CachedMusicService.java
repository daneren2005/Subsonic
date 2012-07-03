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
package net.sourceforge.subsonic.androidapp.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import net.sourceforge.subsonic.androidapp.domain.Indexes;
import net.sourceforge.subsonic.androidapp.domain.JukeboxStatus;
import net.sourceforge.subsonic.androidapp.domain.Lyrics;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.domain.MusicFolder;
import net.sourceforge.subsonic.androidapp.domain.Playlist;
import net.sourceforge.subsonic.androidapp.domain.SearchCritera;
import net.sourceforge.subsonic.androidapp.domain.SearchResult;
import net.sourceforge.subsonic.androidapp.domain.Version;
import net.sourceforge.subsonic.androidapp.util.CancellableTask;
import net.sourceforge.subsonic.androidapp.util.LRUCache;
import net.sourceforge.subsonic.androidapp.util.ProgressListener;
import net.sourceforge.subsonic.androidapp.util.TimeLimitedCache;
import net.sourceforge.subsonic.androidapp.util.Util;

/**
 * @author Sindre Mehus
 */
public class CachedMusicService implements MusicService {

    private static final int MUSIC_DIR_CACHE_SIZE = 20;
    private static final int TTL_MUSIC_DIR = 5 * 60; // Five minutes

    private final MusicService musicService;
    private final LRUCache<String, TimeLimitedCache<MusicDirectory>> cachedMusicDirectories;
    private final TimeLimitedCache<Boolean> cachedLicenseValid = new TimeLimitedCache<Boolean>(120, TimeUnit.SECONDS);
    private final TimeLimitedCache<Indexes> cachedIndexes = new TimeLimitedCache<Indexes>(60 * 60, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<Playlist>> cachedPlaylists = new TimeLimitedCache<List<Playlist>>(60, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<MusicFolder>> cachedMusicFolders = new TimeLimitedCache<List<MusicFolder>>(10 * 3600, TimeUnit.SECONDS);
    private String restUrl;

    public CachedMusicService(MusicService musicService) {
        this.musicService = musicService;
        cachedMusicDirectories = new LRUCache<String, TimeLimitedCache<MusicDirectory>>(MUSIC_DIR_CACHE_SIZE);
    }

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        musicService.ping(context, progressListener);
    }

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        Boolean result = cachedLicenseValid.get();
        if (result == null) {
            result = musicService.isLicenseValid(context, progressListener);
            cachedLicenseValid.set(result, result ? 30L * 60L : 2L * 60L, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedMusicFolders.clear();
        }
        List<MusicFolder> result = cachedMusicFolders.get();
        if (result == null) {
            result = musicService.getMusicFolders(refresh, context, progressListener);
            cachedMusicFolders.set(result);
        }
        return result;
    }

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedIndexes.clear();
            cachedMusicFolders.clear();
            cachedMusicDirectories.clear();
        }
        Indexes result = cachedIndexes.get();
        if (result == null) {
            result = musicService.getIndexes(musicFolderId, refresh, context, progressListener);
            cachedIndexes.set(result);
        }
        return result;
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        TimeLimitedCache<MusicDirectory> cache = refresh ? null : cachedMusicDirectories.get(id);
        MusicDirectory dir = cache == null ? null : cache.get();
        if (dir == null) {
            dir = musicService.getMusicDirectory(id, refresh, context, progressListener);
            cache = new TimeLimitedCache<MusicDirectory>(TTL_MUSIC_DIR, TimeUnit.SECONDS);
            cache.set(dir);
            cachedMusicDirectories.put(id, cache);
        }
        return dir;
    }

    @Override
    public SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception {
        return musicService.search(criteria, context, progressListener);
    }

    @Override
    public MusicDirectory getPlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getPlaylist(id, context, progressListener);
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        List<Playlist> result = refresh ? null : cachedPlaylists.get();
        if (result == null) {
            result = musicService.getPlaylists(refresh, context, progressListener);
            cachedPlaylists.set(result);
        }
        return result;
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
        musicService.createPlaylist(id, name, entries, context, progressListener);
    }

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLyrics(artist, title, context, progressListener);
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        musicService.scrobble(id, submission, context, progressListener);
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getAlbumList(type, size, offset, context, progressListener);
    }

    @Override
    public MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getRandomSongs(size, context, progressListener);
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, boolean saveToFile, ProgressListener progressListener) throws Exception {
        return musicService.getCoverArt(context, entry, size, saveToFile, progressListener);
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception {
        return musicService.getDownloadInputStream(context, song, offset, maxBitrate, task);
    }

    @Override
    public Version getLocalVersion(Context context) throws Exception {
        return musicService.getLocalVersion(context);
    }

    @Override
    public Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLatestVersion(context, progressListener);
    }

    @Override
    public String getVideoUrl(Context context, String id) {
        return musicService.getVideoUrl(context, id);
    }

    @Override
    public JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        return musicService.updateJukeboxPlaylist(ids, context, progressListener);
    }

    @Override
    public JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        return musicService.skipJukebox(index, offsetSeconds, context, progressListener);
    }

    @Override
    public JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.stopJukebox(context, progressListener);
    }

    @Override
    public JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.startJukebox(context, progressListener);
    }

    @Override
    public JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getJukeboxStatus(context, progressListener);
    }

    @Override
    public JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        return musicService.setJukeboxGain(gain, context, progressListener);
    }

    private void checkSettingsChanged(Context context) {
        String newUrl = Util.getRestUrl(context, null);
        if (!Util.equals(newUrl, restUrl)) {
            cachedMusicFolders.clear();
            cachedMusicDirectories.clear();
            cachedLicenseValid.clear();
            cachedIndexes.clear();
            cachedPlaylists.clear();
            restUrl = newUrl;
        }
    }
}
