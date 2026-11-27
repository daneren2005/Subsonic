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
	Copyright 2014 (C) Scott Jackson
*/

package github.paroj.dsub2000.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.view.KeyEvent;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.activity.SubsonicActivity;
import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.provider.DSubWidgetProvider;
import github.paroj.dsub2000.service.DownloadFile;
import github.paroj.dsub2000.service.DownloadService;

public final class Notifications {
	private static final String TAG = Notifications.class.getSimpleName();

	// Notification IDs.
	public static final int NOTIFICATION_ID_PLAYING = 100;
	public static final int NOTIFICATION_ID_DOWNLOADING = 102;
	public static final int NOTIFICATION_ID_SHUT_GOOGLE_UP = 103;
	public static final String NOTIFICATION_SYNC_GROUP = "github.paroj.dsub2000.sync";

	private static boolean playShowing = false;
	private static boolean downloadShowing = false;
	private static boolean downloadForeground = false;
	private static boolean persistentPlayingShowing = false;

	private static NotificationChannel playingChannel;
	private static NotificationChannel downloadingChannel;
	private static NotificationChannel syncChannel;

	private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();

	@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
	public static Notification buildPlayingNotification(final Context context, final DownloadService downloadService, androidx.media3.session.MediaSession session, MusicDirectory.Entry song) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getPlayingNotificationChannel(context);
		}

		final boolean playing = downloadService.getPlayerState() == PlayerState.STARTED;
		boolean isSingle = downloadService.isCurrentPlayingSingle();

		androidx.media3.session.MediaStyleNotificationHelper.MediaStyle mediaStyle =
				new androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session);

		if (isSingle) {
			mediaStyle.setShowActionsInCompactView(1);
		} else {
			mediaStyle.setShowActionsInCompactView(0, 2, 4);
		}

		String title = song.getTitle();
		String artist = song.getDisplayArtist();
		String album = song.getAlbum();

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "now-playing-channel")
			.setSmallIcon(R.drawable.stat_notify_playing)
			.setContentTitle(title)
			.setContentText(artist + " - " + album)
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			.setStyle(mediaStyle);

		// Set the album art.
		try {
			ImageLoader imageLoader = SubsonicActivity.getStaticImageLoader(context);
			if(imageLoader != null) {
				Bitmap bitmap = imageLoader.getCachedImage(context, song, false);

				if(bitmap != null) {
					notificationBuilder.setLargeIcon(bitmap);
				}
			}
		} catch (Exception x) {
			Log.w(TAG, "Failed to get notification cover art", x);
		}

		PendingIntent prevIntent = null;
		PendingIntent nextIntent = null;
		if (!isSingle) {
			prevIntent = getMediaPendingIntent("KEYCODE_MEDIA_PREVIOUS", context);
			nextIntent = getMediaPendingIntent("KEYCODE_MEDIA_NEXT", context);
		}

		PendingIntent rewindIntent = getMediaPendingIntent("KEYCODE_MEDIA_REWIND", context);
		PendingIntent playIntent = getMediaPendingIntent(playing ? "KEYCODE_MEDIA_PLAY_PAUSE" : "KEYCODE_MEDIA_START", context);
		PendingIntent forwardIntent = getMediaPendingIntent("KEYCODE_MEDIA_FAST_FORWARD", context);

		if (!isSingle) {
			notificationBuilder.addAction(R.drawable.ic_baseline_skip_previous_32, "Previous", prevIntent);
		}

		notificationBuilder.addAction(R.drawable.ic_baseline_fast_rewind_32, "Rewind", rewindIntent);
		notificationBuilder.addAction(playing ? R.drawable.ic_baseline_pause_48 : R.drawable.ic_baseline_play_arrow_48, playing ? "Pause" : "Play", playIntent);
		notificationBuilder.addAction(R.drawable.ic_baseline_fast_forward_32, "Forward", forwardIntent);

		if (!isSingle) {
			notificationBuilder.addAction(R.drawable.ic_baseline_skip_next_32, "Next", nextIntent);
		}

		Notification notification = notificationBuilder.build();

		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, /* flags */ PendingIntent.FLAG_IMMUTABLE);
		return notification;
	}

	/** Must run on the main looper before the session posts a playback notification. */
	public static void playbackNotificationShowing(final Context context, final DownloadService downloadService, final Handler handler, boolean foreground) {
		playShowing = foreground;
		persistentPlayingShowing = !foreground;
		if(foreground && downloadForeground && downloadShowing) {
			// The playback notification takes over foreground; keep the download one as a plain notification.
			downloadForeground = false;
			stopForeground(downloadService, true);
			showDownloadingNotification(context, downloadService, handler, downloadService.getCurrentDownloading(), downloadService.getBackgroundDownloads().size());
		}
		DSubWidgetProvider.notifyInstances(context, downloadService, foreground);
	}

	private static PendingIntent getMediaPendingIntent(String action, Context context) {
		Intent intent = new Intent(action);
		intent.setComponent(new ComponentName(context, DownloadService.class));

		int keyCode = 0;
		switch (action) {
			case "KEYCODE_MEDIA_PREVIOUS":
				keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
				break;

			case "KEYCODE_MEDIA_REWIND":
				keyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
				break;

			case "KEYCODE_MEDIA_PLAY_PAUSE":
				keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
				break;

			case "KEYCODE_MEDIA_START":
				keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
				break;

			case "KEYCODE_MEDIA_NEXT":
				keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
				break;

			case "KEYCODE_MEDIA_FAST_FORWARD":
				keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
				break;

			case "KEYCODE_MEDIA_STOP":
				keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
				break;
		}

		if (keyCode != 0) {
			intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
		}

		return PendingIntent.getService(context, 0, intent, /* flags */ PendingIntent.FLAG_IMMUTABLE);
	}

	public static void hidePlayingNotification(final Context context, final DownloadService downloadService, Handler handler) {
		playShowing = false;

		// Remove notification and remove the service from the foreground
		handler.post(new Runnable() {
			@Override
			public void run() {
				stopForeground(downloadService, true);

				if(persistentPlayingShowing) {
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.cancel(NOTIFICATION_ID_PLAYING);
					persistentPlayingShowing = false;
				}
			}
		});

		// Get downloadNotification in foreground if playing
		if(downloadShowing) {
			showDownloadingNotification(context, downloadService, handler, downloadService.getCurrentDownloading(), downloadService.getBackgroundDownloads().size());
		}

		// Update widget
		DSubWidgetProvider.notifyInstances(context, downloadService, false);
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static NotificationChannel getPlayingNotificationChannel(Context context) {
		if(playingChannel == null) {
			playingChannel = new NotificationChannel("now-playing-channel", "Now Playing", NotificationManager.IMPORTANCE_LOW);
			playingChannel.setDescription("Now playing notification");

			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(playingChannel);
		}

		return playingChannel;
	}

	public static void showDownloadingNotification(final Context context, final DownloadService downloadService, Handler handler, DownloadFile file, int size) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getDownloadingNotificationChannel(context);
		}

		Intent cancelIntent = new Intent(context, DownloadService.class);
		cancelIntent.setAction(DownloadService.CANCEL_DOWNLOADS);
		PendingIntent cancelPI = PendingIntent.getService(context, 0, cancelIntent, /* flags */ PendingIntent.FLAG_IMMUTABLE);

		String currentDownloading, currentSize;
		if(file != null) {
			currentDownloading = file.getSong().getTitle();
			currentSize = Util.formatLocalizedBytes(file.getEstimatedSize(), context);
		} else {
			currentDownloading = "none";
			currentSize = "0";
		}

		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(context)
				.setSmallIcon(android.R.drawable.stat_sys_download)
				.setContentTitle(context.getResources().getString(R.string.download_downloading_title, size))
				.setContentText(context.getResources().getString(R.string.download_downloading_summary, currentDownloading))
				.setStyle(new NotificationCompat.BigTextStyle()
						.bigText(context.getResources().getString(R.string.download_downloading_summary_expanded, currentDownloading, currentSize)))
				.setProgress(10, 5, true)
				.setOngoing(true)
				.addAction(R.drawable.notification_close,
						context.getResources().getString(R.string.common_cancel),
						cancelPI)
				.setChannelId("downloading-channel");

		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		builder.setContentIntent(PendingIntent.getActivity(context, 2, notificationIntent, /* flags */ PendingIntent.FLAG_IMMUTABLE));

		final Notification notification = builder.build();
		downloadShowing = true;
		if(playShowing) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(NOTIFICATION_ID_DOWNLOADING, notification);
		} else {
			downloadForeground = true;
			handler.post(new Runnable() {
				@Override
				public void run() {
					startForeground(downloadService, NOTIFICATION_ID_DOWNLOADING, notification);
				}
			});
		}

	}
	public static void hideDownloadingNotification(final Context context, final DownloadService downloadService, Handler handler) {
		downloadShowing = false;
		if(playShowing) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(NOTIFICATION_ID_DOWNLOADING);
		} else {
			downloadForeground = false;
			handler.post(new Runnable() {
				@Override
				public void run() {
					stopForeground(downloadService, true);
				}
			});
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static NotificationChannel getDownloadingNotificationChannel(Context context) {
		if(downloadingChannel == null) {
			downloadingChannel = new NotificationChannel("downloading-channel", "Downloading Notification", NotificationManager.IMPORTANCE_LOW);
			downloadingChannel.setDescription("Ongoing downloading notification to keep the service alive");

			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(downloadingChannel);
		}

		return downloadingChannel;
	}

	@TargetApi(Build.VERSION_CODES.O)
	public static void shutGoogleUpNotification(final DownloadService downloadService) {
		// On Android O+, service crashes if startForeground isn't called within 5 seconds of starting
		getDownloadingNotificationChannel(downloadService);

		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(downloadService)
				.setSmallIcon(android.R.drawable.stat_sys_download)
				.setContentTitle(downloadService.getResources().getString(R.string.download_downloading_title, 0))
				.setContentText(downloadService.getResources().getString(R.string.download_downloading_summary, "Temp"))
				.setChannelId("downloading-channel");

		final Notification notification = builder.build();
		startForeground(downloadService, NOTIFICATION_ID_SHUT_GOOGLE_UP, notification);
		stopForeground(downloadService, true);
	}

	public static void showSyncNotification(final Context context, int stringId, String extra) {
		showSyncNotification(context, stringId, extra, null);
	}
	public static void showSyncNotification(final Context context, int stringId, String extra, String extraId) {
		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_SYNC_NOTIFICATION, true)) {
			if(extra == null) {
				extra = "";
			}

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				getSyncNotificationChannel(context);
			}

			NotificationCompat.Builder builder;
			builder = new NotificationCompat.Builder(context)
					.setSmallIcon(R.drawable.stat_notify_sync)
					.setContentTitle(context.getResources().getString(stringId))
					.setContentText(extra)
					.setStyle(new NotificationCompat.BigTextStyle().bigText(extra.replace(", ", "\n")))
					.setOngoing(false)
					.setGroup(NOTIFICATION_SYNC_GROUP)
					.setPriority(NotificationCompat.PRIORITY_LOW)
					.setChannelId("sync-channel")
					.setAutoCancel(true);

			Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			String tab = null, type = null;
			switch(stringId) {
				case R.string.sync_new_albums:
					type = "newest";
					break;
				case R.string.sync_new_playlists:
					tab = "Playlist";
					break;
				case R.string.sync_new_podcasts:
					tab = "Podcast";
					break;
				case R.string.sync_new_starred:
					type = "starred";
					break;
			}
			if(tab != null) {
				notificationIntent.putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, tab);
			}
			if(type != null) {
				notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
			}
			if(extraId != null) {
				notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_ID, extraId);
			}

			builder.setContentIntent(PendingIntent.getActivity(context, stringId, notificationIntent, /* flags */ PendingIntent.FLAG_IMMUTABLE));

			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(stringId, builder.build());
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static NotificationChannel getSyncNotificationChannel(Context context) {
		if(syncChannel == null) {
			syncChannel = new NotificationChannel("sync-channel", "Sync Notifications", NotificationManager.IMPORTANCE_MIN);
			syncChannel.setDescription("Sync notifications");

			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(syncChannel);
		}

		return syncChannel;
	}

	private static void startForeground(DownloadService downloadService, int notificationId, Notification notification) {
		try {
			downloadService.startForeground(notificationId, notification);
			downloadService.setIsForeground(true);
		} catch (Exception e) {
			if (Build.VERSION.SDK_INT >= 31 && e instanceof android.app.ForegroundServiceStartNotAllowedException) {
				Log.w(TAG, "ForegroundServiceStartNotAllowedException: " + e.getMessage());
			} else {
				throw e;
			}
		}
	}

	private static void stopForeground(DownloadService downloadService, boolean removeNotification) {
		downloadService.stopForeground(removeNotification);
		downloadService.setIsForeground(false);
	}
}
