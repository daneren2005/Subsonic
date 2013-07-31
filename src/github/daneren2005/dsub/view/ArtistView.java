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
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;
import java.io.File;

/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class ArtistView extends UpdateView {
	private static final String TAG = ArtistView.class.getSimpleName();
	
	private Context context;
	private Artist artist;
	private File file;

    private TextView titleView;
    private ImageButton starButton;
	private ImageView moreButton;
	
	private boolean exists = false;
	private boolean shaded = false;
	private boolean starred = true;

    public ArtistView(Context context) {
        super(context);
		this.context = context;
        LayoutInflater.from(context).inflate(R.layout.artist_list_item, this, true);

        titleView = (TextView) findViewById(R.id.artist_name);
        starButton = (ImageButton) findViewById(R.id.artist_star);
		moreButton = (ImageView) findViewById(R.id.artist_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
    }

    public void setArtist(Artist artist) {
    	this.artist = artist;
        titleView.setText(artist.getName());
		file = FileUtil.getArtistDirectory(context, artist);
		updateBackground();
		update();
    }
    
    @Override
	protected void updateBackground() {
		exists = file.exists(); 
	}
	
	@Override
	protected void update() {
		if(artist.isStarred()) {
			if(!starred) {
				starButton.setVisibility(View.VISIBLE);
				starred = true;
			}
		} else {
			if(starred) {
				starButton.setVisibility(View.GONE);
				starred = false;
			}
		}
		
		if(exists) {
			if(!shaded) {
				moreButton.setImageResource(R.drawable.list_item_more_shaded);
				shaded = true;
			}
		} else {
			if(shaded) {
				moreButton.setImageResource(R.drawable.list_item_more);
				shaded = false;
			}
		}
	}
}
