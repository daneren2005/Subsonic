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

public abstract class RemoteController {
	private static final String TAG = RemoteController.class.getSimpleName();
	protected DownloadService downloadService;
	private VolumeToast volumeToast;

	public abstract void create(boolean playing, int seconds);
	public abstract void start();
	public abstract void stop();
	public abstract void shutdown();
	
	public abstract void updatePlaylist();
	public abstract void changePosition(int seconds);
	public abstract void changeTrack(int index, DownloadFile song);
	public abstract void setVolume(int volume);
	public abstract void updateVolume(boolean up);
	public abstract double getVolume();
	
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
	
	protected VolumeToast getVolumeToast() {
		if(volumeToast == null) {
			volumeToast = new VolumeToast(downloadService);
		}
		return volumeToast;
	}
	
	protected static class VolumeToast extends Toast {
		private final ProgressBar progressBar;
		
		public VolumeToast(Context context) {
			super(context);
			setDuration(Toast.LENGTH_SHORT);
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.jukebox_volume, null);
			progressBar = (ProgressBar) view.findViewById(R.id.jukebox_volume_progress_bar);
			
			setView(view);
			setGravity(Gravity.TOP, 0, 0);
		}
		
		public void setVolume(float volume) {
			progressBar.setProgress(Math.round(100 * volume));
			show();
		}
	}
}
