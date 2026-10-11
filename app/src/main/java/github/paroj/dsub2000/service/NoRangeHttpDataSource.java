/*
 This file is part of DSub2000.
 Released under GPLv3, see project LICENSE.txt.
*/
package github.paroj.dsub2000.service;

import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Minimal {@link DataSource} that fetches an HTTP/HTTPS URL with a plain GET,
 * never sending a {@code Range} header.
 *
 * Some Icecast/Liquidsoap radio servers respond with HTTP 404 when a Range
 * header is present (e.g. {@code https://zeppelin.streampunk.cc/_stream/...}).
 * ExoPlayer's default HTTP data source always issues {@code Range: bytes=0-}
 * to determine the resource length, which trips that misconfiguration. This
 * implementation deliberately omits Range and reports an unknown length, which
 * is fine for live audio streams that don't need seeking.
 *
 * Used only by {@link LocalPlayback} for stream playback (Internet radios).
 * Not intended for general-purpose use: it does not support range requests,
 * resumption, or content-length reporting.
 */
@OptIn(markerClass = UnstableApi.class)
public class NoRangeHttpDataSource extends BaseDataSource {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    /** Max redirects to follow manually (HTTP→HTTPS and viceversa). */
    private static final int MAX_REDIRECTS = 5;

    private DataSpec dataSpec;
    private HttpURLConnection connection;
    private InputStream inputStream;
    private boolean opened;

    public NoRangeHttpDataSource() {
        super(/* isNetwork= */ true);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        this.dataSpec = dataSpec;
        transferInitializing(dataSpec);

        URL currentUrl = new URL(dataSpec.uri.toString());
        int redirects = 0;
        while (true) {
            connection = (HttpURLConnection) currentUrl.openConnection();
            connection.setRequestMethod("GET");
            // We follow redirects manually so we can handle HTTP↔HTTPS hops
            // (Java's setInstanceFollowRedirects refuses cross-protocol).
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            // Forward any caller-supplied request headers, but never Range — that's
            // the whole reason this data source exists.
            if (dataSpec.httpRequestHeaders != null) {
                for (Map.Entry<String, String> entry : dataSpec.httpRequestHeaders.entrySet()) {
                    if (!"Range".equalsIgnoreCase(entry.getKey())) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }

            int responseCode;
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                disconnectQuietly();
                throw e;
            }

            if (responseCode >= 300 && responseCode < 400) {
                String location = connection.getHeaderField("Location");
                disconnectQuietly();
                if (location == null) {
                    throw new IOException("HTTP " + responseCode + " without Location header for " + dataSpec.uri);
                }
                if (++redirects > MAX_REDIRECTS) {
                    throw new IOException("Too many redirects (" + MAX_REDIRECTS + ") starting at " + dataSpec.uri);
                }
                // Resolve relative Location against the URL we just queried.
                currentUrl = new URL(currentUrl, location);
                continue;
            }

            if (responseCode < 200 || responseCode >= 300) {
                disconnectQuietly();
                throw new IOException("Unexpected HTTP " + responseCode + " for " + currentUrl);
            }

            inputStream = connection.getInputStream();
            opened = true;
            transferStarted(dataSpec);
            return C.LENGTH_UNSET;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        int n = inputStream.read(buffer, offset, length);
        if (n == -1) {
            return C.RESULT_END_OF_INPUT;
        }
        bytesTransferred(n);
        return n;
    }

    @Override
    public Uri getUri() {
        return dataSpec == null ? null : dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } finally {
            inputStream = null;
            disconnectQuietly();
            if (opened) {
                opened = false;
                transferEnded();
            }
        }
    }

    private void disconnectQuietly() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception ignored) {
                // disconnect() is best-effort
            }
            connection = null;
        }
    }

    /** Factory that produces fresh {@link NoRangeHttpDataSource} instances. */
    public static class Factory implements DataSource.Factory {
        @Override
        public DataSource createDataSource() {
            return new NoRangeHttpDataSource();
        }
    }
}
