package github.daneren2005.dsub.service;

import static github.daneren2005.dsub.domain.PlayerState.COMPLETED;
import static github.daneren2005.dsub.domain.PlayerState.IDLE;
import static github.daneren2005.dsub.domain.PlayerState.PAUSED;
import static github.daneren2005.dsub.domain.PlayerState.STARTED;
import static github.daneren2005.dsub.domain.PlayerState.STOPPED;
import java.util.List;

import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;

import java.util.LinkedList;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class DownloadServiceTest extends
		ActivityInstrumentationTestCase2<SubsonicFragmentActivity> {

	private SubsonicFragmentActivity activity;
	private DownloadService downloadService;

	public DownloadServiceTest() {
		super(SubsonicFragmentActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
		downloadService = activity.getDownloadService();
		downloadService.clear();
	}

	/**
	 * Test the get player duration without playlist.
	 */
	public void testGetPlayerDurationWithoutPlayList() {
		int duration = downloadService.getPlayerDuration();
		assertEquals(0, duration);
	}

	/**
	 * Test the get player position without playlist.
	 */
	public void testGetPlayerPositionWithoutPlayList() {
		int position = downloadService.getPlayerPosition();
		assertEquals(0, position);
	}

	public void testGetRecentDownloadsWithoutPlaylist() {
		int output_length = downloadService.getRecentDownloads().size();
		assertEquals(0, output_length);
	}

	public void testGetRecentDownloadsWithPlaylist() {
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(2), false, false, false,
				false, 0, 0);

		int output_length = downloadService.getRecentDownloads().size();
		assertEquals(1, output_length);
	}

	public void testGetCurrentPlayingIndexWithoutPlayList() {
		int currentPlayingIndex = activity.getDownloadService()
				.getCurrentPlayingIndex();
		assertEquals(currentPlayingIndex, -1);
	}

	/**
	 * Test next action without playlist.
	 */
	public void testNextWithoutPlayList() {
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		downloadService.next();
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertTrue(oldCurrentPlayingIndex == newCurrentPlayingIndex);
	}

	/**
	 * Test previous action without playlist.
	 */
	public void testPreviousWithoutPlayList() {
		int oldCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		downloadService.previous();
		int newCurrentPlayingIndex = downloadService.getCurrentPlayingIndex();
		assertTrue(oldCurrentPlayingIndex == newCurrentPlayingIndex);
	}

	/**
	 * Test next action with playlist.
	 */
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
		PlayerState newPlayState = downloadService.getPlayerState();
		assertEquals(PAUSED, newPlayState);
	}

	/**
	 * Test the autoplay.
	 * 
	 * @throws InterruptedException
	 */
	public void testAutoplay() throws InterruptedException {
		// Download one songs
		downloadService.getDownloads().clear();
		downloadService.download(this.createMusicSongs(1), false, true, false,
				false, 0, 0);

		Log.w("testPreviousWithPlayList", "Start waiting downloads");
		Thread.sleep(5000);
		Log.w("testPreviousWithPlayList", "Stop waiting downloads");

		PlayerState playerState = downloadService.getPlayerState();
		assertEquals(STARTED, playerState);
	}

	/**
	 * Test if the download list is empty.
	 */
	public void testGetDownloadsEmptyList() {
		List<DownloadFile> list = downloadService.getDownloads();
		assertEquals(0, list.size());
	}

	/**
	 * Test if the download service add the given song to its queue.
	 */
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
	 * Generate a list containing some music directory entries.
	 * 
	 * @return list containing some music directory entries.
	 */
	private List<MusicDirectory.Entry> createMusicSongs(int size) {
		MusicDirectory.Entry musicEntry = new MusicDirectory.Entry();
		musicEntry.setAlbum("Itchy Hitchhiker");
		musicEntry.setBitRate(198);
		musicEntry.setAlbumId("49");
		musicEntry.setDuration(247);
		musicEntry.setSize(Long.valueOf(6162717));
		musicEntry.setArtistId("23");
		musicEntry.setArtist("The Dada Weatherman");
		musicEntry.setCloseness(0);
		musicEntry.setContentType("audio/mpeg");
		musicEntry.setCoverArt("433");
		musicEntry.setDirectory(false);
		musicEntry.setGenre("Easy Listening/New Age");
		musicEntry.setGrandParent("306");
		musicEntry.setId("466");
		musicEntry.setParent("433");
		musicEntry
				.setPath("The Dada Weatherman/Itchy Hitchhiker/08 - The Dada Weatherman - Harmonies.mp3");
		musicEntry.setStarred(true);
		musicEntry.setSuffix("mp3");
		musicEntry.setTitle("Harmonies");
		musicEntry.setType(0);
		musicEntry.setVideo(false);

		List<MusicDirectory.Entry> musicEntries = new LinkedList<MusicDirectory.Entry>();

		for (int i = 0; i < size; i++) {
			musicEntries.add(musicEntry);
		}

		return musicEntries;

	}

}
