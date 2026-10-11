/*
 * This file is part of Subsonic.
 * Copyright 2015 (C) Scott Jackson
 *
 * Subsonic is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See <http://www.gnu.org/licenses/>.
 */
package github.paroj.dsub2000.util.compat;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaLibraryService.LibraryParams;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionError;
import androidx.media3.session.SessionResult;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.activity.SubsonicActivity;
import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.service.DownloadFile;
import github.paroj.dsub2000.service.DownloadService;
import github.paroj.dsub2000.service.SessionPlayer;
import github.paroj.dsub2000.service.SessionQueueResolver;
import github.paroj.dsub2000.service.SessionLibrary;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.ImageLoader;

@OptIn(markerClass = UnstableApi.class)
public class RemoteControlClientLP {
    private static final String TAG = RemoteControlClientLP.class.getSimpleName();
    private static final String THUMBS_UP = "github.paroj.dsub2000.THUMBS_UP";
    private static final String THUMBS_DOWN = "github.paroj.dsub2000.THUMBS_DOWN";
    private static final String STAR = "github.paroj.dsub2000.STAR";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DownloadService service;
    private MediaLibrarySession session;
    private SessionPlayer player;
    private SessionQueueResolver resolver;
    private SessionLibrary library;
    private ImageLoader imageLoader;
    private MediaRouter router;
    private boolean released;
    private final MediaRouter.Callback routeListener = new MediaRouter.Callback() {
        @Override public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) { refresh(); }
        @Override public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) { refresh(); }
    };

    public void register(Context context, ComponentName mediaButtonReceiverComponent) {
        service = (DownloadService) context;
        player = new SessionPlayer(service);
        library = new SessionLibrary(service);
        resolver = new SessionQueueResolver(service, library);
        Intent activity = new Intent(context, SubsonicFragmentActivity.class)
                .putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle extras = new Bundle();
        extras.putBoolean("android.support.wearable.media.extra.BACKGROUND_COLOR_FROM_THEME", true);
        extras.putBoolean("android.support.wearable.media.extra.RESERVE_SLOT_SKIP_TO_PREVIOUS", true);
        extras.putBoolean("android.support.wearable.media.extra.RESERVE_SLOT_SKIP_TO_NEXT", true);
        extras.putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS", true);
        extras.putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT", true);
        session = new MediaLibrarySession.Builder(context, player, new Callback())
                .setId("DSub").setSessionExtras(extras)
                .setLibraryErrorReplicationMode(MediaLibrarySession.LIBRARY_ERROR_REPLICATION_MODE_NON_FATAL)
                .setSessionActivity(PendingIntent.getActivity(context, 0, activity, PendingIntent.FLAG_IMMUTABLE))
                .build();
        imageLoader = SubsonicActivity.getStaticImageLoader(context);
    }

    public void unregister(Context context) {
        released = true;
        handler.removeCallbacksAndMessages(null);
        if (router != null) {
            router.removeCallback(routeListener);
        }
        resolver.release();
        library.release();
        session.release();
        player.release();
    }

    private void refresh() {
        handler.post(() -> {
            if (!released) {
                player.refresh();
                updateCustomLayout();
            }
        });
    }

    public void setPlaybackState(int state, int index, int queueSize) { refresh(); }
    public void metadataChanged(Entry entry) { refresh(); }
    public void updatePlaylist(List<DownloadFile> playlist) { refresh(); }

    public void updateMetadata(Context context, Entry entry) {
        if (entry != null && imageLoader != null) {
            imageLoader.loadImage(context, this, entry);
            updateAlbumArt(entry, imageLoader.getCachedImage(context, entry, false));
        } else {
            updateAlbumArt(entry, null);
        }
    }

    public void updateAlbumArt(Entry entry, Bitmap bitmap) {
        byte[] artwork = null;
        if (bitmap != null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)) {
                artwork = bytes.toByteArray();
            } else {
                Log.w(TAG, "Unable to encode session artwork");
            }
        }
        final byte[] encoded = artwork;
        handler.post(() -> {
            if (!released) {
                player.setArtwork(entry, encoded);
                updateCustomLayout();
            }
        });
    }

    public void registerRoute(MediaRouter router) {
        if (this.router != null) {
            this.router.removeCallback(routeListener);
        }
        this.router = router;
        router.addCallback(MediaRouteSelector.EMPTY, routeListener, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        refresh();
    }

    public void unregisterRoute(MediaRouter router) {
        router.removeCallback(routeListener);
        this.router = null;
        refresh();
    }

    public MediaLibrarySession getMediaSession() {
        return session;
    }

    public SessionLibrary getLibrary() {
        return library;
    }

    private void updateCustomLayout() {
        DownloadFile current = service.getCurrentPlaying();
        List<CommandButton> buttons = new ArrayList<>();
        if (current != null) {
            Entry entry = current.getSong();
            buttons.add(button(THUMBS_DOWN, R.string.download_thumbs_down,
                    entry.getRating() == 1 ? R.drawable.ic_action_rating_bad_selected : R.drawable.ic_action_rating_bad));
            buttons.add(button(STAR, R.string.common_star,
                    entry.isStarred() ? R.drawable.ic_toggle_star_selected : R.drawable.ic_toggle_star));
            buttons.add(button(THUMBS_UP, R.string.download_thumbs_up,
                    entry.getRating() == 5 ? R.drawable.ic_action_rating_good_selected : R.drawable.ic_action_rating_good));
        }
        session.setCustomLayout(buttons);
    }

    private CommandButton button(String action, int title, int icon) {
        return new CommandButton.Builder().setDisplayName(service.getString(title)).setIconResId(icon)
                .setSessionCommand(new SessionCommand(action, Bundle.EMPTY)).build();
    }

    private final class Callback implements MediaLibrarySession.Callback {
        @Override
        public void onPlayerInteractionFinished(MediaSession session,
                MediaSession.ControllerInfo controller, Player.Commands commands) {
            player.finishInteraction();
        }

        @Override
        public MediaSession.ConnectionResult onConnect(MediaSession session, MediaSession.ControllerInfo controller) {
            SessionCommands.Builder commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon();
            if (controller.isTrusted() || service.getPackageName().equals(controller.getPackageName())) {
                commands.add(new SessionCommand(THUMBS_DOWN, Bundle.EMPTY))
                        .add(new SessionCommand(STAR, Bundle.EMPTY))
                        .add(new SessionCommand(THUMBS_UP, Bundle.EMPTY));
            }
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands.build()).build();
        }

        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(MediaLibrarySession session,
                MediaSession.ControllerInfo controller, LibraryParams params) {
            return Futures.immediateFuture(LibraryResult.ofItem(library.root(), params));
        }

        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(MediaLibrarySession session,
                MediaSession.ControllerInfo controller, String id) {
            MediaItem item = library.getItem(id);
            if (item == null) {
                Log.w(TAG, "Unknown library media ID: " + id);
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
            }
            return Futures.immediateFuture(LibraryResult.ofItem(item, null));
        }

        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                MediaLibrarySession session, MediaSession.ControllerInfo controller, String parentId,
                int page, int pageSize, LibraryParams params) {
            return libraryResult(library.children(parentId, page, pageSize), params);
        }

        @Override
        public ListenableFuture<LibraryResult<Void>> onSearch(MediaLibrarySession session,
                MediaSession.ControllerInfo controller, String query, LibraryParams params) {
            ListenableFuture<LibraryResult<Void>> result = Futures.transform(library.search(query), items -> {
                session.notifySearchResultChanged(controller, query, items.size(), params);
                return LibraryResult.ofVoid();
            }, command -> handler.post(command));
            return Futures.catching(result, Exception.class,
                    error -> LibraryResult.ofError(SessionError.ERROR_IO), MoreExecutors.directExecutor());
        }

        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(
                MediaLibrarySession session, MediaSession.ControllerInfo controller, String query,
                int page, int pageSize, LibraryParams params) {
            if (page < 0 || pageSize < 1) {
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
            }
            ListenableFuture<List<MediaItem>> items = Futures.transform(library.searchResults(query), found -> {
                long from = (long) page * pageSize;
                return from >= found.size() ? java.util.Collections.emptyList()
                        : found.subList((int) from, (int) Math.min(found.size(), from + pageSize));
            }, MoreExecutors.directExecutor());
            return libraryResult(items, params);
        }

        @Override
        public ListenableFuture<SessionResult> onCustomCommand(MediaSession session,
                MediaSession.ControllerInfo controller, SessionCommand command, Bundle args) {
            switch (command.customAction) {
                case THUMBS_UP: service.toggleRating(5); break;
                case THUMBS_DOWN: service.toggleRating(1); break;
                case STAR: service.toggleStarred(); break;
                default: return Futures.immediateFuture(new SessionResult(SessionError.ERROR_NOT_SUPPORTED));
            }
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }

        @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(MediaSession session,
                MediaSession.ControllerInfo controller, List<MediaItem> items, int index, long position) {
            return resolver.resolve(items, index, position);
        }

        @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
                MediaSession session, MediaSession.ControllerInfo controller) {
            return resolver.resume();
        }

        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession session,
                MediaSession.ControllerInfo controller, List<MediaItem> items) {
            return Futures.transform(resolver.resolve(items, 0, 0),
                    queue -> queue.mediaItems, MoreExecutors.directExecutor());
        }

        @Override
        public boolean onMediaButtonEvent(MediaSession session, MediaSession.ControllerInfo controller,
                                          Intent intent) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return false;
            }
            service.handleKeyEvent(event);
            return true;
        }

        private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> libraryResult(
                ListenableFuture<List<MediaItem>> items, LibraryParams params) {
            ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> result = Futures.transform(
                    items, found -> LibraryResult.ofItemList(found, params), MoreExecutors.directExecutor());
            return Futures.catching(result, Exception.class, error -> LibraryResult.ofError(
                    error instanceof IllegalArgumentException ? SessionError.ERROR_BAD_VALUE
                            : SessionError.ERROR_IO), MoreExecutors.directExecutor());
        }
    }
}
