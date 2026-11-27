package github.paroj.dsub2000.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jupnp.android.AndroidUpnpService;
import org.jupnp.model.meta.Device;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the jUPnP Android service startup lifecycle: the
 * UPnP stack must already be initialized when the service binder is
 * connected, without the app calling {@code startup()} itself.
 */
@RunWith(AndroidJUnit4.class)
public class DSubUpnpServiceTest {
	private static final String TAG = "DLNATEST";

	@Test
	public void upnpServiceIsStartedOnBind() throws Exception {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		CountDownLatch connected = new CountDownLatch(1);
		AtomicReference<AndroidUpnpService> service = new AtomicReference<>();

		ServiceConnection connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder) {
				service.set((AndroidUpnpService) binder);
				connected.countDown();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
		};

		assertTrue(context.bindService(new Intent(context, DSubUpnpService.class),
				connection, Context.BIND_AUTO_CREATE));
		try {
			assertTrue("UPnP service did not connect", connected.await(10, TimeUnit.SECONDS));
			AndroidUpnpService upnp = service.get();
			assertNotNull(upnp);
			assertNotNull("Registry is null; UPnP stack was not started", upnp.getRegistry());
			assertNotNull("Control point is null; UPnP stack was not started", upnp.getControlPoint());

			upnp.getControlPoint().search();
			Thread.sleep(3000);
			for (Device<?, ?, ?> device : upnp.getRegistry().getDevices()) {
				Log.i(TAG, "found device: " + device.getDisplayString()
						+ " (" + device.getType().getType() + ")");
			}
		} finally {
			context.unbindService(connection);
		}
	}
}
