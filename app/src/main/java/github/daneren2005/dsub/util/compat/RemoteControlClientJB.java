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
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.SilentBackgroundTask;

@TargetApi(18)
public class RemoteControlClientJB extends RemoteControlClientICS {
	@Override
	public void register(final Context context, final ComponentName mediaButtonReceiverComponent) {
		super.register(context, mediaButtonReceiverComponent);
		
		mRemoteControl.setOnGetPlaybackPositionListener(new RemoteControlClient.OnGetPlaybackPositionListener() {
			@Override
			public long onGetPlaybackPosition() {
				return downloadService.getPlayerPosition();
			}
		});
		mRemoteControl.setPlaybackPositionUpdateListener(new RemoteControlClient.OnPlaybackPositionUpdateListener() {
			@Override
			public void onPlaybackPositionUpdate(final long newPosition) {
				new SilentBackgroundTask<Void>(context) {
					@Override
					protected Void doInBackground() throws Throwable {
						downloadService.seekTo((int) newPosition);
						return null;
					}
				}.execute();
				setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			}
		});
	}
	
	@Override
	public void setPlaybackState(final int state) {
		if(mRemoteControl == null) {
			return;
		}

		long position = -1;
		if(state == RemoteControlClient.PLAYSTATE_PLAYING || state == RemoteControlClient.PLAYSTATE_PAUSED) {
			position = downloadService.getPlayerPosition();
		}
		mRemoteControl.setPlaybackState(state, position, 1.0f);
	}
	
	@Override
	protected int getTransportFlags() {
		return super.getTransportFlags() | RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;
	}

}
