package github.paroj.dsub2000.service;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import github.paroj.dsub2000.domain.Bookmark;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@OptIn(markerClass = UnstableApi.class)
public class SessionQueueResolverTest {
    private SessionQueueResolver resolver;
    private SessionLibrary library;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TestService service = new TestService(InstrumentationRegistry.getInstrumentation().getTargetContext());
            library = new SessionLibrary(service);
            resolver = new SessionQueueResolver(service, library);
        });
    }

    @After
    public void tearDown() {
        resolver.release();
        library.release();
    }

    @Test
    public void browsedPlayOptionResolvesByIdWithoutResendingExtras() throws Exception {
        library.children("md-album", 0, 10).get(5, TimeUnit.SECONDS);
        Entry first = entry("first");
        Entry second = entry("second");
        second.setBookmark(new Bookmark(3456));
        MediaItemsWithStartPosition queue = resolver.resolveRequest(music(first, second),
                new MediaItem.Builder().setMediaId("play-album").setRequestMetadata(
                        new MediaItem.RequestMetadata.Builder().setExtras(Bundle.EMPTY).build()).build());
        assertEquals(2, queue.mediaItems.size());
        assertEquals(1, queue.startIndex);
        assertEquals(3456, queue.startPositionMs);
    }

    @Test
    public void legacyPodcastExtrasPreserveBookmarkPosition() throws Exception {
        Entry podcast = entry("episode");
        podcast.setBookmark(new Bookmark(4567));
        Bundle extras = new Bundle();
        extras.putByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES, podcast.toByteArray());
        extras.putString(Constants.INTENT_EXTRA_NAME_PODCAST_ID, podcast.getId());
        MediaItem request = new MediaItem.Builder().setMediaId("po-episode")
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setExtras(extras).build()).build();
        MediaItemsWithStartPosition queue = resolver.resolveRequest(music(), request);
        assertEquals(1, queue.mediaItems.size());
        assertEquals("episode", queue.mediaItems.get(0).mediaId);
        assertEquals(4567, queue.startPositionMs);
    }

    @Test
    public void albumExpansionPreservesSelectedSongAndExcludesDislikedTracks() throws Exception {
        Entry disliked = entry("disliked");
        disliked.setRating(1);
        Entry first = entry("first");
        Entry selected = entry("selected");
        Bundle extras = new Bundle();
        extras.putByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES, selected.toByteArray());
        extras.putString(Constants.INTENT_EXTRA_NAME_CHILD_ID, selected.getId());
        MediaItem request = new MediaItem.Builder().setMediaId(selected.getId())
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setExtras(extras).build()).build();
        MediaItemsWithStartPosition queue = resolver.resolveRequest(music(disliked, first, selected), request);
        assertEquals(2, queue.mediaItems.size());
        assertEquals(1, queue.startIndex);
        assertEquals("selected", queue.mediaItems.get(queue.startIndex).mediaId);
    }

    @Test
    public void playLastIsResolvedAsAppendOperation() throws Exception {
        List<MediaItem> options = library.children("pl-playlist", 0, 10).get(5, TimeUnit.SECONDS);
        MediaItemsWithStartPosition queue = resolver.resolveRequest(music(entry("last")), options.get(2));
        assertTrue(queue.mediaItems.get(0).mediaMetadata.extras.getBoolean(SessionQueueResolver.APPEND));
    }

    @Test
    public void normalSongSelectionDoesNotRestoreServerBookmark() throws Exception {
        Entry selected = entry("selected");
        Bundle extras = new Bundle();
        extras.putByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES, selected.toByteArray());
        extras.putString(Constants.INTENT_EXTRA_NAME_CHILD_ID, selected.getId());
        selected.setBookmark(new Bookmark(9876));
        MediaItem request = new MediaItem.Builder().setMediaId(selected.getId())
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setExtras(extras).build()).build();
        MediaItemsWithStartPosition queue = resolver.resolveRequest(music(selected), request);
        assertEquals(0, queue.startPositionMs);
        assertNotNull(selected.getBookmark());
    }

    @Test
    public void unknownIdFailsExplicitlyWithoutReplacingQueue() throws Exception {
        try {
            resolver.resolve(Collections.singletonList(new MediaItem.Builder().setMediaId("unknown").build()),
                    0, 0).get(5, TimeUnit.SECONDS);
            fail("Unknown ID must not resolve to an empty successful queue");
        } catch (ExecutionException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void changingMusicFolderInvalidatesCachedMediaIds() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String original = Util.getSelectedMusicFolderId(context);
        try {
            library.children("md-album", 0, 10).get(5, TimeUnit.SECONDS);
            assertNotNull(library.getItem("play-album"));
            Util.setSelectedMusicFolderId(context, "session-test-folder");
            assertNull(library.getItem("play-album"));
            assertEquals(SessionLibrary.ROOT, library.getItem(SessionLibrary.ROOT).mediaId);
        } finally {
            Util.setSelectedMusicFolderId(context, original);
        }
    }

    private static Entry entry(String id) {
        Entry entry = new Entry(id);
        entry.setTitle(id);
        entry.setParent("directory");
        entry.setAlbumId("album");
        entry.setDuration(30);
        return entry;
    }

    private static MusicService music(Entry... songs) {
        return (MusicService) Proxy.newProxyInstance(MusicService.class.getClassLoader(),
                new Class<?>[] {MusicService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getAlbum") || method.getName().equals("getMusicDirectory")
                            || method.getName().equals("getPlaylist")) {
                        MusicDirectory directory = new MusicDirectory();
                        for (Entry song : songs) directory.addChild(song);
                        return directory;
                    }
                    throw new AssertionError("Unexpected music service call: " + method.getName());
                });
    }

    private static final class TestService extends DownloadService {
        TestService(Context context) { attachBaseContext(context); }
        @Override public List<DownloadFile> getSongs() { return Collections.emptyList(); }
    }
}
