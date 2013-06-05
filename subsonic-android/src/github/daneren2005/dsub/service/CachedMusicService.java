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
package github.daneren2005.dsub.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import github.daneren2005.dsub.domain.ChatMessage;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.JukeboxStatus;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.domain.Version;
import github.daneren2005.dsub.util.CancellableTask;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LRUCache;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.TimeLimitedCache;
import github.daneren2005.dsub.util.Util;

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
    private final TimeLimitedCache<List<Playlist>> cachedPlaylists = new TimeLimitedCache<List<Playlist>>(3600, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<MusicFolder>> cachedMusicFolders = new TimeLimitedCache<List<MusicFolder>>(10 * 3600, TimeUnit.SECONDS);
	private final TimeLimitedCache<List<Genre>> cachedGenres = new TimeLimitedCache<List<Genre>>(10 * 3600, TimeUnit.SECONDS);
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
    public MusicDirectory getMusicDirectory(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        TimeLimitedCache<MusicDirectory> cache = refresh ? null : cachedMusicDirectories.get(id);
        MusicDirectory dir = cache == null ? null : cache.get();
        if (dir == null) {
            dir = musicService.getMusicDirectory(id, name, refresh, context, progressListener);
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
    public MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getPlaylist(id, name, context, progressListener);
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
		cachedPlaylists.clear();
        musicService.createPlaylist(id, name, entries, context, progressListener);
    }
	
	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
		musicService.deletePlaylist(id, context, progressListener);
	}
	
	@Override
	public void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		musicService.addToPlaylist(id, toAdd, context, progressListener);
	}
	
	@Override
	public void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		musicService.removeFromPlaylist(id, toRemove, context, progressListener);
	}
	
	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		musicService.updatePlaylist(id, name, comment, pub, context, progressListener);
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
    public MusicDirectory getStarredList(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getStarredList(context, progressListener);
    }

    @Override
    public MusicDirectory getRandomSongs(int size, String folder, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getRandomSongs(size, folder, genre, startYear, endYear, context, progressListener);
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception {
        return musicService.getCoverArt(context, entry, size, saveSize, progressListener);
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
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        return musicService.getVideoUrl(maxBitrate, context, id);
    }
	
	@Override
    public String getVideoStreamUrl(int maxBitrate, Context context, String id) {
        return musicService.getVideoStreamUrl(maxBitrate, context, id);
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
    
	@Override
	public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
		musicService.setStarred(id, starred, context, progressListener);
	}
	
	@Override
	public List<Share> getShares(Context context, ProgressListener progressListener) throws Exception {
		return musicService.getShares(context, progressListener);	
	}

	@Override
	public List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getChatMessages(since, context, progressListener);
	}

	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
		musicService.addChatMessage(message, context, progressListener);
	}
	
	@Override
	public List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkSettingsChanged(context);
		List<Genre> result = refresh ? null : cachedGenres.get();

		if (result == null) {
			result = musicService.getGenres(refresh, context, progressListener);
			cachedGenres.set(result);
		}

		return result;
	}

	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getSongsByGenre(genre, count, offset, context, progressListener);
	}
	
	@Override
  public boolean hasOfflineScrobbles(){
    return musicService.hasOfflineScrobbles();
  }
	
	@Override
  public void processOfflineScrobbles(final Context context, final ProgressListener progressListener) throws Exception{
    musicService.processOfflineScrobbles(context, progressListener);
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
