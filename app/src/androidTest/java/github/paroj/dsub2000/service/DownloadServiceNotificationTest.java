package github.paroj.dsub2000.service;

import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;

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

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.util.Notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Media3 session drives the one playback notification and the service foreground state. */
public class DownloadServiceNotificationTest {
    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> activity = new ActivityTestRule<>(SubsonicFragmentActivity.class);
    private DownloadService service;
    private File audio;

    @Before
    public void setUp() {
        service = activity.getActivity().getDownloadService();
        onMain(() -> { service.clear(); return null; });
    }

    @After
    public void tearDown() {
        onMain(() -> { service.clear(); return null; });
        if (audio != null) {
            assertTrue("Remove generated audio", audio.delete());
        }
    }

    @Test
    public void playbackNotificationFollowsPlayPauseAndStop() throws Exception {
        Entry song = new Entry(UUID.randomUUID().toString());
        song.setTitle("Notification " + song.getId());
        song.setSuffix("wav");
        song.setDuration(30);
        audio = new DownloadFile(activity.getActivity(), song, false).getSaveFile();
        File parent = audio.getParentFile();
        assertTrue("Audio directory exists", parent.isDirectory() || parent.mkdirs());
        int bytes = 8000 * 2 * 30;
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
        await(() -> service.getPlayerState() == PlayerState.STARTED && notificationShown());
        assertTrue("Playing notification owns the foreground", onMain(service::isForeground));

        onMain(() -> { service.pause(); return null; });
        await(() -> service.getPlayerState() == PlayerState.PAUSED);
        assertTrue("Paused notification persists by default", onMain(this::notificationShown));

        onMain(() -> { service.clear(); return null; });
        await(() -> !notificationShown());
        assertFalse(onMain(service::isForeground));
    }

    private boolean notificationShown() {
        NotificationManager manager = activity.getActivity().getSystemService(NotificationManager.class);
        for (StatusBarNotification active : manager.getActiveNotifications()) {
            if (active.getId() == Notifications.NOTIFICATION_ID_PLAYING) return true;
        }
        return false;
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
        assertTrue("State reaches expected value", reached);
    }
}
