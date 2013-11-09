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
import android.widget.SectionIndexer;
import github.daneren2005.dsub.domain.Artist;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Sindre Mehus
 */
public class ArtistAdapter extends ArrayAdapter<Artist> implements SectionIndexer {

    private final Context activity;
	
	// Both arrays are indexed by section ID.
    private final Object[] sections;
    private final Integer[] positions;

    public ArtistAdapter(Context activity, List<Artist> artists) {
        super(activity, R.layout.basic_list_item, artists);
        this.activity = activity;
		
		Set<String> sectionSet = new LinkedHashSet<String>(30);
        List<Integer> positionList = new ArrayList<Integer>(30);
        for (int i = 0; i < artists.size(); i++) {
            Artist artist = artists.get(i);
            String index = artist.getIndex();
            if (!sectionSet.contains(index)) {
                sectionSet.add(index);
                positionList.add(i);
            }
        }
        sections = sectionSet.toArray(new Object[sectionSet.size()]);
        positions = positionList.toArray(new Integer[positionList.size()]);
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
		view.setObject(entry);
		return view;
    }
	
	@Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    public int getPositionForSection(int section) {
        section = Math.min(section, positions.length - 1);
        return positions[section];
    }

    @Override
    public int getSectionForPosition(int pos) {
        for (int i = 0; i < sections.length - 1; i++) {
            if (pos < positions[i + 1]) {
                return i;
            }
        }
        return sections.length - 1;
    }
}
