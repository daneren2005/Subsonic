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
package net.sourceforge.subsonic.androidapp.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;

/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class AlbumView extends LinearLayout {

    private TextView titleView;
    private TextView artistView;
    private View coverArtView;

    public AlbumView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.album_list_item, this, true);

        titleView = (TextView) findViewById(R.id.album_title);
        artistView = (TextView) findViewById(R.id.album_artist);
        coverArtView = findViewById(R.id.album_coverart);
    }

    public void setAlbum(MusicDirectory.Entry album, ImageLoader imageLoader) {
        titleView.setText(album.getTitle());
        artistView.setText(album.getArtist());
        artistView.setVisibility(album.getArtist() == null ? View.GONE : View.VISIBLE);
        imageLoader.loadImage(coverArtView, album, false, true);
    }
}
