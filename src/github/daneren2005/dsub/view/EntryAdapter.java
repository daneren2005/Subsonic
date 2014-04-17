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

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;

/**
 * @author Sindre Mehus
 */
public class EntryAdapter extends ArrayAdapter<MusicDirectory.Entry> {
	private final static String TAG = EntryAdapter.class.getSimpleName();
    private final Context activity;
    private final ImageLoader imageLoader;
    private final boolean checkable;
	private List<MusicDirectory.Entry> entries;

    public EntryAdapter(Context activity, ImageLoader imageLoader, List<MusicDirectory.Entry> entries, boolean checkable) {
        super(activity, android.R.layout.simple_list_item_1, entries);
		this.entries = entries;
        this.activity = activity;
        this.imageLoader = imageLoader;
        this.checkable = checkable;
    }
	
	public void removeAt(int position) {
		entries.remove(position);
	}

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MusicDirectory.Entry entry = getItem(position);

        if (entry.isDirectory()) {
			if(entry.getArtist() != null || entry.getParent() != null) {
				AlbumView view;
				view = new AlbumView(activity);
				view.setObject(entry, imageLoader);
				return view;
			} else {
				ArtistEntryView view = new ArtistEntryView(activity);
				view.setObject(entry);
				return view;
			}
        } else {
            SongView view;
            if (convertView != null && convertView instanceof SongView) {
                view = (SongView) convertView;
            } else {
                view = new SongView(activity);
            }
            view.setObject(entry, checkable);
            return view;
        }
    }
}
