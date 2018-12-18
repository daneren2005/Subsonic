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
	Copyright 2014 (C) Scott Jackson
*/

package github.vrih.xsub.service;

import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.ResultCallback;

import github.vrih.xsub.R;
import github.vrih.xsub.domain.MusicDirectory;
import github.vrih.xsub.domain.PlayerState;
import github.vrih.xsub.domain.RemoteControlState;
import github.vrih.xsub.util.Util;

/**
 * Created by owner on 2/9/14.
 */
public class ChromeCastController extends RemoteController {
	private static final String TAG = ChromeCastController.class.getSimpleName();

	private boolean applicationStarted = false;
	private boolean waitingForReconnect = false;
	private boolean error = false;
	private boolean ignoreNextPaused = false;
	private String sessionId;
	private boolean isStopping = false;
	private Runnable afterUpdateComplete = null;

	private RemoteMediaClient mediaPlayer;
	private double gain = 0.5;
	private CastSession mCastSession;

	public ChromeCastController(DownloadService downloadService) {
		super(downloadService);
	}

	@Override
	public void create(boolean playing, int seconds) {
		downloadService.setPlayerState(PlayerState.PREPARING);

		//ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks(playing, seconds);
//		ConnectionFailedListener connectionFailedListener = new ConnectionFailedListener();
		Cast.Listener castClientListener = new Cast.Listener() {
			@Override
			public void onApplicationStatusChanged() {
			}

			@Override
			public void onVolumeChanged() {
				if (applicationStarted) {
					try {
					    // TODO: Vol
						//gain = Cast.CastApi.getVolume(apiClient);
					} catch (Exception e) {
						Log.w(TAG, "Failed to get volume");
					}
				}
			}

			@Override
			public void onApplicationDisconnected(int errorCode) {
				shutdownInternal();
			}

		};
	}

	@Override
	public void start() {
		Log.w(TAG, "Attempting to start chromecast song");

		if(error) {
			error = false;
			Log.w(TAG, "Attempting to restart song");
			startSong(downloadService.getCurrentPlaying(), true, 0);
			return;
		}

		try {
			mediaPlayer.play();
		} catch(Exception e) {
			Log.e(TAG, "Failed to start");
		}
	}

	@Override
	public void stop() {
		Log.w(TAG, "Attempting to stop chromecast song");
		try {
			mediaPlayer.pause();
		} catch(Exception e) {
			Log.e(TAG, "Failed to pause");
		}
	}


	@Override
	public void shutdown() {
		try {
			if(mediaPlayer != null && !error) {
				mediaPlayer.stop();
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to stop mediaPlayer", e);
		}

		try {
				mediaPlayer = null;
				applicationStarted = false;

		} catch(Exception e) {
			Log.e(TAG, "Failed to shutdown application", e);
		}

		if(proxy != null) {
			proxy.stop();
			proxy = null;
		}
	}

	private void shutdownInternal() {
		// This will call this.shutdown() indirectly
		downloadService.setRemoteEnabled(RemoteControlState.LOCAL, null);
	}

	@Override
	public void updatePlaylist() {
		if(downloadService.getCurrentPlaying() == null) {
			startSong(null, false, 0);
		}
	}

	@Override
	public void changePosition(int seconds) {
		try {
			mediaPlayer.seek(seconds * 1000L);
		} catch(Exception e) {
			Log.e(TAG, "FAiled to seek to " + seconds);
		}
	}

	@Override
	public void changeTrack(int index, DownloadFile song) {
		startSong(song, true, 0);
	}

	public void changeTrack(int index, DownloadFile song, int position) {
		startSong(song, true, position);
	}

	@Override
	public void setVolume(int volume) {
		gain = volume / 10.0;

		try {
			mediaPlayer.setStreamVolume(gain);
		} catch(Exception e) {
			Log.e(TAG, "Failed to the volume");
		}
	}
	@Override
	public void updateVolume(boolean up) {
		double delta = up ? 0.1 : -0.1;
		gain += delta;
		gain = Math.max(gain, 0.0);
		gain = Math.min(gain, 1.0);

		try {
			mediaPlayer.setStreamVolume(gain);
		} catch(Exception e) {
			Log.e(TAG, "Failed to the volume");
		}
	}
	@Override
	public double getVolume() {
	    // TODO: Make sensible
	    return 0;
	}

	@Override
	public int getRemotePosition() {
		if(mediaPlayer != null) {
			return (int) (mediaPlayer.getApproximateStreamPosition() / 1000L);
		} else {
			return 0;
		}
	}

	@Override
	public int getRemoteDuration() {
		if(mediaPlayer != null) {
			return (int) (mediaPlayer.getStreamDuration() / 1000L);
		} else {
			return 0;
		}
	}

	private void startSong(final DownloadFile currentPlaying, final boolean autoStart, final int position) {
		Log.w(TAG, "Starting song");

		if(currentPlaying == null) {
			try {
				if (mediaPlayer != null && !error && !isStopping) {
					isStopping = true;
					mediaPlayer.stop().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
						@Override
						public void onResult(RemoteMediaClient.MediaChannelResult mediaChannelResult) {
							isStopping = false;

							if(afterUpdateComplete != null) {
								afterUpdateComplete.run();
								afterUpdateComplete = null;
							}
						}
					});
				}
			} catch(Exception e) {
				// Just means it didn't need to be stopped
			}
			downloadService.setPlayerState(PlayerState.IDLE);
			return;
		} else if(isStopping) {
			afterUpdateComplete = new Runnable() {
				@Override
				public void run() {
					startSong(currentPlaying, autoStart, position);
				}
			};
			return;
		}

		downloadService.setPlayerState(PlayerState.PREPARING);
		MusicDirectory.Entry song = currentPlaying.getSong();

		try {
			MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
			String url = getStreamUrl(musicService, currentPlaying);

			// Setup song/video information
			MediaMetadata meta = new MediaMetadata(song.isVideo() ? MediaMetadata.MEDIA_TYPE_MOVIE : MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
			meta.putString(MediaMetadata.KEY_TITLE, song.getTitle());
			if(song.getTrack() != null) {
				meta.putInt(MediaMetadata.KEY_TRACK_NUMBER, song.getTrack());
			}
			if(!song.isVideo()) {
				meta.putString(MediaMetadata.KEY_ARTIST, song.getArtist());
				meta.putString(MediaMetadata.KEY_ALBUM_ARTIST, song.getArtist());
				meta.putString(MediaMetadata.KEY_ALBUM_TITLE, song.getAlbum());

/*				if(castDevice.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT)) {
					if (proxy == null || proxy instanceof WebProxy) {
						String coverArt = musicService.getCoverArtUrl(downloadService, song);

						// If proxy is going, it is a web proxy
						if (proxy != null) {
							coverArt = proxy.getPublicAddress(coverArt);
						}

						meta.addImage(new WebImage(Uri.parse(coverArt)));
					} else {
						File coverArtFile = FileUtil.getAlbumArtFile(downloadService, song);
						if (coverArtFile != null && coverArtFile.exists()) {
							String coverArt = proxy.getPublicAddress(coverArtFile.getPath());
							meta.addImage(new WebImage(Uri.parse(coverArt)));
						}
					}
				}*/
			}

			String contentType;
			if(song.isVideo()) {
				contentType = "application/x-mpegURL";
			}
			else if(song.getTranscodedContentType() != null) {
				contentType = song.getTranscodedContentType();
			} else if(song.getContentType() != null) {
				contentType = song.getContentType();
			} else {
				contentType = "audio/mpeg";
			}

			// Load it into a MediaInfo wrapper
			MediaInfo mediaInfo = new MediaInfo.Builder(url)
				.setContentType(contentType)
				.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
				.setMetadata(meta)
				.build();

			if(autoStart) {
				ignoreNextPaused = true;
			}

			ResultCallback callback = new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
				@Override
				public void onResult(RemoteMediaClient.MediaChannelResult result) {
					if (result.getStatus().isSuccess()) {
						// Handled in other handler
					} else if(result.getStatus().getStatusCode() == CastStatusCodes.REPLACED) {
						Log.w(TAG, "Request was replaced: " + currentPlaying.toString());
					} else {
						Log.e(TAG, "Failed to load: " + result.getStatus().toString());
						failedLoad();
					}
				}
			};

			Log.w("CAST", "CC start" + position * 1000L);
            MediaLoadOptions mlo = new MediaLoadOptions.Builder()
                    .setAutoplay(autoStart)
                    .setPlayPosition(position * 1000L)
                    .build();

            mediaPlayer.load(mediaInfo, mlo).setResultCallback(callback);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Problem occurred with media during loading", e);
			failedLoad();
		} catch (Exception e) {
			Log.e(TAG, "Problem opening media during loading", e);
			failedLoad();
		}
	}

	private void failedLoad() {
		Util.toast(downloadService, downloadService.getResources().getString(R.string.download_failed_to_load));
		downloadService.setPlayerState(PlayerState.STOPPED);
		error = true;
	}

    public void setSession(CastSession mCastSession) {
		this.mCastSession = mCastSession;
		mediaPlayer = mCastSession.getRemoteMediaClient();
	}
}


//    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
//		private boolean isPlaying;
//		private int position;
//		private ResultCallback<Cast.ApplicationConnectionResult> resultCallback;
//
//		ConnectionCallbacks(boolean isPlaying, int position) {
//			this.isPlaying = isPlaying;
//			this.position = position;
//
//			resultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
//				@Override
//				public void onResult(Cast.ApplicationConnectionResult result) {
//					Status status = result.getStatus();
////					if (status.isSuccess()) {
////						sessionId = result.getSessionId();
////
////						applicationStarted = true;
//				//		setupChannel();
////					} else {
////						shutdownInternal();
////					}
//				}
//			};
//		}
//
//		@Override
//		public void onConnected(Bundle connectionHint) {
////			if (waitingForReconnect) {
////				Log.i(TAG, "Reconnecting");
//				//reconnectApplication();
////			} else {
////				launchApplication();
////			}
//		}
//
//		@Override
//		public void onConnectionSuspended(int cause) {
////			Log.w(TAG, "Connection suspended");
////			isPlaying = downloadService.getPlayerState() == PlayerState.STARTED;
////			position = getRemotePosition();
////			waitingForReconnect = true;
//		}
//
////		void launchApplication() {
////			try {
////				Cast.CastApi.launchApplication(apiClient, EnvironmentVariables.CAST_APPLICATION_ID, false).setResultCallback(resultCallback);
////			} catch (Exception e) {
////				Log.e(TAG, "Failed to launch application", e);
////			}
////		}
////		void reconnectApplication() {
////			try {
////				Cast.CastApi.joinApplication(apiClient, EnvironmentVariables.CAST_APPLICATION_ID, sessionId).setResultCallback(resultCallback);
////			} catch (Exception e) {
////				Log.e(TAG, "Failed to reconnect application", e);
////			}
////		}
////		void setupChannel() {
////			if(!waitingForReconnect) {
////				mediaPlayer = new RemoteMediaClient();
////				mediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
////					@Override
////					public void onStatusUpdated() {
////						MediaStatus mediaStatus = mediaPlayer.getMediaStatus();
////						if (mediaStatus == null) {
////							return;
////						}
////
////						switch (mediaStatus.getPlayerState()) {
////							case MediaStatus.PLAYER_STATE_PLAYING:
////								if (ignoreNextPaused) {
////									ignoreNextPaused = false;
////								}
////								downloadService.setPlayerState(PlayerState.STARTED);
////								break;
////							case MediaStatus.PLAYER_STATE_PAUSED:
////								if (!ignoreNextPaused) {
////									downloadService.setPlayerState(PlayerState.PAUSED);
////								}
////								break;
////							case MediaStatus.PLAYER_STATE_BUFFERING:
////								downloadService.setPlayerState(PlayerState.PREPARING);
////								break;
////							case MediaStatus.PLAYER_STATE_IDLE:
////								if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
////									if(downloadService.getPlayerState() != PlayerState.PREPARING) {
////										downloadService.onSongCompleted();
////									}
////								} else if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_INTERRUPTED) {
////									if (downloadService.getPlayerState() != PlayerState.PREPARING) {
////										downloadService.setPlayerState(PlayerState.PREPARING);
////									}
////								} else if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_ERROR) {
////									Log.e(TAG, "Idle due to unknown error");
////									downloadService.onSongCompleted();
////								} else {
////									Log.w(TAG, "Idle reason: " + mediaStatus.getIdleReason());
////									downloadService.setPlayerState(PlayerState.IDLE);
////								}
////								break;
////						}
////					}
////				});
////			}
////
////			try {
////				Cast.CastApi.setMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace(), mediaPlayer);
////			} catch (Exception e) {
////				Log.e(TAG, "Exception while creating channel", e);
////			}
////
////			if(!waitingForReconnect) {
////				DownloadFile currentPlaying = downloadService.getCurrentPlaying();
////				startSong(currentPlaying, isPlaying, position);
////			}
////			if(waitingForReconnect) {
////				waitingForReconnect = false;
////			}
////		}
//	}
//
////	private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
////		@Override
////		public void onConnectionFailed(ConnectionResult result) {
////			shutdownInternal();
////		}



