package github.paroj.dsub2000;

import github.paroj.dsub2000.service.RESTMusicService;

import junit.framework.TestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends TestCase {
	public ApplicationTest() {
	}

	public void testFilterLegacyTlsProtocolsRemovesOnlyLegacyProtocols() {
		String[] filteredProtocols = RESTMusicService.filterLegacyTlsProtocols(new String[] {
				"SSL",
				"SSLv3",
				"TLSv1",
				"TLSv1.0",
				"TLSv1.1",
				"TLSv1.2",
				"TLSv1.3",
				"TLSv1.4"
		});

		assertDoesNotContain(filteredProtocols, "SSL");
		assertDoesNotContain(filteredProtocols, "SSLv3");
		assertDoesNotContain(filteredProtocols, "TLSv1");
		assertDoesNotContain(filteredProtocols, "TLSv1.0");
		assertDoesNotContain(filteredProtocols, "TLSv1.1");
		assertContains(filteredProtocols, "TLSv1.2");
		assertContains(filteredProtocols, "TLSv1.3");
		assertContains(filteredProtocols, "TLSv1.4");
	}

	public void testFilterLegacyTlsProtocolsFailsClosedWithoutAcceptableProtocol() {
		try {
			RESTMusicService.filterLegacyTlsProtocols(new String[] {
					"SSLv3",
					"TLSv1",
					"TLSv1.0",
					"TLSv1.1"
			});
			fail("Expected filtering an all-legacy protocol set to fail closed");
		} catch (IllegalStateException expected) {
			// Expected: no provider-enabled protocol remains safe to use.
		}
	}

	private void assertContains(String[] protocols, String expectedProtocol) {
		for (String protocol : protocols) {
			if (expectedProtocol.equals(protocol)) {
				return;
			}
		}
		fail("Expected protocol to remain enabled: " + expectedProtocol);
	}

	private void assertDoesNotContain(String[] protocols, String unwantedProtocol) {
		for (String protocol : protocols) {
			assertFalse("Legacy protocol must be removed: " + unwantedProtocol,
					unwantedProtocol.equals(protocol));
		}
	}
}
