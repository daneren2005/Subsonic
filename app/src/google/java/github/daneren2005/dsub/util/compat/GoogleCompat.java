package github.daneren2005.dsub.util.compat;

import android.content.Context;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import static com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

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

    public static String castApplicationId() {
        if (EnvironmentVariables.CAST_APPLICATION_ID != null) {
            return EnvironmentVariables.CAST_APPLICATION_ID;
        } else {
            return DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
        }
    }

    public static boolean castAvailable() {
        if (castApplicationId() == DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)  {
            Log.i(TAG, "Using DEFAULT_MEDIA_RECEIVER_APPLICATION_ID for casting");
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
        return CastMediaControlIntent.categoryForCast(castApplicationId());
    }
}
