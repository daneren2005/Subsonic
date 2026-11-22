package github.paroj.dsub2000.service;

import static github.paroj.dsub2000.domain.PlayerState.COMPLETED;
import static github.paroj.dsub2000.domain.PlayerState.IDLE;
import static github.paroj.dsub2000.domain.PlayerState.PAUSED;
import static github.paroj.dsub2000.domain.PlayerState.STARTED;
import static github.paroj.dsub2000.domain.PlayerState.STOPPED;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.util.FileUtil;

import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class DownloadServiceTest {
	private static final int SECONDS = 30;

	@Rule
	public ActivityTestRule<SubsonicFragmentActivity> activityRule = new ActivityTestRule<>(SubsonicFragmentActivity.class, true, true);

	private SubsonicFragmentActivity activity;
	private DownloadService downloadService;
	private final List<MusicDirectory.Entry> generatedSongs = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		activity = activityRule.getActivity();
		downloadService = activity.getDownloadService();

		// The restored queue is loaded on a background thread. Clearing before that
		// finishes lets the old queue start playing and compete with the test.
		long deadline = SystemClock.elapsedRealtime() + 30000;
		while (!downloadService.isInitialized() && SystemClock.elapsedRealtime() < deadline) {
			Thread.sleep(50);
		}

		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> downloadService.clear());
	}

	@After
	public void tearDown() {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> downloadService.clear());
		for (MusicDirectory.Entry song : generatedSongs) {
			new DownloadFile(activity, song, false).delete();
		}
		generatedSongs.clear();
	}

	/**
	 * Test the get player duration without playlist.
	 */
	@Test
	public void testGetPlayerDurationWithoutPlayList() {
		int duration = downloadService.getPlayerDuration();
		assertEquals(0, duration);
	}

	/**
	 * Test the get player position without playlist.
	 */
	@Test
	public void testGetPlayerPositionWithoutPlayList() {
		int position = downloadService.getPlayerPosition();
		assertEquals(0, position);
	}
	@Test
	public void testGetRecentDownloadsWithoutPlaylist() {
		int output_length = downloadService.getRecentDownloads().size();
		assertEquals(0, output_length);
	}

	@Test
	public void testGetRecentDownloadsWithPlaylist() {
		downloadService.getDownloads().clear();
		downloadService.download(this.createSongs(2), false, false, false,
				false, 0, 0);

		int output_length = downloadService.getRecentDownloads().size();
		assertEquals(2, output_length);
	}

	@Test
	public void testGetRecentDownloadsDoesNotChangePlaybackQueue() {
		downloadService.clear();
		downloadService.clearBackground();

		List<MusicDirectory.Entry> queueSongs = createSongs(2);
		downloadService.download(queueSongs, false, false, false, false, 0, 0);
		int queueSize = downloadService.size();

		List<MusicDirectory.Entry> backgroundSongs = createSongs(1);
		// Keep the background song out of the playback queue but ready to be promoted
		// without consulting the server.
		FileUtil.SongFiles backgroundFiles = FileUtil.getSongFiles(activity, backgroundSongs.get(0));
		assertTrue("Background audio prepared", backgroundFiles.save().renameTo(backgroundFiles.complete()));
		downloadService.downloadBackground(backgroundSongs, true);
		int backgroundSize = downloadService.getBackgroundDownloads().size();
		assertEquals(1, backgroundSize);
		try {
			// getRecentDownloads() used to add the background downloads to the
			// playback queue itself (via the subList view).
			assertEquals(queueSize + backgroundSize, downloadService.getRecentDownloads().size());
		} finally {
			downloadService.clearBackground();
		}
		assertEquals(queueSize, downloadService.size());
	}

	@Test
	public void testGetCurrentPlayingIndexWithoutPlayList() {
		int currentPlayingIndex = activity.getDownloadService()
				.getCurrentPlayingIndex();
		assertEquals(currentPlayingIndex, -1);
	}

	/**
	 * Test next action without playlist.
	 */
	@Test
	public void testNextWithoutPlayList() {
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		downloadService.next();
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertTrue(oldCurrentPlayingIndex == newCurrentPlayingIndex);
	}

	/**
	 * Test previous action without playlist.
	 */
	@Test
	public void testPreviousWithoutPlayList() {
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		downloadService.previous();
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertTrue(oldCurrentPlayingIndex == newCurrentPlayingIndex);
	}

	/**
	 * Test next action with playlist.
	 */
	@Test
	public void testNextWithPlayList() {
		// Download two songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createSongs(2), false, false, false,
				false, 0, 0);

		// Get the current index
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();

		// Do the next
		downloadService.next();

		// Check that the new current index is incremented
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertEquals(oldCurrentPlayingIndex + 1, newCurrentPlayingIndex);
	}

	/**
	 * Test previous action with playlist.
	 */
	@Test
	public void testPreviousWithPlayList() {
		// Download two songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createSongs(2), false, false, false,
				false, 0, 0);

		// Get the current index
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();

		// Do a next before the previous
		downloadService.next();

		downloadService.setPlayerState(STARTED);
		// Do the previous
		downloadService.previous();

		// Check that the new current index is incremented
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertEquals(oldCurrentPlayingIndex, newCurrentPlayingIndex);
	}

	/**
	 * Test seek feature.
	 */
	@Test
	public void testSeekTo() {
		// seek with negative
		downloadService.seekTo(Integer.MIN_VALUE);

		// seek with null
		downloadService.seekTo(0);

		// seek with big value
		downloadService.seekTo(Integer.MAX_VALUE);
	}

	/**
	 * Test toggle play pause.
	 */
	@Test
	public void testTogglePlayPause() {
		PlayerState oldPlayState = downloadService.getPlayerState();
		downloadService.togglePlayPause();
		PlayerState newPlayState = downloadService.getPlayerState();
		if (oldPlayState == PAUSED || oldPlayState == COMPLETED
				|| oldPlayState == STOPPED) {
			assertEquals(STARTED, newPlayState);
		} else if (oldPlayState == STOPPED || oldPlayState == IDLE) {
			if (downloadService.size() == 0) {
				assertEquals(IDLE, newPlayState);
			} else {
				assertEquals(STARTED, newPlayState);
			}
		} else if (oldPlayState == STARTED) {
			assertEquals(PAUSED, newPlayState);
		}
		downloadService.togglePlayPause();
		newPlayState = downloadService.getPlayerState();
		assertEquals(oldPlayState, newPlayState);
	}

	/**
	 * Test toggle play pause without playlist.
	 */
	@Test
	public void testTogglePlayPauseWithoutPlayList() {
		PlayerState oldPlayState = downloadService.getPlayerState();
		downloadService.togglePlayPause();
		PlayerState newPlayState = downloadService.getPlayerState();

		assertEquals(IDLE, oldPlayState);
		assertEquals(IDLE, newPlayState);
	}

	/**
	 * Test toggle play pause without playlist.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testTogglePlayPauseWithPlayList() throws InterruptedException {
		// Download two songs
		List<MusicDirectory.Entry> songs = this.createSongs(2);
		downloadService.getDownloads().clear();
		downloadService.download(songs, false, false, false,
				false, 0, 0);

		// Downloading without autoplay leaves the player idle. Toggling starts the
		// current track, which may still have to buffer and prepare.
		assertEquals(IDLE, downloadService.getPlayerState());
		downloadService.togglePlayPause();
		awaitPlayerState(STARTED, 60000);
		downloadService.togglePlayPause();
		// The paused state is published by the player's own event shortly after the command.
		awaitPlayerState(PAUSED, 10000);
	}

	/**
	 * Test the autoplay.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testAutoplay() throws InterruptedException {
		// Download one song
		List<MusicDirectory.Entry> songs = this.createSongs(1);
		downloadService.getDownloads().clear();
		downloadService.download(songs, false, true, false,
				false, 0, 0);

		// The cached file still has to be opened and prepared, which is asynchronous.
		awaitPlayerState(STARTED, 60000);
	}

	/**
	 * Test if the download list is empty.
	 */
	@Test
	public void testGetDownloadsEmptyList() {
		List<DownloadFile> list = downloadService.getDownloads();
		assertEquals(0, list.size());
	}

	/**
	 * Test if the download service add the given song to its queue.
	 */
	@Test
	public void testAddMusicToDownload() {
		assertNotNull(downloadService);

		// Download list before
		List<DownloadFile> downloadList = downloadService.getDownloads();
		int beforeDownloadAction = 0;
		if (downloadList != null) {
			beforeDownloadAction = downloadList.size();
		}

		// Launch download
		downloadService.download(this.createSongs(1), false, false, false,
				false, 0, 0);

		// Check number of download after
		int afterDownloadAction = 0;
		downloadList = downloadService.getDownloads();
		if (downloadList != null && !downloadList.isEmpty()) {
			afterDownloadAction = downloadList.size();
		}
		assertEquals(beforeDownloadAction + 1, afterDownloadAction);
	}

	/**
	 * Waits for the player to reach the expected state. Opening and preparing a track is
	 * asynchronous, so a fixed sleep is not reliable.
	 */
	private void awaitPlayerState(PlayerState expected, long timeoutMillis) throws InterruptedException {
		long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
		PlayerState state = downloadService.getPlayerState();
		while (state != expected && SystemClock.elapsedRealtime() < deadline) {
			Thread.sleep(100);
			state = downloadService.getPlayerState();
		}
		assertEquals(expected, state);
	}

	/**
	 * Creates local tracks so the tests do not depend on a configured server or credentials.
	 */
	private List<MusicDirectory.Entry> createSongs(int size) {
		List<MusicDirectory.Entry> songs = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			songs.add(createSong());
		}
		return songs;
	}

	private MusicDirectory.Entry createSong() {
		MusicDirectory.Entry song = new MusicDirectory.Entry(UUID.randomUUID().toString());
		song.setTitle("Test " + song.getId());
		song.setSuffix("wav");
		song.setDuration(SECONDS);

		File audio = new DownloadFile(activity, song, false).getSaveFile();
		File parent = audio.getParentFile();
		assertTrue("Audio directory exists", parent.isDirectory() || parent.mkdirs());
		int bytes = 8000 * 2 * SECONDS;
		ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
		header.putInt(0x46464952).putInt(36 + bytes).putInt(0x45564157);
		header.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
		header.putInt(8000).putInt(16000).putShort((short) 2).putShort((short) 16);
		header.putInt(0x61746164).putInt(bytes);
		try (FileOutputStream output = new FileOutputStream(audio)) {
			output.write(header.array());
			output.write(new byte[bytes]);
		} catch (IOException error) {
			throw new AssertionError(error);
		}

		generatedSongs.add(song);
		return song;
	}

}
