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
 */

package github.paroj.dsub2000.service.sync;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.domain.Artist;
import github.paroj.dsub2000.domain.Indexes;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.util.FileUtil;
import github.paroj.dsub2000.util.Notifications;
import github.paroj.dsub2000.util.SyncUtil;
import github.paroj.dsub2000.util.Util;

public class LibrarySyncAdapter extends SubsonicSyncAdapter {
	private static String TAG = LibrarySyncAdapter.class.getSimpleName();

	public LibrarySyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	public LibrarySyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onExecuteSync(Context context, int instance) throws NetworkNotValidException {
		try {
			ArrayList<String> syncedList = new ArrayList<String>();

			String musicFolderId = Util.getSelectedMusicFolderId(context, instance);
			Indexes indexes = musicService.getIndexes(musicFolderId, true, context, null);

			// Build a virtual root directory whose children are all top-level artists
			// (and any loose top-level songs). downloadRecursively will then walk each
			// artist via getMusicDirectory(Entry), which already handles tag vs folder
			// browsing in the base class.
			MusicDirectory root = new MusicDirectory();
			for (Artist artist : indexes.getShortcuts()) {
				root.addChild(new MusicDirectory.Entry(artist));
			}
			for (Artist artist : indexes.getArtists()) {
				root.addChild(new MusicDirectory.Entry(artist));
			}
			if (indexes.getEntries() != null) {
				root.addChildren(indexes.getEntries());
			}

			boolean updated = downloadRecursively(syncedList, root, context, true);

			ArrayList<String> oldSyncedList = SyncUtil.getSyncedLibrary(context, instance);
			oldSyncedList.removeAll(syncedList);
			for (String path : oldSyncedList) {
				File saveFile = new File(path);
				FileUtil.unpinSong(context, saveFile);
			}

			SyncUtil.setSyncedLibrary(syncedList, context, instance);
			if (updated) {
				Notifications.showSyncNotification(context, R.string.sync_new_library, null);
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to sync library for " + Util.getServerName(context, instance), e);
		}
	}
}
