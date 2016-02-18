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
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Notifications;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 8/28/13.
 */

public class MostRecentSyncAdapter extends SubsonicSyncAdapter {
	private static String TAG = MostRecentSyncAdapter.class.getSimpleName();

	public MostRecentSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	@TargetApi(14)
	public MostRecentSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onExecuteSync(Context context, int instance) throws NetworkNotValidException {
		try {
			ArrayList<String> syncedList = SyncUtil.getSyncedMostRecent(context, instance);
			MusicDirectory albumList = musicService.getAlbumList("newest", 20, 0, tagBrowsing, context, null);
			List<String> updated = new ArrayList<String>();
			boolean firstRun = false;
			if(syncedList.size() == 0) {
				// Get the initial set of albums on first run, don't sync any of these!
				for(MusicDirectory.Entry album: albumList.getChildren()) {
					syncedList.add(album.getId());
				}
				firstRun = true;
			} else {
				for(MusicDirectory.Entry album: albumList.getChildren()) {
					if(!syncedList.contains(album.getId())) {
						if(!"Podcast".equals(album.getGenre())) {
							try {
								if(downloadRecursively(null, getMusicDirectory(album), context, false)) {
									updated.add(album.getTitle());
								}
							} catch(Exception e) {
								Log.w(TAG, "Failed to get songs for " + album.getId() + " on " + Util.getServerName(context, instance));
							}
						}
						syncedList.add(album.getId());
					}
				}
			}

			if(updated.size() > 0) {
				while(syncedList.size() > 40) {
					syncedList.remove(0);
				}
				
				FileUtil.serialize(context, syncedList, SyncUtil.getMostRecentSyncFile(context, instance));

				// If there is a new album on the active server, chances are artists need to be refreshed
				if(Util.getActiveServer(context) == instance) {
					musicService.getIndexes(Util.getSelectedMusicFolderId(context), true, context, null);
				}

				Notifications.showSyncNotification(context, R.string.sync_new_albums, SyncUtil.joinNames(updated));
			} else if(firstRun) {
				FileUtil.serialize(context, syncedList, SyncUtil.getMostRecentSyncFile(context, instance));
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to get most recent list for " + Util.getServerName(context, instance));
		}
	}
}
