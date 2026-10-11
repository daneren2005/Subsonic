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
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(2), false, false, false,
				false, 0, 0);

		Log.w("testPreviousWithPlayList", "Start waiting downloads");
		Thread.sleep(5000);
		Log.w("testPreviousWithPlayList", "Stop waiting downloads");

		PlayerState oldPlayState = downloadService.getPlayerState();
		downloadService.togglePlayPause();
		Thread.sleep(500);
		assertEquals(STARTED, downloadService.getPlayerState());
		downloadService.togglePlayPause();
		// The paused state is published by the player's own event shortly after the command.
		Thread.sleep(500);
		PlayerState newPlayState = downloadService.getPlayerState();
		assertEquals(PAUSED, newPlayState);
	}

	/**
	 * Test the autoplay.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testAutoplay() throws InterruptedException, Exception {
		// Download one songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(1), false, true, false,
				false, 0, 0);

		Log.w("testPreviousWithPlayList", "Start waiting downloads");
		Thread.sleep(10000);
		Log.w("testPreviousWithPlayList", "Stop waiting downloads");

		PlayerState playerState = downloadService.getPlayerState();
		assertEquals(STARTED, playerState);
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
