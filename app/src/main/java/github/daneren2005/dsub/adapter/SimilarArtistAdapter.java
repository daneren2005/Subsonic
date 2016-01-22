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
	Copyright 2016 (C) Scott Jackson
*/

package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.view.ViewGroup;

import java.util.List;

import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.view.ArtistView;
import github.daneren2005.dsub.view.UpdateView;

public class SimilarArtistAdapter extends SectionAdapter<Artist> {
	public static int VIEW_TYPE_ARTIST = 4;

	public SimilarArtistAdapter(Context context, List<Artist> artists, OnItemClickedListener onItemClickedListener) {
		super(context, artists);
		this.onItemClickedListener = onItemClickedListener;
	}
	public SimilarArtistAdapter(Context context, List<String> headers, List<List<Artist>> sections, OnItemClickedListener onItemClickedListener) {
		super(context, headers, sections);
		this.onItemClickedListener = onItemClickedListener;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new ArtistView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Artist item, int viewType) {
		holder.getUpdateView().setObject(item);
	}

	@Override
	public int getItemViewType(Artist item) {
		return VIEW_TYPE_ARTIST;
	}
}
