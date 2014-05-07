/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;

public class AlbumGridAdapter extends ArrayAdapter<MusicDirectory.Entry> {
	private final static String TAG = AlbumGridAdapter.class.getSimpleName();
	private final Context activity;
	private final ImageLoader imageLoader;
	private List<MusicDirectory.Entry> entries;
	private boolean showArtist;

	public AlbumGridAdapter(Context activity, ImageLoader imageLoader, List<MusicDirectory.Entry> entries, boolean showArtist) {
		super(activity, android.R.layout.simple_list_item_1, entries);
		this.entries = entries;
		this.activity = activity;
		this.imageLoader = imageLoader;
		
		// Always show artist if they aren't all the same
		if(!showArtist) {
			for(MusicDirectory.Entry entry: entries) {
				if(artist == null) {
					artist = entry.getArtist();
				}
				
				if(artist != null && !artist.equals(entry.getArtist())) {
					showArtist = true;
				}
			}
		}
		this.showArtist = showArtist;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		MusicDirectory.Entry entry = getItem(position);

		AlbumCell view;
		if(convertView instanceof AlbumCell) {
			view = (AlbumCell) convertView;
		} else {
			view = new AlbumCell(activity);
		}

		view.setShowArtist(showArtist);
		view.setObject(entry, imageLoader);
		return view;
	}
}
