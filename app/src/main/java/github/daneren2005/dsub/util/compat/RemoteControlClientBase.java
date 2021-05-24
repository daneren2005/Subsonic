package github.daneren2005.dsub.util.compat;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v7.media.MediaRouter;

import java.util.List;

import cz.fhucho.android.util.SimpleDiskCache;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;

public abstract class RemoteControlClientBase {

	protected final SimpleDiskCache cache;

	public static RemoteControlClientBase createInstance(SimpleDiskCache cache) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new RemoteControlClientLP(cache);
		} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return new RemoteControlClientJB(cache);
		} else {
			return new RemoteControlClientICS(cache);
		}
	}
	
	protected RemoteControlClientBase(SimpleDiskCache cache) {
		this.cache = cache;
	}
	
	public abstract void register(final Context context, final ComponentName mediaButtonReceiverComponent);
	public abstract void unregister(final Context context);
	public abstract void setPlaybackState(int state, int index, int queueSize);
	public abstract void updateMetadata(Context context, MusicDirectory.Entry currentSong);
	public abstract void metadataChanged(MusicDirectory.Entry currentSong);
	public abstract void updateAlbumArt(MusicDirectory.Entry currentSong, Bitmap bitmap);
	public abstract void registerRoute(MediaRouter router);
	public abstract void unregisterRoute(MediaRouter router);
	public abstract void updatePlaylist(List<DownloadFile> playlist);
}
