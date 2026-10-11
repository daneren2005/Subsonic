package github.paroj.dsub2000.service;

import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@OptIn(markerClass = UnstableApi.class)
public class NoRangeHttpDataSourceTest {
    @Test
    public void suppressesRangeHeaderForRadioServer() throws Exception {
        assertRequest(false, false);
    }

    @Test
    public void followsRelativeRadioRedirectWithoutAddingRange() throws Exception {
        assertRequest(true, false);
    }

    @Test
    public void reportsHttpFailureInsteadOfEmptySuccessfulStream() throws Exception {
        assertRequest(false, true);
    }

    private void assertRequest(boolean redirect, boolean reject) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5000);
            Future<?> response = executor.submit(() -> {
                try {
                    if (redirect) {
                        respond(server, "/radio", "302 Found", "Location: /stream\r\n", "");
                    }
                    respond(server, redirect ? "/stream" : "/radio",
                            reject ? "404 Not Found" : "200 OK", "", reject ? "" : "radio");
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
            NoRangeHttpDataSource source = new NoRangeHttpDataSource();
            try {
                DataSpec spec = new DataSpec.Builder()
                        .setUri(Uri.parse("http://127.0.0.1:" + server.getLocalPort() + "/radio"))
                        .setHttpRequestHeaders(Collections.singletonMap("Range", "bytes=0-"))
                        .build();
                if (reject) {
                    try {
                        source.open(spec);
                        fail("An HTTP error must reach the caller");
                    } catch (IOException expected) {
                        assertTrue(expected.getMessage().contains("404"));
                    }
                } else {
                    assertEquals(C.LENGTH_UNSET, source.open(spec));
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16];
                    int count;
                    while ((count = source.read(buffer, 0, buffer.length)) != C.RESULT_END_OF_INPUT) {
                        bytes.write(buffer, 0, count);
                    }
                    assertArrayEquals("radio".getBytes(StandardCharsets.US_ASCII), bytes.toByteArray());
                }
                response.get(5, TimeUnit.SECONDS);
            } finally {
                source.close();
            }
        } finally {
            executor.shutdownNow();
            assertTrue("Radio fixture shuts down", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void respond(ServerSocket server, String path, String status, String headers,
                                String body) throws IOException {
        try (Socket client = server.accept()) {
            client.setSoTimeout(5000);
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    client.getInputStream(), StandardCharsets.US_ASCII));
            assertEquals("GET " + path + " HTTP/1.1", input.readLine());
            String line;
            while ((line = input.readLine()) != null && !line.isEmpty()) {
                assertFalse("Radio must not receive Range requests",
                        line.toLowerCase(Locale.ROOT).startsWith("range:"));
            }
            client.getOutputStream().write(("HTTP/1.1 " + status + "\r\n" + headers
                    + "Content-Length: " + body.length() + "\r\nConnection: close\r\n\r\n"
                    + body).getBytes(StandardCharsets.US_ASCII));
        }
    }
}
