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
package github.vrih.xsub.provider;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRouter;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;

import github.vrih.xsub.domain.RemoteControlState;
import github.vrih.xsub.service.DownloadService;
import github.vrih.xsub.service.RemoteController;

/**
 * Created by Scott on 11/28/13.
 */
public class JukeboxRouteProvider extends MediaRouteProvider {
	public static final String CATEGORY_JUKEBOX_ROUTE = "github.vrih.xsub.SERVER_JUKEBOX";
	private RemoteController controller;
	private static final int MAX_VOLUME = 10;

	private final DownloadService downloadService;

	public JukeboxRouteProvider(Context context) {
		super(context);
		this.downloadService = (DownloadService) context;

		broadcastDescriptor();
	}

	private void broadcastDescriptor() {
		// Create intents
		IntentFilter routeIntentFilter = new IntentFilter();
		routeIntentFilter.addCategory(CATEGORY_JUKEBOX_ROUTE);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_START_SESSION);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_GET_SESSION_STATUS);
		routeIntentFilter.addAction(MediaControlIntent.ACTION_END_SESSION);

		// Create route descriptor
		MediaRouteDescriptor.Builder routeBuilder = new MediaRouteDescriptor.Builder("Jukebox Route", "Subsonic Jukebox");
		routeBuilder.addControlFilter(routeIntentFilter)
				.setPlaybackStream(AudioManager.STREAM_MUSIC)
				.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
				.setDescription("Subsonic Jukebox")
				.setVolume(controller == null ? 5 : (int) (controller.getVolume() * 10))
				.setVolumeMax(MAX_VOLUME)
				.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE);

		// Create descriptor
		MediaRouteProviderDescriptor.Builder providerBuilder = new MediaRouteProviderDescriptor.Builder();
		providerBuilder.addRoute(routeBuilder.build());
		setDescriptor(providerBuilder.build());
	}

	@Override
	public MediaRouteProvider.RouteController onCreateRouteController(String routeId) {
		return new JukeboxRouteController(downloadService);
	}

	private class JukeboxRouteController extends RouteController {
		private final DownloadService downloadService;

		JukeboxRouteController(DownloadService downloadService) {
			this.downloadService = downloadService;
		}

		@Override
		public boolean onControlRequest(Intent intent, androidx.mediarouter.media.MediaRouter.ControlRequestCallback callback) {
			if (intent.hasCategory(CATEGORY_JUKEBOX_ROUTE)) {
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
			downloadService.setRemoteEnabled(RemoteControlState.JUKEBOX_SERVER);
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
			broadcastDescriptor();
		}

		@Override
		public void onSetVolume(int volume) {
			if(controller != null) {
				controller.setVolume(volume);
			}
			broadcastDescriptor();
		}
	}
}
