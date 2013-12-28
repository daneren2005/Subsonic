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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.SyncUtil.SyncSet;
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

		ArrayList<SyncSet> playlistList = SyncUtil.getSyncedPlaylists(context, instance);
		List<String> updated = new ArrayList<String>();
		boolean removed = false;
		for(int i = 0; i < playlistList.size(); i++) {
			SyncSet cachedPlaylist = playlistList.get(i);
			String id = cachedPlaylist.id;
			try {
				MusicDirectory playlist = musicService.getPlaylist(true, id, serverName, context, null);

				// Get list of original paths
				List<String> origPathList = new ArrayList<String>();
				if(cachedPlaylist.synced != null) {
					origPathList.addAll(cachedPlaylist.synced);
				} else {
					cachedPlaylist.synced = new ArrayList<String>();
				}

				for(MusicDirectory.Entry entry: playlist.getChildren()) {
					DownloadFile file = new DownloadFile(context, entry, true);
					String path = file.getCompleteFile().getPath();
					if(!cachedPlaylist.synced.contains(path)) {
						while(!file.isSaved() && !file.isFailedMax()) {
							file.downloadNow(musicService);
							if(!updated.contains(playlist.getName())) {
								updated.add(playlist.getName());
							}
						}

						// Add to cached path set if saved
						if(file.isSaved()) {
							cachedPlaylist.synced.add(path);
						}
					}

					origPathList.remove(path);
				}

				// Check to unpin all paths which are no longer in playlist
				if(origPathList.size() > 0) {
					for(String path: origPathList) {
						File saveFile = new File(path);
						FileUtil.unpinSong(saveFile);
						cachedPlaylist.synced.remove(path);
					}

					removed = true;
				}
			} catch(SubsonicRESTException e) {
				if(e.getCode() == 70) {
					SyncUtil.removeSyncedPlaylist(context, id, instance);
					Log.i(TAG, "Unsync deleted playlist " + id + " for " + serverName);
				}
			} catch(Exception e) {
				Log.e(TAG, "Failed to get playlist " + id + " for " + serverName, e);
			}

			if(updated.size() > 0) {
				SyncUtil.showSyncNotification(context, R.string.sync_new_playlists, SyncUtil.joinNames(updated));
			}
			if(updated.size() > 0 || removed) {
				SyncUtil.setSyncedPlaylists(context, instance, playlistList);
			}
		}
	}
}
