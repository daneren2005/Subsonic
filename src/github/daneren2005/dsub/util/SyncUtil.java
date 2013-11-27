package github.daneren2005.dsub.util;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott on 11/24/13.
 */
public final class SyncUtil {
	private static String TAG = SyncUtil.class.getSimpleName();
	private static ArrayList<String> syncedPlaylists;
	private static ArrayList<SyncSet> syncedPodcasts;

	// Playlist sync
	public static boolean isSyncedPlaylist(Context context, String playlistId) {
		if(syncedPlaylists == null) {
			syncedPlaylists = getSyncedPlaylists(context);
		}
		return syncedPlaylists.contains(playlistId);
	}
	public static ArrayList<String> getSyncedPlaylists(Context context) {
		return getSyncedPlaylists(context, Util.getActiveServer(context));
	}
	public static ArrayList<String> getSyncedPlaylists(Context context, int instance) {
		ArrayList<String> playlists = FileUtil.deserialize(context, getPlaylistSyncFile(context, instance), ArrayList.class);
		if(playlists == null) {
			playlists = new ArrayList<String>();
		}
		return playlists;
	}
	public static void addSyncedPlaylist(Context context, String playlistId) {
		String playlistFile = getPlaylistSyncFile(context);
		ArrayList<String> playlists = getSyncedPlaylists(context);
		if(!playlists.contains(playlistId)) {
			playlists.add(playlistId);
		}
		FileUtil.serialize(context, playlists, playlistFile);
		syncedPlaylists = playlists;
	}
	public static void removeSyncedPlaylist(Context context, String playlistId) {
		String playlistFile = getPlaylistSyncFile(context);
		ArrayList<String> playlists = getSyncedPlaylists(context);
		if(playlists.contains(playlistId)) {
			playlists.remove(playlistId);
			FileUtil.serialize(context, playlists, playlistFile);
			syncedPlaylists = playlists;
		}
	}
	public static String getPlaylistSyncFile(Context context) {
		int instance = Util.getActiveServer(context);
		return getPlaylistSyncFile(context, instance);
	}
	public static String getPlaylistSyncFile(Context context, int instance) {
		return "sync-playlist-" + (Util.getRestUrl(context, null, instance)).hashCode() + ".ser";
	}

	// Podcast sync
	public static boolean isSyncedPodcast(Context context, String podcastId) {
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
		String podcastFile = getPodcastSyncFile(context);
		ArrayList<SyncSet> podcasts = getSyncedPodcasts(context);
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
		return "sync-podcast-" + (Util.getRestUrl(context, null, instance)).hashCode() + ".ser";
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
