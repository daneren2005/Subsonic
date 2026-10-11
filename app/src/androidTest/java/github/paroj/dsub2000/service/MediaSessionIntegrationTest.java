package github.paroj.dsub2000.service;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.compat.RemoteControlClientLP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@OptIn(markerClass = UnstableApi.class)
public class MediaSessionIntegrationTest {
    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> activity = new ActivityTestRule<>(SubsonicFragmentActivity.class);
    private DownloadService service;
    private MediaBrowser browser;
    private MediaBrowserCompat legacy;
    private File audio;

    @Before
    public void setUp() throws Exception {
        service = activity.getActivity().getDownloadService();
        onMain(() -> { service.clear(); return null; });
        ListenableFuture<MediaBrowser> connection = onMain(() -> new MediaBrowser.Builder(
                activity.getActivity(), new SessionToken(activity.getActivity(),
                new ComponentName(activity.getActivity(), DownloadService.class))).buildAsync());
        browser = connection.get(10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        onMain(() -> {
            if (browser != null) browser.release();
            if (legacy != null) legacy.disconnect();
            service.clear();
            return null;
        });
        if (audio != null) {
            assertTrue("Remove generated audio", audio.delete());
        }
    }

    @Test
    public void nativeBrowserConnectsToPlaybackServiceAndPagesCatalogue() throws Exception {
        LibraryResult<MediaItem> root = onMain(() -> browser.getLibraryRoot(null)).get(5, TimeUnit.SECONDS);
        assertEquals(LibraryResult.RESULT_SUCCESS, root.resultCode);
        assertNotNull(root.value);
        assertEquals("root", root.value.mediaId);
        LibraryResult<ImmutableList<MediaItem>> first = onMain(() -> browser.getChildren("root", 0, 2, null))
                .get(5, TimeUnit.SECONDS);
        assertEquals(LibraryResult.RESULT_SUCCESS, first.resultCode);
        assertNotNull(first.value);
        assertEquals(2, first.value.size());
        assertEquals("albumLists", first.value.get(0).mediaId);
        assertEquals("library", first.value.get(1).mediaId);
        LibraryResult<ImmutableList<MediaItem>> second = onMain(() -> browser.getChildren("root", 1, 2, null))
                .get(5, TimeUnit.SECONDS);
        assertEquals("playlists", second.value.get(0).mediaId);
        assertEquals(Player.STATE_IDLE, onMain(browser::getPlaybackState).intValue());
    }

    @Test
    public void unknownParentReturnsErrorInsteadOfEmptySuccess() throws Exception {
        LibraryResult<ImmutableList<MediaItem>> result = onMain(() -> browser.getChildren(
                "unknown-parent", 0, 10, null)).get(5, TimeUnit.SECONDS);
        assertEquals(LibraryResult.RESULT_ERROR_BAD_VALUE, result.resultCode);
    }

    @Test
    public void nativeAndLegacyControllersOperateTheSameCachedTrack() throws Exception {
        Entry song = new Entry(UUID.randomUUID().toString());
        song.setTitle("Media3 session " + song.getId());
        song.setArtist("Session tests");
        song.setAlbum("Generated audio");
        song.setSuffix("wav");
        song.setDuration(10);
        audio = new DownloadFile(activity.getActivity(), song, false).getSaveFile();
        File parent = audio.getParentFile();
        assertTrue("Audio directory exists", parent.isDirectory() || parent.mkdirs());
        int bytes = 8000 * 2 * 10;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x46464952).putInt(36 + bytes).putInt(0x45564157);
        header.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
        header.putInt(8000).putInt(16000).putShort((short) 2).putShort((short) 16);
        header.putInt(0x61746164).putInt(bytes);
        try (FileOutputStream output = new FileOutputStream(audio)) {
            output.write(header.array());
            output.write(new byte[bytes]);
        }
        Bundle extras = new Bundle();
        extras.putByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES, song.toByteArray());
        extras.putString(Constants.INTENT_EXTRA_NAME_PODCAST_ID, song.getId());
        MediaItem request = new MediaItem.Builder().setMediaId(song.getId())
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setExtras(extras).build()).build();
        onMain(() -> {
            browser.setMediaItem(request);
            browser.prepare();
            browser.play();
            return null;
        });
        await(browser::isPlaying);
        await(() -> service.getPlayerPosition() >= 500);
        MediaControllerCompat compat = onMain(() -> new MediaControllerCompat(activity.getActivity(),
                MediaSessionCompat.Token.fromToken(service.getRemoteControlClient().getMediaSession().getPlatformToken())));
        onMain(() -> { compat.getTransportControls().pause(); return null; });
        await(() -> !browser.getPlayWhenReady());
        onMain(() -> { browser.seekTo(3000); return null; });
        await(() -> Math.abs(browser.getCurrentPosition() - 3000) < 200);
        Bundle append = new Bundle();
        append.putByteArray(Constants.INTENT_EXTRA_ENTRY_BYTES, song.toByteArray());
        append.putString(Constants.INTENT_EXTRA_NAME_CHILD_ID, song.getId());
        append.putBoolean(Constants.INTENT_EXTRA_PLAY_LAST, true);
        onMain(() -> { compat.getTransportControls().playFromMediaId(song.getId(), append); return null; });
        await(() -> browser.getMediaItemCount() == 2);
        assertTrue("Play last preserves paused state", onMain(() -> !browser.getPlayWhenReady()));
        assertEquals(0, onMain(browser::getCurrentMediaItemIndex).intValue());
        onMain(() -> { compat.getTransportControls().play(); return null; });
        await(browser::isPlaying);
        assertEquals(song.getId(), onMain(() -> browser.getCurrentMediaItem().mediaId));
        onMain(() -> { browser.pause(); return null; });
        await(() -> !browser.getPlayWhenReady());
    }

    @Test
    public void legacyBrowserUsesSameSessionAndCatalogue() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch loaded = new CountDownLatch(1);
        List<String> ids = new ArrayList<>();
        onMain(() -> {
            legacy = new MediaBrowserCompat(activity.getActivity(),
                    new ComponentName(activity.getActivity(), DownloadService.class),
                    new MediaBrowserCompat.ConnectionCallback() {
                        @Override public void onConnected() { connected.countDown(); }
                    }, Bundle.EMPTY);
            legacy.connect();
            return null;
        });
        assertTrue("Legacy browser connects", connected.await(10, TimeUnit.SECONDS));
        onMain(() -> {
            RemoteControlClientLP remote = service.getRemoteControlClient();
            assertEquals(MediaSessionCompat.Token.fromToken(remote.getMediaSession().getPlatformToken()), legacy.getSessionToken());
            MediaControllerCompat controller = new MediaControllerCompat(activity.getActivity(), legacy.getSessionToken());
            assertNotNull(controller.getPlaybackState());
            legacy.subscribe("root", new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parent, List<MediaBrowserCompat.MediaItem> children) {
                    for (MediaBrowserCompat.MediaItem child : children) {
                        ids.add(child.getMediaId());
                    }
                    loaded.countDown();
                }
            });
            return null;
        });
        assertTrue("Legacy children load", loaded.await(5, TimeUnit.SECONDS));
        assertTrue(ids.contains("albumLists"));
        assertTrue(ids.contains("library"));
        assertTrue(ids.contains("playlists"));
    }

    private static <T> T onMain(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result.set(action.get()));
        return result.get();
    }

    private static void await(Supplier<Boolean> condition) throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + 10_000;
        boolean reached = onMain(condition);
        while (!reached && android.os.SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(50);
            reached = onMain(condition);
        }
        assertTrue("Controller state reaches expected value", reached);
    }
}
