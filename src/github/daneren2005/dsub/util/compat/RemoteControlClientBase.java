package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.media.MediaRouter;
import android.util.Log;

public class RemoteControlClientBase extends RemoteControlClientHelper {

    private static final String TAG = RemoteControlClientBase.class.getSimpleName();

	@Override
	public void register(Context context, ComponentName mediaButtonReceiverComponent) {

	}

	@Override
	public void unregister(Context context) {

	}

	@Override
	public void setPlaybackState(int state) {

	}

	@Override
	public void updateMetadata(Context context, Entry currentSong) {

	}

	@Override
	public void registerRoute(MediaRouter router) {

	}

	@Override
	public void unregisterRoute(MediaRouter router) {

	}

}
