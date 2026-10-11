package github.paroj.dsub2000.service;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.PlayerState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Drives the production queue through two cached tracks and checks the native transition. */
public class DownloadServiceGaplessTest {
    private static final int SECONDS = 3;

    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> activity = new ActivityTestRule<>(SubsonicFragmentActivity.class);
    private DownloadService service;
    private final List<File> files = new ArrayList<>();

    @Before
    public void setUp() {
        service = activity.getActivity().getDownloadService();
        onMain(() -> { service.clear(); return null; });
    }

    @After
    public void tearDown() {
        onMain(() -> { service.clear(); return null; });
        for (File file : files) {
            assertTrue("Remove generated audio", file.delete());
        }
    }

    @Test
    public void queueAdvancesOnceAcrossCachedTracksAndEnds() throws Exception {
        Entry first = song("first");
        Entry second = song("second");
        onMain(() -> { service.download(Arrays.asList(first, second), false, true, false, false); return null; });

        List<Integer> indexes = new ArrayList<>();
        long deadline = android.os.SystemClock.elapsedRealtime() + 20_000;
        boolean sawSecondPlaying = false;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            int index = onMain(service::getCurrentPlayingIndex);
            PlayerState state = onMain(service::getPlayerState);
            if (indexes.isEmpty() || indexes.get(indexes.size() - 1) != index) {
                indexes.add(index);
            }
            if (index == 1 && state == PlayerState.STARTED && !sawSecondPlaying) {
                sawSecondPlaying = true;
                assertTrue("Position restarts for the second track", onMain(service::getPlayerPosition) < SECONDS * 500);
            }
            // The legacy restart path passes through PREPARING for the second track.
            assertTrue("Second track starts without re-preparing", index != 1 || state != PlayerState.PREPARING);
            if (sawSecondPlaying && state != PlayerState.STARTED && state != PlayerState.PREPARING) {
                break;
            }
            Thread.sleep(20);
        }
        assertTrue("Second track played", sawSecondPlaying);
        // Index 0 is the restart after the queue ends; no other index may appear.
        assertEquals(Arrays.asList(0, 1), indexes.subList(0, 2));
        assertTrue("Queue wraps to start without replaying", indexes.size() <= 3);
    }

    private Entry song(String name) throws Exception {
        Entry song = new Entry(UUID.randomUUID().toString());
        song.setTitle(name);
        song.setSuffix("wav");
        song.setDuration(SECONDS);
        File audio = new DownloadFile(activity.getActivity(), song, false).getSaveFile();
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
        files.add(audio);
        return song;
    }

    private static <T> T onMain(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result.set(action.get()));
        return result.get();
    }
}
