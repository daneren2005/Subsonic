package github.paroj.dsub2000.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import github.paroj.dsub2000.domain.MusicDirectory;

import org.junit.Test;

import java.io.File;

public class FileUtilCoverArtTest {

	private static MusicDirectory.Entry song(String id, String coverArt) {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		entry.setId(id);
		entry.setTitle("Track " + id);
		entry.setArtist("Artist");
		entry.setAlbum("Album");
		entry.setDirectory(false);
		entry.setCoverArt(coverArt);
		return entry;
	}

	@Test
	public void perSongCoverArtIdsGetDistinctFiles() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		File first = FileUtil.getAlbumArtFile(context, song("1", "track-1"));
		File second = FileUtil.getAlbumArtFile(context, song("2", "track-2"));

		assertNotEquals(first, second);
	}

	@Test
	public void sharedCoverArtIdGetsOneFile() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		File first = FileUtil.getAlbumArtFile(context, song("1", "album-1"));
		File second = FileUtil.getAlbumArtFile(context, song("2", "album-1"));

		assertEquals(first, second);
	}

	@Test
	public void offlineCoverArtIsAnExistingPath() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		File art = new File(FileUtil.getAlbumArtDirectory(context), "offline-art.jpeg");

		MusicDirectory.Entry entry = song("/music/Artist/Album/01-Track.mp3", art.getAbsolutePath());

		assertEquals(art, FileUtil.getAlbumArtFile(context, entry));
	}

	@Test
	public void downloadedSongsGetDistinctOfflineArtFiles() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		File first = FileUtil.getOfflineSongArtFile(context, new File("/music/Artist/Album/01-First.mp3"));
		File second = FileUtil.getOfflineSongArtFile(context, new File("/music/Artist/Album/02-Second.mp3"));

		assertNotEquals(first, second);
	}
}
