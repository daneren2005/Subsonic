package github.paroj.serverproxy;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@OptIn(markerClass = UnstableApi.class)
public class BufferProxyTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final List<File> files = new ArrayList<>();
    private BufferProxy proxy;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();

    @After
    public void tearDown() throws Exception {
        if (proxy != null) {
            proxy.close();
        }
        reader.shutdownNow();
        assertTrue("Reader shuts down", reader.awaitTermination(10, TimeUnit.SECONDS));
        for (File file : files) {
            assertTrue("Delete proxy fixture", file.delete());
        }
    }

    @Test
    public void inFlightRequestRetainsItsBufferAndLifecycleOwner() throws Exception {
        TestBuffer first = buffer(new byte[] {1, 2, 3}, 3L);
        TestBuffer second = buffer(new byte[] {4, 5}, 2L);
        proxy = new BufferProxy(context);
        proxy.setBufferFile(first);
        try (Socket socket = new Socket()) {
            BufferProxy.BufferFileTask task = (BufferProxy.BufferFileTask) proxy.getTask(socket);
            assertEquals(first.file, task.getFile(first.file.getPath()));
            task.onStart();
            proxy.setBufferFile(second);
            assertEquals("An in-flight request must keep its original length", Long.valueOf(3),
                    task.getContentLength());
            task.onResume();
            task.onStop();
        }
        assertEquals(1, first.starts);
        assertEquals(1, first.resumes);
        assertEquals(1, first.stops);
        assertEquals(0, second.stops);
    }

    @Test
    public void media3ReadsGrowingFileWithKnownLength() throws Exception {
        assertGrowingFile(8L);
    }

    @Test
    public void staleRequestCannotReadNewlySelectedTrack() throws Exception {
        TestBuffer first = buffer(new byte[] {1, 2, 3}, 3L);
        TestBuffer second = buffer(new byte[] {4, 5}, 2L);
        proxy = new BufferProxy(context);
        proxy.setBufferFile(first);
        proxy.setBufferFile(second);
        try (Socket socket = new Socket()) {
            BufferProxy.BufferFileTask task = (BufferProxy.BufferFileTask) proxy.getTask(socket);
            assertNull(task.getFile(first.file.getPath()));
            assertEquals(second.file, task.getFile(second.file.getPath()));
        }
    }

    @Test
    public void requestPathSurvivesDownloadCompletionBeforeBinding() throws Exception {
        TestBuffer completed = buffer(new byte[] {1, 2, 3}, 3L);
        String partialPath = completed.file.getPath() + ".old";
        proxy = new BufferProxy(context);
        proxy.setBufferFile(completed, partialPath);
        try (Socket socket = new Socket()) {
            BufferProxy.BufferFileTask task = (BufferProxy.BufferFileTask) proxy.getTask(socket);
            assertEquals(completed.file, task.getFile(partialPath));
        }
    }

    @Test
    public void rejectedRequestClosesConnectionInsteadOfHanging() throws Exception {
        TestBuffer buffer = buffer(new byte[] {1, 2, 3}, 3L);
        startProxy(buffer);
        Uri uri = Uri.parse(proxy.getPrivateAddress("/not-the-selected-buffer"));
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(("GET " + uri.getEncodedPath()
                    + " HTTP/1.0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            assertEquals(-1, socket.getInputStream().read());
        }
    }

    @Test
    public void seekBeyondKnownFinalLengthIsRejectedEvenWhileDownloading() throws Exception {
        TestBuffer buffer = buffer(new byte[] {1, 2}, 8L);
        startProxy(buffer);
        Uri uri = Uri.parse(proxy.getPrivateAddress(buffer.file.getPath()));
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(("GET " + uri.getEncodedPath()
                    + " HTTP/1.0\r\nRange: bytes=9-\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            assertEquals(-1, socket.getInputStream().read());
        }
        assertEquals(0, buffer.starts);
    }

    @Test
    public void media3ReadsGrowingFileWithUnknownLength() throws Exception {
        assertGrowingFile(null);
    }

    @Test
    public void media3SeeksWithinCompletedFile() throws Exception {
        byte[] contents = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        TestBuffer buffer = buffer(contents, (long) contents.length);
        buffer.done = true;
        startProxy(buffer);
        assertArrayEquals(Arrays.copyOfRange(contents, 3, contents.length), read(buffer, 3));
    }

    @Test
    public void media3SeeksBeyondDownloadedBytes() throws Exception {
        assertSeekBeyondDownloadedBytes(8L);
    }

    @Test
    public void media3SeeksBeyondDownloadedBytesWithUnknownLength() throws Exception {
        assertSeekBeyondDownloadedBytes(null);
    }

    @Test
    public void closingProxyStopsStarvedReaderAndReleasesBuffer() throws Exception {
        TestBuffer buffer = buffer(new byte[] {1, 2, 3, 4}, null);
        startProxy(buffer);
        Future<byte[]> result = reader.submit(() -> read(buffer, 0));
        assertTrue("Read the initial bytes", buffer.firstChunk.await(5, TimeUnit.SECONDS));
        proxy.close();
        assertArrayEquals(new byte[] {1, 2, 3, 4}, result.get(5, TimeUnit.SECONDS));
        assertTrue("Release the file after stopping", buffer.stopped.await(5, TimeUnit.SECONDS));
    }

    private void assertSeekBeyondDownloadedBytes(Long contentLength) throws Exception {
        TestBuffer buffer = buffer(new byte[] {1, 2}, contentLength);
        startProxy(buffer);
        Future<byte[]> result = reader.submit(() -> read(buffer, 4));
        assertTrue("Seeking should wait for the downloader, not reject the request",
                buffer.started.await(5, TimeUnit.SECONDS));
        try (FileOutputStream output = new FileOutputStream(buffer.file, true)) {
            output.write(new byte[] {3, 4, 5, 6, 7, 8});
        }
        buffer.done = true;
        assertArrayEquals(new byte[] {5, 6, 7, 8}, result.get(10, TimeUnit.SECONDS));
    }

    private void assertGrowingFile(Long contentLength) throws Exception {
        TestBuffer buffer = buffer(new byte[] {1, 2, 3, 4}, contentLength);
        startProxy(buffer);
        Future<byte[]> result = reader.submit(() -> read(buffer, 0));
        assertTrue("Proxy opens the source", buffer.started.await(5, TimeUnit.SECONDS));
        assertTrue("Read the initial bytes", buffer.firstChunk.await(5, TimeUnit.SECONDS));
        try {
            result.get(200, TimeUnit.MILLISECONDS);
            fail("Temporary EOF must wait for the downloader");
        } catch (TimeoutException expected) {
            // Deliberately starve the source before completing the file.
        }
        try (FileOutputStream output = new FileOutputStream(buffer.file, true)) {
            output.write(new byte[] {5, 6, 7, 8});
        }
        buffer.done = true;
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, result.get(10, TimeUnit.SECONDS));
    }

    private byte[] read(TestBuffer buffer, long position) throws Exception {
        DefaultHttpDataSource source = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(3000).setReadTimeoutMs(3000).createDataSource();
        try {
            source.open(new DataSpec.Builder()
                    .setUri(Uri.parse(proxy.getPrivateAddress(buffer.file.getPath())))
                    .setPosition(position).build());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] bytes = new byte[16];
            int count;
            while ((count = source.read(bytes, 0, bytes.length)) != C.RESULT_END_OF_INPUT) {
                output.write(bytes, 0, count);
                if (output.size() >= 4) {
                    buffer.firstChunk.countDown();
                }
            }
            return output.toByteArray();
        } finally {
            source.close();
        }
    }

    private void startProxy(TestBuffer buffer) {
        proxy = new BufferProxy(context);
        proxy.setBufferFile(buffer);
        proxy.start();
    }

    private TestBuffer buffer(byte[] contents, Long length) throws Exception {
        File file = File.createTempFile("buffer-proxy-", ".partial", context.getCacheDir());
        files.add(file);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(contents);
        }
        return new TestBuffer(file, length);
    }

    private static final class TestBuffer implements BufferFile {
        private final File file;
        private final Long length;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch firstChunk = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
        private volatile boolean done;
        private int starts;
        private int stops;
        private int resumes;

        TestBuffer(File file, Long length) {
            this.file = file;
            this.length = length;
        }

        @Override public File getFile() { return file; }
        @Override public Long getContentLength() { return length; }
        @Override public long getEstimatedSize() { return 8; }
        @Override public boolean isWorkDone() { return done; }
        @Override public void onStart() { starts++; started.countDown(); }
        @Override public void onStop() { stops++; stopped.countDown(); }
        @Override public void onResume() { resumes++; }
    }
}
