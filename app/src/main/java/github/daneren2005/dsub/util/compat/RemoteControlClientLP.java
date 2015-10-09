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
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v7.media.MediaRouter;

import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RemoteControlClientLP extends RemoteControlClientBase {
	private MediaSession mediaSession;
	private DownloadService downloadService;

	private PlaybackState previousState;

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
	}

	@Override
	public void unregister(Context context) {
		mediaSession.release();
	}

	@Override
	public void setPlaybackState(int state) {
		PlaybackState.Builder builder;
		if(previousState == null) {
			builder = new PlaybackState.Builder();
		} else {
			builder = new PlaybackState.Builder(previousState);
		}

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

		PlaybackState playbackState = builder.build();
		mediaSession.setPlaybackState(playbackState);
		previousState = playbackState;
	}

	@Override
	public void updateMetadata(Context context, MusicDirectory.Entry currentSong) {
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

		mediaSession.setMetadata(builder.build());
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
				PlaybackState.ACTION_SET_RATING;
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
		public void onSetRating(Rating rating) {
			if(rating.getRatingStyle() != Rating.RATING_THUMB_UP_DOWN) {
				return;
			}

			if(rating.isRated()) {
				if(rating.isThumbUp()) {

				} else {

				}
			} else {

			}
		}
	}
}
