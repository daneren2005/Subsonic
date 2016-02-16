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

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Notifications;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 8/28/13.
 */

public class StarredSyncAdapter extends SubsonicSyncAdapter {
	private static String TAG = StarredSyncAdapter.class.getSimpleName();

	public StarredSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	@TargetApi(14)
	public StarredSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onExecuteSync(Context context, int instance) throws NetworkNotValidException {
		try {
			ArrayList<String> syncedList = new ArrayList<String>();
			MusicDirectory starredList = musicService.getStarredList(context, null);

			// Pin all the starred stuff
			boolean updated = downloadRecursively(syncedList, starredList, context, true);

			// Get old starred list
			ArrayList<String> oldSyncedList = SyncUtil.getSyncedStarred(context, instance);

			// Check to make sure there aren't any old starred songs that now need to be removed
			oldSyncedList.removeAll(syncedList);

			for(String path: oldSyncedList) {
				File saveFile = new File(path);
				FileUtil.unpinSong(context, saveFile);
			}

			SyncUtil.setSyncedStarred(syncedList, context, instance);
			if(updated) {
				Notifications.showSyncNotification(context, R.string.sync_new_starred, null);
			}
		} catch(Exception e) {
			Log.e(TAG, "Failed to get starred list for " + Util.getServerName(context, instance));
		}
	}
}
