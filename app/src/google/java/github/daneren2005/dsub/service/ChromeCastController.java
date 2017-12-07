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

package github.daneren2005.dsub.service;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

import java.io.File;
import java.io.IOException;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.EnvironmentVariables;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.serverproxy.FileProxy;
import github.daneren2005.serverproxy.ServerProxy;
import github.daneren2005.serverproxy.WebProxy;

/**
 * Created by owner on 2/9/14.
 */
public class ChromeCastController extends RemoteController {
	private static final String TAG = ChromeCastController.class.getSimpleName();

	private CastDevice castDevice;
	private GoogleApiClient apiClient;

	private boolean applicationStarted = false;
	private boolean waitingForReconnect = false;
	private boolean error = false;
	private boolean ignoreNextPaused = false;
	private String sessionId;
	private boolean isStopping = false;
	private Runnable afterUpdateComplete = null;

	private RemoteMediaPlayer mediaPlayer;
	private double gain = 0.5;

	public ChromeCastController(DownloadService downloadService, CastDevice castDevice) {
		super(downloadService);
		this.castDevice = castDevice;
	}

	@Override
	public void create(boolean playing, int seconds) {
		downloadService.setPlayerState(PlayerState.PREPARING);

		ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks(playing, seconds);
		ConnectionFailedListener connectionFailedListener = new ConnectionFailedListener();
		Cast.Listener castClientListener = new Cast.Listener() {
			@Override
			public void onApplicationStatusChanged() {
				if (apiClient != null && apiClient.isConnected()) {
					Log.i(TAG, "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(apiClient));
				}
			}

			@Override
			public void onVolumeChanged() {
				if (apiClient != null && applicationStarted) {
					try {
						gain = Cast.CastApi.getVolume(apiClient);
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

		Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(castDevice, castClientListener).setVerboseLoggingEnabled(true);
		apiClient = new GoogleApiClient.Builder(downloadService).useDefaultAccount()
				.addApi(Cast.API, apiOptionsBuilder.build())
				.addConnectionCallbacks(connectionCallbacks)
				.addOnConnectionFailedListener(connectionFailedListener)
				.build();

		apiClient.connect();
	}

	@Override
	public void start() {
		if(error) {
			error = false;
			Log.w(TAG, "Attempting to restart song");
			startSong(downloadService.getCurrentPlaying(), true, 0);
			return;
		}

		try {
			mediaPlayer.play(apiClient);
		} catch(Exception e) {
			Log.e(TAG, "Failed to start");
		}
	}

	@Override
	public void stop() {
		try {
			mediaPlayer.pause(apiClient);
		} catch(Exception e) {
			Log.e(TAG, "Failed to pause");
		}
	}

	@Override
	public void shutdown() {
		try {
			if(mediaPlayer != null && !error) {
				mediaPlayer.stop(apiClient);
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to stop mediaPlayer", e);
		}

		try {
			if(apiClient != null) {
				Cast.CastApi.stopApplication(apiClient);
				Cast.CastApi.removeMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace());
				mediaPlayer = null;
				applicationStarted = false;
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to shutdown application", e);
		}

		if(apiClient != null && apiClient.isConnected()) {
			apiClient.disconnect();
		}
		apiClient = null;

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
			mediaPlayer.seek(apiClient, seconds * 1000L);
		} catch(Exception e) {
			Log.e(TAG, "FAiled to seek to " + seconds);
		}
	}

	@Override
	public void changeTrack(int index, DownloadFile song) {
		startSong(song, true, 0);
	}

	@Override
	public void setVolume(int volume) {
		gain = volume / 10.0;

		try {
			Cast.CastApi.setVolume(apiClient, gain);
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
			Cast.CastApi.setVolume(apiClient, gain);
		} catch(Exception e) {
			Log.e(TAG, "Failed to the volume");
		}
	}
	@Override
	public double getVolume() {
		return Cast.CastApi.getVolume(apiClient);
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

	void startSong(final DownloadFile currentPlaying, final boolean autoStart, final int position) {
		if(currentPlaying == null) {
			try {
				if (mediaPlayer != null && !error && !isStopping) {
					isStopping = true;
					mediaPlayer.stop(apiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
						@Override
						public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
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

				if(castDevice.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT)) {
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
				}
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

			ResultCallback callback = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
				@Override
				public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
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

			if(position > 0) {
				mediaPlayer.load(apiClient, mediaInfo, autoStart, position * 1000L).setResultCallback(callback);
			} else {
				mediaPlayer.load(apiClient, mediaInfo, autoStart).setResultCallback(callback);
			}
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


	private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
		private boolean isPlaying;
		private int position;
		private ResultCallback<Cast.ApplicationConnectionResult> resultCallback;

		ConnectionCallbacks(boolean isPlaying, int position) {
			this.isPlaying = isPlaying;
			this.position = position;

			resultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
				@Override
				public void onResult(Cast.ApplicationConnectionResult result) {
					Status status = result.getStatus();
					if (status.isSuccess()) {
						ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
						sessionId = result.getSessionId();
						String applicationStatus = result.getApplicationStatus();
						boolean wasLaunched = result.getWasLaunched();

						applicationStarted = true;
						setupChannel();
					} else {
						shutdownInternal();
					}
				}
			};
		}

		@Override
		public void onConnected(Bundle connectionHint) {
			if (waitingForReconnect) {
				Log.i(TAG, "Reconnecting");
				reconnectApplication();
			} else {
				launchApplication();
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			Log.w(TAG, "Connection suspended");
			isPlaying = downloadService.getPlayerState() == PlayerState.STARTED;
			position = getRemotePosition();
			waitingForReconnect = true;
		}

		void launchApplication() {
			try {
				Cast.CastApi.launchApplication(apiClient, EnvironmentVariables.CAST_APPLICATION_ID, false).setResultCallback(resultCallback);
			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}
		void reconnectApplication() {
			try {
				Cast.CastApi.joinApplication(apiClient, EnvironmentVariables.CAST_APPLICATION_ID, sessionId).setResultCallback(resultCallback);
			} catch (Exception e) {
				Log.e(TAG, "Failed to reconnect application", e);
			}
		}
		void setupChannel() {
			if(!waitingForReconnect) {
				mediaPlayer = new RemoteMediaPlayer();
				mediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
					@Override
					public void onStatusUpdated() {
						MediaStatus mediaStatus = mediaPlayer.getMediaStatus();
						if (mediaStatus == null) {
							return;
						}

						switch (mediaStatus.getPlayerState()) {
							case MediaStatus.PLAYER_STATE_PLAYING:
								if (ignoreNextPaused) {
									ignoreNextPaused = false;
								}
								downloadService.setPlayerState(PlayerState.STARTED);
								break;
							case MediaStatus.PLAYER_STATE_PAUSED:
								if (!ignoreNextPaused) {
									downloadService.setPlayerState(PlayerState.PAUSED);
								}
								break;
							case MediaStatus.PLAYER_STATE_BUFFERING:
								downloadService.setPlayerState(PlayerState.PREPARING);
								break;
							case MediaStatus.PLAYER_STATE_IDLE:
								if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
									if(downloadService.getPlayerState() != PlayerState.PREPARING) {
										downloadService.onSongCompleted();
									}
								} else if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_INTERRUPTED) {
									if (downloadService.getPlayerState() != PlayerState.PREPARING) {
										downloadService.setPlayerState(PlayerState.PREPARING);
									}
								} else if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_ERROR) {
									Log.e(TAG, "Idle due to unknown error");
									downloadService.onSongCompleted();
								} else {
									Log.w(TAG, "Idle reason: " + mediaStatus.getIdleReason());
									downloadService.setPlayerState(PlayerState.IDLE);
								}
								break;
						}
					}
				});
			}

			try {
				Cast.CastApi.setMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace(), mediaPlayer);
			} catch (Exception e) {
				Log.e(TAG, "Exception while creating channel", e);
			}

			if(!waitingForReconnect) {
				DownloadFile currentPlaying = downloadService.getCurrentPlaying();
				startSong(currentPlaying, isPlaying, position);
			}
			if(waitingForReconnect) {
				waitingForReconnect = false;
			}
		}
	}

	private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			shutdownInternal();
		}
	}
}
