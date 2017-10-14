package github.daneren2005.dsub.util.compat;

import android.content.Context;
import android.support.v7.media.MediaRouter;

import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.RemoteController;


// Provides stubs for Google-related functionality
public final class GoogleCompat {

    public static boolean playServicesAvailable(Context context) {
        return false;
    }

    public static void installProvider(Context context) throws Exception {
    }

    public static boolean castAvailable() {
        return false;
    }

    public static RemoteController getController(DownloadService downloadService, MediaRouter.RouteInfo info) {
        return null;
    }

    public static String getCastControlCategory() {
        return null;
    }
}
