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
import github.daneren2005.dsub.view.UpdateView;

public final class Notifications {
	private static final String TAG = Notifications.class.getSimpleName();

	// Notification IDs.
	public static final int NOTIFICATION_ID_PLAYING = 100;
	public static final int NOTIFICATION_ID_DOWNLOADING = 102;
	public static final int NOTIFICATION_ID_SHUT_GOOGLE_UP = 103;
	public static final String NOTIFICATION_SYNC_GROUP = "github.daneren2005.dsub.sync";

	private static boolean playShowing = false;
	private static boolean downloadShowing = false;
	private static boolean downloadForeground = false;
	private static boolean persistentPlayingShowing = false;

	private static NotificationChannel playingChannel;
	private static NotificationChannel downloadingChannel;
	private static NotificationChannel syncChannel;

	private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();

	public static void showPlayingNotification(final Context context, final DownloadService downloadService, final Handler handler, MusicDirectory.Entry song) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getPlayingNotificationChannel(context);
		}

		// Set the icon, scrolling text and timestamp
		final Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.stat_notify_playing)
				.setTicker(song.getTitle())
				.setWhen(System.currentTimeMillis())
				.setChannelId("now-playing-channel")
				.build();

		final boolean playing = downloadService.getPlayerState() == PlayerState.STARTED;
		if(playing) {
			notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		}
		boolean remote = downloadService.isRemoteEnabled();
		boolean isSingle = downloadService.isCurrentPlayingSingle();
		boolean shouldFastForward = downloadService.shouldFastForward();
		if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN){
			RemoteViews expandedContentView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
			setupViews(expandedContentView ,context, song, true, playing, remote, isSingle, shouldFastForward);
			notification.bigContentView = expandedContentView;
			notification.priority = Notification.PRIORITY_HIGH;
		}
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			notification.visibility = Notification.VISIBILITY_PUBLIC;

			if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_HEADS_UP_NOTIFICATION, false) && !UpdateView.hasActiveActivity()) {
				notification.vibrate = new long[0];
			}
		}

		RemoteViews smallContentView = new RemoteViews(context.getPackageName(), R.layout.notification);
		setupViews(smallContentView, context, song, false, playing, remote, isSingle, shouldFastForward);
		notification.contentView = smallContentView;

		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		playShowing = true;
		if(downloadForeground && downloadShowing) {
			downloadForeground = false;
			handler.post(new Runnable() {
				@Override
				public void run() {
					stopForeground(downloadService, true);
					showDownloadingNotification(context, downloadService, handler, downloadService.getCurrentDownloading(), downloadService.getBackgroundDownloads().size());

					try {
						startForeground(downloadService, NOTIFICATION_ID_PLAYING, notification);
					} catch(Exception e) {
						Log.e(TAG, "Failed to start notifications after stopping foreground download");
					}
				}
			});
		} else {
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (playing) {
						try {
							startForeground(downloadService, NOTIFICATION_ID_PLAYING, notification);
						} catch(Exception e) {
							Log.e(TAG, "Failed to start notifications while playing");
						}
					} else {
						playShowing = false;
						persistentPlayingShowing = true;
						NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
						stopForeground(downloadService, false);

						try {
							notificationManager.notify(NOTIFICATION_ID_PLAYING, notification);
						} catch(Exception e) {
							Log.e(TAG, "Failed to start notifications while paused");
						}
					}
				}
			});
		}

		// Update widget
		DSubWidgetProvider.notifyInstances(context, downloadService, playing);
	}

	private static void setupViews(RemoteViews rv, Context context, MusicDirectory.Entry song, boolean expanded, boolean playing, boolean remote, boolean isSingleFile, boolean shouldFastForward) {
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
				imageLoader.setNowPlayingSmall(bitmap);
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

		boolean persistent = Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_PERSISTENT_NOTIFICATION, false);
		if(persistent) {
			if(expanded) {
				rv.setImageViewResource(R.id.control_pause, playing ? R.drawable.notification_pause : R.drawable.notification_start);

				if(shouldFastForward) {
					rv.setImageViewResource(R.id.control_previous, R.drawable.notification_rewind);
					rv.setImageViewResource(R.id.control_next, R.drawable.notification_fastforward);
				} else {
					rv.setImageViewResource(R.id.control_previous, R.drawable.notification_backward);
					rv.setImageViewResource(R.id.control_next, R.drawable.notification_forward);
				}
			} else {
				rv.setImageViewResource(R.id.control_previous, playing ? R.drawable.notification_pause : R.drawable.notification_start);
				if(shouldFastForward) {
					rv.setImageViewResource(R.id.control_pause, R.drawable.notification_fastforward);
				} else {
					rv.setImageViewResource(R.id.control_pause, R.drawable.notification_forward);
				}
				rv.setImageViewResource(R.id.control_next, R.drawable.notification_close);
			}
		} else if(shouldFastForward) {
			rv.setImageViewResource(R.id.control_previous, R.drawable.notification_rewind);
			rv.setImageViewResource(R.id.control_next, R.drawable.notification_fastforward);
		} else {
			// Necessary for switching back since it appears to re-use the same layout
			rv.setImageViewResource(R.id.control_previous, R.drawable.notification_backward);
			rv.setImageViewResource(R.id.control_next, R.drawable.notification_forward);
		}

		// Create actions for media buttons
		int previous = 0, pause = 0, next = 0, close = 0, rewind = 0, fastForward = 0;
		if (expanded) {
			pause = R.id.control_pause;

			if (shouldFastForward) {
				rewind = R.id.control_previous;
				fastForward = R.id.control_next;
			} else {
				previous = R.id.control_previous;
				next = R.id.control_next;
			}

			if (remote || persistent) {
				close = R.id.notification_close;
				rv.setViewVisibility(close, View.VISIBLE);
			}
		} else {
			if (persistent) {
				pause = R.id.control_previous;
				if(shouldFastForward) {
					fastForward = R.id.control_pause;
				} else {
					next = R.id.control_pause;
				}
				close = R.id.control_next;
			} else {
				if (shouldFastForward) {
					rewind = R.id.control_previous;
					fastForward = R.id.control_next;
				} else {
					previous = R.id.control_previous;
					next = R.id.control_next;
				}

				pause = R.id.control_pause;
			}
		}

		if(isSingleFile) {
			if(previous > 0) {
				rv.setViewVisibility(previous, View.GONE);
				previous = 0;
			}
			if(rewind > 0) {
				rv.setViewVisibility(rewind, View.GONE);
				rewind = 0;
			}

			if(next > 0) {
				rv.setViewVisibility(next, View.GONE);
				next = 0;
			}

			if(fastForward > 0) {
				rv.setViewVisibility(fastForward, View.GONE);
				fastForward = 0;
			}
		}

		PendingIntent pendingIntent;
		if(previous > 0) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_PREVIOUS");
			prevIntent.setComponent(new ComponentName(context, DownloadService.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(previous, pendingIntent);
		}
		if(rewind > 0) {
			Intent rewindIntent = new Intent("KEYCODE_MEDIA_REWIND");
			rewindIntent.setComponent(new ComponentName(context, DownloadService.class));
			rewindIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND));
			pendingIntent = PendingIntent.getService(context, 0, rewindIntent, 0);
			rv.setOnClickPendingIntent(rewind, pendingIntent);
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
		if(fastForward > 0) {
			Intent fastForwardIntent = new Intent("KEYCODE_MEDIA_FAST_FORWARD");
			fastForwardIntent.setComponent(new ComponentName(context, DownloadService.class));
			fastForwardIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));
			pendingIntent = PendingIntent.getService(context, 0, fastForwardIntent, 0);
			rv.setOnClickPendingIntent(fastForward, pendingIntent);
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
		PendingIntent cancelPI = PendingIntent.getService(context, 0, cancelIntent, 0);

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
		builder.setContentIntent(PendingIntent.getActivity(context, 2, notificationIntent, 0));

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

			builder.setContentIntent(PendingIntent.getActivity(context, stringId, notificationIntent, 0));

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
		downloadService.startForeground(notificationId, notification);
		downloadService.setIsForeground(true);
	}

	private static void stopForeground(DownloadService downloadService, boolean removeNotification) {
		downloadService.stopForeground(removeNotification);
		downloadService.setIsForeground(false);
	}
}
