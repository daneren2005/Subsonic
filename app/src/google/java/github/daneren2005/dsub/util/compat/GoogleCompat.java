package github.daneren2005.dsub.util.compat;

import android.content.Context;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

import github.daneren2005.dsub.service.ChromeCastController;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.RemoteController;
import github.daneren2005.dsub.util.EnvironmentVariables;

public final class GoogleCompat {

    private static final String TAG = GoogleCompat.class.getSimpleName();

    public static boolean playServicesAvailable(Context context){
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if(result != ConnectionResult.SUCCESS){
            Log.w(TAG, "No play services, failed with result: " + result);
            return false;
        }
        return true;
    }

    public static void installProvider(Context context) throws Exception{
        ProviderInstaller.installIfNeeded(context);
    }

    public static boolean castAvailable() {
        if (EnvironmentVariables.CAST_APPLICATION_ID == null) {
            Log.w(TAG, "CAST_APPLICATION_ID not provided");
            return false;
        }
        try {
            Class.forName("com.google.android.gms.cast.CastDevice");
        } catch (Exception ex) {
            Log.w(TAG, "Chromecast library not available");
            return false;
        }
        return true;
    }

    public static RemoteController getController(DownloadService downloadService, MediaRouter.RouteInfo info) {
        CastDevice device = CastDevice.getFromBundle(info.getExtras());
        if(device != null) {
            return new ChromeCastController(downloadService, device);
        } else {
            return null;
        }
    }

    public static String getCastControlCategory() {
        return CastMediaControlIntent.categoryForCast(EnvironmentVariables.CAST_APPLICATION_ID);
    }
}
