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

package net.sourceforge.subsonic.androidapp.activity;

import android.os.Bundle;
import android.widget.TextView;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.Lyrics;
import net.sourceforge.subsonic.androidapp.service.MusicService;
import net.sourceforge.subsonic.androidapp.service.MusicServiceFactory;
import net.sourceforge.subsonic.androidapp.util.BackgroundTask;
import net.sourceforge.subsonic.androidapp.util.Constants;
import net.sourceforge.subsonic.androidapp.util.TabActivityBackgroundTask;

/**
 * Displays song lyrics.
 *
 * @author Sindre Mehus
 */
public final class LyricsActivity extends SubsonicTabActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.lyrics);
        load();
    }

    private void load() {
        BackgroundTask<Lyrics> task = new TabActivityBackgroundTask<Lyrics>(this) {
            @Override
            protected Lyrics doInBackground() throws Throwable {
                String artist = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ARTIST);
                String title = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_TITLE);
                MusicService musicService = MusicServiceFactory.getMusicService(LyricsActivity.this);
                return musicService.getLyrics(artist, title, LyricsActivity.this, this);
            }

            @Override
            protected void done(Lyrics result) {
                TextView artistView = (TextView) findViewById(R.id.lyrics_artist);
                TextView titleView = (TextView) findViewById(R.id.lyrics_title);
                TextView textView = (TextView) findViewById(R.id.lyrics_text);
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