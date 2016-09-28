package github.daneren2005.dsub.service;

import android.content.Context;
import android.util.Log;

import github.daneren2005.dsub.domain.InternetRadioStation;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.SongDBHandler;
import github.daneren2005.dsub.util.Util;

/**
 * Scrobbles played songs to Last.fm.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class Scrobbler {
	private static final String TAG = Scrobbler.class.getSimpleName();
	private static final int FOUR_MINUTES = 4 * 60 * 1000;

	private String lastSubmission;
	private String lastNowPlaying;

	public void conditionalScrobble(Context context, DownloadFile song, int playerPosition, int duration, boolean isPastCutoff) {
		// More than 4 minutes
		if(playerPosition > FOUR_MINUTES) {
			scrobble(context, song, true, isPastCutoff);
		}
		// More than 50% played
		else if(duration > 0 && playerPosition > (duration / 2)) {
			scrobble(context, song, true, isPastCutoff);
		}
	}

	public void scrobble(final Context context, final DownloadFile song, final boolean submission, final boolean isPastCutoff) {
		if(song == null) {
			return;
		}

		final String id = song.getSong().getId();
		// Avoid duplicate registrations.
		if (submission && id.equals(lastSubmission)) {
			return;
		}
		if (!submission && id.equals(lastNowPlaying)) {
			return;
		}

		if (submission) {
			lastSubmission = id;
		} else {
			lastNowPlaying = id;
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() {
				if(isPastCutoff) {
					SongDBHandler.getHandler(context).setSongPlayed(song, submission);
				}

				// Scrobbling disabled
				if (!Util.isScrobblingEnabled(context)) {
					return null;
				}
				// Ignore if online with no network access
				else if(!Util.isOffline(context) && !Util.isNetworkConnected(context)) {
					return null;
				}
				// Ignore podcasts
				else if(song.getSong() instanceof PodcastEpisode || song.getSong() instanceof InternetRadioStation) {
					return null;
				}

				// Ignore songs which are under 30 seconds per Last.FM guidelines
				Integer duration = song.getSong().getDuration();
				if(duration != null && duration > 0 && duration < 30) {
					return null;
				}

				MusicService service = MusicServiceFactory.getMusicService(context);
				try {
					service.scrobble(id, submission, context, null);
					Log.i(TAG, "Scrobbled '" + (submission ? "submission" : "now playing") + "' for " + song);
				} catch (Exception x) {
					Log.i(TAG, "Failed to scrobble'" + (submission ? "submission" : "now playing") + "' for " + song, x);
				}
				return null;
			}
		}.execute();
	}
}
