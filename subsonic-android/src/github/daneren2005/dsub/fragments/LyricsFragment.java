/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */

package github.daneren2005.dsub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;

/**
 * Displays song lyrics.
 *
 * @author Sindre Mehus
 */
public final class LyricsFragment extends SubsonicFragment {
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.lyrics, container, false);
		load();

		return rootView;
	}

	private void load() {
		BackgroundTask<Lyrics> task = new TabBackgroundTask<Lyrics>(this) {
			@Override
			protected Lyrics doInBackground() throws Throwable {
				String artist = getArguments().getString(Constants.INTENT_EXTRA_NAME_ARTIST);
				String title = getArguments().getString(Constants.INTENT_EXTRA_NAME_TITLE);
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				return musicService.getLyrics(artist, title, context, this);
			}

			@Override
			protected void done(Lyrics result) {
				TextView artistView = (TextView) rootView.findViewById(R.id.lyrics_artist);
				TextView titleView = (TextView) rootView.findViewById(R.id.lyrics_title);
				TextView textView = (TextView) rootView.findViewById(R.id.lyrics_text);
				if (result != null && result.getArtist() != null) {
					artistView.setText(result.getArtist());
					titleView.setText(result.getTitle());
					textView.setText(result.getText());
				} else {
					artistView.setText(R.string.lyrics_nomatch);
				}
			}
		};
		task.execute();
	}
}