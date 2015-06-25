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
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Genre;

public class GenreView extends UpdateView<Genre> {
	private static final String TAG = GenreView.class.getSimpleName();

	private TextView titleView;
	private TextView songsView;
	private TextView albumsView;

	public GenreView(Context context) {
		super(context, false);
		LayoutInflater.from(context).inflate(R.layout.genre_list_item, this, true);

		titleView = (TextView) findViewById(R.id.genre_name);
		songsView = (TextView) findViewById(R.id.genre_songs);
		albumsView = (TextView) findViewById(R.id.genre_albums);
	}

	public void setObjectImpl(Genre genre) {
		titleView.setText(genre.getName());

		if(genre.getAlbumCount() != null) {
			songsView.setVisibility(View.VISIBLE);
			albumsView.setVisibility(View.VISIBLE);
			songsView.setText(context.getResources().getString(R.string.select_genre_songs, genre.getSongCount()));
			albumsView.setText(context.getResources().getString(R.string.select_genre_albums, genre.getAlbumCount()));
		} else {
			songsView.setVisibility(View.GONE);
			albumsView.setVisibility(View.GONE);
		}
	}
}
