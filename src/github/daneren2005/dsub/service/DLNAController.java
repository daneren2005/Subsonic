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

import android.util.Log;

import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.lastchange.LastChange;

import java.util.Map;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.DLNADevice;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.util.Util;

public class DLNAController extends RemoteController {
	private static final String TAG = DLNAController.class.getSimpleName();

	DLNADevice device;
	ControlPoint controlPoint;
	SubscriptionCallback callback;

	public DLNAController(DownloadService downloadService, ControlPoint controlPoint, DLNADevice device) {
		this.downloadService = downloadService;
		this.controlPoint = controlPoint;
		this.device = device;
	}

	@Override
	public void create(final boolean playing, final int seconds) {
		downloadService.setPlayerState(PlayerState.PREPARING);

		Device renderer = device.renderer;

		callback = new SubscriptionCallback(renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport")), 600) {
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
							downloadService.setPlayerState(PlayerState.STOPPED);
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
		controlPoint.execute(new Play(device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport"))) {
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
		controlPoint.execute(new Pause(device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport"))) {
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
		controlPoint.execute(new Stop(device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport"))) {
			@Override
			public void failure(ActionInvocation invocation, org.teleal.cling.model.message.UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Stop failed: " + defaultMessage);
			}
		});

		if(callback != null) {
			callback.end();
			callback = null;
		}
	}

	@Override
	public void updatePlaylist() {

	}

	@Override
	public void changePosition(int seconds) {

	}

	@Override
	public void changeTrack(int index, DownloadFile song) {

	}

	@Override
	public void setVolume(int volume) {

	}

	@Override
	public void updateVolume(boolean up) {

	}

	@Override
	public double getVolume() {
		return 0;
	}

	@Override
	public int getRemotePosition() {
		return 0;
	}

	private void startSong(final DownloadFile currentPlaying, final boolean autoStart, final int position) {
		if(currentPlaying == null) {
			downloadService.setPlayerState(PlayerState.IDLE);
			return;
		}

		downloadService.setPlayerState(PlayerState.PREPARING);
		MusicDirectory.Entry song = currentPlaying.getSong();

		try {
			MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
			String url = musicService.getMusicUrl(downloadService, song, currentPlaying.getBitRate());
			url = Util.replaceInternalUrl(downloadService, url);
			String metadata = "";

			controlPoint.execute(new SetAVTransportURI(device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport")), url, metadata) {
				@Override
				public void success(ActionInvocation invocation) {
					if (autoStart) {
						controlPoint.execute(new Play(device.renderer.findService(new ServiceType("schemas-upnp-org", "AVTransport"))) {
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
					} else {
						downloadService.setPlayerState(PlayerState.PAUSED);
					}
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
		Util.toast(downloadService, downloadService.getResources().getString(R.string.download_failed_to_load));
		downloadService.setPlayerState(PlayerState.STOPPED);
	}
}
