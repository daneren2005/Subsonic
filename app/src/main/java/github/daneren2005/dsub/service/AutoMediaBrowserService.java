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

	Copyright 2015 (C) Scott Jackson
*/
package github.daneren2005.dsub.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.util.compat.RemoteControlClientLP;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoMediaBrowserService extends MediaBrowserService {
	private static final String TAG = AutoMediaBrowserService.class.getSimpleName();
	private static final String BROWSER_ROOT = "root";
	MediaSession mediaSession;

	@Override
	public void onCreate() {
		super.onCreate();

		DownloadService downloadService = getDownloadService();
		RemoteControlClientLP remoteControlClient = (RemoteControlClientLP) downloadService.getRemoteControlClient();
		setSessionToken(remoteControlClient.getMediaSession().getSessionToken());
	}

		@Nullable
	@Override
	public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
		BrowserRoot root = new BrowserRoot(BROWSER_ROOT, null);
		return root;
	}

	@Override
	public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
		if(BROWSER_ROOT.equals(parentId)) {
			getRootFolders(result);
		} else {

		}
	}

	private void getRootFolders(Result<List<MediaBrowser.MediaItem>> result) {
		List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

		MediaDescription.Builder library = new MediaDescription.Builder();
		library.setDescription("Library")
			.setMediaId("library");
		mediaItems.add(new MediaBrowser.MediaItem(library.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE));

		result.sendResult(mediaItems);
	}

	public DownloadService getDownloadService() {
		// If service is not available, request it to start and wait for it.
		for (int i = 0; i < 5; i++) {
			DownloadService downloadService = DownloadService.getInstance();
			if (downloadService != null) {
				break;
			}
			Log.w(TAG, "DownloadService not running. Attempting to start it.");
			startService(new Intent(this, DownloadService.class));
			Util.sleepQuietly(50L);
		}

		return DownloadService.getInstance();
	}
}
