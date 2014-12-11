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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.DLNADevice;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.serverproxy.FileProxy;

public class DLNAController extends RemoteController {
	private static final String TAG = DLNAController.class.getSimpleName();
	private static final long STATUS_UPDATE_INTERVAL_SECONDS = 3L;

	DLNADevice device;
	ControlPoint controlPoint;
	SubscriptionCallback callback;

	private FileProxy proxy;
	String rootLocation = "";
	boolean error = false;

	final AtomicLong lastUpdate = new AtomicLong();
	int currentPosition = 0;
	String currentPlayingURI;
	boolean running = true;

	public DLNAController(DownloadService downloadService, ControlPoint controlPoint, DLNADevice device) {
		this.downloadService = downloadService;
		this.controlPoint = controlPoint;
		this.device = device;

		SharedPreferences prefs = Util.getPreferences(downloadService);
		rootLocation = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
	}

	@Override
	public void create(final boolean playing, final int seconds) {
		downloadService.setPlayerState(PlayerState.PREPARING);

		callback = new SubscriptionCallback(getTransportService(), 600) {
			@Override
			protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e, String msg) {
				Log.w(TAG, "Register subscription callback failed: " + msg, e);
			}

			@Override
			protected void established(GENASubscription genaSubscription) {
				startSong(downloadService.getCurrentPlaying(), playing, seconds);
			}

			@Override
			protected void ended(GENASubscription genaSubscription, CancelReason cancelReason, UpnpResponse upnpResponse) {

			}

			@Override
			protected void eventReceived(GENASubscription genaSubscription) {
				Map<String, StateVariableValue> m = genaSubscription.getCurrentValues();
				try {
					LastChange lastChange = new LastChange(new AVTransportLastChangeParser(), m.get("LastChange").toString());
					if (playing || lastChange.getEventedValue(0, AVTransportVariable.TransportState.class) == null) {
						return;
					}

					switch (lastChange.getEventedValue(0, AVTransportVariable.TransportState.class).getValue()) {
						case PLAYING:
							downloadService.setPlayerState(PlayerState.STARTED);
							break;
						case PAUSED_PLAYBACK:
							downloadService.setPlayerState(PlayerState.PAUSED);
							break;
						case STOPPED:
							boolean failed = false;
							for(StateVariableValue val: m.values()) {
								if(val.toString().indexOf("TransportStatus val=\"ERROR_OCCURRED\"") != -1) {
									Log.w(TAG, "Failed to load with event: " + val.toString());
									failed = true;
								}
							}

							if(failed) {
								failedLoad();
							} else {
								downloadService.setPlayerState(PlayerState.STOPPED);
							}
							break;
						case TRANSITIONING:
							downloadService.setPlayerState(PlayerState.PREPARING);
							break;
						case NO_MEDIA_PRESENT:
							downloadService.setPlayerState(PlayerState.IDLE);
							break;
						default:
					}
				}
				catch (Exception e) {
					Log.w(TAG, "Failed to parse UPNP event", e);
					failedLoad();
				}
			}

			@Override
			protected void eventsMissed(GENASubscription genaSubscription, int i) {

			}
		};
		controlPoint.execute(callback);
	}

	@Override
	public void start() {
		if(error) {
			Log.w(TAG, "Attempting to restart song");
			startSong(downloadService.getCurrentPlaying(), true, 0);
			return;
		}

		controlPoint.execute(new Play(getTransportService()) {
			@Override
			public void success(ActionInvocation invocation) {
				downloadService.setPlayerState(PlayerState.STARTED);
			}

			@Override
			public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String msg) {
				Log.w(TAG, "Failed to start playing: " + msg);
				failedLoad();
			}
		});
	}

	@Override
	public void stop() {
		controlPoint.execute(new Pause(getTransportService()) {
			@Override
			public void success(ActionInvocation invocation) {
				downloadService.setPlayerState(PlayerState.PAUSED);
			}

			@Override
			public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String msg) {
				Log.w(TAG, "Failed to pause playing: " + msg);
			}
		});
	}

	@Override
	public void shutdown() {
		controlPoint.execute(new Stop(getTransportService()) {
			@Override
			public void failure(ActionInvocation invocation, org.fourthline.cling.model.message.UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Stop failed: " + defaultMessage);
			}
		});

		if(callback != null) {
			callback.end();
			callback = null;
		}

		running = false;
	}

	@Override
	public void updatePlaylist() {
		if(downloadService.getCurrentPlaying() == null) {
			startSong(null, false, 0);
		}
	}

	@Override
	public void changePosition(int seconds) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		controlPoint.execute(new Seek(getTransportService(), SeekMode.REL_TIME, df.format(new Date(seconds * 1000))) {
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Seek failed: " + defaultMessage);
			}
		});
	}

	@Override
	public void changeTrack(int index, DownloadFile song) {
		startSong(song, true, 0);
	}

	@Override
	public void setVolume(int volume) {
		if(volume < 0) {
			volume = 0;
		} else if(volume > device.volumeMax) {
			volume = device.volumeMax;
		}

		device.volume = volume;
		controlPoint.execute(new SetVolume(device.renderer.findService(new ServiceType("schemas-upnp-org", "RenderingControl")), volume) {
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Set volume failed: " + defaultMessage);
			}
		});
	}

	@Override
	public void updateVolume(boolean up) {
		int increment = device.volumeMax / 10;
		setVolume(device.volume + (up ? increment : -increment));
	}

	@Override
	public double getVolume() {
		return device.volume;
	}

	@Override
	public int getRemotePosition() {
		int secondsSinceLastUpdate = (int) ((System.currentTimeMillis() - lastUpdate.get()) / 1000L);
		return currentPosition + secondsSinceLastUpdate;
	}

	private void startSong(final DownloadFile currentPlaying, final boolean autoStart, final int position) {
		if(currentPlaying == null) {
			downloadService.setPlayerState(PlayerState.IDLE);
			return;
		}
		error = false;

		downloadService.setPlayerState(PlayerState.PREPARING);
		MusicDirectory.Entry song = currentPlaying.getSong();

		try {
			// Get url for entry
			MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
			String url;
			if(Util.isOffline(downloadService) || song.getId().indexOf(rootLocation) != -1) {
				if(proxy == null) {
					proxy = new FileProxy(downloadService);
					proxy.start();
				}

				url = proxy.getPublicAddress(song.getId());
			} else {
				if(proxy != null) {
					proxy.stop();
					proxy = null;
				}

				if(song.isVideo()) {
					url = musicService.getHlsUrl(song.getId(), currentPlaying.getBitRate(), downloadService);
				} else {
					url = musicService.getMusicUrl(downloadService, song, currentPlaying.getBitRate());
				}

				url = Util.replaceInternalUrl(downloadService, url);
			}

			// Create metadata for entry
			Item track;
			if(song.isVideo()) {
				track = new VideoItem(song.getId(), song.getParent(), song.getTitle(), song.getArtist());
			} else {
				MusicTrack musicTrack = new MusicTrack(song.getId(), song.getParent(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getArtist());
				musicTrack.setOriginalTrackNumber(song.getTrack());
				track = musicTrack;
			}

			DIDLParser parser = new DIDLParser();
			DIDLContent didl = new DIDLContent();
			didl.addItem(track);

			String metadata = "";
			try {
				// metadata = parser.generate(didl);
			} catch(Exception e) {
				Log.w(TAG, "Metadata generation failed", e);
			}

			currentPlayingURI = url;
			controlPoint.execute(new SetAVTransportURI(getTransportService(), url, metadata) {
				@Override
				public void success(ActionInvocation invocation) {
					if(position != 0) {
						changePosition(position);
					}

					if (autoStart) {
						start();
					} else {
						downloadService.setPlayerState(PlayerState.PAUSED);
					}

					getUpdatedStatus();
				}

				@Override
				public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String msg) {
					Log.w(TAG, "Set URI failed: " + msg);
					failedLoad();
				}
			});
		} catch (Exception e) {
			Log.w(TAG, "Failed startSong", e);
			failedLoad();
		}
	}

	private void failedLoad() {
		downloadService.setPlayerState(PlayerState.STOPPED);
		error = true;

		if(Looper.myLooper() != Looper.getMainLooper()) {
			downloadService.post(new Runnable() {
				@Override
				public void run() {
					Util.toast(downloadService, downloadService.getResources().getString(R.string.download_failed_to_load));
				}
			});
		} else {
			Util.toast(downloadService, downloadService.getResources().getString(R.string.download_failed_to_load));
		}
	}

	private Service getTransportService() {
		return device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport"));
	}

	private void getUpdatedStatus() {
		// Don't care if shutdown in the meantime
		if(!running) {
			return;
		}

		controlPoint.execute(new GetPositionInfo(getTransportService()) {
			@Override
			public void received(ActionInvocation actionInvocation, PositionInfo positionInfo) {
				// Don't care if shutdown in the meantime
				if(!running) {
					return;
				}

				lastUpdate.set(System.currentTimeMillis());

				// Playback was stopped
				if(positionInfo.getTrackURI() == null) {
					if(downloadService.getCurrentPlaying() != null && downloadService.getPlayerState() != PlayerState.IDLE) {
						Log.w(TAG, "Nothing is playing on DLNA device");
						downloadService.setCurrentPlaying(null, false);
					}
				}
				// End device started playing something else, no idea what
				else if(!positionInfo.getTrackURI().equals(currentPlayingURI) && downloadService.getPlayerState() != PlayerState.IDLE) {
					Log.w(TAG, "A different song is playing on the remote device: " + positionInfo.getTrackURI());
					downloadService.setCurrentPlaying(null, false);
				} else {
					// Let's get the updated position
					currentPosition = (int) positionInfo.getTrackElapsedSeconds();
				}

				downloadService.postDelayed(new Runnable() {
					@Override
					public void run() {
						getUpdatedStatus();
					}
				}, STATUS_UPDATE_INTERVAL_SECONDS);
			}

			@Override
			public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
				Log.w(TAG, "Failed to get an update");

				downloadService.postDelayed(new Runnable() {
					@Override
					public void run() {
						getUpdatedStatus();
					}
				}, STATUS_UPDATE_INTERVAL_SECONDS);
			}
		});
	}
}
