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

package github.daneren2005.dsub.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.provider.DSubWidgetProvider;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;

public final class Notifications {
	private static final String TAG = Notifications.class.getSimpleName();

	// Notification IDs.
	public static final int NOTIFICATION_ID_PLAYING = 100;
	public static final int NOTIFICATION_ID_DOWNLOADING = 102;
	public static final String NOTIFICATION_SYNC_GROUP = "github.daneren2005.dsub.sync";

	private static boolean playShowing = false;
	private static boolean downloadShowing = false;
	private static boolean downloadForeground = false;

	private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();

	public static void showPlayingNotification(final Context context, final DownloadService downloadService, final Handler handler, MusicDirectory.Entry song) {
		// Set the icon, scrolling text and timestamp
		final Notification notification = new Notification(R.drawable.stat_notify_playing, song.getTitle(), System.currentTimeMillis());
		notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		boolean playing = downloadService.getPlayerState() == PlayerState.STARTED;
		boolean remote = downloadService.isRemoteEnabled();
		if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN){
			RemoteViews expandedContentView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
			setupViews(expandedContentView ,context, song, true, playing, remote);
			notification.bigContentView = expandedContentView;
			notification.priority = Notification.PRIORITY_HIGH;
		}

		RemoteViews smallContentView = new RemoteViews(context.getPackageName(), R.layout.notification);
		setupViews(smallContentView, context, song, false, playing, remote);
		notification.contentView = smallContentView;

		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		playShowing = true;
		if(downloadForeground && downloadShowing) {
			downloadForeground = false;
			handler.post(new Runnable() {
				@Override
				public void run() {
					downloadService.stopForeground(true);
					showDownloadingNotification(context, downloadService, handler, downloadService.getCurrentDownloading(), downloadService.getBackgroundDownloads().size());
					downloadService.startForeground(NOTIFICATION_ID_PLAYING, notification);
				}
			});
		} else {
			handler.post(new Runnable() {
				@Override
				public void run() {
					downloadService.startForeground(NOTIFICATION_ID_PLAYING, notification);
				}
			});
		}

		// Update widget
		DSubWidgetProvider.notifyInstances(context, downloadService, playing);
	}

	private static void setupViews(RemoteViews rv, Context context, MusicDirectory.Entry song, boolean expanded, boolean playing, boolean remote){

		// Use the same text for the ticker and the expanded notification
		String title = song.getTitle();
		String arist = song.getArtist();
		String album = song.getAlbum();

		// Set the album art.
		try {
			ImageLoader imageLoader = SubsonicActivity.getStaticImageLoader(context);
			Bitmap bitmap = null;
			if(imageLoader != null) {
				bitmap = imageLoader.getCachedImage(context, song, false);
			}
			if (bitmap == null) {
				// set default album art
				rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
			} else {
				rv.setImageViewBitmap(R.id.notification_image, bitmap);
			}
		} catch (Exception x) {
			Log.w(TAG, "Failed to get notification cover art", x);
			rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
		}

		// set the text for the notifications
		rv.setTextViewText(R.id.notification_title, title);
		rv.setTextViewText(R.id.notification_artist, arist);
		rv.setTextViewText(R.id.notification_album, album);

		Pair<Integer, Integer> colors = getNotificationTextColors(context);
		if (colors.getFirst() != null) {
			rv.setTextColor(R.id.notification_title, colors.getFirst());
		}
		if (colors.getSecond() != null) {
			rv.setTextColor(R.id.notification_artist, colors.getSecond());
		}

		boolean persistent = Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_PERSISTENT_NOTIFICATION, false);
		if(persistent && !expanded) {
			rv.setImageViewResource(R.id.control_previous, playing ? R.drawable.notification_pause : R.drawable.notification_play);
			rv.setImageViewResource(R.id.control_pause, R.drawable.notification_next);
			rv.setImageViewResource(R.id.control_next, R.drawable.notification_close);
		}

		// Create actions for media buttons
		PendingIntent pendingIntent;
		int previous = 0, pause = 0, next = 0, close = 0;
		if(persistent && !expanded) {
			pause = R.id.control_previous;
			next = R.id.control_pause;
			close = R.id.control_next;
		} else {
			previous = R.id.control_previous;
			pause = R.id.control_pause;
			next = R.id.control_next;
		}

		if((remote || persistent) && close == 0) {
			close = R.id.notification_close;
			rv.setViewVisibility(close, View.VISIBLE);
		}

		if(previous > 0) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_PREVIOUS");
			prevIntent.setComponent(new ComponentName(context, DownloadService.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(previous, pendingIntent);
		}
		if(pause > 0) {
			if(playing) {
				Intent pauseIntent = new Intent("KEYCODE_MEDIA_PLAY_PAUSE");
				pauseIntent.setComponent(new ComponentName(context, DownloadService.class));
				pauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
				pendingIntent = PendingIntent.getService(context, 0, pauseIntent, 0);
				rv.setOnClickPendingIntent(pause, pendingIntent);
			} else {
				Intent prevIntent = new Intent("KEYCODE_MEDIA_START");
				prevIntent.setComponent(new ComponentName(context, DownloadService.class));
				prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
				pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
				rv.setOnClickPendingIntent(pause, pendingIntent);
			}
		}
		if(next > 0) {
			Intent nextIntent = new Intent("KEYCODE_MEDIA_NEXT");
			nextIntent.setComponent(new ComponentName(context, DownloadService.class));
			nextIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
			pendingIntent = PendingIntent.getService(context, 0, nextIntent, 0);
			rv.setOnClickPendingIntent(next, pendingIntent);
		}
		if(close > 0) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_STOP");
			prevIntent.setComponent(new ComponentName(context, DownloadService.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(close, pendingIntent);
		}
	}

	public static void hidePlayingNotification(final Context context, final DownloadService downloadService, Handler handler) {
		playShowing = false;

		// Remove notification and remove the service from the foreground
		handler.post(new Runnable() {
			@Override
			public void run() {
				downloadService.stopForeground(true);
			}
		});

		// Get downloadNotification in foreground if playing
		if(downloadShowing) {
			showDownloadingNotification(context, downloadService, handler, downloadService.getCurrentDownloading(), downloadService.getBackgroundDownloads().size());
		}

		// Update widget
		DSubWidgetProvider.notifyInstances(context, downloadService, false);
	}

	public static void showDownloadingNotification(final Context context, final DownloadService downloadService, Handler handler, DownloadFile file, int size) {
		Intent cancelIntent = new Intent(context, DownloadService.class);
		cancelIntent.setAction(DownloadService.CANCEL_DOWNLOADS);
		PendingIntent cancelPI = PendingIntent.getService(context, 0, cancelIntent, 0);

		String currentDownloading, currentSize;
		if(file != null) {
			currentDownloading = file.getSong().getTitle();
			currentSize = Util.formatBytes(file.getEstimatedSize());
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
						cancelPI);

		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		builder.setContentIntent(PendingIntent.getActivity(context, 1, notificationIntent, 0));

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
					downloadService.startForeground(NOTIFICATION_ID_DOWNLOADING, notification);
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
					downloadService.stopForeground(true);
				}
			});
		}
	}

	public static void showSyncNotification(final Context context, int stringId, String extra) {
		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_SYNC_NOTIFICATION, true)) {
			String content = (extra != null) ? context.getResources().getString(stringId, extra) : context.getResources().getString(stringId);

			NotificationCompat.Builder builder;
			builder = new NotificationCompat.Builder(context)
					.setSmallIcon(R.drawable.stat_notify_sync)
					.setContentTitle(context.getResources().getString(R.string.sync_title))
					.setContentText(content)
					.setStyle(new NotificationCompat.BigTextStyle().bigText(content.replace(", ", "\n")))
					.setOngoing(false)
					.setGroup(NOTIFICATION_SYNC_GROUP)
					.setPriority(NotificationCompat.PRIORITY_LOW);

			Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			builder.setContentIntent(PendingIntent.getActivity(context, 2, notificationIntent, 0));

			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(stringId, builder.build());
		}
	}

	/**
	 * Resolves the default text color for notifications.
	 *
	 * Based on http://stackoverflow.com/questions/4867338/custom-notification-layouts-and-text-colors/7320604#7320604
	 */
	private static Pair<Integer, Integer> getNotificationTextColors(Context context) {
		if (NOTIFICATION_TEXT_COLORS.getFirst() == null && NOTIFICATION_TEXT_COLORS.getSecond() == null) {
			try {
				Notification notification = new Notification();
				String title = "title";
				String content = "content";
				notification.setLatestEventInfo(context, title, content, null);
				LinearLayout group = new LinearLayout(context);
				ViewGroup event = (ViewGroup) notification.contentView.apply(context, group);
				findNotificationTextColors(event, title, content);
				group.removeAllViews();
			} catch (Exception x) {
				Log.w(TAG, "Failed to resolve notification text colors.", x);
			}
		}
		return NOTIFICATION_TEXT_COLORS;
	}

	private static void findNotificationTextColors(ViewGroup group, String title, String content) {
		for (int i = 0; i < group.getChildCount(); i++) {
			if (group.getChildAt(i) instanceof TextView) {
				TextView textView = (TextView) group.getChildAt(i);
				String text = textView.getText().toString();
				if (title.equals(text)) {
					NOTIFICATION_TEXT_COLORS.setFirst(textView.getTextColors().getDefaultColor());
				}
				else if (content.equals(text)) {
					NOTIFICATION_TEXT_COLORS.setSecond(textView.getTextColors().getDefaultColor());
				}
			}
			else if (group.getChildAt(i) instanceof ViewGroup)
				findNotificationTextColors((ViewGroup) group.getChildAt(i), title, content);
		}
	}
}
