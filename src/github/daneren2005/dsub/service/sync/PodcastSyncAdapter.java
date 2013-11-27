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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.SyncUtil.SyncSet;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 8/28/13.
 */

public class PodcastSyncAdapter extends SubsonicSyncAdapter {
	private static String TAG = PodcastSyncAdapter.class.getSimpleName();

	public PodcastSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	@TargetApi(14)
	public PodcastSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
	}

	@Override
	public void onExecuteSync(Context context, int instance) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		ArrayList<SyncSet> podcastList = SyncUtil.getSyncedPodcasts(context, instance);

		try {
			// Only refresh if syncs exist (implies a server where supported)
			if(podcastList.size() > 0) {
				// Refresh podcast listings before syncing
				musicService.refreshPodcasts(context, null);
			}

			boolean updated = false;
			for(int i = 0; i < podcastList.size(); i++) {
				SyncSet set = podcastList.get(i);
				String id = set.id;
				List<String> existingEpisodes = set.synced;
				try {
					MusicDirectory podcasts = musicService.getPodcastEpisodes(true, id, context, null);

					for(MusicDirectory.Entry entry: podcasts.getChildren()) {
						// Make sure podcast is valid and not already synced
						if(entry.getId() != null && "completed".equals(((PodcastEpisode)entry).getStatus()) && !existingEpisodes.contains(entry.getId())) {
							DownloadFile file = new DownloadFile(context, entry, true);
							while(!file.isSaved() && !file.isFailedMax()) {
								file.downloadNow();
							}
							// Only add if actualy downloaded correctly
							if(file.isSaved()) {
								existingEpisodes.add(entry.getId());
							}
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "Failed to get podcasts for " + id + " on " + Util.getServerName(context, instance));
				}
			}

			// Make sure there are is at least one change before re-syncing
			if(updated) {
				FileUtil.serialize(context, podcastList, SyncUtil.getPodcastSyncFile(context, instance));
			}
		} catch(Exception e) {
			Log.w(TAG, "Failed to get podcasts for " + Util.getServerName(context, instance));
		}
	}
}
