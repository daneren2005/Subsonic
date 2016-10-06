package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.ImageLoader;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.support.v7.media.MediaRouter;

import java.util.List;

import github.daneren2005.dsub.activity.SubsonicActivity;

@TargetApi(14)
public class RemoteControlClientICS extends RemoteControlClientBase {
	private static String TAG = RemoteControlClientICS.class.getSimpleName();

	protected RemoteControlClient mRemoteControl;
	protected ImageLoader imageLoader;
	protected DownloadService downloadService;
	
	public void register(final Context context, final ComponentName mediaButtonReceiverComponent) {
		downloadService = (DownloadService) context;
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		// build the PendingIntent for the remote control client
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, mediaButtonIntent, 0);

		// create and register the remote control client
		mRemoteControl = new RemoteControlClient(mediaPendingIntent);
		audioManager.registerRemoteControlClient(mRemoteControl);

		mRemoteControl.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		mRemoteControl.setTransportControlFlags(getTransportFlags());
		imageLoader = SubsonicActivity.getStaticImageLoader(context);
	}
	
	public void unregister(final Context context) {
		if(mRemoteControl == null) {
			return;
		}

		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.unregisterRemoteControlClient(mRemoteControl);
	}
	
	public void setPlaybackState(final int state, int index, int queueSize) {
		if(mRemoteControl == null) {
			return;
		}

		mRemoteControl.setPlaybackState(state);
	}
	
	public void updateMetadata(final Context context, final MusicDirectory.Entry currentSong) {
		if(mRemoteControl == null) {
			return;
		}

		if(imageLoader == null) {
			imageLoader = SubsonicActivity.getStaticImageLoader(context);
		}
		
		// Update the remote controls
		RemoteControlClient.MetadataEditor editor = mRemoteControl.editMetadata(true);
		updateMetadata(currentSong, editor);
		editor.apply();
    	if (currentSong == null || imageLoader == null) {
    		updateAlbumArt(currentSong, null);
    	} else {
    		imageLoader.loadImage(context, this, currentSong);
    	}
	}

	@Override
	public void metadataChanged(MusicDirectory.Entry currentSong) {

	}

	@Override
	public void updateAlbumArt(MusicDirectory.Entry currentSong, Bitmap bitmap) {
		mRemoteControl.editMetadata(false)
				.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap).
				apply();
	}

	@Override
	public void registerRoute(MediaRouter router) {
		if(mRemoteControl == null) {
			return;
		}

		router.addRemoteControlClient(mRemoteControl);
	}

	@Override
	public void unregisterRoute(MediaRouter router) {
		if(mRemoteControl == null) {
			return;
		}

		router.removeRemoteControlClient(mRemoteControl);
	}

	@Override
	public void updatePlaylist(List<DownloadFile> playlist) {

	}

	protected void updateMetadata(final MusicDirectory.Entry currentSong, final RemoteControlClient.MetadataEditor editor) {
		editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, (currentSong == null) ? null : currentSong.getArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, (currentSong == null) ? null : currentSong.getAlbum())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, (currentSong == null) ? null : currentSong.getArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, (currentSong) == null ? null : currentSong.getTitle())
			.putString(MediaMetadataRetriever.METADATA_KEY_GENRE, (currentSong) == null ? null : currentSong.getGenre())
			.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, (currentSong == null) ? 
				0 : ((currentSong.getTrack() == null) ? 0 : currentSong.getTrack()))
			.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, (currentSong == null) ? 
				0 : ((currentSong.getDuration() == null) ? 0 : (currentSong.getDuration() * 1000)));
	}
	
	protected int getTransportFlags() {
		return RemoteControlClient.FLAG_KEY_MEDIA_PLAY | 
			RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | 
			RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
			RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
			RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
			RemoteControlClient.FLAG_KEY_MEDIA_STOP;
	}

}
