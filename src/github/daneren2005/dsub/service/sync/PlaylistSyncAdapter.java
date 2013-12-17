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

 Copyright 2009 (C) Sindre Mehus
*/

package github.daneren2005.dsub.service.sync;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 8/28/13.
*/

public class PlaylistSyncAdapter extends SubsonicSyncAdapter {
	private static String TAG = PlaylistSyncAdapter.class.getSimpleName();

	public PlaylistSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	@TargetApi(14)
	public PlaylistSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onExecuteSync(Context context, int instance) {
		String serverName = Util.getServerName(context, instance);

		try {
			// Just update playlist listings so user doesn't have to
			musicService.getPlaylists(true, context, null);
		} catch(Exception e) {
			Log.e(TAG, "Failed to refresh playlist list for " + serverName);
		}

		List<String> playlistList = SyncUtil.getSyncedPlaylists(context, instance);
		List<String> updated = new ArrayList<String>();
		for(int i = 0; i < playlistList.size(); i++) {
			String id = playlistList.get(i);
			try {
				MusicDirectory playlist = musicService.getPlaylist(true, id, serverName, context, null);

				for(MusicDirectory.Entry entry: playlist.getChildren()) {
					DownloadFile file = new DownloadFile(context, entry, true);
					while(!file.isSaved() && !file.isFailedMax()) {
						file.downloadNow(musicService);
						if(!updated.contains(playlist.getName())) {
							updated.add(playlist.getName());
						}
					}
				}
			} catch(SubsonicRESTException e) {
				if(e.getCode() == 70) {
					SyncUtil.removeSyncedPlaylist(context, id, instance);
					Log.i(TAG, "Unsync deleted playlist " + id + " for " + serverName);
				}
			} catch(Exception e) {
				Log.e(TAG, "Failed to get playlist " + id + " for " + serverName);
			}

			if(updated.size() > 0) {
				SyncUtil.showSyncNotification(context, R.string.sync_new_playlists, SyncUtil.joinNames(updated));
			}
		}
	}
}
