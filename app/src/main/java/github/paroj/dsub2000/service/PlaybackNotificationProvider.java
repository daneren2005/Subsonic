package github.paroj.dsub2000.service;

import android.app.Notification;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Notifications;
import github.paroj.dsub2000.util.Util;

/** Supplies the app's existing now-playing notification to the Media3 notification manager. */
@OptIn(markerClass = UnstableApi.class)
final class PlaybackNotificationProvider implements MediaNotification.Provider {
    private final DownloadService service;

    PlaybackNotificationProvider(DownloadService service) {
        this.service = service;
    }

    @Override
    public MediaNotification createNotification(MediaSession session, ImmutableList<CommandButton> customLayout,
                                                MediaNotification.ActionFactory actionFactory,
                                                Callback onNotificationChangedCallback) {
        DownloadFile current = service.getCurrentPlaying();
        Notification notification;
        if (current == null) {
            notification = new NotificationCompat.Builder(service, "now-playing-channel")
                    .setSmallIcon(R.drawable.stat_notify_playing).build();
        } else {
            boolean mediaStyle = Util.getPreferences(service)
                    .getBoolean(Constants.PREFERENCES_KEY_MEDIA_STYLE_NOTIFICATION, true);
            notification = Notifications.buildPlayingNotification(service, service, session, current.getSong(), mediaStyle);
        }
        return new MediaNotification(Notifications.NOTIFICATION_ID_PLAYING, notification);
    }

    @Override
    public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
        return false;
    }

    @Override
    public NotificationChannelInfo getNotificationChannelInfo() {
        return new NotificationChannelInfo("now-playing-channel", "Now Playing");
    }
}
