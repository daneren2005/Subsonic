package github.paroj.dsub2000.service;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.audio.TeeAudioProcessor;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import github.paroj.serverproxy.BufferFile;
import github.paroj.serverproxy.BufferProxy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@OptIn(markerClass = UnstableApi.class)
public class Media3PlaybackTest {
    private static final int SAMPLE_RATE = 44100;
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final List<File> files = new ArrayList<>();
    private ExoPlayer player;
    private BufferProxy proxy;

    @After
    public void tearDown() {
        onMain(() -> {
            if (player != null) {
                player.release();
            }
        });
        if (proxy != null) {
            proxy.close();
        }
        for (File file : files) {
            assertTrue("Delete generated audio fixture", file.delete());
        }
    }

    @Test
    public void localWavePlaysToEnd() throws Exception {
        File file = wave(pcm(0, SAMPLE_RATE));
        CountDownLatch ended = new CountDownLatch(1);
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        onMain(() -> {
            player = new ExoPlayer.Builder(context).build();
            observeEnd(ended, error);
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
            player.prepare();
            player.play();
        });
        awaitEnd(ended, error);
        onMain(() -> {
            assertEquals(1000, player.getDuration());
            assertEquals(Player.STATE_ENDED, player.getPlaybackState());
        });
    }

    @Test
    public void growingWavePlaysToEndThroughExistingProxy() throws Exception {
        byte[] audio = pcm(0, SAMPLE_RATE * 6);
        File file = wave(audio);
        try (RandomAccessFile partial = new RandomAccessFile(file, "rw")) {
            partial.setLength(44 + audio.length / 2);
        }
        AtomicBoolean complete = new AtomicBoolean();
        proxy = new BufferProxy(context);
        proxy.setBufferFile(new BufferFile() {
            @Override public File getFile() { return file; }
            @Override public Long getContentLength() { return null; }
            @Override public long getEstimatedSize() { return 44 + audio.length; }
            @Override public boolean isWorkDone() { return complete.get(); }
            @Override public void onStart() { }
            @Override public void onStop() { }
            @Override public void onResume() { }
        });
        proxy.start();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch ended = new CountDownLatch(1);
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        onMain(() -> {
            player = new ExoPlayer.Builder(context).build();
            observeEnd(ended, error);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        ready.countDown();
                    }
                }
            });
            player.setMediaItem(MediaItem.fromUri(proxy.getPrivateAddress(file.getPath())));
            player.prepare();
            player.play();
        });
        assertTrue("Playback starts before the download completes", ready.await(10, TimeUnit.SECONDS));
        assertEquals("Partial EOF must not finish playback", 1, ended.getCount());
        try (FileOutputStream output = new FileOutputStream(file, true)) {
            output.write(audio, audio.length / 2, audio.length / 2);
        }
        complete.set(true);
        awaitEnd(ended, error);
        onMain(() -> assertEquals(6000, player.getDuration()));
    }

    @Test
    public void playlistPreservesEveryPcmFrameAcrossTransition() throws Exception {
        byte[] first = pcm(0, SAMPLE_RATE);
        byte[] second = pcm(SAMPLE_RATE, SAMPLE_RATE);
        File firstFile = wave(first);
        File secondFile = wave(second);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        List<String> transitions = new ArrayList<>();
        CountDownLatch ended = new CountDownLatch(1);
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        onMain(() -> {
            DefaultRenderersFactory renderers = new DefaultRenderersFactory(context) {
                @Override
                protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput,
                                                   boolean enableAudioTrackPlaybackParams) {
                    TeeAudioProcessor capture = new TeeAudioProcessor(new TeeAudioProcessor.AudioBufferSink() {
                        @Override
                        public void flush(int sampleRateHz, int channelCount, int encoding) {
                            assertEquals(SAMPLE_RATE, sampleRateHz);
                            assertEquals(1, channelCount);
                            assertEquals(C.ENCODING_PCM_16BIT, encoding);
                        }

                        @Override
                        public void handleBuffer(ByteBuffer buffer) {
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            captured.write(bytes, 0, bytes.length);
                        }
                    });
                    return new DefaultAudioSink.Builder(context)
                            .setAudioProcessors(new AudioProcessor[] { capture })
                            .build();
                }
            };
            player = new ExoPlayer.Builder(context, renderers).build();
            player.addListener(new Player.Listener() {
                @Override
                public void onMediaItemTransition(MediaItem item, int reason) {
                    assertNotNull(item);
                    transitions.add(item.mediaId);
                }
            });
            observeEnd(ended, error);
            DefaultDataSource.Factory sources = new DefaultDataSource.Factory(context);
            player.setMediaSources(Arrays.asList(
                    new ProgressiveMediaSource.Factory(sources).createMediaSource(new MediaItem.Builder()
                            .setMediaId("first").setUri(Uri.fromFile(firstFile)).build()),
                    new ProgressiveMediaSource.Factory(sources).createMediaSource(new MediaItem.Builder()
                            .setMediaId("second").setUri(Uri.fromFile(secondFile)).build())));
            player.prepare();
            player.play();
        });
        awaitEnd(ended, error);
        onMain(() -> assertEquals(Arrays.asList("first", "second"), transitions));
        ByteBuffer expected = ByteBuffer.wrap(pcm(0, SAMPLE_RATE * 2)).order(ByteOrder.LITTLE_ENDIAN);
        assertArrayEquals(expected.array(), captured.toByteArray());
    }

    @Test
    public void seekWhilePausedKeepsRequestedPositionAndSpeed() throws Exception {
        File file = wave(pcm(0, SAMPLE_RATE * 4));
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        onMain(() -> {
            player = new ExoPlayer.Builder(context).build();
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        ready.countDown();
                    }
                }

                @Override
                public void onPlayerError(PlaybackException failure) {
                    error.set(failure);
                    ready.countDown();
                }
            });
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)), 1500);
            player.setPlaybackSpeed(1.5f);
            player.prepare();
        });
        assertTrue("Player prepares", ready.await(15, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        onMain(() -> {
            assertEquals(Player.STATE_READY, player.getPlaybackState());
            assertEquals(1500, player.getCurrentPosition());
            assertEquals(1.5f, player.getPlaybackParameters().speed, 0);
            assertTrue("Prepare must not autoplay", !player.getPlayWhenReady());
        });
    }

    private void observeEnd(CountDownLatch ended, AtomicReference<PlaybackException> error) {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    ended.countDown();
                }
            }

            @Override
            public void onPlayerError(PlaybackException failure) {
                error.set(failure);
                ended.countDown();
            }
        });
    }

    private static void awaitEnd(CountDownLatch ended, AtomicReference<PlaybackException> error)
            throws Exception {
        assertTrue("Playback reaches end", ended.await(15, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
    }

    private File wave(byte[] pcm) throws Exception {
        File file = File.createTempFile("media3-", ".wav", context.getCacheDir());
        files.add(file);
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x46464952).putInt(36 + pcm.length).putInt(0x45564157);
        header.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
        header.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort((short) 2).putShort((short) 16);
        header.putInt(0x61746164).putInt(pcm.length);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(header.array());
            output.write(pcm);
        }
        return file;
    }

    private static byte[] pcm(int firstFrame, int frames) {
        ByteBuffer buffer = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frames; i++) {
            buffer.putShort((short) (8192 * Math.sin((firstFrame + i) * 2 * Math.PI * 440 / SAMPLE_RATE)));
        }
        return buffer.array();
    }

    private static void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
}
