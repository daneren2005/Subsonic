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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.view;

import android.content.Context;
import github.daneren2005.dsub.R;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.domain.Playlist;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Sindre Mehus
 */
public class PlaylistAdapter extends ArrayAdapter<Playlist> {

	private final Context activity;

	public PlaylistAdapter(Context activity, List<Playlist> Playlists) {
		super(activity, R.layout.basic_list_item, Playlists);
		this.activity = activity;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Playlist entry = getItem(position);
		PlaylistView view;
		if (convertView != null && convertView instanceof PlaylistView) {
			view = (PlaylistView) convertView;
		} else {
			view = new PlaylistView(activity);
		}
		view.setObject(entry);
		return view;
	}

	public static class PlaylistComparator implements Comparator<Playlist> {
		@Override
		public int compare(Playlist playlist1, Playlist playlist2) {
			return playlist1.getName().compareToIgnoreCase(playlist2.getName());
		}

		public static List<Playlist> sort(List<Playlist> playlists) {
			Collections.sort(playlists, new PlaylistComparator());
			return playlists;
		}

	}
}
