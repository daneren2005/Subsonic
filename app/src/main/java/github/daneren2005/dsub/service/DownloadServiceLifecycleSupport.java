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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import github.daneren2005.dsub.domain.InternetRadioStation;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerQueue;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.CacheCleaner;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.SongDBHandler;
import github.daneren2005.dsub.util.Util;

import static github.daneren2005.dsub.domain.PlayerState.PREPARING;

/**
 * @author Sindre Mehus
 */
public class DownloadServiceLifecycleSupport {
	private static final String TAG = DownloadServiceLifecycleSupport.class.getSimpleName();
	public static final String FILENAME_DOWNLOADS_SER = "downloadstate2.ser";
	private static final int DEBOUNCE_TIME = 200;

	private final DownloadService downloadService;
	private Looper eventLooper;
	private Handler eventHandler;
	private BroadcastReceiver ejectEventReceiver;
	private PhoneStateListener phoneStateListener;
	private boolean externalStorageAvailable= true;
	private ReentrantLock lock = new ReentrantLock();
	private final AtomicBoolean setup = new AtomicBoolean(false);
	private long lastPressTime = 0;
	private SilentBackgroundTask<Void> currentSavePlayQueueTask = null;
	private Date lastChange = null;

	/**
	 * This receiver manages the intent that could come from other applications.
	 */
	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			eventHandler.post(new Runnable() {
				@Override
				public void run() {
					String action = intent.getAction();
					Log.i(TAG, "intentReceiver.onReceive: " + action);
					if (DownloadService.CMD_PLAY.equals(action)) {
						downloadService.play();
					} else if (DownloadService.CMD_NEXT.equals(action)) {
						downloadService.next();
					} else if (DownloadService.CMD_PREVIOUS.equals(action)) {
						downloadService.previous();
					} else if (DownloadService.CMD_TOGGLEPAUSE.equals(action)) {
						downloadService.togglePlayPause();
					} else if (DownloadService.CMD_PAUSE.equals(action)) {
						downloadService.pause();
					} else if (DownloadService.CMD_STOP.equals(action)) {
						downloadService.pause();
						downloadService.seekTo(0);
					}
				}
			});
		}
	};


	public DownloadServiceLifecycleSupport(DownloadService downloadService) {
		this.downloadService = downloadService;
	}

	public void onCreate() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				eventLooper = Looper.myLooper();
				eventHandler = new Handler(eventLooper);

				// Deserialize queue before starting looper
				try {
					lock.lock();
					deserializeDownloadQueueNow();

					// Wait until PREPARING is done to mark lifecycle as ready to receive events
					while(downloadService.getPlayerState() == PREPARING) {
						Util.sleepQuietly(50L);
					}

					setup.set(true);
				} finally {
					lock.unlock();
				}

				Looper.loop();
			}
		}, "DownloadServiceLifecycleSupport").start();

		// Stop when SD card is ejected.
		ejectEventReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				externalStorageAvailable = Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction());
				if (!externalStorageAvailable) {
					Log.i(TAG, "External media is ejecting. Stopping playback.");
					downloadService.reset();
				} else {
					Log.i(TAG, "External media is available.");
				}
			}
		};
		IntentFilter ejectFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
		ejectFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		ejectFilter.addDataScheme("file");
		downloadService.registerReceiver(ejectEventReceiver, ejectFilter);

		// React to media buttons.
		Util.registerMediaButtonEventReceiver(downloadService);

		// Pause temporarily on incoming phone calls.
		phoneStateListener = new MyPhoneStateListener();

		// Android 6.0 removes requirement for android.Manifest.permission.READ_PHONE_STATE;
		TelephonyManager telephonyManager = (TelephonyManager) downloadService.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		// Register the handler for outside intents.
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(DownloadService.CMD_PLAY);
		commandFilter.addAction(DownloadService.CMD_TOGGLEPAUSE);
		commandFilter.addAction(DownloadService.CMD_PAUSE);
		commandFilter.addAction(DownloadService.CMD_STOP);
		commandFilter.addAction(DownloadService.CMD_PREVIOUS);
		commandFilter.addAction(DownloadService.CMD_NEXT);
		commandFilter.addAction(DownloadService.CANCEL_DOWNLOADS);
		downloadService.registerReceiver(intentReceiver, commandFilter);

		new CacheCleaner(downloadService, downloadService).clean();
	}

	public boolean isInitialized() {
		return setup.get();
	}

	public void onStart(final Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();

			if(eventHandler == null) {
				Util.sleepQuietly(100L);
			}
			if(eventHandler == null) {
				return;
			}

			eventHandler.post(new Runnable() {
				@Override
				public void run() {
					if(!setup.get()) {
						lock.lock();
						lock.unlock();
					}

					if(DownloadService.START_PLAY.equals(action)) {
						int offlinePref = intent.getIntExtra(Constants.PREFERENCES_KEY_OFFLINE, 0);
						if(offlinePref != 0) {
							boolean offline = (offlinePref == 2);
							Util.setOffline(downloadService, offline);
							if (offline) {
								downloadService.clearIncomplete();
							} else {
								downloadService.checkDownloads();
							}
						}

						if(intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false)) {
							// Add shuffle parameters
							SharedPreferences.Editor editor = Util.getPreferences(downloadService).edit();
							String startYear = intent.getStringExtra(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR);
							if(startYear != null) {
								editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, startYear);
							}

							String endYear = intent.getStringExtra(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR);
							if(endYear != null) {
								editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, endYear);
							}

							String genre = intent.getStringExtra(Constants.PREFERENCES_KEY_SHUFFLE_GENRE);
							if(genre != null) {
								editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, genre);
							}
							editor.commit();

							downloadService.clear();
							downloadService.setShufflePlayEnabled(true);
						} else {
							downloadService.start();
						}
					} else if(DownloadService.CMD_TOGGLEPAUSE.equals(action)) {
						downloadService.togglePlayPause();
					} else if(DownloadService.CMD_NEXT.equals(action)) {
						downloadService.next();
					} else if(DownloadService.CMD_PREVIOUS.equals(action)) {
						downloadService.previous();
					} else if(DownloadService.CANCEL_DOWNLOADS.equals(action)) {
						downloadService.clearBackground();
					} else if(intent.getExtras() != null) {
						final KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
						if (event != null) {
							handleKeyEvent(event);
						}
					}
				}
			});
		}
	}

	public void onDestroy() {
		serializeDownloadQueue();
		eventLooper.quit();
		downloadService.unregisterReceiver(ejectEventReceiver);
		downloadService.unregisterReceiver(intentReceiver);

		TelephonyManager telephonyManager = (TelephonyManager) downloadService.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	public boolean isExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	public void serializeDownloadQueue() {
		serializeDownloadQueue(true);
	}
	public void serializeDownloadQueue(final boolean serializeRemote) {
		if(!setup.get()) {
			return;
		}

		final List<DownloadFile> songs = new ArrayList<DownloadFile>(downloadService.getSongs());
		eventHandler.post(new Runnable() {
			@Override
			public void run() {
				if(lock.tryLock()) {
					try {
						serializeDownloadQueueNow(songs, serializeRemote);
					} finally {
						lock.unlock();
					}
				}
			}
		});
	}

	public void serializeDownloadQueueNow(List<DownloadFile> songs, boolean serializeRemote) {
		final PlayerQueue state = new PlayerQueue();
		for (DownloadFile downloadFile : songs) {
			state.songs.add(downloadFile.getSong());
		}
		for (DownloadFile downloadFile : downloadService.getToDelete()) {
			state.toDelete.add(downloadFile.getSong());
		}
		state.currentPlayingIndex = downloadService.getCurrentPlayingIndex();
		state.currentPlayingPosition = downloadService.getPlayerPosition();

		DownloadFile currentPlaying = downloadService.getCurrentPlaying();
		if(currentPlaying != null) {
			state.renameCurrent = currentPlaying.isWorkDone() && !currentPlaying.isCompleteFileAvailable();
		}
		state.changed = lastChange = new Date();

		Log.i(TAG, "Serialized currentPlayingIndex: " + state.currentPlayingIndex + ", currentPlayingPosition: " + state.currentPlayingPosition);
		FileUtil.serialize(downloadService, state, FILENAME_DOWNLOADS_SER);

		// If we are on Subsonic 5.2+, save play queue
		if(serializeRemote && ServerInfo.canSavePlayQueue(downloadService) && !Util.isOffline(downloadService) && state.songs.size() > 0 && !(state.songs.get(0) instanceof InternetRadioStation)) {
			// Cancel any currently running tasks
			if(currentSavePlayQueueTask != null) {
				currentSavePlayQueueTask.cancel();
			}

			currentSavePlayQueueTask = new SilentBackgroundTask<Void>(downloadService) {
				@Override
				protected Void doInBackground() throws Throwable {
					try {
						int index = state.currentPlayingIndex;
						int position = state.currentPlayingPosition;
						if(index == -1) {
							index = 0;
							position = 0;
						}

						MusicDirectory.Entry currentPlaying = state.songs.get(index);
						List<MusicDirectory.Entry> songs = new ArrayList<>();

						SongDBHandler dbHandler = SongDBHandler.getHandler(downloadService);
						for(MusicDirectory.Entry song: state.songs) {
							Pair<Integer, String> onlineSongIds = dbHandler.getOnlineSongId(song);
							if(onlineSongIds != null && onlineSongIds.getSecond() != null) {
								song.setId(onlineSongIds.getSecond());
								songs.add(song);
							}
						}

						MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
						musicService.savePlayQueue(songs, currentPlaying, position, downloadService, null);
					} catch (Exception e) {
						Log.e(TAG, "Failed to save playing queue to server", e);
					} finally {
						currentSavePlayQueueTask = null;
					}

					return null;
				}

				@Override
				protected void error(Throwable error) {
					currentSavePlayQueueTask = null;
					super.error(error);
				}
			};
			currentSavePlayQueueTask.execute();
		}
	}

	public void post(Runnable runnable) {
		eventHandler.post(runnable);
	}

	private void deserializeDownloadQueueNow() {
		PlayerQueue state = FileUtil.deserialize(downloadService, FILENAME_DOWNLOADS_SER, PlayerQueue.class);
		if (state == null) {
			return;
		}
		Log.i(TAG, "Deserialized currentPlayingIndex: " + state.currentPlayingIndex + ", currentPlayingPosition: " + state.currentPlayingPosition);

		// Rename first thing before anything else starts
		if(state.renameCurrent && state.currentPlayingIndex != -1 && state.currentPlayingIndex < state.songs.size()) {
			DownloadFile currentPlaying = new DownloadFile(downloadService, state.songs.get(state.currentPlayingIndex), false);
			currentPlaying.renamePartial();
		}

		downloadService.restore(state.songs, state.toDelete, state.currentPlayingIndex, state.currentPlayingPosition);

		if(state != null) {
			lastChange = state.changed;
		}
	}

	public Date getLastChange() {
		return lastChange;
	}

	public void handleKeyEvent(KeyEvent event) {
		if(event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0) {
			switch (event.getKeyCode()) {
				case RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS:
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					downloadService.fastForward();
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_NEXT:
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					downloadService.rewind();
					break;
			}
		} else if(event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
				case RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE:
					downloadService.togglePlayPause();
					break;
				case KeyEvent.KEYCODE_HEADSETHOOK:
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					if(lastPressTime < (System.currentTimeMillis() - 500)) {
						lastPressTime = System.currentTimeMillis();
						downloadService.togglePlayPause();
					} else {
						downloadService.next(false, true);
					}
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS:
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					if(lastPressTime < (System.currentTimeMillis() - DEBOUNCE_TIME)) {
						lastPressTime = System.currentTimeMillis();
						downloadService.previous();
					}
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_NEXT:
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if(lastPressTime < (System.currentTimeMillis() - DEBOUNCE_TIME)) {
						lastPressTime = System.currentTimeMillis();
						downloadService.next();
					}
					break;
				case KeyEvent.KEYCODE_MEDIA_REWIND:
					downloadService.rewind();
					break;
				case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
					downloadService.fastForward();
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_STOP:
				case KeyEvent.KEYCODE_MEDIA_STOP:
					downloadService.stop();
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_PLAY:
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					if(downloadService.getPlayerState() != PlayerState.STARTED) {
						downloadService.start();
					}
					break;
				case RemoteControlClient.FLAG_KEY_MEDIA_PAUSE:
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					downloadService.pause();
				default:
					break;
			}
		}
	}

	/**
	 * Logic taken from packages/apps/Music.  Will pause when an incoming
	 * call rings or if a call (incoming or outgoing) is connected.
	 */
	private class MyPhoneStateListener extends PhoneStateListener {
		private boolean resumeAfterCall;

		@Override
		public void onCallStateChanged(final int state, String incomingNumber) {
			eventHandler.post(new Runnable() {
				@Override
				public void run() {
					switch (state) {
						case TelephonyManager.CALL_STATE_RINGING:
						case TelephonyManager.CALL_STATE_OFFHOOK:
							if (downloadService.getPlayerState() == PlayerState.STARTED) {
								resumeAfterCall = true;
								downloadService.pause(true);
							}
							break;
						case TelephonyManager.CALL_STATE_IDLE:
							if (resumeAfterCall) {
								resumeAfterCall = false;
								if(downloadService.getPlayerState() == PlayerState.PAUSED_TEMP) {
									downloadService.start();
								}
							}
							break;
						default:
							break;
					}
				}
			});
		}
	}
}
