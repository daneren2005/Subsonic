package github.paroj.dsub2000.service.parser;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Collections;

import github.paroj.dsub2000.domain.ServerInfo;
import github.paroj.dsub2000.domain.Version;

/**
 * Servers like Nextcloud Music add OpenSubsonic extensions without changing
 * their Subsonic API version, so the cached extensions must follow the
 * server's own version.
 */
public class ServerInfoExtensionsTest extends TestCase {
	private static final int INSTANCE = 0;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ServerInfo cached = new ServerInfo();
		cached.setRestVersion(new Version("1.16.1"));
		cached.setServerVersion("3.3.0");
		cached.setOpenSubsonicExtensions(Collections.singleton("formPost"));
		cached.saveServerInfo(context(), INSTANCE, true);
	}

	@Override
	protected void tearDown() throws Exception {
		new ServerInfo().saveServerInfo(context(), INSTANCE, true);
		super.tearDown();
	}

	public void testExtensionsKeptForSameServerVersion() throws Exception {
		parse("3.3.0");

		assertEquals(Collections.singleton("formPost"),
				ServerInfo.getServerInfo(context(), INSTANCE).getOpenSubsonicExtensions());
	}

	public void testExtensionsDroppedWhenServerVersionChanges() throws Exception {
		parse("3.3.1");

		ServerInfo server = ServerInfo.getServerInfo(context(), INSTANCE);
		assertEquals("3.3.1", server.getServerVersion());
		assertNull(server.getOpenSubsonicExtensions());
		assertFalse(ServerInfo.hasOpenSubsonicExtensionsInfo(context(), INSTANCE));
	}

	private void parse(String serverVersion) throws Exception {
		String xml = "<subsonic-response xmlns=\"http://subsonic.org/restapi\" status=\"ok\""
				+ " version=\"1.16.1\" type=\"nextcloud music\" serverVersion=\"" + serverVersion + "\""
				+ " openSubsonic=\"true\"><songList/></subsonic-response>";
		new EntryListParser(context(), INSTANCE).parse(new StringReader(xml), null);
	}

	private Context context() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}
}
