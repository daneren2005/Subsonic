/*
	This file is part of Subsonic.
	
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	
	Copyright 2009 (C) Sindre Mehus
*/

package github.daneren2005.dsub.service;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.RemoteStatus;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.serverproxy.FileProxy;
import github.daneren2005.serverproxy.ServerProxy;
import github.daneren2005.serverproxy.WebProxy;

public abstract class RemoteController {
	private static final String TAG = RemoteController.class.getSimpleName();
	protected DownloadService downloadService;
	protected boolean nextSupported = false;
	protected ServerProxy proxy;
	protected String rootLocation = "";

	public RemoteController(DownloadService downloadService) {
		this.downloadService = downloadService;
		SharedPreferences prefs = Util.getPreferences(downloadService);
		rootLocation = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
	}

	public abstract void create(boolean playing, int seconds);
	public abstract void start();
	public abstract void stop();
	public abstract void shutdown();
	
	public abstract void updatePlaylist();
	public abstract void changePosition(int seconds);
	public abstract void changeTrack(int index, DownloadFile song);
	// Really is abstract, just don't want to require RemoteController's support it
	public void changeNextTrack(DownloadFile song) {}
	public boolean isNextSupported() {
		if(Util.getPreferences(downloadService).getBoolean(Constants.PREFERENCES_KEY_CAST_GAPLESS_PLAYBACK, true)) {
			return this.nextSupported;
		} else {
			return false;
		}
	}
	public abstract void setVolume(int volume);
	public abstract void updateVolume(boolean up);
	public abstract double getVolume();
	public boolean isSeekable() {
		return true;
	}
	
	public abstract int getRemotePosition();
	public int getRemoteDuration() {
		return 0;
	}

	protected abstract class RemoteTask {
		abstract RemoteStatus execute() throws Exception;

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

	protected static class TaskQueue {
		private final LinkedBlockingQueue<RemoteTask> queue = new LinkedBlockingQueue<RemoteTask>();

		void add(RemoteTask jukeboxTask) {
			queue.add(jukeboxTask);
		}

		RemoteTask take() throws InterruptedException {
			return queue.take();
		}

		void remove(Class<? extends RemoteTask> clazz) {
			try {
				Iterator<RemoteTask> iterator = queue.iterator();
				while (iterator.hasNext()) {
					RemoteTask task = iterator.next();
					if (clazz.equals(task.getClass())) {
						iterator.remove();
					}
				}
			} catch (Throwable x) {
				Log.w(TAG, "Failed to clean-up task queue.", x);
			}
		}

		void clear() {
			queue.clear();
		}
	}

	protected WebProxy createWebProxy() {
		MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
		if(musicService instanceof CachedMusicService) {
			RESTMusicService restMusicService = ((CachedMusicService)musicService).getMusicService();
			return new WebProxy(downloadService, restMusicService.getSSLSocketFactory(), restMusicService.getHostNameVerifier());
		} else {
			return new WebProxy(downloadService);
		}
	}

	protected String getStreamUrl(MusicService musicService, DownloadFile downloadFile) throws Exception {
		MusicDirectory.Entry song = downloadFile.getSong();

		String url;
		// In offline mode or playing offline song
		if(downloadFile.isStream()) {
			url = downloadFile.getStream();
		} else if(Util.isOffline(downloadService) || song.getId().indexOf(rootLocation) != -1) {
			if(proxy == null) {
				proxy = new FileProxy(downloadService);
				proxy.start();
			}

			// Offline song
			if(song.getId().indexOf(rootLocation) != -1) {
				url = proxy.getPublicAddress(song.getId());
			} else {
				// Playing online song in offline mode
				url = proxy.getPublicAddress(downloadFile.getCompleteFile().getPath());
			}
		} else {
			// Check if we want a proxy going still
			if(Util.isCastProxy(downloadService)) {
				if(proxy instanceof FileProxy) {
					proxy.stop();
					proxy = null;
				}

				if(proxy == null) {
					proxy = createWebProxy();
					proxy.start();
				}
			} else if(proxy != null) {
				proxy.stop();
				proxy = null;
			}

			if(song.isVideo()) {
				url = musicService.getHlsUrl(song.getId(), downloadFile.getBitRate(), downloadService);
			} else {
				url = musicService.getMusicUrl(downloadService, song, downloadFile.getBitRate());
			}

			// If proxy is going, it is a WebProxy
			if(proxy != null) {
				url = proxy.getPublicAddress(url);
			}
		}

		return url;
	}
}
