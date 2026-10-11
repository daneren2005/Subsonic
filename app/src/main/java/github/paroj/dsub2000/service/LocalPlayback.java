package github.paroj.dsub2000.service;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

/** One native decoder/AudioTrack for cached tracks, growing files and radio. */
@OptIn(markerClass = UnstableApi.class)
final class LocalPlayback {
    interface Listener {
        void onState(DownloadFile file, int state, boolean playWhenReady);
        void onTransition(DownloadFile previous, DownloadFile current);
        void onError(DownloadFile file, PlaybackException error);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExoPlayer player;
    private final DataSource.Factory files;
    private final DataSource.Factory radio = new NoRangeHttpDataSource.Factory();
    private final Listener listener;
    private final int audioSessionId;
    private Source current;
    private volatile Source next;
    private volatile Snapshot snapshot = new Snapshot(0, 0, false, 1f);
    private volatile boolean released;

    LocalPlayback(Context context, Listener listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Create local playback on the main looper");
        }
        this.listener = listener;
        files = new DefaultDataSource.Factory(context);
        audioSessionId = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).generateAudioSessionId();
        player = new ExoPlayer.Builder(context).build();
        player.setAudioSessionId(audioSessionId);
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        player.addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && next != null) {
                    DownloadFile previous = current.file;
                    current = next;
                    next = null;
                    player.removeMediaItem(0);
                    player.setVolume(current.gain);
                    updateSnapshot();
                    listener.onTransition(previous, current.file);
                }
            }
            @Override public void onEvents(Player ignored, Player.Events events) {
                updateSnapshot();
                if (current != null) {
                    listener.onState(current.file, player.getPlaybackState(), player.getPlayWhenReady());
                }
            }
            @Override public void onPlayerError(PlaybackException error) {
                if (current != null) listener.onError(current.file, error);
            }
        });
        handler.post(new Runnable() {
            @Override public void run() {
                if (!released) {
                    updateSnapshot();
                    handler.postDelayed(this, 100);
                }
            }
        });
    }

    void prepare(DownloadFile file, String uri, int position, boolean playing, float gain, float speed) {
        Source source = source(file, uri, gain);
        execute(() -> {
            current = source;
            next = null;
            player.setMediaSource(source.mediaSource, position);
            player.setVolume(source.gain);
            player.setPlaybackSpeed(speed);
            player.setPlayWhenReady(playing);
            player.prepare();
            updateSnapshot();
        });
    }

    void setNext(DownloadFile expectedCurrent, DownloadFile file, String uri, float gain) {
        Source source = source(file, uri, gain);
        execute(() -> {
            if (current == null || current.file != expectedCurrent) return;
            next = source;
            if (player.getMediaItemCount() > 1) player.removeMediaItems(1, player.getMediaItemCount());
            player.addMediaSource(source.mediaSource);
        });
    }

    void clearNext() {
        execute(() -> {
            next = null;
            if (player.getMediaItemCount() > 1) player.removeMediaItems(1, player.getMediaItemCount());
        });
    }

    void setGain(DownloadFile file, float gain) {
        execute(() -> {
            if (current != null && current.file == file) {
                current.gain = gain;
                player.setVolume(gain);
            }
            if (next != null && next.file == file) next.gain = gain;
        });
    }

    void play() { execute(player::play); }
    void pause() { execute(player::pause); }
    void stop() { execute(player::stop); }
    void seekTo(int position) {
        // Publish the target now so a read right after the call sees it.
        Snapshot old = snapshot;
        snapshot = new Snapshot(position, old.duration, old.playing, old.speed);
        execute(() -> { player.seekTo(position); updateSnapshot(); });
    }
    void setSpeed(float speed) { execute(() -> player.setPlaybackSpeed(speed)); }
    void setPreferredDevice(AudioDeviceInfo device) { execute(() -> player.setPreferredAudioDevice(device)); }
    int getAudioSessionId() { return audioSessionId; }
    int getDuration() { return snapshot.duration; }
    int getCurrentPosition() { return snapshot.position(); }
    boolean isQueued(DownloadFile file) {
        Source queued = next;
        return queued != null && queued.file == file;
    }

    void reset() {
        execute(() -> {
            current = null;
            next = null;
            player.stop();
            player.clearMediaItems();
            updateSnapshot();
        });
    }

    void release() {
        execute(() -> {
            released = true;
            handler.removeCallbacksAndMessages(null);
            player.release();
        });
    }

    private void execute(Runnable action) {
        if (Looper.myLooper() == handler.getLooper()) {
            if (!released) action.run();
        } else {
            handler.post(() -> { if (!released) action.run(); });
        }
    }

    private Source source(DownloadFile file, String uri, float gain) {
        MediaItem item = SessionPlayer.mediaItem(file.getSong()).buildUpon()
                .setUri(Uri.parse(uri)).setTag(file).build();
        return new Source(file, gain, new ProgressiveMediaSource.Factory(file.isStream() ? radio : files)
                .createMediaSource(item));
    }

    private void updateSnapshot() {
        long duration = player.getDuration();
        snapshot = new Snapshot((int) Math.min(Integer.MAX_VALUE, Math.max(0, player.getCurrentPosition())),
                duration == C.TIME_UNSET ? 0 : (int) Math.min(Integer.MAX_VALUE, duration),
                player.isPlaying(), player.getPlaybackParameters().speed);
    }

    private static final class Source {
        final DownloadFile file;
        float gain;
        final MediaSource mediaSource;
        Source(DownloadFile file, float gain, MediaSource mediaSource) {
            this.file = file;
            this.gain = gain;
            this.mediaSource = mediaSource;
        }
    }

    private static final class Snapshot {
        final int position;
        final int duration;
        final boolean playing;
        final float speed;
        final long capturedAt = SystemClock.elapsedRealtime();
        Snapshot(int position, int duration, boolean playing, float speed) {
            this.position = position;
            this.duration = duration;
            this.playing = playing;
            this.speed = speed;
        }
        int position() {
            long value = position + (playing ? (long) ((SystemClock.elapsedRealtime() - capturedAt) * speed) : 0);
            return (int) Math.min(duration > 0 ? duration : Integer.MAX_VALUE, value);
        }
    }
}
