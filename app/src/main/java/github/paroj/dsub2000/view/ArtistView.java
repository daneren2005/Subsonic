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
package github.paroj.dsub2000.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import github.paroj.dsub2000.R;
import github.paroj.dsub2000.domain.Artist;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.util.FileUtil;
import github.paroj.dsub2000.util.ImageLoader;
import github.paroj.dsub2000.util.TouchDelegateUtil;

import java.io.File;

/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class ArtistView extends UpdateView<Artist> {
	private static final String TAG = ArtistView.class.getSimpleName();

	private File file;
    private TextView titleView;
	private ImageLoader imageLoader;

    public ArtistView(Context context) {
		this(context, null);
	}

    public ArtistView(Context context, ImageLoader imageLoader) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.artist_list_item, this, true);

        titleView = (TextView) findViewById(R.id.item_name);
		coverArtView = findViewById(R.id.item_art);
		starButton = (ImageButton) findViewById(R.id.item_star);
		starButton.setFocusable(false);
		moreButton = (ImageView) findViewById(R.id.item_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
        TouchDelegateUtil.expandTouchArea(moreButton);

		this.imageLoader = imageLoader;
		((RecyclingImageView) coverArtView).setOnInvalidated(new RecyclingImageView.OnInvalidated() {
			@Override
			public void onInvalidated(RecyclingImageView imageView) {
				if(item != null) {
					setObjectImpl(item);
				}
			}
		});
    }
    
    protected void setObjectImpl(Artist artist) {
    	titleView.setText(artist.getName());
		file = FileUtil.getArtistDirectory(context, artist);

		if(imageLoader == null || (artist.getCoverArt() == null && artist.getArtistImageUrl() == null)) {
			coverArtView.setVisibility(GONE);
			imageTask = null;
		} else if(artist.getCoverArt() != null) {
			coverArtView.setVisibility(VISIBLE);
			MusicDirectory.Entry entry = new MusicDirectory.Entry(artist);
			entry.setCoverArt(artist.getCoverArt());
			imageTask = imageLoader.loadImage(coverArtView, entry, false, false);
		} else {
			coverArtView.setVisibility(VISIBLE);
			imageTask = imageLoader.loadImage(coverArtView, artist.getArtistImageUrl(), false);
		}
    }
    
    @Override
	protected void updateBackground() {
		exists = file.exists();
		isStarred = item.isStarred();
	}

	public File getFile() {
		return file;
	}
}
