package github.paroj.dsub2000.service.parser;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * The OpenSubsonic authentication errors should be shown with an explanation
 * of how to obtain usable credentials, and the server's helpUrl must survive.
 */
public class AuthErrorParserTest extends TestCase {
	private static final int INSTANCE = 0;

	public void testUnsupportedAuthMechanismIncludesHelpUrl() {
		String helpUrl = "https://cloud.example.com/settings/user/security";
		SubsonicRESTException exception = parse(42, helpUrl);

		assertEquals(42, exception.getCode());
		assertTrue(exception.getMessage().endsWith(helpUrl));
	}

	public void testConflictingAuthMechanisms() {
		assertEquals(43, parse(43, null).getCode());
	}

	public void testInvalidApiKey() {
		SubsonicRESTException exception = parse(44, null);

		assertEquals(44, exception.getCode());
		assertNotNull(exception.getMessage());
		assertFalse(exception.getMessage().contains("http"));
	}

	private SubsonicRESTException parse(int code, String helpUrl) {
		String error = "<error code=\"" + code + "\" message=\"server message\"";
		if (helpUrl != null) {
			error += " helpUrl=\"" + helpUrl + "\"";
		}
		error += "/>";

		String xml = "<subsonic-response xmlns=\"http://subsonic.org/restapi\" status=\"failed\""
				+ " version=\"1.16.1\">" + error + "</subsonic-response>";

		try {
			new ErrorParser(context(), INSTANCE).parse(new StringReader(xml));
			fail("Expected a SubsonicRESTException");
		} catch (SubsonicRESTException e) {
			return e;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		return null;
	}

	private Context context() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}
}
