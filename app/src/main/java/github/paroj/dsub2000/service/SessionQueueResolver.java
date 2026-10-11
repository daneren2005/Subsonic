package github.paroj.dsub2000.service;

import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import github.paroj.dsub2000.domain.Bookmark;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.Playlist;
import github.paroj.dsub2000.domain.SearchCritera;
import github.paroj.dsub2000.domain.SearchResult;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Util;

@OptIn(markerClass = UnstableApi.class)
public final class SessionQueueResolver {
    static final String SHUFFLE_PLAY = "github.paroj.dsub2000.session.SHUFFLE_PLAY";
    static final String APPEND = "github.paroj.dsub2000.session.APPEND";
    private final DownloadService service;
    private final SessionLibrary library;
    private final ListeningExecutorService executor =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public SessionQueueResolver(DownloadService service, SessionLibrary library) {
        this.service = service;
        this.library = library;
    }

    public void release() {
        for (Runnable pending : executor.shutdownNow()) {
            if (pending instanceof java.util.concurrent.Future<?>) {
                ((java.util.concurrent.Future<?>) pending).cancel(false);
            }
        }
    }

    public ListenableFuture<MediaItemsWithStartPosition> resume() {
        return executor.submit(() -> {
            long deadline = android.os.SystemClock.elapsedRealtime() + 10_000;
            while (!service.isInitialized()) {
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    throw new java.util.concurrent.TimeoutException("Playback queue restoration timed out");
                }
                Thread.sleep(50);
            }
            synchronized (service) {
                List<MediaItem> items = new ArrayList<>();
                for (DownloadFile file : service.getSongs()) {
                    items.add(SessionPlayer.mediaItem(file.getSong()));
                }
                if (items.isEmpty()) {
                    Log.i("SessionQueueResolver", "No saved playback queue to resume");
                    throw new IllegalStateException("No saved playback queue");
                }
                return new MediaItemsWithStartPosition(items, Math.max(0, service.getCurrentPlayingIndex()),
                        service.getPlayerPosition());
            }
        });
    }

    public ListenableFuture<MediaItemsWithStartPosition> resolve(List<MediaItem> requests,
                                                                int index, long position) {
        return executor.submit(() -> {
            try {
                MusicService music = MusicServiceFactory.getMusicService(service);
                List<MediaItem> resolved = new ArrayList<>();
                int resolvedIndex = index == C.INDEX_UNSET ? 0 : index;
                long resolvedPosition = position == C.TIME_UNSET ? 0 : position;
                for (int i = 0; i < requests.size(); i++) {
                    MediaItemsWithStartPosition queue = resolveRequest(music, requests.get(i));
                    if (i == (index == C.INDEX_UNSET ? 0 : index)) {
                        resolvedIndex = resolved.size() + Math.max(0, queue.startIndex);
                        if (position == C.TIME_UNSET) {
                            resolvedPosition = queue.startPositionMs;
                        }
                    }
                    resolved.addAll(queue.mediaItems);
                }
                return new MediaItemsWithStartPosition(resolved, resolvedIndex, resolvedPosition);
            } catch (Exception error) {
                Log.e("SessionQueueResolver", "Unable to resolve media request", error);
                throw error;
            }
        });
    }

    MediaItemsWithStartPosition resolveRequest(MusicService music, MediaItem request) throws Exception {
        Bundle extras = request.requestMetadata.extras;
        if (extras == null || extras.isEmpty()) {
            extras = request.mediaMetadata.extras;
        }
        if (extras == null || extras.isEmpty()) {
            MediaItem known = library.getItem(request.mediaId);
            extras = known == null ? null : known.mediaMetadata.extras;
        }
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        boolean shuffle = extras.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE);
        boolean append = extras.getBoolean(Constants.INTENT_EXTRA_PLAY_LAST);
        String query = request.requestMetadata.searchQuery;
        if (query != null) {
            return search(music, query, extras);
        }
        Entry entry = null;
        byte[] bytes = extras.getByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES);
        if (bytes != null) {
            entry = Entry.fromByteArray(bytes);
        } else if (extras.getSerializable(Constants.INTENT_EXTRA_ENTRY) instanceof Entry) {
            entry = (Entry) extras.getSerializable(Constants.INTENT_EXTRA_ENTRY);
        }
        String playlistId = extras.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
        if (playlistId != null) {
            return queue(music.getPlaylist(false, playlistId, "", service, null).getSongs(),
                    shuffle, append, null, false);
        }
        String directoryId = extras.getString(Constants.INTENT_EXTRA_NAME_ID);
        if (directoryId != null) {
            return queue(directory(music, directoryId).getSongs(), shuffle, append, null, true);
        }
        if (entry != null && extras.containsKey(Constants.INTENT_EXTRA_NAME_PODCAST_ID)) {
            return queue(Collections.singletonList(entry), false, false, entry, true);
        }
        if (entry != null && extras.containsKey(Constants.INTENT_EXTRA_NAME_CHILD_ID)) {
            String parent = Util.isTagBrowsing(service) && !Util.isOffline(service)
                    ? entry.getAlbumId() : entry.getParent();
            return queue(parent == null ? Collections.singletonList(entry) : directory(music, parent).getSongs(),
                    shuffle, append, entry, true);
        }
        synchronized (service) {
            for (DownloadFile file : service.getSongs()) {
                if (request.mediaId.equals(file.getSong().getId())) {
                    return queue(Collections.singletonList(file.getSong()), false, false, null, false);
                }
            }
        }
        throw new IllegalArgumentException("Unknown media ID: " + request.mediaId);
    }

    private MusicDirectory directory(MusicService music, String id) throws Exception {
        return Util.isTagBrowsing(service) && !Util.isOffline(service)
                ? music.getAlbum(id, "", false, service, null)
                : music.getMusicDirectory(id, "", false, service, null);
    }

    private MediaItemsWithStartPosition search(MusicService music, String query, Bundle extras) throws Exception {
        String focus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS);
        if (MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE.equals(focus)) {
            String name = extras.getString(MediaStore.EXTRA_MEDIA_PLAYLIST);
            for (Playlist playlist : music.getPlaylists(false, service, null)) {
                if (playlist.getName().equals(name)) {
                    return queue(music.getPlaylist(false, playlist.getId(), name, service, null).getSongs(),
                            false, false, null, false);
                }
            }
        } else if (MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE.equals(focus)) {
            String genre = extras.getString(MediaStore.EXTRA_MEDIA_GENRE);
            Util.getPreferences(service).edit()
                    .remove(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR)
                    .remove(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR)
                    .putString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, genre).apply();
            return random(music);
        } else if (!query.isEmpty()) {
            int artists = 10;
            int albums = 10;
            int songs = 10;
            if (MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE.equals(focus)) {
                query = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, query);
                albums = songs = 0;
            } else if (MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE.equals(focus)) {
                query = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, query);
                artists = songs = 0;
            } else if (MediaStore.Audio.Media.ENTRY_CONTENT_TYPE.equals(focus)) {
                query = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, query);
                artists = albums = 0;
            }
            SearchResult found = music.search(new SearchCritera(query, artists, albums, songs), service, null);
            if (found.hasArtists()) {
                List<Entry> entries = new ArrayList<>();
                collect(music, new Entry(found.getArtists().get(0)), true, entries);
                return queue(entries, true, false, null, false);
            } else if (found.hasAlbums()) {
                List<Entry> entries = new ArrayList<>();
                collect(music, found.getAlbums().get(0), false, entries);
                return queue(entries, false, false, null, false);
            } else if (found.hasSongs()) {
                return queue(Collections.singletonList(found.getSongs().get(0)), false, false, null, false);
            }
        }
        return random(music);
    }

    private void collect(MusicService music, Entry parent, boolean artist, List<Entry> entries) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Media request cancelled");
        }
        MusicDirectory directory = artist && Util.isTagBrowsing(service) && !Util.isOffline(service)
                ? music.getArtist(parent.getId(), parent.getTitle(), false, service, null)
                : directory(music, parent.getId());
        for (Entry child : directory.getChildren()) {
            if (child.getRating() == 1 || child.isVideo()) {
                continue;
            }
            if (child.isDirectory()) {
                collect(music, child, false, entries);
            } else {
                entries.add(child);
            }
        }
    }

    private MediaItemsWithStartPosition random(MusicService music) throws Exception {
        int count = Math.max(1, Integer.parseInt(Util.getPreferences(service)
                .getString(Constants.PREFERENCES_KEY_RANDOM_SIZE, "20")));
        String genre = Util.getPreferences(service).getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, null);
        String from = Util.getPreferences(service).getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, null);
        String to = Util.getPreferences(service).getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, null);
        String folder = Util.isTagBrowsing(service) ? null : Util.getSelectedMusicFolderId(service);
        List<Entry> entries = music.getRandomSongs(count, folder, genre, from, to, service, null).getSongs();
        MediaItemsWithStartPosition queue = queue(entries, false, false, null, false);
        List<MediaItem> items = new ArrayList<>(queue.mediaItems);
        Bundle options = new Bundle();
        options.putBoolean(SHUFFLE_PLAY, true);
        items.set(0, items.get(0).buildUpon().setMediaMetadata(items.get(0).mediaMetadata.buildUpon()
                .setExtras(options).build()).build());
        return new MediaItemsWithStartPosition(items, 0, 0);
    }

    private MediaItemsWithStartPosition queue(List<Entry> entries, boolean shuffle, boolean append,
                                              Entry selected, boolean resume) {
        List<Entry> songs = new ArrayList<>();
        for (Entry entry : entries) {
            if (!entry.isVideo() && entry.getRating() != 1) {
                songs.add(entry);
            }
        }
        if (songs.isEmpty()) {
            throw new IllegalArgumentException("No playable media in request");
        }
        if (shuffle) {
            Collections.shuffle(songs);
        }
        int start = selected == null ? 0 : Math.max(0, songs.indexOf(selected));
        long position = 0;
        if (resume && selected != null) {
            if (selected.getBookmark() != null) {
                position = selected.getBookmark().getPosition();
            }
        } else if (resume) {
            for (int i = 0; i < songs.size(); i++) {
                Bookmark bookmark = songs.get(i).getBookmark();
                if (bookmark != null) {
                    start = i;
                    position = bookmark.getPosition();
                    break;
                }
            }
        }
        List<MediaItem> items = new ArrayList<>();
        for (Entry entry : songs) {
            items.add(SessionPlayer.mediaItem(entry));
        }
        if (append) {
            Bundle options = new Bundle();
            options.putBoolean(APPEND, true);
            items.set(0, items.get(0).buildUpon().setMediaMetadata(items.get(0).mediaMetadata.buildUpon()
                    .setExtras(options).build()).build());
        }
        return new MediaItemsWithStartPosition(items, start, position);
    }
}
