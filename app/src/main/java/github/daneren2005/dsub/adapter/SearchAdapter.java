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
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.AlbumView;
import github.daneren2005.dsub.view.ArtistView;
import github.daneren2005.dsub.view.BasicHeaderView;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.view.UpdateView;

import static github.daneren2005.dsub.adapter.ArtistAdapter.VIEW_TYPE_ARTIST;
import static github.daneren2005.dsub.adapter.EntryGridAdapter.VIEW_TYPE_ALBUM_CELL;
import static github.daneren2005.dsub.adapter.EntryGridAdapter.VIEW_TYPE_ALBUM_LINE;
import static github.daneren2005.dsub.adapter.EntryGridAdapter.VIEW_TYPE_SONG;

public class SearchAdapter extends ExpandableSectionAdapter<Serializable> {
	private ImageLoader imageLoader;
	private boolean largeAlbums;

	private static final int MAX_ARTISTS = 10;
	private static final int MAX_ALBUMS = 4;
	private static final int MAX_SONGS = 10;

	public SearchAdapter(Context context, SearchResult searchResult, ImageLoader imageLoader, boolean largeAlbums, OnItemClickedListener listener) {
		this.imageLoader = imageLoader;
		this.largeAlbums = largeAlbums;

		List<List<Serializable>> sections = new ArrayList<>();
		List<String> headers = new ArrayList<>();
		List<Integer> defaultVisible = new ArrayList<>();
		Resources res = context.getResources();
		if(!searchResult.getArtists().isEmpty()) {
			sections.add((List<Serializable>) (List<?>) searchResult.getArtists());
			headers.add(res.getString(R.string.search_artists));
			defaultVisible.add(MAX_ARTISTS);
		}
		if(!searchResult.getAlbums().isEmpty()) {
			sections.add((List<Serializable>) (List<?>) searchResult.getAlbums());
			headers.add(res.getString(R.string.search_albums));
			defaultVisible.add(MAX_ALBUMS);
		}
		if(!searchResult.getSongs().isEmpty()) {
			sections.add((List<Serializable>) (List<?>) searchResult.getSongs());
			headers.add(res.getString(R.string.search_songs));
			defaultVisible.add(MAX_SONGS);
		}
		init(context, headers, sections, defaultVisible);

		this.onItemClickedListener = listener;
		checkable = true;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		UpdateView updateView = null;
		if(viewType == VIEW_TYPE_ALBUM_CELL || viewType == VIEW_TYPE_ALBUM_LINE) {
			updateView = new AlbumView(context, viewType == VIEW_TYPE_ALBUM_CELL);
		} else if(viewType == VIEW_TYPE_SONG) {
			updateView = new SongView(context);
		} else if(viewType == VIEW_TYPE_ARTIST) {
			updateView = new ArtistView(context);
		}

		return new UpdateView.UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Serializable item, int viewType) {
		UpdateView view = holder.getUpdateView();
		if(viewType == VIEW_TYPE_ALBUM_CELL || viewType == VIEW_TYPE_ALBUM_LINE) {
			AlbumView albumView = (AlbumView) view;
			albumView.setObject((Entry) item, imageLoader);
		} else if(viewType == VIEW_TYPE_SONG) {
			SongView songView = (SongView) view;
			songView.setObject((Entry) item, true);
		} else if(viewType == VIEW_TYPE_ARTIST) {
			view.setObject(item);
		}
	}

	@Override
	public int getItemViewType(Serializable item) {
		if(item instanceof Entry) {
			Entry entry = (Entry) item;
			if (entry.isDirectory()) {
				if (largeAlbums) {
					return VIEW_TYPE_ALBUM_CELL;
				} else {
					return VIEW_TYPE_ALBUM_LINE;
				}
			} else {
				return VIEW_TYPE_SONG;
			}
		} else {
			return VIEW_TYPE_ARTIST;
		}
	}

	@Override
	public void onCreateActionModeMenu(Menu menu, MenuInflater menuInflater) {
		if(Util.isOffline(context)) {
			menuInflater.inflate(R.menu.multiselect_media_offline, menu);
		} else {
			menuInflater.inflate(R.menu.multiselect_media, menu);
		}

		menu.removeItem(R.id.menu_remove_playlist);
	}
}
