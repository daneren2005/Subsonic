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

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.compat.CastCompat;

/**
 * Created by owner on 2/9/14.
 */
public class ChromeCastController extends RemoteController {
	private static final String TAG = ChromeCastController.class.getSimpleName();

	private CastDevice castDevice;
	private GoogleApiClient apiClient;
	private ConnectionCallbacks connectionCallbacks;
	private ConnectionFailedListener connectionFailedListener;
	private Cast.Listener castClientListener;

	private boolean applicationStarted = false;
	private boolean waitingForReconnect = false;

	private RemoteMediaPlayer mediaPlayer;
	private double gain = 0.5;

	public ChromeCastController(DownloadServiceImpl downloadService, CastDevice castDevice) {
		this.downloadService = downloadService;
		this.castDevice = castDevice;

		connectionCallbacks = new ConnectionCallbacks();
		connectionFailedListener = new ConnectionFailedListener();
		castClientListener = new Cast.Listener() {
			@Override
			public void onApplicationStatusChanged() {
				if (apiClient != null) {
					Log.d(TAG, "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(apiClient));
				}
			}

			@Override
			public void onVolumeChanged() {
				if (apiClient != null) {
					gain = Cast.CastApi.getVolume(apiClient);
				}
			}

			@Override
			public void onApplicationDisconnected(int errorCode) {
				shutdown();
			}

		};

		Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(castDevice, castClientListener);
		apiClient = new GoogleApiClient.Builder(downloadService)
			.addApi(Cast.API, apiOptionsBuilder.build())
			.addConnectionCallbacks(connectionCallbacks)
			.addOnConnectionFailedListener(connectionFailedListener)
			.build();

		apiClient.connect();
	}

	@Override
	public void start() {
		try {
			mediaPlayer.play(apiClient);
		} catch(Exception e) {
			Log.e(TAG, "Failed to pause");
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
			if(mediaPlayer != null) {
				mediaPlayer.stop(apiClient);
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to stop mediaPlayer", e);
		}

		try {
			Cast.CastApi.stopApplication(apiClient);
			Cast.CastApi.removeMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace());
			mediaPlayer = null;
			applicationStarted = false;
		} catch(IOException e) {
			Log.e(TAG, "Failed to shutdown application", e);
		}

		if(apiClient.isConnected()) {
			apiClient.disconnect();
		}
	}

	@Override
	public void updatePlaylist() {

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
		startSong(song, true);
	}

	@Override
	public void setVolume(boolean up) {
		double delta = up ? 0.1 : -0.1;
		gain += delta;
		gain = Math.max(gain, 0.0);
		gain = Math.min(gain, 1.0);

		getVolumeToast().setVolume(gain);
		try {
			Cast.CastApi.setVolume(apiClient, gain);
		} catch(Exception e) {
			Log.e(TAG, "Failed to the volume");
		}
	}

	@Override
	public int getRemotePosition() {
		if(remotePlayer != null) {
			return remotePlayer.getApproximateStreamPosition();
		} else {
			return 0;
		}
	}

	void startSong(DownloadFile currentPlaying, boolean autoStart) {
		if(currentPlaying == null) {
			// Don't start anything
			return;
		}
		MusicDirectory.Entry song = currentPlaying.getSong();

		try {
			MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
			String url = song.isVideo() ? musicVideo.getHlsUrl(song.getId(), downloadFile.getBitRate(), downloadService) : musicService.getMusicUrl(downloadService, song, currentPlaying.getBitRate());
			//  Use separate profile for Chromecast so users can do ogg on phone, mp3 for CC
			url = url.replace(Constants.REST_CLIENT_ID, Constants.CHROMECAST_CLIENT_ID);

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
			}

			// Load it into a MediaInfo wrapper
			MediaInfo mediaInfo = new MediaInfo.Builder(url)
				.setContentType(song.isVideo() ? "application/x-mpegURL" : song.getTranscodedContentType())
				.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
				.setMetadata(meta)
				.build();

			mediaPlayer.load(apiClient, mediaInfo, autoPlay).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
				@Override
				public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
					if (result.getStatus().isSuccess()) {
						if(mediaPlayer.getMediaStatus().getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
							downloadService.setPlayerState(PlayerState.STARTED);
						} else {
							downloadService.setPlayerState(PlayerState.PREPARED);
						}
					} else {
						Log.e(TAG, "Failed to load");
						downloadService.setPlayerState(PlayerState.STOPPED);
					}
				}
			});
		} catch (IllegalStateException e) {
			Log.e(TAG, "Problem occurred with media during loading", e);
		} catch (Exception e) {
			Log.e(TAG, "Problem opening media during loading", e);
		}
	}


	private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			if (waitingForReconnect) {
				waitingForReconnect = false;
				// reconnectChannels();
			} else {
				launchApplication();
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			waitingForReconnect = true;
		}

		void launchApplication() {
			try {
				Cast.CastApi.launchApplication(apiClient, CastCompat.APPLICATION_ID, false).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
					@Override
					public void onResult(Cast.ApplicationConnectionResult result) {
						Status status = result.getStatus();
						if (status.isSuccess()) {
							ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
							String sessionId = result.getSessionId();
							String applicationStatus = result.getApplicationStatus();
							boolean wasLaunched = result.getWasLaunched();

							applicationStarted = true;
							setupChannel();
						} else {
							shutdown();
						}
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}
		void setupChannel() {
			mediaPlayer = new RemoteMediaPlayer();
			mediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
				@Override
				public void onStatusUpdated() {
					MediaStatus mediaStatus = mediaPlayer.getMediaStatus();
					switch(mediaStatus.getPlayerState()) {
						case PLAYER_STATE_PLAYING:
							downloadService.setPlayerState(PlayerState.STARTED);
							break;
						case PLAYER_STATE_PAUSED:
							downloadService.setPlayerState(PlayerState.PAUSED);
							break;
						case PLAYER_STATE_BUFFERING:
							downloadService.setPlayerState(PlayerState.PREPARING);
							break;
						case PLAYER_STATE_IDLE:
							downloadService.setPlayerState(PlayerState.COMPLETED);
							downloadService.next();
							break;
					}
				}
			});
			mediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
				@Override
				public void onMetadataUpdated() {
					MediaInfo mediaInfo = mediaPlayer.getMediaInfo();
					MediaMetadata metadata = mediaInfo.getMetadata();
					// TODO: Do I care about this?
				}
			});

			try {
				Cast.CastApi.setMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace(), mediaPlayer);
			} catch (IOException e) {
				Log.e(TAG, "Exception while creating channel", e);
			}

			DownloadFile currentPlaying = downloadService.getCurrentPlaying();
			startSong(currentPlaying, false);
		}
	}

	private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			shutdown();
		}
	}
}
