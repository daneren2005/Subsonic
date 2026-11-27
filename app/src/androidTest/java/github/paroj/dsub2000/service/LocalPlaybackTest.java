package github.paroj.dsub2000.service;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import github.paroj.dsub2000.domain.MusicDirectory.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@OptIn(markerClass = UnstableApi.class)
public class LocalPlaybackTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final List<File> files = new ArrayList<>();
    private LocalPlayback playback;

    @After
    public void tearDown() {
        if (playback != null) onMain(playback::release);
        for (File file : files) assertTrue(file.delete());
    }

    @Test
    public void queuedSourcesTransitionWithoutReplacingAudioSession() throws Exception {
        DownloadFile first = entry("first");
        DownloadFile second = entry("second");
        File firstAudio = wave(1);
        File secondAudio = wave(1);
        CountDownLatch ended = new CountDownLatch(1);
        AtomicInteger transitions = new AtomicInteger();
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        AtomicInteger session = new AtomicInteger();
        onMain(() -> {
            playback = new LocalPlayback(context, new LocalPlayback.Listener() {
                @Override public void onState(DownloadFile file, int state, boolean playing) {
                    if (state == Player.STATE_ENDED) {
                        assertSame(second, file);
                        ended.countDown();
                    }
                }
                @Override public void onTransition(DownloadFile previous, DownloadFile current) {
                    assertSame(first, previous);
                    assertSame(second, current);
                    transitions.incrementAndGet();
                }
                @Override public void onError(DownloadFile file, PlaybackException failure) {
                    error.set(failure);
                    ended.countDown();
                }
            });
            session.set(playback.getAudioSessionId());
            playback.prepare(first, firstAudio.getPath(), 0, true, 0.5f, 1f);
            playback.setNext(first, second, secondAudio.getPath(), 0.25f);
        });
        assertTrue(ended.await(10, TimeUnit.SECONDS));
        if (error.get() != null) throw error.get();
        assertEquals(1, transitions.get());
        assertTrue(session.get() > 0);
        assertEquals(session.get(), playback.getAudioSessionId());
        assertEquals(1000, playback.getDuration());
        assertEquals(1000, playback.getCurrentPosition());
    }

    @Test
    public void pauseDuringPreparationPreservesSeekAndAllowsOffThreadReads() throws Exception {
        DownloadFile song = entry("paused");
        File audio = wave(5);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        onMain(() -> {
            playback = new LocalPlayback(context, new LocalPlayback.Listener() {
                @Override public void onState(DownloadFile file, int state, boolean playing) {
                    if (state == Player.STATE_READY && !playing) ready.countDown();
                }
                @Override public void onTransition(DownloadFile previous, DownloadFile current) {
                    throw new AssertionError("Unexpected transition");
                }
                @Override public void onError(DownloadFile file, PlaybackException failure) {
                    error.set(failure);
                    ready.countDown();
                }
            });
            playback.prepare(song, audio.getPath(), 2000, true, 1f, 1.5f);
            playback.pause();
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        if (error.get() != null) throw error.get();
        assertEquals(2000, playback.getCurrentPosition());
        assertEquals(5000, playback.getDuration());
    }

    private DownloadFile entry(String id) {
        Entry song = new Entry(id);
        song.setTitle(id);
        song.setSuffix("wav");
        return new DownloadFile(context, song, false);
    }

    private File wave(int seconds) throws Exception {
        File file = File.createTempFile("native-playback-", ".wav", context.getCacheDir());
        files.add(file);
        int length = 8000 * 2 * seconds;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x46464952).putInt(36 + length).putInt(0x45564157);
        header.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
        header.putInt(8000).putInt(16000).putShort((short) 2).putShort((short) 16);
        header.putInt(0x61746164).putInt(length);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(header.array());
            output.write(new byte[length]);
        }
        return file;
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
