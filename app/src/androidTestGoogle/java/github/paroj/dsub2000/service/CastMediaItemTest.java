package github.paroj.dsub2000.service;

import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.CastOptions;

import org.junit.Test;

import github.paroj.dsub2000.util.compat.DSubCastOptionsProvider;
import github.paroj.dsub2000.util.compat.GoogleCompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@OptIn(markerClass = UnstableApi.class)
public class CastMediaItemTest {
    private final ChromeCastController.RadioItemConverter converter = new ChromeCastController.RadioItemConverter();

    @Test
    public void receiverUrlAndMetadataSurviveConversion() {
        MediaItem item = new MediaItem.Builder().setMediaId("track-id")
                .setUri("http://127.0.0.1/music.flac").setMimeType("audio/flac")
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Title").setArtist("Artist")
                        .setAlbumTitle("Album").setArtworkUri(Uri.parse("http://127.0.0.1/cover.jpg"))
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()).build();
        MediaQueueItem converted = converter.toMediaQueueItem(item);
        MediaInfo info = converted.getMedia();
        assertEquals("track-id", info.getContentId());
        assertEquals("http://127.0.0.1/music.flac", info.getContentUrl());
        assertEquals("audio/flac", info.getContentType());
        assertEquals(MediaInfo.STREAM_TYPE_BUFFERED, info.getStreamType());
        MediaItem restored = converter.toMediaItem(converted);
        assertEquals(item.mediaId, restored.mediaId);
        assertEquals(item.localConfiguration.uri, restored.localConfiguration.uri);
        assertEquals("Title", restored.mediaMetadata.title);
        assertEquals("Artist", restored.mediaMetadata.artist);
    }

    @Test
    public void internetRadioUsesLiveReceiverStreamType() {
        MediaItem item = new MediaItem.Builder().setMediaId("radio")
                .setUri("http://127.0.0.1/radio.ogg").setMimeType("audio/ogg")
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Radio")
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION).build()).build();
        assertEquals(MediaInfo.STREAM_TYPE_LIVE, converter.toMediaQueueItem(item).getMedia().getStreamType());
    }

    @Test
    public void castFrameworkDoesNotCreateCompetingNotification() {
        CastOptions options = new DSubCastOptionsProvider().getCastOptions(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        assertEquals(GoogleCompat.castApplicationId(), options.getReceiverApplicationId());
        assertNull(options.getCastMediaOptions().getNotificationOptions());
    }
}
