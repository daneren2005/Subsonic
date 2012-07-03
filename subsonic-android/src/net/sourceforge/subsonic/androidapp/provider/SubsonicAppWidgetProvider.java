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
package net.sourceforge.subsonic.androidapp.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.activity.DownloadActivity;
import net.sourceforge.subsonic.androidapp.activity.MainActivity;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.service.DownloadService;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceImpl;
import net.sourceforge.subsonic.androidapp.util.FileUtil;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.
 * <p/>
 * Based on source code from the stock Android Music app.
 *
 * @author Sindre Mehus
 */
public class SubsonicAppWidgetProvider extends AppWidgetProvider {

    private static SubsonicAppWidgetProvider instance;
    private static final String TAG = SubsonicAppWidgetProvider.class.getSimpleName();

    public static synchronized SubsonicAppWidgetProvider getInstance() {
        if (instance == null) {
            instance = new SubsonicAppWidgetProvider();
        }
        return instance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
    }

    /**
     * Initialize given widgets to default state, where we launch Subsonic on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));

        linkButtons(context, views, false);
        pushUpdate(context, appWidgetIds, views);
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
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        MusicDirectory.Entry currentPlaying = service.getCurrentPlaying() == null ? null : service.getCurrentPlaying().getSong();
        String title = currentPlaying == null ? null : currentPlaying.getTitle();
        CharSequence artist = currentPlaying == null ? null : currentPlaying.getArtist();
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
            views.setImageViewResource(R.id.appwidget_coverart, R.drawable.appwidget_art_default);
        } else {
            // No error, so show normal titles
            views.setTextViewText(R.id.title, title);
            views.setTextViewText(R.id.artist, artist);
        }

        // Set correct drawable for pause state
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }

        // Set the cover art
        try {
            int size = context.getResources().getDrawable(R.drawable.appwidget_art_default).getIntrinsicHeight();
            Bitmap bitmap = currentPlaying == null ? null : FileUtil.getAlbumArtBitmap(context, currentPlaying, size);

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
     * @param playerActive True if player is active in background, which means
     *                     widget click will launch {@link DownloadActivity},
     *                     otherwise we launch {@link MainActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {

        Intent intent = new Intent(context, playerActive ? DownloadActivity.class : MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_coverart, pendingIntent);
        views.setOnClickPendingIntent(R.id.appwidget_top, pendingIntent);
        
        // Emulate media button clicks.
        intent = new Intent("1");
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent("2");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
        
        intent = new Intent("3");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
    }
}
