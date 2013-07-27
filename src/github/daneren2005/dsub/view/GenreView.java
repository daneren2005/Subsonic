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
import github.daneren2005.dsub.domain.Genre;

public class GenreView extends UpdateView {
	private static final String TAG = GenreView.class.getSimpleName();

	private TextView titleView;
	private ImageButton starButton;
	private ImageView moreButton;

	public GenreView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.artist_list_item, this, true);

		titleView = (TextView) findViewById(R.id.artist_name);
		starButton = (ImageButton) findViewById(R.id.artist_star);
		moreButton = (ImageView) findViewById(R.id.artist_more);
		moreButton.setClickable(false);
	}

	public void setGenre(Genre genre) {
		titleView.setText(genre.getName());

		starButton.setVisibility(View.GONE);
		starButton.setFocusable(false);
	}
}
