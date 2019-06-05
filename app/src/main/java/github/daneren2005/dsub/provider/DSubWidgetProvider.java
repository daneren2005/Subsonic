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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerQueue;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceLifecycleSupport;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.
 * <p/>
 * Based on source code from the stock Android Music app.
 *
 * @author Sindre Mehus
 */
public class DSubWidgetProvider extends AppWidgetProvider {
    private static final String TAG = DSubWidgetProvider.class.getSimpleName();
	private static DSubWidget4x1 instance4x1;
	private static DSubWidget4x2 instance4x2;
	private static DSubWidget4x3 instance4x3;
	private static DSubWidget4x4 instance4x4;

	public static synchronized void notifyInstances(Context context, DownloadService service, boolean playing) {
		if(instance4x1 == null) {
			instance4x1 = new DSubWidget4x1();
		}
		if(instance4x2 == null) {
			instance4x2 = new DSubWidget4x2();
		}
		if(instance4x3 == null) {
			instance4x3 = new DSubWidget4x3();
		}
		if(instance4x4 == null) {
			instance4x4 = new DSubWidget4x4();
		}
		
		instance4x1.notifyChange(context, service, playing);
		instance4x2.notifyChange(context, service, playing);
		instance4x3.notifyChange(context, service, playing);
		instance4x4.notifyChange(context, service, playing);
	}

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
    }
    
	@Override
	public void onEnabled(Context context) {
		notifyInstances(context, DownloadService.getInstance(), false);
	}
	
	protected int getLayout() {
		return 0;
	}

    /**
     * Initialize given widgets to default state, where we launch Subsonic on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), getLayout());

        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
		if(getLayout() == R.layout.appwidget4x2) {
			views.setTextViewText(R.id.album, "");
		}

        linkButtons(context, views, false);
		performUpdate(context, null, appWidgetIds, false);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            manager.updateAppWidget(appWidgetIds, views);
        } else {
            manager.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    /**
     * Handle a change notification coming over from {@link DownloadService}
     */
    public void notifyChange(Context context, DownloadService service, boolean playing) {
        if (hasInstances(context)) {
            performUpdate(context, service, null, playing);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    private void performUpdate(Context context, DownloadService service, int[] appWidgetIds, boolean playing) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), getLayout());

		if(playing) {
			views.setViewVisibility(R.id.widget_root, View.VISIBLE);
		} else {
			// Hide widget
			SharedPreferences prefs = Util.getPreferences(context);
			if(prefs.getBoolean(Constants.PREFERENCES_KEY_HIDE_WIDGET, false)) {
				views.setViewVisibility(R.id.widget_root, View.GONE);
			}
		}

	// Get Entry from current playing DownloadFile
        MusicDirectory.Entry currentPlaying = null;
        if(service == null) {
        	// Deserialize from playling list to setup
            try {
                PlayerQueue state = FileUtil.deserialize(context, DownloadServiceLifecycleSupport.FILENAME_DOWNLOADS_SER, PlayerQueue.class);
                if (state != null && state.currentPlayingIndex != -1) {
                    currentPlaying = state.songs.get(state.currentPlayingIndex);
                }
            } catch(Exception e) {
                Log.e(TAG, "Failed to grab current playing", e);
            }
        } else {
			currentPlaying = service.getCurrentPlaying() == null ? null : service.getCurrentPlaying().getSong();
        }
        
        String title = currentPlaying == null ? null : currentPlaying.getTitle();
        CharSequence artist = currentPlaying == null ? null : currentPlaying.getArtist();
		CharSequence album = currentPlaying == null ? null : currentPlaying.getAlbum();
        CharSequence errorState = null;

        // Show error message?
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
            status.equals(Environment.MEDIA_UNMOUNTED)) {
            errorState = res.getText(R.string.widget_sdcard_busy);
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            errorState = res.getText(R.string.widget_sdcard_missing);
        } else if (currentPlaying == null) {
            errorState = res.getText(R.string.widget_initial_text);
        }

        if (errorState != null) {
            // Show error state to user
        	views.setTextViewText(R.id.title,null);
            views.setTextViewText(R.id.artist, errorState);
			views.setTextViewText(R.id.album, "");
			if(getLayout() != R.layout.appwidget4x1) {
				views.setImageViewResource(R.id.appwidget_coverart, R.drawable.appwidget_art_default);
			}
        } else {
            // No error, so show normal titles
            views.setTextViewText(R.id.title, title);
            views.setTextViewText(R.id.artist, artist);
			if(getLayout() != R.layout.appwidget4x1) {
				views.setTextViewText(R.id.album, album);
			}
        }

        // Set correct drawable for pause state
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.media_pause_dark);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.media_start_dark);
        }

        // Set the cover art
        try {
            boolean large = false;
			if(getLayout() != R.layout.appwidget4x1 && getLayout() != R.layout.appwidget4x2) {
				large = true;
			}
			ImageLoader imageLoader = SubsonicActivity.getStaticImageLoader(context);
            Bitmap bitmap = imageLoader == null ? null : imageLoader.getCachedImage(context, currentPlaying, large);

            if (bitmap == null) {
                // Set default cover art
                views.setImageViewResource(R.id.appwidget_coverart, R.drawable.appwidget_art_unknown);
            } else {
                bitmap = getRoundedCornerBitmap(bitmap);
                views.setImageViewBitmap(R.id.appwidget_coverart, bitmap);
            }
        } catch (Exception x) {
            Log.e(TAG, "Failed to load cover art", x);
            views.setImageViewResource(R.id.appwidget_coverart, R.drawable.appwidget_art_unknown);
        }

        // Link actions buttons to intents
        linkButtons(context, views, currentPlaying != null);

        pushUpdate(context, appWidgetIds, views);
    }
    
    /**
     * Round the corners of a bitmap for the cover art image
     */
    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final float roundPx = 10;

        // Add extra width to the rect so the right side wont be rounded.
        final Rect rect = new Rect(0, 0, bitmap.getWidth() + (int) roundPx, bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     *
     * @param playerActive @param playerActive True if player is active in background.  Launch {@link github.daneren2005.dsub.activity.SubsonicFragmentActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
		Intent intent = new Intent(context, SubsonicFragmentActivity.class);
		intent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_coverart, pendingIntent);
        views.setOnClickPendingIntent(R.id.appwidget_top, pendingIntent);

        // Emulate media button clicks.
        intent = new Intent("DSub.PLAY_PAUSE");
        intent.setComponent(new ComponentName(context, DownloadService.class));
		intent.setAction(DownloadService.CMD_TOGGLEPAUSE);
        if (Build.VERSION.SDK_INT >= 26)
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        else
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent("DSub.NEXT");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadService.class));
		intent.setAction(DownloadService.CMD_NEXT);
        if (Build.VERSION.SDK_INT >= 26)
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        else
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);

        intent = new Intent("DSub.PREVIOUS");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadService.class));
		intent.setAction(DownloadService.CMD_PREVIOUS);
        if (Build.VERSION.SDK_INT >= 26)
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        else
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
    }
}
