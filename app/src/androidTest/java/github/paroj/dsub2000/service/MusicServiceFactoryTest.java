package github.paroj.dsub2000.service;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import github.paroj.dsub2000.util.Util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MusicServiceFactoryTest {
    private Context context;
    private boolean wasOffline;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        wasOffline = Util.isOffline(context);
    }

    @After
    public void tearDown() {
        Util.setOffline(context, wasOffline);
    }

    @Test
    public void offlineModeUsesOfflineService() {
        Util.setOffline(context, true);
        assertTrue(MusicServiceFactory.getMusicService(context) instanceof OfflineMusicService);
    }

    @Test
    public void forcedOnlineBypassesOfflineMode() {
        Util.setOffline(context, true);
        assertFalse(MusicServiceFactory.getMusicService(context, true) instanceof OfflineMusicService);
    }
}
