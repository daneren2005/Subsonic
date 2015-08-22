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
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.adapter;

import android.content.Context;

import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.FastScroller;

public class AlphabeticalAlbumAdapter extends EntryInfiniteGridAdapter implements FastScroller.BubbleTextGetter {
	public AlphabeticalAlbumAdapter(Context context, List<MusicDirectory.Entry> entries, ImageLoader imageLoader, boolean largeCell) {
		super(context, entries, imageLoader, largeCell);
	}

	@Override
	public String getTextToShowInBubble(int position) {
		// Make sure that we are not trying to get an item for the loading placeholder
		if(position >= sections.get(0).size()) {
			if(sections.get(0).size() > 0) {
				return getTextToShowInBubble(position - 1);
			} else {
				return "*";
			}
		} else {
			return getNameIndex(getItemForPosition(position).getAlbum());
		}
	}
}
