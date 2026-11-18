package github.paroj.dsub2000.service;

import static github.paroj.dsub2000.domain.PlayerState.COMPLETED;
import static github.paroj.dsub2000.domain.PlayerState.IDLE;
import static github.paroj.dsub2000.domain.PlayerState.PAUSED;
import static github.paroj.dsub2000.domain.PlayerState.STARTED;
import static github.paroj.dsub2000.domain.PlayerState.STOPPED;
import java.util.List;

import github.paroj.dsub2000.activity.SubsonicFragmentActivity;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerState;

import java.util.LinkedList;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class DownloadServiceTest {

	@Rule
	public ActivityTestRule<SubsonicFragmentActivity> activityRule = new ActivityTestRule<>(SubsonicFragmentActivity.class, true, true);

	private SubsonicFragmentActivity activity;
	private DownloadService downloadService;

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

		downloadService.clear();
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
		downloadService.download(this.createMusicSongs(2), false, false, false,
				false, 0, 0);

		int output_length = downloadService.getRecentDownloads().size();
		assertEquals(2, output_length);
	}

	@Test
	public void testGetRecentDownloadsDoesNotChangePlaybackQueue() {
		downloadService.clear();
		downloadService.clearBackground();

		List<MusicDirectory.Entry> queueSongs = createMusicSongs(2);
		deleteCachedFiles(queueSongs);
		downloadService.download(queueSongs, false, false, false, false, 0, 0);
		int queueSize = downloadService.size();

		List<MusicDirectory.Entry> backgroundSongs = createMusicSongs(1);
		deleteCachedFiles(backgroundSongs);
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
	public void testNextWithPlayList() throws InterruptedException {
		// Download two songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(2), false, false, false,
				false, 0, 0);

		Log.w("testPreviousWithPlayList", "Start waiting to downloads");
		Thread.sleep(5000);
		Log.w("testPreviousWithPlayList", "Stop waiting downloads");

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
	public void testPreviousWithPlayList() throws InterruptedException {
		// Download two songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(2), false, false, false,
				false, 0, 0);

		Log.w("testPreviousWithPlayList", "Start waiting downloads");
		Thread.sleep(5000);
		Log.w("testPreviousWithPlayList", "Stop waiting downloads");

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
		List<MusicDirectory.Entry> songs = this.createMusicSongs(2);
		deleteCachedFiles(songs);
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
	public void testAutoplay() throws InterruptedException, Exception {
		// Download one songs
		List<MusicDirectory.Entry> songs = this.createMusicSongs(1);
		deleteCachedFiles(songs);
		downloadService.getDownloads().clear();
		downloadService.download(songs, false, true, false,
				false, 0, 0);

		// Starting a remote track involves downloading enough of it to buffer,
		// preparing the decoder and then actually starting playback.
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
		downloadService.download(this.createMusicSongs(1), false, false, false,
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
	 * Waits for the player to reach the expected state. Remote tracks need to be
	 * buffered and prepared first, so a fixed sleep is not reliable.
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
	 * Removes cached copies so a previous run (or a server error that was saved as
	 * audio) cannot make the player use a stale or unreadable file.
	 */
	private void deleteCachedFiles(List<MusicDirectory.Entry> songs) {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		for (MusicDirectory.Entry song : songs) {
			new DownloadFile(context, song, false).delete();
		}
	}

	/**
	 * Generate a list containing some music directory entries. Produced from current subsonic
	 * server. Is uses *alphabeticalByName* album list. Given server stays with the
	 * same music content, this method should produce the same results for every call with
	 * unique *size* argument. It is important for this method to return really existing on server
	 * songs, as other tests rely on it.
	 * 
	 * @return list containing some music directory entries.
	 */
	private List<MusicDirectory.Entry> createMusicSongs(int size) {
		try {
			Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
			MusicService service = MusicServiceFactory.getMusicService(context);
			String albumId = service
					.getAlbumList("alphabeticalByName", 1, 0, false, context, null)
					.getChildren().get(0).getId();

			MusicDirectory.Entry musicEntry = service
					.getMusicDirectory(albumId, "", false, context, null)
					.getSongs()
					.get(0);


			List<MusicDirectory.Entry> musicEntries = new LinkedList<>();

			for (int i = 0; i < size; i++) {
				musicEntries.add(musicEntry);
			}

			return musicEntries;

		} catch (Exception ex) {
			throw new AssertionError(ex);
			// Throwing exactly AssertError because it will not require to add *throws Exception*
			// to every test case where this method gets used
		}


	}

}
