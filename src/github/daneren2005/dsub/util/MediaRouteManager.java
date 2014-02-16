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

package github.daneren2005.dsub.util;

import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.provider.JukeboxRouteProvider;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.RemoteController;
import github.daneren2005.dsub.util.compat.CastCompat;

import static android.support.v7.media.MediaRouter.RouteInfo;

/**
 * Created by owner on 2/8/14.
 */
public class MediaRouteManager extends MediaRouter.Callback {
	private static final String TAG = MediaRouteManager.class.getSimpleName();
	private static boolean castAvailable = false;

	private DownloadServiceImpl downloadService;
	private MediaRouter router;
	private MediaRouteSelector selector;
	private List<MediaRouteProvider> providers = new ArrayList<MediaRouteProvider>();

	static {
		try {
			CastCompat.checkAvailable();
			castAvailable = true;
		} catch(Throwable t) {
			castAvailable = false;
		}
	}

	public MediaRouteManager(DownloadServiceImpl downloadService) {
		this.downloadService = downloadService;
		router = MediaRouter.getInstance(downloadService);
		addProviders();
		buildSelector();
	}

	public void destroy() {
		for(MediaRouteProvider provider: providers) {
			router.removeProvider(provider);
		}
	}

	@Override
	public void onRouteSelected(MediaRouter router, RouteInfo info) {
		if(castAvailable) {
			RemoteController controller = CastCompat.getController(downloadService, info);
			if(controller != null) {
				downloadService.setRemoteEnabled(RemoteControlState.CHROMECAST, controller);
			}
		}
	}
	@Override
	public void onRouteUnselected(MediaRouter router, RouteInfo info) {
		downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
	}

	public void startScan() {
		router.addCallback(selector, this, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}
	public void stopScan() {
		router.removeCallback(this);
	}

	public MediaRouteSelector getSelector() {
		return selector;
	}

	public RouteInfo getSelectedRoute() {
		return router.getSelectedRoute();
	}
	public RouteInfo getRouteForId(String id) {
		if(id == null) {
			return null;
		}

		// Try to find matching id
		for(RouteInfo info: router.getRoutes()) {
			if(id.equals(info.getId())) {
				router.selectRoute(info);
				return info;
			}
		}

		return null;
	}
	public RemoteController getRemoteController(RouteInfo info) {
		if(castAvailable) {
			return CastCompat.getController(downloadService, info);
		} else {
			return null;
		}
	}

	private void addProviders() {
		JukeboxRouteProvider routeProvider = new JukeboxRouteProvider(downloadService);
		router.addProvider(routeProvider);
		providers.add(routeProvider);
	}
	private void buildSelector() {
		MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
		builder.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		if(castAvailable) {
			builder.addControlCategory(CastCompat.getCastControlCategory());
		}
		selector = builder.build();
	}
}
