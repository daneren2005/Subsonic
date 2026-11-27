package github.paroj.dsub2000.fragments;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.service.DownloadFile;
import github.paroj.dsub2000.service.DownloadService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pins the transport actions on the Now Playing screen. */
public class NowPlayingFragmentTest {
    private static final int SECONDS = 30;

    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> activity = new ActivityTestRule<>(SubsonicFragmentActivity.class);
    private DownloadService service;
    private File audio;

    @Before
    public void setUp() throws Exception {
        service = activity.getActivity().getDownloadService();
        onMain(() -> { service.clear(); return null; });
        Entry song = new Entry(UUID.randomUUID().toString());
        song.setTitle("Now Playing " + song.getId());
        song.setSuffix("wav");
        song.setDuration(SECONDS);
        audio = new DownloadFile(activity.getActivity(), song, false).getSaveFile();
        File parent = audio.getParentFile();
        assertTrue("Audio directory exists", parent.isDirectory() || parent.mkdirs());
        int bytes = 8000 * 2 * SECONDS;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x46464952).putInt(36 + bytes).putInt(0x45564157);
        header.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
        header.putInt(8000).putInt(16000).putShort((short) 2).putShort((short) 16);
        header.putInt(0x61746164).putInt(bytes);
        try (FileOutputStream output = new FileOutputStream(audio)) {
            output.write(header.array());
            output.write(new byte[bytes]);
        }
        onMain(() -> { service.download(Collections.singletonList(song), false, true, false, false); return null; });
        awaitState(PlayerState.STARTED);
    }

    @After
    public void tearDown() {
        onMain(() -> { service.clear(); return null; });
        assertTrue("Remove generated audio", audio.delete());
    }

    /** A permanent audio-focus loss leaves the service in PAUSED_TEMP; Play must still resume. */
    @Test
    public void playResumesAfterTemporaryPause() throws Exception {
        onMain(() -> { service.pause(true); return null; });
        awaitState(PlayerState.PAUSED_TEMP);
        onMain(() -> { startButton().performClick(); return null; });
        awaitState(PlayerState.STARTED);
        int resumed = position();
        Thread.sleep(400);
        assertTrue("Position advances after resume", position() > resumed);
    }

    private View startButton() {
        FragmentManager manager = activity.getActivity().getSupportFragmentManager();
        manager.executePendingTransactions();
        for (Fragment fragment : manager.getFragments()) {
            if (fragment instanceof NowPlayingFragment && fragment.getView() != null) {
                View button = fragment.getView().findViewById(R.id.download_start);
                if (button != null) {
                    return button;
                }
            }
        }
        throw new AssertionError("Now Playing start button not found");
    }

    private int position() {
        return onMain(service::getPlayerPosition);
    }

    private void awaitState(PlayerState expected) throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + 10_000;
        PlayerState state = onMain(service::getPlayerState);
        while (state != expected && android.os.SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(25);
            state = onMain(service::getPlayerState);
        }
        assertEquals(expected, state);
    }

    private static <T> T onMain(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result.set(action.get()));
        return result.get();
    }
}
