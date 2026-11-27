package github.paroj.dsub2000.util.compat;

import android.content.Context;
import androidx.mediarouter.media.MediaRouter;

import github.paroj.dsub2000.service.DownloadService;
import github.paroj.dsub2000.service.RemoteController;


// Provides stubs for Google-related functionality
public final class GoogleCompat {
    public static void initializeCast(Context context) { }

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
