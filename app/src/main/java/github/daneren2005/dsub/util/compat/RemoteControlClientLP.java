/*
	This file is part of Subsonic.

	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.

	Copyright 2015 (C) Scott Jackson
*/
package github.daneren2005.dsub.util.compat;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.UpdateHelper;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RemoteControlClientLP extends RemoteControlClientBase {
	private static final String TAG = RemoteControlClientLP.class.getSimpleName();
	private static final String CUSTOM_ACTION_THUMBS_UP = "github.daneren2005.dsub.THUMBS_UP";
	private static final String CUSTOM_ACTION_THUMBS_DOWN = "github.daneren2005.dsub.THUMBS_DOWN";
	private static final String CUSTOM_ACTION_STAR = "github.daneren2005.dsub.STAR";

	protected MediaSession mediaSession;
	protected DownloadService downloadService;
	protected ImageLoader imageLoader;

	@Override
	public void register(Context context, ComponentName mediaButtonReceiverComponent) {
		downloadService = (DownloadService) context;
		mediaSession = new MediaSession(downloadService, "DSub MediaSession");

		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, mediaButtonIntent, 0);
		mediaSession.setMediaButtonReceiver(mediaPendingIntent);

		Intent activityIntent = new Intent(context, SubsonicFragmentActivity.class);
		activityIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0, activityIntent, 0);
		mediaSession.setSessionActivity(activityPendingIntent);

		mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
		mediaSession.setCallback(new EventCallback());

		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
			mediaSession.setRatingType(Rating.RATING_THUMB_UP_DOWN);
		}

		AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
		audioAttributesBuilder.setUsage(AudioAttributes.USAGE_MEDIA)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
		mediaSession.setPlaybackToLocal(audioAttributesBuilder.build());
		mediaSession.setActive(true);

		imageLoader = SubsonicActivity.getStaticImageLoader(context);
	}

	@Override
	public void unregister(Context context) {
		mediaSession.release();
	}

	@Override
	public void setPlaybackState(int state) {
		PlaybackState.Builder builder = new PlaybackState.Builder();

		int newState = PlaybackState.STATE_NONE;
		switch(state) {
			case RemoteControlClient.PLAYSTATE_PLAYING:
				newState = PlaybackState.STATE_PLAYING;
				break;
			case RemoteControlClient.PLAYSTATE_STOPPED:
				newState = PlaybackState.STATE_STOPPED;
				break;
			case RemoteControlClient.PLAYSTATE_PAUSED:
				newState = PlaybackState.STATE_PAUSED;
				break;
			case RemoteControlClient.PLAYSTATE_BUFFERING:
				newState = PlaybackState.STATE_BUFFERING;
				break;
		}

		long position = -1;
		if(state == RemoteControlClient.PLAYSTATE_PLAYING || state == RemoteControlClient.PLAYSTATE_PAUSED) {
			position = downloadService.getPlayerPosition();
		}
		builder.setState(newState, position, 1.0f);
		builder.setActions(getPlaybackActions());

		DownloadFile downloadFile = downloadService.getCurrentPlaying();
		if(downloadFile != null) {
			addCustomActions(downloadFile.getSong(), builder);
		}

		PlaybackState playbackState = builder.build();
		mediaSession.setPlaybackState(playbackState);
	}

	@Override
	public void updateMetadata(Context context, MusicDirectory.Entry currentSong) {
		setMetadata(currentSong, null);

		if(currentSong != null && imageLoader != null) {
			imageLoader.loadImage(context, this, currentSong);
		}
	}

	public void setMetadata(MusicDirectory.Entry currentSong, Bitmap bitmap) {
		MediaMetadata.Builder builder = new MediaMetadata.Builder();
		builder.putString(MediaMetadata.METADATA_KEY_ARTIST, (currentSong == null) ? null : currentSong.getArtist())
				.putString(MediaMetadata.METADATA_KEY_ALBUM, (currentSong == null) ? null : currentSong.getAlbum())
				.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, (currentSong == null) ? null : currentSong.getArtist())
				.putString(MediaMetadata.METADATA_KEY_TITLE, (currentSong) == null ? null : currentSong.getTitle())
				.putString(MediaMetadata.METADATA_KEY_GENRE, (currentSong) == null ? null : currentSong.getGenre())
				.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, (currentSong == null) ?
						0 : ((currentSong.getTrack() == null) ? 0 : currentSong.getTrack()))
				.putLong(MediaMetadata.METADATA_KEY_DURATION, (currentSong == null) ?
						0 : ((currentSong.getDuration() == null) ? 0 : (currentSong.getDuration() * 1000)));

		int userRating = currentSong.getRating();
		Rating rating;
		if(userRating == 1) {
			rating = Rating.newThumbRating(false);
		} else if(userRating == 5) {
			rating = Rating.newThumbRating(true);
		} else {
			rating = Rating.newUnratedRating(Rating.RATING_THUMB_UP_DOWN);
		}
		builder.putRating(MediaMetadata.METADATA_KEY_USER_RATING, rating);

		if(bitmap != null) {
			builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
		}

		mediaSession.setMetadata(builder.build());
	}

	@Override
	public void updateAlbumArt(MusicDirectory.Entry currentSong, Bitmap bitmap) {
		setMetadata(currentSong, bitmap);
	}

	@Override
	public void registerRoute(MediaRouter router) {
		router.setMediaSession(mediaSession);
	}

	@Override
	public void unregisterRoute(MediaRouter router) {
		router.setMediaSession(null);
	}

	public MediaSession getMediaSession() {
		return mediaSession;
	}

	protected long getPlaybackActions() {
		return PlaybackState.ACTION_PLAY |
				PlaybackState.ACTION_PAUSE |
				PlaybackState.ACTION_SEEK_TO |
				PlaybackState.ACTION_SKIP_TO_NEXT |
				PlaybackState.ACTION_SKIP_TO_PREVIOUS;
	}
	protected void addCustomActions(MusicDirectory.Entry currentSong, PlaybackState.Builder builder) {
		PlaybackState.CustomAction thumbsUp = new PlaybackState.CustomAction.Builder(CUSTOM_ACTION_THUMBS_UP,
				downloadService.getString(R.string.download_thumbs_up),
				R.drawable.ic_action_rating_good_selected).build();

		PlaybackState.CustomAction thumbsDown = new PlaybackState.CustomAction.Builder(CUSTOM_ACTION_THUMBS_DOWN,
				downloadService.getString(R.string.download_thumbs_down),
				R.drawable.ic_action_rating_bad_selected).build();

		PlaybackState.CustomAction star = new PlaybackState.CustomAction.Builder(CUSTOM_ACTION_STAR,
				downloadService.getString(R.string.common_star),
				R.drawable.ic_toggle_star).build();

		builder.addCustomAction(star).addCustomAction(thumbsDown).addCustomAction(thumbsUp);
	}

	private class EventCallback extends MediaSession.Callback {
		@Override
		public void onPlay() {
			downloadService.start();
		}

		@Override
		public void onStop() {
			downloadService.pause();
		}

		@Override
		public void onPause() {
			downloadService.pause();
		}

		@Override
		public void onSeekTo(long position) {
			downloadService.seekTo((int) position);
		}

		@Override
		public void onSkipToNext() {
			downloadService.next();
		}
		@Override
		public void onSkipToPrevious() {
			downloadService.previous();
		}

		@Override
		public void onCustomAction(String action, Bundle extras) {
			if(CUSTOM_ACTION_THUMBS_UP.equals(action)) {
				downloadService.toggleRating(5);
			} else if(CUSTOM_ACTION_THUMBS_DOWN.equals(action)) {
				downloadService.toggleRating(1);
			} else if(CUSTOM_ACTION_STAR.equals(action)) {
				downloadService.toggleStarred();
			}
		}
	}
}
