package github.paroj.dsub2000.service;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.paroj.dsub2000.domain.MusicDirectory.Entry;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.domain.RepeatMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@OptIn(markerClass = UnstableApi.class)
public class SessionPlayerTest {
    private FakeService service;
    private SessionPlayer player;

    @Before
    public void setUp() {
        onMain(() -> {
            service = new FakeService(InstrumentationRegistry.getInstrumentation().getTargetContext());
            player = new SessionPlayer(service);
        });
    }

    @After
    public void tearDown() {
        onMain(() -> player.release());
    }

    @Test
    public void emptyQueueHasIdleStateAndNoSeekCommands() {
        onMain(() -> {
            assertEquals(0, player.getMediaItemCount());
            assertEquals(Player.STATE_IDLE, player.getPlaybackState());
            assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM));
            player.play();
            assertEquals(0, service.starts);
        });
    }

    @Test
    public void duplicateSongsKeepDistinctTimelineIdentities() {
        onMain(() -> {
            Entry song = entry("duplicate");
            player.setMediaItems(Arrays.asList(SessionPlayer.mediaItem(song), SessionPlayer.mediaItem(song)), 1, 2500);
            assertEquals(2, player.getMediaItemCount());
            assertEquals(1, player.getCurrentMediaItemIndex());
            assertEquals(2500, player.getCurrentPosition());
            Timeline timeline = player.getCurrentTimeline();
            assertNotEquals(timeline.getWindow(0, new Timeline.Window()).uid,
                    timeline.getWindow(1, new Timeline.Window()).uid);
        });
    }

    @Test
    public void transportCommandsReachExistingPlaybackPolicy() {
        onMain(() -> {
            player.setMediaItems(Arrays.asList(SessionPlayer.mediaItem(entry("first")),
                    SessionPlayer.mediaItem(entry("second"))));
            player.prepare();
            player.play();
            assertEquals(1, service.starts);
            assertTrue(player.getPlayWhenReady());
            player.seekTo(1234);
            assertEquals(1234, service.position);
            player.seekToNext();
            assertEquals(1, service.nextCalls);
            assertEquals(1, player.getCurrentMediaItemIndex());
            player.seekToPrevious();
            assertEquals(1, service.previousCalls);
            player.pause();
            assertEquals(PlayerState.PAUSED, service.state);
            assertFalse(player.getPlayWhenReady());
        });
    }

    @Test
    public void queueMovesRetainCurrentEntryAndRepeatSpeedArePublished() {
        onMain(() -> {
            player.setMediaItems(Arrays.asList(SessionPlayer.mediaItem(entry("first")),
                    SessionPlayer.mediaItem(entry("second"))));
            DownloadFile current = service.getCurrentPlaying();
            player.moveMediaItem(0, 1);
            assertSame(current, service.getCurrentPlaying());
            assertEquals(1, player.getCurrentMediaItemIndex());
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.setPlaybackSpeed(1.5f);
            assertEquals(RepeatMode.ALL, service.repeat);
            assertEquals(1.5f, player.getPlaybackParameters().speed, 0);
        });
    }

    @Test
    public void externalStateChangesAreReflectedWithoutReplacingQueue() {
        onMain(() -> {
            player.setMediaItem(SessionPlayer.mediaItem(entry("first")));
            service.state = PlayerState.PREPARING;
            player.refresh();
            assertEquals(Player.STATE_BUFFERING, player.getPlaybackState());
            service.state = PlayerState.PAUSED;
            service.position = 3000;
            player.refresh();
            assertEquals(Player.STATE_READY, player.getPlaybackState());
            assertEquals(3000, player.getCurrentPosition());
            player.release();
            player.refresh();
        });
    }

    @Test
    public void stopPublishesIdleAndQueueReplacementPreservesPlayIntent() {
        onMain(() -> {
            player.setMediaItem(SessionPlayer.mediaItem(entry("first")));
            player.prepare();
            player.play();
            player.setMediaItem(SessionPlayer.mediaItem(entry("replacement")));
            assertTrue(player.getPlayWhenReady());
            player.stop();
            assertEquals(Player.STATE_IDLE, player.getPlaybackState());
            assertFalse(player.getPlayWhenReady());
            assertEquals(1, player.getMediaItemCount());
        });
    }

    private static Entry entry(String id) {
        Entry entry = new Entry(id);
        entry.setTitle(id);
        entry.setDuration(30);
        entry.setSuffix("wav");
        return entry;
    }

    private static void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private static final class FakeService extends DownloadService {
        final List<DownloadFile> queue = new ArrayList<>();
        int index = -1;
        int position;
        int starts;
        int nextCalls;
        int previousCalls;
        float speed = 1f;
        RepeatMode repeat = RepeatMode.OFF;
        PlayerState state = PlayerState.IDLE;

        FakeService(Context context) { attachBaseContext(context); }
        @Override public List<DownloadFile> getSongs() { return queue; }
        @Override public int size() { return queue.size(); }
        @Override public DownloadFile getCurrentPlaying() { return index < 0 ? null : queue.get(index); }
        @Override public int getCurrentPlayingIndex() { return index; }
        @Override public int getPlayerPosition() { return position; }
        @Override public PlayerState getPlayerState() { return state; }
        @Override public boolean isPlayWhenReady() { return state == PlayerState.STARTED; }
        @Override public boolean isSeekable() { return index >= 0; }
        @Override public boolean shouldFastForward() { return queue.size() == 1; }
        @Override public float getPlaybackSpeed() { return speed; }
        @Override public void setPlaybackSpeed(float value) { speed = value; }
        @Override public RepeatMode getRepeatMode() { return repeat; }
        @Override public void setRepeatMode(RepeatMode value) { repeat = value; }
        @Override public void start() { starts++; state = PlayerState.STARTED; }
        @Override public void pause() { state = PlayerState.PAUSED; }
        @Override public void stop() { state = PlayerState.STOPPED; }
        @Override public void seekTo(int value) { position = value; }
        @Override public void next() { nextCalls++; index++; position = 0; }
        @Override public void previous() { previousCalls++; index--; position = 0; }
        @Override public void clear() { queue.clear(); index = -1; position = 0; state = PlayerState.IDLE; }
        @Override void play(int index, boolean play, int position) {
            this.index = index;
            this.position = position;
            state = play ? PlayerState.STARTED : PlayerState.PAUSED;
        }
        @Override public void download(List<Entry> entries, boolean save, boolean autoplay,
                                       boolean next, boolean shuffle, int start, int position) {
            for (Entry entry : entries) {
                queue.add(new DownloadFile(this, entry, false) {
                    @Override public synchronized boolean isWorkDone() { return true; }
                });
            }
            index = queue.isEmpty() ? -1 : start;
            this.position = position;
            if (autoplay) state = PlayerState.STARTED;
        }
        @Override synchronized void moveSessionQueue(int from, int to, int newIndex) {
            DownloadFile current = getCurrentPlaying();
            List<DownloadFile> moved = new ArrayList<>(queue.subList(from, to));
            queue.subList(from, to).clear();
            queue.addAll(newIndex, moved);
            index = queue.indexOf(current);
        }
    }
}
