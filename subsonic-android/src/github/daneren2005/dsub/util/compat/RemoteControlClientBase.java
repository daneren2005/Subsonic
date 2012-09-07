package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

public class RemoteControlClientBase extends RemoteControlClientHelper {

    private static final String TAG = RemoteControlClientBase.class.getSimpleName();

	@Override
	public void register(Context context, ComponentName mediaButtonReceiverComponent) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void unregister(Context context) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void setPlaybackState(int state) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void updateMetadata(Context context, Entry currentSong) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

}
