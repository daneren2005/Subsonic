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

package github.paroj.dsub2000.service;

import android.os.Handler;
import android.util.Log;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.domain.RemoteStatus;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.domain.RemoteControlState;
import github.paroj.dsub2000.domain.RepeatMode;
import github.paroj.dsub2000.service.parser.SubsonicRESTException;
import github.paroj.dsub2000.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JukeboxController extends RemoteController {
	private static final String TAG = JukeboxController.class.getSimpleName();
	private static final long STATUS_UPDATE_INTERVAL_SECONDS = 5L;
	
	private final Handler handler;
	private volatile boolean running = false;
	private volatile boolean wantsPlayback;
	private volatile int pendingIndex = -1;
	private volatile int pendingPosition;
	private Thread worker;
	private final TaskQueue tasks = new TaskQueue();
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> statusUpdateFuture;
	private final AtomicLong timeOfLastUpdate = new AtomicLong();
	private RemoteStatus jukeboxStatus;
	private float gain = 0.5f;
    
    public JukeboxController(DownloadService downloadService, Handler handler) {
		super(downloadService);
		this.handler = handler;
    }

	@Override
	public void create(boolean playing, int seconds) {
		wantsPlayback = playing;
		pendingPosition = seconds;
		pendingIndex = downloadService.getCurrentPlayingIndex();
		running = true;
		worker = new Thread("JukeboxController") {
			@Override
			public void run() {
				processTasks();
			}
		};
		worker.start();
		updatePlaylist();
	}

	@Override
	public void start() {
		wantsPlayback = true;
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		startStatusUpdate();
		if (pendingIndex >= 0) {
			tasks.remove(Skip.class);
			tasks.add(new Skip(pendingIndex, pendingPosition));
		} else {
			tasks.add(new Start());
		}
	}
	@Override
	public void stop() {
		wantsPlayback = false;
		tasks.remove(Skip.class);
		tasks.remove(Stop.class);
		tasks.remove(Start.class);
		
		stopStatusUpdate();
		tasks.add(new Stop());
	}
	@Override
	public void shutdown() {
		running = false;
		stopStatusUpdate();
		executorService.shutdownNow();
		tasks.clear();
		tasks.add(new ShutdownTask());
		if (worker != null) worker.interrupt();
	}

	@Override public boolean isPlayWhenReady() { return wantsPlayback; }
	
	@Override
	public void updatePlaylist() {
		tasks.remove(SetPlaylist.class);
		
		List<String> ids = new ArrayList<String>();
		for (DownloadFile file : downloadService.getSongs()) {
			ids.add(file.getSong().getId());
		}
		tasks.add(new SetPlaylist(ids));
		prepareTrack(downloadService.getCurrentPlayingIndex(), downloadService.getCurrentPlaying(),
				wantsPlayback, getRemotePosition());
	}
	@Override
	public void changePosition(int seconds) {
		prepareTrack(downloadService.getCurrentPlayingIndex(), downloadService.getCurrentPlaying(),
				wantsPlayback, seconds);
	}
	@Override
	public void changeTrack(int index, DownloadFile song) {
		prepareTrack(index, song, true, 0);
	}

	@Override
	public void prepareTrack(int index, DownloadFile song, boolean playing, int seconds) {
		pendingPosition = seconds;
		pendingIndex = index;
		if (playing && song != null && index >= 0) {
			downloadService.setPlayerState(PlayerState.PREPARING);
			start();
		} else {
			// The jukebox skip API starts audio; apply paused seeks only when play is requested.
			stop();
			downloadService.setPlayerState(song == null ? PlayerState.IDLE : PlayerState.PAUSED);
		}
	}
	@Override
	public void setVolume(int volume) {
		gain = volume / 10.0f;

		tasks.remove(SetGain.class);
		tasks.add(new SetGain(gain));
	}
	@Override
	public void updateVolume(boolean up) {
		float delta = up ? 0.1f : -0.1f;
		gain += delta;
		gain = Math.max(gain, 0.0f);
		gain = Math.min(gain, 1.0f);

		tasks.remove(SetGain.class);
		tasks.add(new SetGain(gain));
	}
	@Override
	public double getVolume() {
		return gain;
	}
	
	@Override
	public int getRemotePosition() {
		if (pendingIndex >= 0) return pendingPosition;
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
		while (running) {
			RemoteTask task = null;
			try {
				task = tasks.take();
				RemoteStatus status = task.execute();
				if(status != null && running) {
					onStatusUpdate(status);
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			} catch (Exception x) {
				if (running) onError(task, x);
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
    
	private void onStatusUpdate(RemoteStatus jukeboxStatus) {
		if (!running || downloadService.getRemoteController() != this) return;
		timeOfLastUpdate.set(System.currentTimeMillis());
		this.jukeboxStatus = jukeboxStatus;
		
		// Track change?
		Integer index = jukeboxStatus.getCurrentPlayingIndex();
		if (pendingIndex >= 0) {
			if (wantsPlayback && jukeboxStatus.isPlaying() && index != null && index == pendingIndex) {
				pendingIndex = -1;
			} else {
				return;
			}
		}
		wantsPlayback = jukeboxStatus.isPlaying();
		int currentPlayingIndex = downloadService.getCurrentPlayingIndex();
		if (index != null && index != -1 && index != downloadService.getCurrentPlayingIndex()) {
			downloadService.setPlayerState(PlayerState.COMPLETED);
			downloadService.setCurrentPlaying(index, true);
			if(jukeboxStatus.isPlaying()) {
				downloadService.setPlayerState(PlayerState.STARTED);
			} else if(index == 0 && currentPlayingIndex == downloadService.size() - 1 && downloadService.getRepeatMode() == RepeatMode.ALL) {
				// Jukebox does not support any form of auto repeat
				start();
			}
			PlayerState state = index == null || index < 0 ? PlayerState.IDLE
					: jukeboxStatus.isPlaying() ? PlayerState.STARTED : PlayerState.PAUSED;
			if (downloadService.getPlayerState() != state) downloadService.setPlayerState(state);
		}
	}

	private void onError(RemoteTask task, Throwable x) {
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
				downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
			}
		});
	}
	
	MusicService getMusicService() {
		return MusicServiceFactory.getMusicService(downloadService);
	}

	private class GetStatus extends RemoteTask {
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().getJukeboxStatus(downloadService, null);
		}
	}

	private class SetPlaylist extends RemoteTask {
		private final List<String> ids;
		
		SetPlaylist(List<String> ids) {
			this.ids = ids;
		}
		
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().updateJukeboxPlaylist(ids, downloadService, null);
		}
	}

	private class Skip extends RemoteTask {
		private final int index;
		private final int offsetSeconds;
		
		Skip(int index, int offsetSeconds) {
			this.index = index;
			this.offsetSeconds = offsetSeconds;
		}
		
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().skipJukebox(index, offsetSeconds, downloadService, null);
		}
	}

	private class Stop extends RemoteTask {
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().stopJukebox(downloadService, null);
		}
	}

	private class Start extends RemoteTask {
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().startJukebox(downloadService, null);
		}
	}

	private class SetGain extends RemoteTask {
		private final float gain;
		
		private SetGain(float gain) {
			this.gain = gain;
		}
		
		@Override
		RemoteStatus execute() throws Exception {
			return getMusicService().setJukeboxGain(gain, downloadService, null);
		}
	}

	private class ShutdownTask extends RemoteTask {
		@Override
		RemoteStatus execute() throws Exception {
			return null;
		}
	}
}
