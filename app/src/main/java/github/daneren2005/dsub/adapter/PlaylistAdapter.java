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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.view.ViewGroup;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.view.PlaylistView;
import github.daneren2005.dsub.view.UpdateView;

public class PlaylistAdapter extends SectionAdapter<Playlist> {
	public static int VIEW_TYPE_PLAYLIST = 1;

	public PlaylistAdapter(Context context, List<Playlist> playlists, OnItemClickedListener listener) {
		super(context, playlists);
		this.onItemClickedListener = listener;
	}
	public PlaylistAdapter(Context context, List<String> headers, List<List<Playlist>> sections, OnItemClickedListener listener) {
		super(context, headers, sections);
		this.onItemClickedListener = listener;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new PlaylistView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Playlist playlist, int viewType) {
		holder.getUpdateView().setObject(playlist);
		holder.setItem(playlist);
	}

	@Override
	public int getItemViewType(Playlist playlist) {
		return VIEW_TYPE_PLAYLIST;
	}
}
