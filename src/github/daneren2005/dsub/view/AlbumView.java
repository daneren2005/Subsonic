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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import java.io.File;
/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class AlbumView extends UpdateView {
	private static final String TAG = AlbumView.class.getSimpleName();

	private Context context;
	private MusicDirectory.Entry album;
	private File file;

    private TextView titleView;
    private TextView artistView;
    private View coverArtView;
    private ImageButton starButton;
	private ImageView moreButton;
	
	private boolean exists = false;
	private boolean shaded = false;
	private boolean starred = true;

    public AlbumView(Context context) {
        super(context);
		this.context = context;
        LayoutInflater.from(context).inflate(R.layout.album_list_item, this, true);

        titleView = (TextView) findViewById(R.id.album_title);
        artistView = (TextView) findViewById(R.id.album_artist);
        coverArtView = findViewById(R.id.album_coverart);
        starButton = (ImageButton) findViewById(R.id.album_star);
		
		moreButton = (ImageView) findViewById(R.id.album_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
    }

    public void setAlbum(MusicDirectory.Entry album, ImageLoader imageLoader) {
    	this.album = album;
        
        titleView.setText(album.getTitle());
        artistView.setText(album.getArtist());
        artistView.setVisibility(album.getArtist() == null ? View.GONE : View.VISIBLE);
        imageLoader.loadImage(coverArtView, album, false, true);
        
        starButton.setVisibility(!album.isStarred() ? View.GONE : View.VISIBLE);
		starButton.setFocusable(false);
		
		file = FileUtil.getAlbumDirectory(context, album);
		updateBackground();
		update();
    }
    
    @Override
	protected void updateBackground() {
		exists = file.exists(); 
	}
	
	@Override
	protected void update() {
		if(album.isStarred()) {
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
