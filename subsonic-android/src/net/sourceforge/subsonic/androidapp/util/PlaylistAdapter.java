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
package net.sourceforge.subsonic.androidapp.util;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.Playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
* @author Sindre Mehus
* @version $Id$
*/
public class PlaylistAdapter extends ArrayAdapter<Playlist> implements SectionIndexer {

    // Both arrays are indexed by section ID.
    private final Object[] sections;
    private final Integer[] positions;

    /**
     * Note: playlists must be sorted alphabetically.
     */
    public PlaylistAdapter(Context context, List<Playlist> playlists) {
        super(context, R.layout.playlist_list_item, playlists);

        Set<String> sectionSet = new LinkedHashSet<String>(30);
        List<Integer> positionList = new ArrayList<Integer>(30);
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            if (playlist.getName().length() > 0) {
                String index = playlist.getName().substring(0, 1).toUpperCase();
                if (!sectionSet.contains(index)) {
                    sectionSet.add(index);
                    positionList.add(i);
                }
            }
        }
        sections = sectionSet.toArray(new Object[sectionSet.size()]);
        positions = positionList.toArray(new Integer[positionList.size()]);
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
