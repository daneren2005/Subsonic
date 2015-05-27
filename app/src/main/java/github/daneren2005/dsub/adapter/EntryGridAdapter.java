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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.AlbumView;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.view.UpdateView.UpdateViewHolder;

public class EntryGridAdapter extends SectionAdapter<Entry> {
	private static String TAG = EntryGridAdapter.class.getSimpleName();

	public static int VIEW_TYPE_ALBUM_CELL = 1;
	public static int VIEW_TYPE_ALBUM_LINE = 2;
	public static int VIEW_TYPE_SONG = 3;

	private ImageLoader imageLoader;
	private boolean largeAlbums;
	private boolean showArtist = false;
	private boolean checkable = true;
	private View header;

	public EntryGridAdapter(Context context, List<Entry> entries, ImageLoader imageLoader, boolean largeCell) {
		super(context, entries);
		this.imageLoader = imageLoader;
		this.largeAlbums = largeCell;

		// Always show artist if they aren't all the same
		String artist = null;
		for(MusicDirectory.Entry entry: entries) {
			if(artist == null) {
				artist = entry.getArtist();
			}

			if(artist != null && !artist.equals(entry.getArtist())) {
				showArtist = true;
			}
		}
	}

	@Override
	public UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		UpdateView updateView = null;
		if(viewType == VIEW_TYPE_ALBUM_LINE || viewType == VIEW_TYPE_ALBUM_CELL) {
			updateView = new AlbumView(context, viewType == VIEW_TYPE_ALBUM_CELL);
		} else if(viewType == VIEW_TYPE_SONG) {
			updateView = new SongView(context);
		}

		return new UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateViewHolder holder, Entry entry, int viewType) {
		UpdateView view = holder.getUpdateView();
		if(viewType == VIEW_TYPE_ALBUM_CELL || viewType == VIEW_TYPE_ALBUM_LINE) {
			AlbumView albumView = (AlbumView) view;
			albumView.setShowArtist(showArtist);
			albumView.setObject(entry, imageLoader);
		} else if(viewType == VIEW_TYPE_SONG) {
			SongView songView = (SongView) view;
			songView.setObject(entry, checkable);
		}
	}

	@Override
	public void setChecked(UpdateView updateView, boolean checked) {
		if(updateView instanceof SongView) {
			((SongView) updateView).setChecked(checked);
		}
	}

	public UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		return new UpdateViewHolder(header, false);
	}
	public void onBindHeaderHolder(UpdateViewHolder holder, String header) {

	}

	@Override
	public int getItemViewType(Entry entry) {
		if(entry.isDirectory()) {
			if (largeAlbums) {
				return VIEW_TYPE_ALBUM_CELL;
			} else {
				return VIEW_TYPE_ALBUM_LINE;
			}
		} else {
			return VIEW_TYPE_SONG;
		}
	}

	public void setHeader(View header) {
		this.header = header;
		this.singleSectionHeader = true;
	}

	public void setShowArtist(boolean showArtist) {
		this.showArtist = showArtist;
	}
	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
	}

	public void removeAt(int index) {
		sections.get(0).remove(index);
		notifyItemRemoved(index);
	}
}
