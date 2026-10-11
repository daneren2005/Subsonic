package github.paroj.dsub2000.service;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.DefaultMediaItemConverter;
import androidx.media3.cast.MediaItemConverter;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.domain.RemoteControlState;
import github.paroj.dsub2000.util.FileUtil;
import github.paroj.serverproxy.WebProxy;

/** Cast framework owns the connection; CastPlayer owns receiver playback. */
@OptIn(markerClass = UnstableApi.class)
public final class ChromeCastController extends RemoteController {
    private static final String TAG = "ChromeCastController";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService resolver = Executors.newSingleThreadExecutor();
    private final CastContext castContext;
    private final CastPlayer player;
    private final boolean artworkSupported;
    private RemoteMediaClient client;
    private DownloadFile requested;
    private boolean loading;
    private boolean suspended;
    private volatile boolean closed;
    private volatile boolean wantsPlayback;
    private volatile int position;
    private volatile int duration;
    private volatile double volume;
    private volatile boolean seekable;
    private volatile boolean speedSupported;
    private volatile float speed = 1f;
    private int generation;

    private final RemoteMediaClient.Callback receiverCallback = new RemoteMediaClient.Callback() {
        @Override public void onStatusUpdated() {
            MediaStatus status = client == null ? null : client.getMediaStatus();
            if (closed || requested == null || status == null || status.getMediaInfo() == null
                    || !requested.getSong().getId().equals(status.getMediaInfo().getContentId())) return;
            if (status.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE
                    && status.getIdleReason() == MediaStatus.IDLE_REASON_ERROR) {
                fail(new IOException("Cast receiver could not play the requested media"));
            } else if (status.getPlayerState() != MediaStatus.PLAYER_STATE_IDLE) {
                loading = false;
                handler.post(ChromeCastController.this::refresh);
            }
        }
    };
    private final SessionManagerListener<CastSession> connectionListener = new SessionManagerListener<CastSession>() {
        @Override public void onSessionStarting(CastSession session) { }
        @Override public void onSessionStarted(CastSession session, String id) { }
        @Override public void onSessionEnding(CastSession session) { }
        @Override public void onSessionEnded(CastSession session, int error) { }
        @Override public void onSessionResuming(CastSession session, String id) { }
        @Override public void onSessionResumed(CastSession session, boolean suspended) { }
        @Override public void onSessionSuspended(CastSession session, int reason) {
            suspended = true;
            loading = true;
        }
        @Override public void onSessionStartFailed(CastSession session, int error) { connectionFailed(error); }
        @Override public void onSessionResumeFailed(CastSession session, int error) { connectionFailed(error); }
    };

    public ChromeCastController(DownloadService service, CastDevice device) {
        super(service);
        castContext = CastContext.getSharedInstance(service);
        artworkSupported = device.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT);
        player = new CastPlayer(castContext, new RadioItemConverter());
        player.setSessionAvailabilityListener(new SessionAvailabilityListener() {
            @Override public void onCastSessionAvailable() {
                handler.post(() -> {
                    if (closed) return;
                    attachReceiver();
                    MediaItem current = player.getCurrentMediaItem();
                    if (suspended && requested != null && current != null
                            && requested.getSong().getId().equals(current.mediaId)) {
                        player.setPlayWhenReady(wantsPlayback);
                        loading = false;
                        refresh();
                    } else {
                        load(requested, position);
                    }
                    suspended = false;
                });
            }
            @Override public void onCastSessionUnavailable() {
                if (!closed) handler.post(() -> {
                    if (suspended) return;
                    if (!closed && downloadService.getRemoteController() == ChromeCastController.this) {
                        downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
                    }
                });
                castContext.getSessionManager().addSessionManagerListener(connectionListener, CastSession.class);
            }
        });
        player.addListener(new Player.Listener() {
            @Override public void onEvents(Player ignored, Player.Events events) { refresh(); }
            @Override public void onPlayerError(PlaybackException error) { fail(error); }
        });
    }

    @Override public void create(boolean playing, int seconds) {
        wantsPlayback = playing;
        position = seconds;
        requested = downloadService.getCurrentPlaying();
        downloadService.setPlayerState(PlayerState.PREPARING);
        if (player.isCastSessionAvailable()) {
            attachReceiver();
            load(requested, seconds);
        }
        handler.post(new Runnable() {
            @Override public void run() {
                if (!closed) {
                    refreshPosition();
                    handler.postDelayed(this, 500);
                }
            }
        });
    }

    @Override public void start() {
        onMain(() -> {
            wantsPlayback = true;
            if (requested == null) load(downloadService.getCurrentPlaying(), position);
            else player.play();
        });
    }
    @Override public void stop() {
        onMain(() -> { wantsPlayback = false; player.pause(); });
    }
    @Override public boolean isPlayWhenReady() { return wantsPlayback; }
    @Override public int getRemotePosition() { return position; }
    @Override public int getRemoteDuration() { return duration; }
    @Override public double getVolume() { return volume; }
    @Override public boolean isSeekable() { return seekable; }
    @Override public boolean supportsPlaybackSpeed() { return speedSupported; }
    @Override public float getPlaybackSpeed() { return speed; }
    @Override public void setPlaybackSpeed(float value) { onMain(() -> player.setPlaybackSpeed(value)); }

    @Override public void changePosition(int seconds) {
        onMain(() -> { position = seconds; player.seekTo(seconds * 1000L); });
    }
    @Override public void changeTrack(int index, DownloadFile file) {
        prepareTrack(index, file, true, 0);
    }
    @Override public void prepareTrack(int index, DownloadFile file, boolean playing, int seconds) {
        onMain(() -> { wantsPlayback = playing; load(file, seconds); });
    }
    @Override public void updatePlaylist() {
        if (downloadService.getCurrentPlaying() == null) onMain(() -> load(null, 0));
    }
    @Override public void setVolume(int value) {
        onMain(() -> {
            CastSession session = castContext.getSessionManager().getCurrentCastSession();
            if (session == null) {
                Log.w(TAG, "Cannot set volume without a Cast session");
                return;
            }
            try {
                session.setVolume(Math.max(0, Math.min(1, value / 10.0)));
                volume = session.getVolume();
            } catch (IOException | IllegalStateException error) {
                Log.e(TAG, "Unable to set Cast volume", error);
            }
        });
    }
    @Override public void updateVolume(boolean up) {
        setVolume((int) Math.round(volume * 10) + (up ? 1 : -1));
    }

    @Override public void shutdown() {
        onMain(() -> {
            closed = true;
            generation++;
            handler.removeCallbacksAndMessages(null);
            resolver.shutdownNow();
            castContext.getSessionManager().removeSessionManagerListener(connectionListener, CastSession.class);
            if (client != null) client.unregisterCallback(receiverCallback);
            player.setSessionAvailabilityListener(null);
            player.stop();
            player.release();
            if (proxy != null) {
                proxy.close();
                proxy = null;
            }
        });
    }

    private void attachReceiver() {
        if (client != null) client.unregisterCallback(receiverCallback);
        CastSession session = castContext.getSessionManager().getCurrentCastSession();
        client = session == null ? null : session.getRemoteMediaClient();
        if (client != null) client.registerCallback(receiverCallback);
    }

    private void load(DownloadFile file, int seconds) {
        requested = file;
        int request = ++generation;
        position = seconds;
        if (file == null) {
            loading = false;
            wantsPlayback = false;
            player.stop();
            player.clearMediaItems();
            downloadService.setPlayerState(PlayerState.IDLE);
            return;
        }
        loading = true;
        downloadService.setPlayerState(PlayerState.PREPARING);
        if (!player.isCastSessionAvailable()) return;
        resolver.execute(() -> {
            try {
                MediaItem item = mediaItem(file);
                handler.post(() -> {
                    if (closed || request != generation || downloadService.getRemoteController() != this) return;
                    player.setMediaItem(item, seconds * 1000L);
                    player.prepare();
                    player.setPlayWhenReady(wantsPlayback);
                });
            } catch (Exception error) {
                handler.post(() -> { if (!closed && request == generation) fail(error); });
            } finally {
                if (closed && proxy != null) {
                    proxy.close();
                    proxy = null;
                }
            }
        });
    }

    private MediaItem mediaItem(DownloadFile file) throws Exception {
        Entry song = file.getSong();
        MusicService music = MusicServiceFactory.getMusicService(downloadService);
        String uri = getStreamUrl(music, file);
        String mime = song.isVideo() ? "application/x-mpegURL" : song.getTranscodedContentType();
        if (mime == null) mime = song.getContentType();
        if (mime == null) mime = "audio/mpeg";
        MediaMetadata.Builder metadata = SessionPlayer.mediaItem(song).mediaMetadata.buildUpon()
                .setMediaType(song.isVideo() ? MediaMetadata.MEDIA_TYPE_MOVIE
                        : file.isStream() ? MediaMetadata.MEDIA_TYPE_RADIO_STATION : MediaMetadata.MEDIA_TYPE_MUSIC);
        if (artworkSupported && !song.isVideo()) {
            if (proxy == null || proxy instanceof WebProxy) {
                String cover = music.getCoverArtUrl(downloadService, song);
                if (cover != null) metadata.setArtworkUri(Uri.parse(proxy == null ? cover : proxy.getPublicAddress(cover)));
            } else {
                File cover = FileUtil.getAlbumArtFile(downloadService, song);
                if (cover != null && cover.exists()) metadata.setArtworkUri(Uri.parse(proxy.getPublicAddress(cover.getPath())));
            }
        }
        return new MediaItem.Builder().setMediaId(song.getId()).setUri(uri).setMimeType(mime)
                .setMediaMetadata(metadata.build()).build();
    }

    private void refreshPosition() {
        if (!loading && player.isCastSessionAvailable()) {
            position = (int) Math.min(Integer.MAX_VALUE, player.getCurrentPosition() / 1000);
            long length = player.getDuration();
            duration = length == C.TIME_UNSET ? 0 : (int) Math.min(Integer.MAX_VALUE, length / 1000);
            seekable = player.isCurrentMediaItemSeekable();
            speedSupported = player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH);
            speed = player.getPlaybackParameters().speed;
            CastSession session = castContext.getSessionManager().getCurrentCastSession();
            if (session != null) volume = session.getVolume();
        }
    }

    private void refresh() {
        if (closed || loading || requested == null
                || downloadService.getRemoteController() != this) return;
        refreshPosition();
        int state = player.getPlaybackState();
        if (state == Player.STATE_ENDED) {
            requested = null;
            downloadService.onSongCompleted();
        } else if (state == Player.STATE_BUFFERING) {
            downloadService.setPlayerState(PlayerState.PREPARING);
        } else if (state == Player.STATE_READY) {
            wantsPlayback = player.getPlayWhenReady();
            PlayerState updated = wantsPlayback ? PlayerState.STARTED : PlayerState.PAUSED;
            if (downloadService.getPlayerState() != updated) downloadService.setPlayerState(updated);
        }
    }

    private void fail(Exception error) {
        if (closed) return;
        Log.e(TAG, "Cast playback failed", error);
        generation++;
        loading = false;
        wantsPlayback = false;
        requested = null;
        player.pause();
        if (downloadService.getRemoteController() == this) downloadService.handleError(error);
    }

    private void connectionFailed(int error) {
        Log.e(TAG, "Cast connection failed with status " + error);
        handler.post(() -> {
            if (!closed && downloadService.getRemoteController() == this) {
                downloadService.setRemoteEnabled(RemoteControlState.LOCAL);
            }
        });
    }

    private void onMain(Runnable action) {
        if (Looper.myLooper() == handler.getLooper()) {
            if (!closed) action.run();
        } else {
            handler.post(() -> { if (!closed) action.run(); });
        }
    }

    static final class RadioItemConverter implements MediaItemConverter {
        private final DefaultMediaItemConverter delegate = new DefaultMediaItemConverter();
        @Override public MediaItem toMediaItem(MediaQueueItem item) { return delegate.toMediaItem(item); }
        @Override public MediaQueueItem toMediaQueueItem(MediaItem item) {
            MediaQueueItem result = delegate.toMediaQueueItem(item);
            MediaInfo info = result.getMedia();
            boolean radio = item.mediaMetadata.mediaType != null
                    && item.mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION;
            // Media3 1.11 leaves the stream type invalid for items without live configuration or duration.
            int streamType = radio ? MediaInfo.STREAM_TYPE_LIVE
                    : info.getStreamType() == MediaInfo.STREAM_TYPE_INVALID
                    ? MediaInfo.STREAM_TYPE_BUFFERED : info.getStreamType();
            if (streamType == info.getStreamType()) {
                return result;
            }
            MediaInfo.Builder rebuilt = new MediaInfo.Builder(info.getContentId())
                    .setContentUrl(info.getContentUrl()).setContentType(info.getContentType())
                    .setMetadata(info.getMetadata()).setCustomData(info.getCustomData())
                    .setStreamType(streamType);
            if (!radio && info.getStreamDuration() > 0) {
                rebuilt.setStreamDuration(info.getStreamDuration());
            }
            return new MediaQueueItem.Builder(rebuilt.build()).build();
        }
    }
}
