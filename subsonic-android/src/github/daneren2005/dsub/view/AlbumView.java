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
package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;

/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class AlbumView extends UpdateView {
	private static final String TAG = AlbumView.class.getSimpleName();
	
	private MusicDirectory.Entry album;

    private TextView titleView;
    private TextView artistView;
    private View coverArtView;
    private ImageButton starButton;

    public AlbumView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.album_list_item, this, true);

        titleView = (TextView) findViewById(R.id.album_title);
        artistView = (TextView) findViewById(R.id.album_artist);
        coverArtView = findViewById(R.id.album_coverart);
        starButton = (ImageButton) findViewById(R.id.album_star);
    }

    public void setAlbum(MusicDirectory.Entry album, ImageLoader imageLoader) {
    	this.album = album;
        
        titleView.setText(album.getTitle());
        artistView.setText(album.getArtist());
        artistView.setVisibility(album.getArtist() == null ? View.GONE : View.VISIBLE);
        imageLoader.loadImage(coverArtView, album, false, true);
        
        starButton.setVisibility((Util.isOffline(getContext()) || !album.isStarred()) ? View.GONE : View.VISIBLE);
		starButton.setFocusable(false);
		
		update();
    }
	
	@Override
	protected void update() {
		starButton.setVisibility((Util.isOffline(getContext()) || !album.isStarred()) ? View.GONE : View.VISIBLE);
    }
}
