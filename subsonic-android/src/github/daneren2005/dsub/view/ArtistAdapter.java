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

import github.daneren2005.dsub.R;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.activity.SubsonicTabActivity;
import github.daneren2005.dsub.domain.Artist;

/**
 * @author Sindre Mehus
 */
public class ArtistAdapter extends ArrayAdapter<Artist> {

    private final SubsonicTabActivity activity;

    public ArtistAdapter(SubsonicTabActivity activity, List<Artist> artists) {
        super(activity, R.layout.artist_list_item, artists);
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Artist entry = getItem(position);
		ArtistView view;
		if (convertView != null && convertView instanceof ArtistView) {
			view = (ArtistView) convertView;
		} else {
			view = new ArtistView(activity);
		}
		view.setArtist(entry);
		return view;
    }
}
