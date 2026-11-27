package github.paroj.dsub2000.service;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import github.paroj.dsub2000.domain.MusicDirectory.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/** Cache cleanup must not run while holding the service monitor the main thread needs. */
public class DownloadServiceCleanupTest {
    @Test
    public void cleanupRunsOffTheServiceLock() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // MediaSessionService's superclass creates a Handler in its constructor, so the
        // service has to be instantiated on a thread with a Looper (the main thread).
        AtomicReference<FakeService> serviceRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> serviceRef.set(new FakeService(context)));
        FakeService service = serviceRef.get();

        CountDownLatch cleaning = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> cleanupThread = new AtomicReference<>();
        DownloadFile stale = new DownloadFile(service, entry("stale"), false) {
            @Override
            public boolean cleanup() {
                cleanupThread.set(Thread.currentThread());
                cleaning.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        };
        service.cleanupCandidates.add(stale);

        try {
            service.scheduleCleanup();
            assertTrue("cleanup did not start", cleaning.await(5, TimeUnit.SECONDS));
            assertNotSame(Thread.currentThread(), cleanupThread.get());

            // While the worker is deleting files, the service monitor has to stay
            // available for the media session's getState().
            AtomicBoolean lockAcquired = new AtomicBoolean();
            CountDownLatch lockAttempted = new CountDownLatch(1);
            Thread lockThread = new Thread(() -> {
                synchronized (service) {
                    lockAcquired.set(true);
                    lockAttempted.countDown();
                }
            });
            lockThread.start();
            assertTrue("service lock blocked during cleanup",
                    lockAttempted.await(5, TimeUnit.SECONDS));
            assertTrue(lockAcquired.get());

            release.countDown();
            lockThread.join(5000);

            long deadline = System.currentTimeMillis() + 5000;
            while (!service.cleanupCandidates.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(0, service.cleanupCandidates.size());
        } finally {
            release.countDown();
        }
    }

    private static Entry entry(String id) {
        Entry entry = new Entry(id);
        entry.setTitle(id);
        entry.setDuration(30);
        entry.setSuffix("wav");
        return entry;
    }

    private static final class FakeService extends DownloadService {
        FakeService(Context context) { attachBaseContext(context); }
    }
}
