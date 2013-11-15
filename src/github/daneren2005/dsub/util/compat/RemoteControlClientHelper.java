package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

public abstract class RemoteControlClientHelper {
	
	public static RemoteControlClientHelper createInstance() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return new RemoteControlClientBase();
		} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return new RemoteControlClientJB();
		} else {
			return new RemoteControlClientICS();
		}
	}
	
	protected RemoteControlClientHelper() {
		// Avoid instantiation
	}
	
	public abstract void register(final Context context, final ComponentName mediaButtonReceiverComponent);
	public abstract void unregister(final Context context);
	public abstract void setPlaybackState(final int state);
	public abstract void updateMetadata(final Context context, final MusicDirectory.Entry currentSong);
	
}
