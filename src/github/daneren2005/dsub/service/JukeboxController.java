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
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.JukeboxStatus;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JukeboxController extends RemoteController {
	private static final String TAG = JukeboxService.class.getSimpleName();
	private static final long STATUS_UPDATE_INTERVAL_SECONDS = 5L;
	
	private final Handler handler = new Handler();
	private final TaskQueue tasks = new TaskQueue();
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> statusUpdateFuture;
	private final AtomicLong timeOfLastUpdate = new AtomicLong();
	private JukeboxStatus jukeboxStatus;
	private float gain = 0.5f;
    
    public JukeboxController(DownloadServiceImpl downloadService) {
    	this.downloadService = downloadService;
        new Thread() {
            @Override
            public void run() {
                processTasks();
            }
        }.start();
        downloadService.setPlayerState(PlayerState.IDLE);
        updatePlaylist();
    }

	@Override
	public void start() {
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		startStatusUpdate();
		tasks.add(new Start());
	}
	@Override
	public void stop() {
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		stopStatusUpdate();
		tasks.add(new Stop());
	}
	
	@Override
	public void updatePlaylist() {
		tasks.remove(Skip.class);
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		List<String> ids = new ArrayList<String>();
		for (DownloadFile file : downloadService.getDownloads()) {
			ids.add(file.getSong().getId());
		}
		tasks.add(new SetPlaylist(ids));
	}
	@Override
	public void changePosition(int seconds) {
		// TODO: Break down into changePosition/changeTrack
		/*tasks.remove(Skip.class);
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		startStatusUpdate();
		if (jukeboxStatus != null) {
			jukeboxStatus.setPositionSeconds(seconds);
		}
		tasks.add(new Skip(index, seconds));
		downloadService.setPlayerState(PlayerState.STARTED);*/
	}
	@Override
	public void changeTrack(int index, DownloadFile song) {
		tasks.remove(Skip.class);
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		startStatusUpdate();
		tasks.add(new Skip(index, 0));
		downloadService.setPlayerState(PlayerState.STARTED);
	}
	@Override
	public void setVolume(boolean up) {
		float delta = up ? 0.1f : -0.1f;
		gain += delta;
		gain = Math.max(gain, 0.0f);
		gain = Math.min(gain, 1.0f);
		
		getVolumeToast().setVolume(gain);
		tasks.remove(SetGain.class);
		tasks.add(new SetGain(gain));
	}
	
	@Override
	public int getRemotePosition() {
		if (jukeboxStatus == null || jukeboxStatus.getPositionSeconds() == null || timeOfLastUpdate.get() == 0) {
			return 0;
		}
		
		if (jukeboxStatus.isPlaying()) {
			int secondsSinceLastUpdate = (int) ((System.currentTimeMillis() - timeOfLastUpdate.get()) / 1000L);
			return jukeboxStatus.getPositionSeconds() + secondsSinceLastUpdate;
		}
		
		return jukeboxStatus.getPositionSeconds();
	}
	
	private void processTasks() {
		while (true) {
			JukeboxTask task = null;
			try {
				task = tasks.take();
				JukeboxStatus status = task.execute();
				onStatusUpdate(status);
			} catch (Throwable x) {
				onError(task, x);
			}
		}
	}
	
	private synchronized void startStatusUpdate() {
		stopStatusUpdate();
		Runnable updateTask = new Runnable() {
		@Override
			public void run() {
				tasks.remove(GetStatus.class);
				tasks.add(new GetStatus());
			}
		};
		statusUpdateFuture = executorService.scheduleWithFixedDelay(updateTask, STATUS_UPDATE_INTERVAL_SECONDS,
			STATUS_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}
	
	private synchronized void stopStatusUpdate() {
		if (statusUpdateFuture != null) {
			statusUpdateFuture.cancel(false);
			statusUpdateFuture = null;
		}
	}
    
	private void onStatusUpdate(JukeboxStatus jukeboxStatus) {
		timeOfLastUpdate.set(System.currentTimeMillis());
		this.jukeboxStatus = jukeboxStatus;
		
		// Track change?
		Integer index = jukeboxStatus.getCurrentPlayingIndex();
		if (index != null && index != -1 && index != downloadService.getCurrentPlayingIndex()) {
		downloadService.setPlayerState(PlayerState.COMPLETED);
			downloadService.setCurrentPlaying(index, true);
		downloadService.setPlayerState(PlayerState.STARTED);
		}
	}

	private void onError(JukeboxTask task, Throwable x) {
		if (x instanceof ServerTooOldException && !(task instanceof Stop)) {
			disableJukeboxOnError(x, R.string.download_jukebox_server_too_old);
		} else if (x instanceof OfflineException && !(task instanceof Stop)) {
			disableJukeboxOnError(x, R.string.download_jukebox_offline);
		} else if (x instanceof SubsonicRESTException && ((SubsonicRESTException) x).getCode() == 50 && !(task instanceof Stop)) {
			disableJukeboxOnError(x, R.string.download_jukebox_not_authorized);
		} else {
			Log.e(TAG, "Failed to process jukebox task: " + x, x);
		}
	}

	private void disableJukeboxOnError(Throwable x, final int resourceId) {
		Log.w(TAG, x.toString());
		handler.post(new Runnable() {
			@Override
			public void run() {
				Util.toast(downloadService, resourceId, false);
			}
		});
		downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
	}
	
	private MusicService getMusicService() {
		return MusicServiceFactory.getMusicService(downloadService);
	}
	
	private static class TaskQueue {
		private final LinkedBlockingQueue<JukeboxTask> queue = new LinkedBlockingQueue<JukeboxTask>();
		
		void add(JukeboxTask jukeboxTask) {
			queue.add(jukeboxTask);
		}
		
		JukeboxTask take() throws InterruptedException {
			return queue.take();
		}
		
		void remove(Class<? extends JukeboxTask> clazz) {
			try {
				Iterator<JukeboxTask> iterator = queue.iterator();
				while (iterator.hasNext()) {
					JukeboxTask task = iterator.next();
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

	private abstract class JukeboxTask {
		abstract JukeboxStatus execute() throws Exception;
		
		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

	private class GetStatus extends JukeboxTask {
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().getJukeboxStatus(downloadService, null);
		}
	}

	private class SetPlaylist extends JukeboxTask {
		private final List<String> ids;
		
		SetPlaylist(List<String> ids) {
			this.ids = ids;
		}
		
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().updateJukeboxPlaylist(ids, downloadService, null);
		}
	}

	private class Skip extends JukeboxTask {
		private final int index;
		private final int offsetSeconds;
		
		Skip(int index, int offsetSeconds) {
			this.index = index;
			this.offsetSeconds = offsetSeconds;
		}
		
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().skipJukebox(index, offsetSeconds, downloadService, null);
		}
	}

	private class Stop extends JukeboxTask {
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().stopJukebox(downloadService, null);
		}
	}

	private class Start extends JukeboxTask {
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().startJukebox(downloadService, null);
		}
	}

	private class SetGain extends JukeboxTask {
		private final float gain;
		
		private SetGain(float gain) {
			this.gain = gain;
		}
		
		@Override
		JukeboxStatus execute() throws Exception {
			return getMusicService().setJukeboxGain(gain, downloadService, null);
		}
	}
}
