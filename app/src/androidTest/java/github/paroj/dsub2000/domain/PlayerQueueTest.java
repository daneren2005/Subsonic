package github.paroj.dsub2000.domain;

import junit.framework.TestCase;

public class PlayerQueueTest extends TestCase {

	public void testEmptyQueueIsRestorable() {
		PlayerQueue queue = new PlayerQueue();
		queue.currentPlayingIndex = -1;

		assertTrue(queue.isRestorable());
	}

	public void testNullSongsIsNotRestorable() {
		// Kryo instantiates without running the field initializers
		PlayerQueue queue = new PlayerQueue();
		queue.songs = null;

		assertFalse(queue.isRestorable());
	}

	public void testNullToDeleteIsNotRestorable() {
		PlayerQueue queue = new PlayerQueue();
		queue.toDelete = null;

		assertFalse(queue.isRestorable());
	}

	public void testNullSongIsNotRestorable() {
		PlayerQueue queue = new PlayerQueue();
		queue.songs.add(null);

		assertFalse(queue.isRestorable());
	}

	public void testIndexOutsideQueueIsNotRestorable() {
		PlayerQueue queue = new PlayerQueue();
		queue.songs.add(new MusicDirectory.Entry("1"));
		queue.currentPlayingIndex = 1;

		assertFalse(queue.isRestorable());
	}

	public void testNegativeIndexOtherThanMinusOneIsNotRestorable() {
		PlayerQueue queue = new PlayerQueue();
		queue.songs.add(new MusicDirectory.Entry("1"));
		queue.currentPlayingIndex = -2;

		assertFalse(queue.isRestorable());
	}
}
