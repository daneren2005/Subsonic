package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

@TargetApi(14)
public class RemoteControlClientICS extends RemoteControlClientHelper {
	
	private RemoteControlClient mRemoteControl;
	
	public void register(final Context context, final ComponentName mediaButtonReceiverComponent) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		// build the PendingIntent for the remote control client
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, mediaButtonIntent, 0);

		// create and register the remote control client
		mRemoteControl = new RemoteControlClient(mediaPendingIntent);
		audioManager.registerRemoteControlClient(mRemoteControl);

		mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);

		mRemoteControl.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY | 
				RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | 
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
				RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
				RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
				RemoteControlClient.FLAG_KEY_MEDIA_STOP);
	}
	
	public void unregister(final Context context) {
		if (mRemoteControl != null) {
			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			audioManager.unregisterRemoteControlClient(mRemoteControl);
		}
	}
	
	public void setPlaybackState(final int state) {
		mRemoteControl.setPlaybackState(state);
	}
	
	public void updateMetadata(final Context context, final MusicDirectory.Entry currentSong) {
		// Update the remote controls
    	mRemoteControl.editMetadata(true)
    	.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, (currentSong == null) ? null : currentSong.getArtist())
    	.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, (currentSong == null) ? null : currentSong.getAlbum())
    	.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, (currentSong) == null ? null : currentSong.getTitle())
    	.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, (currentSong == null) ? 
    			0 : ((currentSong.getDuration() == null) ? 0 : currentSong.getDuration()))
    	.apply();
    	if (currentSong == null) {
    		mRemoteControl.editMetadata(true)
        	.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null)
        	.apply();
    	} else {
    		new ImageLoader(context).loadImage(context, mRemoteControl, currentSong);
    	}
	}

}
