package github.paroj.dsub2000.service;

import android.content.Context;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.android.AndroidRouter;
import org.jupnp.android.AndroidUpnpServiceImpl;
import org.jupnp.protocol.ProtocolFactory;

/**
 * UPnP service used for DLNA discovery.
 *
 * <p>jUPnP's {@link AndroidRouter} acquires a {@code WIFI_MODE_FULL_HIGH_PERF}
 * lock whenever the router is enabled, i.e. for as long as this service is
 * bound. That lock is meant for low-latency audio and is too expensive to hold
 * for background discovery. Playback and downloads already manage their own
 * Wi-Fi locks, so keep the multicast lock (needed for SSDP) but skip jUPnP's
 * Wi-Fi lock.</p>
 */
public final class DSubUpnpService extends AndroidUpnpServiceImpl {
    @Override
    protected AndroidRouter createRouter(UpnpServiceConfiguration configuration,
                                         ProtocolFactory protocolFactory, Context context) {
        return new AndroidRouter(configuration, protocolFactory, context) {
            @Override
            protected void setWifiLock(boolean enable) {
                // Intentionally no-op; see class comment.
            }
        };
    }
}
