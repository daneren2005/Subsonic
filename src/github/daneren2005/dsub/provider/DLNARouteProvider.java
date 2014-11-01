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

	Copyright 2014 (C) Scott Jackson
*/
package github.daneren2005.dsub.provider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.IBinder;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.util.Log;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.HashMap;
import java.util.Map;

import github.daneren2005.dsub.domain.DLNADevice;
import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.RemoteController;

/**
 * Created by Scott on 11/28/13.
 */
public class DLNARouteProvider extends MediaRouteProvider {
	private static final String TAG = DLNARouteProvider.class.getSimpleName();
	public static final String CATEGORY_DLNA = "github.daneren2005.dsub.DLNA";

	private DownloadService downloadService;
	private RemoteController controller;

	private HashMap<String, DLNADevice> devices = new HashMap<String, DLNADevice>();
	private AndroidUpnpService dlnaService;
	private ServiceConnection dlnaServiceConnection;

	public DLNARouteProvider(Context context) {
		super(context);
		this.downloadService = (DownloadService) context;
		dlnaServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				dlnaService = (AndroidUpnpService) service;
				dlnaService.getRegistry().addListener(new RegistryListener() {
					@Override
					public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remoteDevice) {

					}

					@Override
					public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {

					}

					@Override
					public void remoteDeviceAdded(Registry registry, RemoteDevice remoteDevice) {
						deviceAdded(remoteDevice);
					}

					@Override
					public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
						deviceAdded(remoteDevice);
					}

					@Override
					public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
						deviceRemoved(remoteDevice);
					}

					@Override
					public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
						deviceAdded(localDevice);
					}

					@Override
					public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
						deviceRemoved(localDevice);
					}

					@Override
					public void beforeShutdown(Registry registry) {

					}

					@Override
					public void afterShutdown() {

					}
				});

				for (Device<?, ?, ?> device : dlnaService.getControlPoint().getRegistry().getDevices()) {
					deviceAdded(device);
				}
				dlnaService.getControlPoint().search();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				dlnaService = null;
			}
		};
		context.bindService(new Intent(context, AndroidUpnpServiceImpl.class), dlnaServiceConnection,Context.BIND_AUTO_CREATE);
	}

	private void broadcastDescriptors() {
		// Create intents
		IntentFilter routeIntentFilter = new IntentFilter();
		routeIntentFilter.addCategory(CATEGORY_DLNA);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_START_SESSION);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_GET_SESSION_STATUS);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_END_SESSION);

		// Create descriptor
		MediaRouteProviderDescriptor.Builder providerBuilder = new MediaRouteProviderDescriptor.Builder();

		// Create route descriptor
		for(Map.Entry<String, DLNADevice> deviceEntry: devices.entrySet()) {
			DLNADevice device = deviceEntry.getValue();

			MediaRouteDescriptor.Builder routeBuilder = new MediaRouteDescriptor.Builder(device.id, device.name);
			routeBuilder.addControlFilter(routeIntentFilter)
					.setPlaybackStream(AudioManager.STREAM_MUSIC)
					.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
					.setDescription(device.description)
					.setVolume(controller == null ? 5 : (int) (controller.getVolume() * 10))
					.setVolumeMax(device.volumeMax)
					.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE);
			providerBuilder.addRoute(routeBuilder.build());
		}

		setDescriptor(providerBuilder.build());
	}

	@Override
	public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
		if (request != null && request.isActiveScan()) {

		}
	}

	@Override
	public RouteController onCreateRouteController(String routeId) {
		return new DLNARouteController(downloadService);
	}

	private void deviceAdded(Device device) {
		if(device.getType().getType().equals("MediaRenderer") && device instanceof RemoteDevice) {
			String id = device.getIdentity().getUdn().toString();
			String name = device.getDetails().getFriendlyName();
			String displayName = device.getDisplayString();
			Log.d(TAG, displayName);

			DLNADevice newDevice = new DLNADevice(id, name, displayName, 0, 10);
			devices.put(id, newDevice);
			broadcastDescriptors();
		}
	}
	private void deviceRemoved(Device device) {
		if(device.getType().getType().equals("MediaRenderer") && device instanceof RemoteDevice) {
			String id = device.getIdentity().getUdn().toString();
			devices.remove(id);
			broadcastDescriptors();
		}
	}

	private class DLNARouteController extends RouteController {
		private DownloadService downloadService;

		public DLNARouteController(DownloadService downloadService) {
			this.downloadService = downloadService;
		}

		@Override
		public boolean onControlRequest(Intent intent, android.support.v7.media.MediaRouter.ControlRequestCallback callback) {
			if (intent.hasCategory(CATEGORY_DLNA)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void onRelease() {
			downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
			controller = null;
		}

		@Override
		public void onSelect() {
			downloadService.setRemoteEnabled(RemoteControlState.DLNA);
			controller = downloadService.getRemoteController();
		}

		@Override
		public void onUnselect() {
			downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
			controller = null;
		}

		@Override
		public void onUpdateVolume(int delta) {
			if(controller != null) {
				controller.updateVolume(delta > 0);
			}
			broadcastDescriptors();
		}

		@Override
		public void onSetVolume(int volume) {
			if(controller != null) {
				controller.setVolume(volume);
			}
			broadcastDescriptors();
		}
	}
}
