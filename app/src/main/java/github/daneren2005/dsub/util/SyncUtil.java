package github.daneren2005.dsub.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;

/**
 * Created by Scott on 11/24/13.
 */
public final class SyncUtil {
	private static String TAG = SyncUtil.class.getSimpleName();
	private static ArrayList<SyncSet> syncedPlaylists;
	private static ArrayList<SyncSet> syncedPodcasts;
	private static String url;

	private static void checkRestURL(Context context) {
		int instance = Util.getActiveServer(context);
		String newURL = Util.getRestUrl(context, null, instance, false);
		if(url == null || !url.equals(newURL)) {
			syncedPlaylists = null;
			syncedPodcasts = null;
			url = newURL;
		}
	}

	// Playlist sync
	public static boolean isSyncedPlaylist(Context context, String playlistId) {
		checkRestURL(context);
		if(syncedPlaylists == null) {
			syncedPlaylists = getSyncedPlaylists(context);
		}
		return syncedPlaylists.contains(new SyncSet(playlistId));
	}
	public static ArrayList<SyncSet> getSyncedPlaylists(Context context) {
		return getSyncedPlaylists(context, Util.getActiveServer(context));
	}
	public static ArrayList<SyncSet> getSyncedPlaylists(Context context, int instance) {
		String syncFile = getPlaylistSyncFile(context, instance);
		ArrayList<SyncSet> playlists = FileUtil.deserializeCompressed(context, syncFile, ArrayList.class);
		if(playlists == null) {
			playlists = new ArrayList<SyncSet>();

			// Try to convert old style into new style
			ArrayList<String> oldPlaylists = FileUtil.deserialize(context, syncFile, ArrayList.class);
			// If exists, time to convert!
			if(oldPlaylists != null) {
				for(String id: oldPlaylists) {
					playlists.add(new SyncSet(id));
				}

				FileUtil.serializeCompressed(context, playlists, syncFile);
			}
		}
		return playlists;
	}
	public static void setSyncedPlaylists(Context context, int instance, ArrayList<SyncSet> playlists) {
		FileUtil.serializeCompressed(context, playlists, getPlaylistSyncFile(context, instance));
	}
	public static void addSyncedPlaylist(Context context, String playlistId) {
		String playlistFile = getPlaylistSyncFile(context);
		ArrayList<SyncSet> playlists = getSyncedPlaylists(context);
		SyncSet set = new SyncSet(playlistId);
		if(!playlists.contains(set)) {
			playlists.add(set);
		}
		FileUtil.serializeCompressed(context, playlists, playlistFile);
		syncedPlaylists = playlists;
	}
	public static void removeSyncedPlaylist(Context context, String playlistId) {
		int instance = Util.getActiveServer(context);
		removeSyncedPlaylist(context, playlistId, instance);
	}
	public static void removeSyncedPlaylist(Context context, String playlistId, int instance) {
		String playlistFile = getPlaylistSyncFile(context, instance);
		ArrayList<SyncSet> playlists = getSyncedPlaylists(context, instance);
		SyncSet set = new SyncSet(playlistId);
		if(playlists.contains(set)) {
			playlists.remove(set);
			FileUtil.serializeCompressed(context, playlists, playlistFile);
			syncedPlaylists = playlists;
		}
	}
	public static String getPlaylistSyncFile(Context context) {
		int instance = Util.getActiveServer(context);
		return getPlaylistSyncFile(context, instance);
	}
	public static String getPlaylistSyncFile(Context context, int instance) {
		return "sync-playlist-" + (Util.getRestUrl(context, null, instance, false)).hashCode() + ".ser";
	}

	// Podcast sync
	public static boolean isSyncedPodcast(Context context, String podcastId) {
		checkRestURL(context);
		if(syncedPodcasts == null) {
			syncedPodcasts = getSyncedPodcasts(context);
		}
		return syncedPodcasts.contains(new SyncSet(podcastId));
	}
	public static ArrayList<SyncSet> getSyncedPodcasts(Context context) {
		return getSyncedPodcasts(context, Util.getActiveServer(context));
	}
	public static ArrayList<SyncSet> getSyncedPodcasts(Context context, int instance) {
		ArrayList<SyncSet> podcasts = FileUtil.deserialize(context, getPodcastSyncFile(context, instance), ArrayList.class);
		if(podcasts == null) {
			podcasts = new ArrayList<SyncSet>();
		}
		return podcasts;
	}
	public static void addSyncedPodcast(Context context, String podcastId, List<String> synced) {
		String podcastFile = getPodcastSyncFile(context);
		ArrayList<SyncSet> podcasts = getSyncedPodcasts(context);
		SyncSet set = new SyncSet(podcastId, synced);
		if(!podcasts.contains(set)) {
			podcasts.add(set);
		}
		FileUtil.serialize(context, podcasts, podcastFile);
		syncedPodcasts = podcasts;
	}
	public static void removeSyncedPodcast(Context context, String podcastId) {
		removeSyncedPodcast(context, podcastId, Util.getActiveServer(context));
	}
	public static void removeSyncedPodcast(Context context, String podcastId, int instance) {
		String podcastFile = getPodcastSyncFile(context, instance);
		ArrayList<SyncSet> podcasts = getSyncedPodcasts(context, instance);
		SyncSet set = new SyncSet(podcastId);
		if(podcasts.contains(set)) {
			podcasts.remove(set);
			FileUtil.serialize(context, podcasts, podcastFile);
			syncedPodcasts = podcasts;
		}
	}
	public static String getPodcastSyncFile(Context context) {
		int instance = Util.getActiveServer(context);
		return getPodcastSyncFile(context, instance);
	}
	public static String getPodcastSyncFile(Context context, int instance) {
		return "sync-podcast-" + (Util.getRestUrl(context, null, instance, false)).hashCode() + ".ser";
	}
	
	// Starred
	public static ArrayList<String> getSyncedStarred(Context context, int instance) {
		ArrayList<String> list = FileUtil.deserializeCompressed(context, getStarredSyncFile(context, instance), ArrayList.class);
		if(list == null) {
			list = new ArrayList<String>();
		}
		return list;
	}
	public static void setSyncedStarred(ArrayList<String> syncedList, Context context, int instance) {
		FileUtil.serializeCompressed(context, syncedList, SyncUtil.getStarredSyncFile(context, instance));
	}
	public static String getStarredSyncFile(Context context, int instance) {
		return "sync-starred-" + (Util.getRestUrl(context, null, instance, false)).hashCode() + ".ser";
	}
	
	// Most Recently Added
	public static ArrayList<String> getSyncedMostRecent(Context context, int instance) {
		ArrayList<String> list = FileUtil.deserialize(context, getMostRecentSyncFile(context, instance), ArrayList.class);
		if(list == null) {
			list = new ArrayList<String>();
		}
		return list;
	}
	public static void removeMostRecentSyncFiles(Context context) {
		int total = Util.getServerCount(context);
		for(int i = 0; i < total; i++) {
			File file = new File(context.getCacheDir(), getMostRecentSyncFile(context, i));
			file.delete();
		}
	}
	public static String getMostRecentSyncFile(Context context, int instance) {
		return "sync-most_recent-" + (Util.getRestUrl(context, null, instance, false)).hashCode() + ".ser";
	}

	public static String joinNames(List<String> names) {
		StringBuilder builder = new StringBuilder();
		for (String val : names) {
			builder.append(val).append(", ");
		}
		builder.setLength(builder.length() - 2);
		return builder.toString();
	}

	public static class SyncSet implements Serializable {
		public String id;
		public List<String> synced;

		protected SyncSet() {

		}
		public SyncSet(String id) {
			this.id = id;
		}
		public SyncSet(String id, List<String> synced) {
			this.id = id;
			this.synced = synced;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof SyncSet) {
				return this.id.equals(((SyncSet)obj).id);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}
