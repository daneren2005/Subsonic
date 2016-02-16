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
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.Notifications;
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
	public void onExecuteSync(Context context, int instance) throws NetworkNotValidException {
		ArrayList<SyncSet> podcastList = SyncUtil.getSyncedPodcasts(context, instance);

		try {
			// Only refresh if syncs exist (implies a server where supported)
			if(podcastList.size() > 0) {
				// Just update podcast listings so user doesn't have to
				musicService.getPodcastChannels(true, context, null);

				// Refresh podcast listings before syncing
				musicService.refreshPodcasts(context, null);
			}

			List<String> updated = new ArrayList<String>();
			String updatedId = null;
			for(int i = 0; i < podcastList.size(); i++) {
				SyncSet set = podcastList.get(i);
				String id = set.id;
				List<String> existingEpisodes = set.synced;
				try {
					MusicDirectory podcasts = musicService.getPodcastEpisodes(true, id, context, null);

					for(MusicDirectory.Entry entry: podcasts.getChildren()) {
						// Make sure podcast is valid and not already synced
						if(entry.getId() != null && "completed".equals(((PodcastEpisode)entry).getStatus()) && !existingEpisodes.contains(entry.getId())) {
							DownloadFile file = new DownloadFile(context, entry, false);
							while(!file.isCompleteFileAvailable() && !file.isFailedMax()) {
								throwIfNetworkInvalid();
								file.downloadNow(musicService);
							}
							// Only add if actualy downloaded correctly
							if(file.isCompleteFileAvailable()) {
								existingEpisodes.add(entry.getId());
								if(!updated.contains(podcasts.getName())) {
									updated.add(podcasts.getName());
									if(updatedId == null) {
										updatedId = podcasts.getId();
									}
								}
							}
						}
					}
				}  catch(SubsonicRESTException e) {
					if(e.getCode() == 70) {
						SyncUtil.removeSyncedPodcast(context, id, instance);
						Log.i(TAG, "Unsync deleted podcasts for " + id + " on " + Util.getServerName(context, instance));
					}
				} catch (Exception e) {
					Log.w(TAG, "Failed to get podcasts for " + id + " on " + Util.getServerName(context, instance));
				}
			}

			// Make sure there are is at least one change before re-syncing
			if(updated.size() > 0) {
				FileUtil.serialize(context, podcastList, SyncUtil.getPodcastSyncFile(context, instance));
				Notifications.showSyncNotification(context, R.string.sync_new_podcasts, SyncUtil.joinNames(updated), updatedId);
			}
		} catch(Exception e) {
			Log.w(TAG, "Failed to get podcasts for " + Util.getServerName(context, instance));
		}
	}
}
