package github.paroj.dsub2000.util.compat;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;

import java.util.Collections;
import java.util.List;

public final class DSubCastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(GoogleCompat.castApplicationId())
                .setStopReceiverApplicationWhenEndingSession(true)
                .setCastMediaOptions(new CastMediaOptions.Builder()
                        .setMediaSessionEnabled(false).setNotificationOptions(null).build())
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return Collections.emptyList();
    }
}
