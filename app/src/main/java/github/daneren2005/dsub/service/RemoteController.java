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

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.RemoteStatus;
import github.daneren2005.serverproxy.WebProxy;

public abstract class RemoteController {
	private static final String TAG = RemoteController.class.getSimpleName();
	protected DownloadService downloadService;
	protected boolean nextSupported = false;

	public abstract void create(boolean playing, int seconds);
	public abstract void start();
	public abstract void stop();
	public abstract void shutdown();
	
	public abstract void updatePlaylist();
	public abstract void changePosition(int seconds);
	public abstract void changeTrack(int index, DownloadFile song);
	// Really is abstract, just don't want to require RemoteController's support it
	public void changeNextTrack(DownloadFile song) {

	};
	public boolean isNextSupported() {
		return this.nextSupported;
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
			return new WebProxy(downloadService, ((CachedMusicService)musicService).getMusicService().getHttpClient());
		} else {
			return new WebProxy(downloadService);
		}
	}
}
