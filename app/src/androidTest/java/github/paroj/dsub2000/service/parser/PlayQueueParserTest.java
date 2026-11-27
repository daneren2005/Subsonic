package github.paroj.dsub2000.service.parser;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import java.io.StringReader;

import github.paroj.dsub2000.domain.PlayerQueue;

/** Pins how the server's play queue timestamp is interpreted. */
public class PlayQueueParserTest extends TestCase {

	public void testZoneLessTimestampIsUtc() throws Exception {
		PlayerQueue queue = parse("2026-11-06T12:00:00");
		assertEquals(1793966400000L, queue.changed.getTime());
	}

	public void testUtcTimestamp() throws Exception {
		PlayerQueue queue = parse("2026-11-06T12:00:00Z");
		assertEquals(1793966400000L, queue.changed.getTime());
	}

	public void testTimestampWithOffsetUsesTheOffset() throws Exception {
		// 12:00+02:00 is 10:00 UTC. Ignoring the offset would shift the queue two hours into the future.
		PlayerQueue queue = parse("2026-11-06T12:00:00+02:00");
		assertEquals(1793959200000L, queue.changed.getTime());
	}

	public void testFractionalSecondsAreAccepted() throws Exception {
		PlayerQueue queue = parse("2026-11-06T12:00:00.123456789Z");
		assertEquals(1793966400123L, queue.changed.getTime());
	}

	public void testInvalidTimestampDoesNotFailTheQueue() throws Exception {
		PlayerQueue queue = parse("not a date");
		assertNull(queue.changed);
		assertEquals(1, queue.songs.size());
	}

	private PlayerQueue parse(String changed) throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<subsonic-response xmlns=\"http://subsonic.org/restapi\" status=\"ok\" version=\"1.16.1\">" +
				"<playQueue current=\"1\" position=\"42\" changed=\"" + changed + "\">" +
				"<entry id=\"1\" title=\"Song\" isDir=\"false\" suffix=\"mp3\"/>" +
				"</playQueue></subsonic-response>";
		PlayQueueParser parser = new PlayQueueParser(InstrumentationRegistry.getInstrumentation().getTargetContext(), 0);
		return parser.parse(new StringReader(xml), null);
	}
}
