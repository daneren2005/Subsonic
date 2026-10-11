package github.paroj.dsub2000.service;

import android.os.Looper;

import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.util.UnstableApi;
import androidx.mediarouter.media.MediaRouter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Util;

import static github.paroj.dsub2000.domain.RepeatMode.ALL;
import static github.paroj.dsub2000.domain.RepeatMode.OFF;
import static github.paroj.dsub2000.domain.RepeatMode.SINGLE;

/** Temporary Media3 boundary while DownloadService still owns playback and queue policy. */
@OptIn(markerClass = UnstableApi.class)
public final class SessionPlayer extends SimpleBasePlayer implements DownloadService.OnSongChangedListener {
    private final DownloadService service;
    private MusicDirectory.Entry artworkEntry;
    private byte[] artwork;
    private boolean released;
    private boolean appendRequest;

    public SessionPlayer(DownloadService service) {
        super(Looper.getMainLooper());
        this.service = service;
        service.addOnSongChangedListener(this);
    }

    public void refresh() {
        verifyApplicationThread();
        if (!released) {
            invalidateState();
        }
    }

    public void setArtwork(MusicDirectory.Entry entry, byte[] bytes) {
        verifyApplicationThread();
        artworkEntry = entry;
        artwork = bytes;
        refresh();
    }

    public void finishInteraction() {
        verifyApplicationThread();
        appendRequest = false;
    }

    public static MediaItem mediaItem(MusicDirectory.Entry entry) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(entry.getTitle()).setArtist(entry.getArtist())
                .setAlbumTitle(entry.getAlbum()).setAlbumArtist(entry.getArtist())
                .setGenre(entry.getGenre()).setTrackNumber(entry.getTrack())
                .setIsBrowsable(false).setIsPlayable(true)
                .build();
        return new MediaItem.Builder().setMediaId(entry.getId())
                .setUri(android.net.Uri.EMPTY).setTag(entry).setMediaMetadata(metadata).build();
    }

    @Override
    protected State getState() {
        synchronized (service) {
            List<MediaItemData> playlist = new ArrayList<>();
            List<DownloadFile> files = new ArrayList<>(service.getSongs());
            DownloadFile current = service.getCurrentPlaying();
            for (DownloadFile file : files) {
                MusicDirectory.Entry entry = file.getSong();
                MediaItem item = mediaItem(entry);
                if (file == current && entry == artworkEntry && artwork != null) {
                    item = item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon()
                            .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build();
                }
                long duration = entry.getDuration() == null || file.isStream()
                        ? C.TIME_UNSET : entry.getDuration() * 1_000_000L;
                playlist.add(new MediaItemData.Builder(file).setMediaItem(item)
                        .setDurationUs(duration)
                        .setIsSeekable(file == current && service.isSeekable())
                        .setIsDynamic(file.isStream()).build());
            }
            int index = files.indexOf(current);
            PlayerState state = service.getPlayerState();
            int playbackState = STATE_IDLE;
            if (!playlist.isEmpty()) {
                switch (state) {
                    case DOWNLOADING:
                    case PREPARING: playbackState = STATE_BUFFERING; break;
                    case COMPLETED: playbackState = STATE_ENDED; break;
                    case STARTED:
                    case PAUSED:
                    case PAUSED_TEMP: playbackState = STATE_READY; break;
                    default: break;
                }
            }
            Commands.Builder commands = new Commands.Builder().addAll(
                    COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE, COMMAND_GET_METADATA,
                    COMMAND_GET_AUDIO_ATTRIBUTES, COMMAND_GET_DEVICE_VOLUME,
                    COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_STOP, COMMAND_RELEASE,
                    COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS, COMMAND_SET_REPEAT_MODE);
            if (current != null) {
                commands.add(COMMAND_SEEK_TO_MEDIA_ITEM);
                if (!service.isRemoteEnabled() || service.getRemoteController().supportsPlaybackSpeed()) {
                    commands.add(COMMAND_SET_SPEED_AND_PITCH);
                }
                if (service.isSeekable()) {
                    commands.addAll(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SEEK_BACK,
                            COMMAND_SEEK_FORWARD);
                }
                if (service.shouldFastForward() || index > 0 || service.getRepeatMode() == ALL) {
                    commands.add(COMMAND_SEEK_TO_PREVIOUS);
                }
                if (service.shouldFastForward() || index < files.size() - 1
                        || service.getRepeatMode() == ALL) {
                    commands.add(COMMAND_SEEK_TO_NEXT);
                }
            }
            State.Builder builder = new State.Builder().setPlaylist(playlist)
                    .setCurrentMediaItemIndex(index < 0 ? C.INDEX_UNSET : index)
                    .setPlaybackState(playbackState)
                    .setPlayerError(service.getPlaybackError())
                    .setIsLoading(playbackState == STATE_BUFFERING)
                    .setPlayWhenReady(service.isPlayWhenReady() && !playlist.isEmpty(),
                            PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .setPlaybackSuppressionReason(state == PlayerState.PAUSED_TEMP
                            ? PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                            : PLAYBACK_SUPPRESSION_REASON_NONE)
                    .setContentPositionMs(Math.max(0, service.getPlayerPosition()))
                    .setPlaybackParameters(new PlaybackParameters(service.getPlaybackSpeed()))
                    .setRepeatMode(repeatMode(service.getRepeatMode()))
                    .setSeekBackIncrementMs(interval(Constants.PREFERENCES_KEY_REWIND_INTERVAL, "10"))
                    .setSeekForwardIncrementMs(interval(Constants.PREFERENCES_KEY_FASTFORWARD_INTERVAL, "30"))
                    .setMaxSeekToPreviousPositionMs(5000)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build());
            if (service.getMediaRouter() != null) {
                MediaRouter.RouteInfo route = service.getMediaRouter().getSelectedRoute();
                boolean remote = service.isRemoteEnabled();
                builder.setDeviceInfo(new DeviceInfo.Builder(remote
                        ? DeviceInfo.PLAYBACK_TYPE_REMOTE : DeviceInfo.PLAYBACK_TYPE_LOCAL)
                        .setMaxVolume(route.getVolumeMax()).build())
                        .setDeviceVolume(route.getVolume());
                if (remote && route.getVolumeHandling() == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE) {
                    commands.addAll(COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                            COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS);
                }
            }
            return builder.setAvailableCommands(commands.build()).build();
        }
    }

    private long interval(String key, String defaultValue) {
        return Long.parseLong(Util.getPreferences(service).getString(key, defaultValue)) * 1000L;
    }

    private static int repeatMode(github.paroj.dsub2000.domain.RepeatMode mode) {
        return mode == ALL ? REPEAT_MODE_ALL : mode == SINGLE ? REPEAT_MODE_ONE : REPEAT_MODE_OFF;
    }

    @Override
    protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
        // Legacy playFromMediaId appends prepare/play to a play-last request.
        if (appendRequest && playWhenReady) {
            return Futures.immediateVoidFuture();
        }
        if (playWhenReady) {
            if (service.getCurrentPlaying() != null) {
                service.start();
            }
        } else {
            service.pause();
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handlePrepare() {
        if (appendRequest) {
            return Futures.immediateVoidFuture();
        }
        PlayerState state = service.getPlayerState();
        if (service.getCurrentPlaying() != null
                && (state == PlayerState.IDLE || state == PlayerState.STOPPED || state == PlayerState.COMPLETED)) {
            service.play(service.getCurrentPlayingIndex(), false, service.getPlayerPosition());
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleStop() {
        service.stop();
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSetRepeatMode(int repeatMode) {
        service.setRepeatMode(repeatMode == REPEAT_MODE_ALL ? ALL : repeatMode == REPEAT_MODE_ONE ? SINGLE : OFF);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSetPlaybackParameters(PlaybackParameters parameters) {
        if (parameters.pitch != 1f) {
            return Futures.immediateFailedFuture(new IllegalArgumentException("Pitch changes are not supported"));
        }
        service.setPlaybackSpeed(parameters.speed);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSeek(int index, long position, int command) {
        switch (command) {
            case COMMAND_SEEK_TO_PREVIOUS: service.previous(); break;
            case COMMAND_SEEK_TO_NEXT: service.next(); break;
            case COMMAND_SEEK_BACK: service.rewind(); break;
            case COMMAND_SEEK_FORWARD: service.fastForward(); break;
            default:
                int millis = position == C.TIME_UNSET ? 0 : Math.toIntExact(position);
                if (index == service.getCurrentPlayingIndex()) {
                    service.seekTo(millis);
                } else if (index != C.INDEX_UNSET) {
                    service.play(index, getPlayWhenReady(), millis);
                }
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSetMediaItems(List<MediaItem> items, int startIndex, long startPositionMs) {
        List<MusicDirectory.Entry> entries = entries(items);
        android.os.Bundle options = items.isEmpty() ? null : items.get(0).mediaMetadata.extras;
        appendRequest = options != null && options.getBoolean(SessionQueueResolver.APPEND);
        if (appendRequest) {
            service.insertSessionQueue(service.size(), entries);
            return Futures.immediateVoidFuture();
        }
        int position = startPositionMs == C.TIME_UNSET ? 0 : Math.toIntExact(startPositionMs);
        boolean playing = getPlayWhenReady();
        service.clear();
        service.download(entries, false, playing, false, false,
                startIndex == C.INDEX_UNSET ? 0 : startIndex, position);
        if (options != null && options.getBoolean(SessionQueueResolver.SHUFFLE_PLAY)) {
            service.setShufflePlayEnabled(true);
        }
        return Futures.immediateVoidFuture();
    }

    private List<MusicDirectory.Entry> entries(List<MediaItem> items) {
        List<MusicDirectory.Entry> entries = new ArrayList<>();
        for (MediaItem item : items) {
            if (item.localConfiguration == null || !(item.localConfiguration.tag instanceof MusicDirectory.Entry)) {
                throw new IllegalArgumentException("Unresolved media ID: " + item.mediaId);
            }
            entries.add((MusicDirectory.Entry) item.localConfiguration.tag);
        }
        return entries;
    }

    @Override
    protected ListenableFuture<?> handleAddMediaItems(int index, List<MediaItem> items) {
        service.insertSessionQueue(index, entries(items));
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleMoveMediaItems(int from, int to, int newIndex) {
        service.moveSessionQueue(from, to, newIndex);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleRemoveMediaItems(int from, int to) {
        synchronized (service) {
            boolean playing = getPlayWhenReady();
            DownloadFile current = service.getCurrentPlaying();
            List<DownloadFile> removed = new ArrayList<>(service.getSongs().subList(from, to));
            for (DownloadFile file : removed) {
                service.remove(file);
            }
            if (removed.contains(current) && service.size() > 0) {
                service.play(Math.min(from, service.size() - 1), playing, 0);
            }
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSetDeviceVolume(int volume, int flags) {
        service.getMediaRouter().getSelectedRoute().requestSetVolume(volume);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleIncreaseDeviceVolume(int flags) {
        service.getMediaRouter().getSelectedRoute().requestUpdateVolume(1);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleDecreaseDeviceVolume(int flags) {
        service.getMediaRouter().getSelectedRoute().requestUpdateVolume(-1);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSetDeviceMuted(boolean muted, int flags) {
        return Futures.immediateFailedFuture(new UnsupportedOperationException("Remote mute is not supported"));
    }

    @Override
    protected ListenableFuture<?> handleRelease() {
        released = true;
        service.removeOnSongChangeListener(this);
        return Futures.immediateVoidFuture();
    }

    @Override public void onSongChanged(DownloadFile file, int index, boolean seek) { refresh(); }
    @Override public void onSongsChanged(List<DownloadFile> files, DownloadFile file, int index, boolean seek) { refresh(); }
    @Override public void onSongProgress(DownloadFile file, int position, Integer duration, boolean seekable) { refresh(); }
    @Override public void onStateUpdate(DownloadFile file, PlayerState state) { refresh(); }
    @Override public void onMetadataUpdate(MusicDirectory.Entry entry, int fields) { refresh(); }
}
