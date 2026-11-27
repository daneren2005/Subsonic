package github.paroj.dsub2000.service;

import android.content.Context;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Exercises the REST client against the server configured in the app. This is the only test that
 * uses the network; keep it to the minimum number of requests so the server is not hammered.
 */
public class ServerIntegrationTest {
    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> activity =
            new ActivityTestRule<>(SubsonicFragmentActivity.class);

    @Test
    public void configuredServerCanBrowseAndStream() throws Exception {
        Context context = activity.getActivity();
        MusicService service = MusicServiceFactory.getMusicService(context, true);

        // One metadata request covers authentication, response parsing and song lookup.
        MusicDirectory random = service.getRandomSongs(1, context, null);
        List<MusicDirectory.Entry> songs = random.getSongs();
        assertFalse("Server returns a song", songs.isEmpty());
        MusicDirectory.Entry song = songs.get(0);

        assertNotNull("Server provides a stream URL", service.getMusicUrl(context, song, 0));

        // The audio stream is a separate endpoint, so this is the second and last request.
        HttpURLConnection connection = service.getDownloadInputStream(context, song, 0, 0, null);
        try {
            InputStream stream = connection.getInputStream();
            assertNotEquals("Server streams audio data", -1, stream.read());
        } finally {
            connection.disconnect();
        }
    }
}
