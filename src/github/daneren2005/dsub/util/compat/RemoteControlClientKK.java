package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.util.Log;

import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import java.io.File;

@TargetApi(19)
public class RemoteControlClientKK extends RemoteControlClientJB {
	private static String TAG = RemoteControlClientKK.class.getSimpleName();
	protected MusicDirectory.Entry currentSong;

	@Override
	public void register(final Context context, final ComponentName mediaButtonReceiverComponent) {
		super.register(context, mediaButtonReceiverComponent);

		mRemoteControl.setMetadataUpdateListener(new RemoteControlClient.OnMetadataUpdateListener() {
			@Override
			public void onMetadataUpdate(int key, Object newValue) {
				if(key == MediaMetadataEditor.RATING_KEY_BY_USER) {
					Rating rating = (Rating) newValue;
					setStarred(currentSong, rating.hasHeart());
				}
			}
		});
	}
	
	@Override
	protected void updateMetadata(final MusicDirectory.Entry currentSong, final RemoteControlClient.MetadataEditor editor) {
		super.updateMetadata(currentSong, editor);
		editor.putObject(MediaMetadataEditor.RATING_KEY_BY_USER, Rating.newHeartRating(currentSong.isStarred()));
		editor.addEditableKey(MediaMetadataEditor.RATING_KEY_BY_USER);
		this.currentSong = currentSong;
	}
	
	@Override
	protected int getTransportFlags() {
		return super.getTransportFlags() | RemoteControlClient.FLAG_KEY_MEDIA_RATING;
	}
	
	private void setStarred(final MusicDirectory.Entry entry, final boolean starred) {
		entry.setStarred(starred);

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					MusicService musicService = MusicServiceFactory.getMusicService(downloadService);
					musicService.setStarred(entry.getId(), starred, downloadService, null);

					// Make sure to clear parent cache
					String s = Util.getRestUrl(downloadService, null) + entry.getParent();
					String parentCache = "directory-" + s.hashCode() + ".ser";
					File file = new File(downloadService.getCacheDir(), parentCache);
					file.delete();
				} catch(Exception e) {
					Log.w(TAG, "Failed to set star for " + entry.getTitle());
				}
				return null;
			}
		}.execute();
	}
}
