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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;

import java.util.HashMap;
import java.util.Map;

import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.RemoteController;

/**
 * Created by Scott on 11/28/13.
 */
public class DLNARouteProvider extends MediaRouteProvider {
	public static final String CATEGORY_DLNA = "github.daneren2005.dsub.DLNA";

	private DownloadService downloadService;
	private RemoteController controller;

	private HashMap<String, Device> devices = new HashMap<String, Device>();

	public DLNARouteProvider(Context context) {
		super(context);
		this.downloadService = (DownloadService) context;
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
		for(Map.Entry<String, Device> deviceEntry: devices.entrySet()) {
			Device device = deviceEntry.getValue();

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

	public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
		if (request != null && request.isActiveScan()) {

		}
	}

	@Override
	public RouteController onCreateRouteController(String routeId) {
		return new DLNARouteController(downloadService);
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

	static public class Device implements Parcelable {
		public String id;
		public String name;
		public String description;
		public int volume;
		public int volumeMax;

		public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
			public Device createFromParcel(Parcel in) {
				return new Device(in);
			}

			public Device[] newArray(int size) {
				return new Device[size];
			}
		};

		private Device(Parcel in) {
			id = in.readString();
			name = in.readString();
			description = in.readString();
			volume = in.readInt();
			volumeMax = in.readInt();
		}

		public Device(String id, String name, String description, int volume, int volumeMax) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.volume = volume;
			this.volumeMax = volumeMax;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(id);
			dest.writeString(name);
			dest.writeString(description);
			dest.writeInt(volume);
			dest.writeInt(volumeMax);
		}
	}
}
