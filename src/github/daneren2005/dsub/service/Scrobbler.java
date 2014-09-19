package github.daneren2005.dsub.service;

import android.content.Context;
import android.util.Log;

import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;

/**
 * Scrobbles played songs to Last.fm.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class Scrobbler {

    private static final String TAG = Scrobbler.class.getSimpleName();

    private String lastSubmission;
    private String lastNowPlaying;

    public void scrobble(final Context context, final DownloadFile song, final boolean submission) {
        if (song == null || !Util.isScrobblingEnabled(context)) {
            return;
        }
        
		// Ignore if online with no network access
		if(!Util.isOffline(context) && !Util.isNetworkConnected(context)) {
			return;
		}

		// Ignore podcasts
		if(song.getSong() instanceof PodcastEpisode) {
			return;
		}
		
		// Ignore songs which are under 30 seconds per Last.FM guidelines
		if(song.getDuration() != null && song.getDuration() > 0 && song.getDuration < 30) {
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
