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
import android.view.ViewGroup;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.FastScroller;
import github.daneren2005.dsub.view.PodcastChannelView;
import github.daneren2005.dsub.view.UpdateView;

import java.util.List;

public class PodcastChannelAdapter extends SectionAdapter<PodcastChannel> implements FastScroller.BubbleTextGetter {
	public static int VIEW_TYPE_PODCAST = 1;
	public static int VIEW_TYPE_PODCAST_LINE = 2;
	public static int VIEW_TYPE_PODCAST_CELL = 3;

	private ImageLoader imageLoader;
	private boolean largeCell;

	public PodcastChannelAdapter(Context context, List<PodcastChannel> podcasts, ImageLoader imageLoader, OnItemClickedListener listener, boolean largeCell) {
		super(context, podcasts);
		this.onItemClickedListener = listener;
		this.imageLoader = imageLoader;
		this.largeCell = largeCell;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		PodcastChannelView view;
		if(viewType == VIEW_TYPE_PODCAST) {
			view = new PodcastChannelView(context);
		} else {
			view = new PodcastChannelView(context, imageLoader, viewType == VIEW_TYPE_PODCAST_CELL);
		}

		return new UpdateView.UpdateViewHolder(view);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, PodcastChannel item, int viewType) {
		holder.getUpdateView().setObject(item);
	}

	@Override
	public int getItemViewType(PodcastChannel item) {
		if(imageLoader != null && item.getCoverArt() != null) {
			return largeCell ? VIEW_TYPE_PODCAST_CELL : VIEW_TYPE_PODCAST_LINE;
		} else {
			return VIEW_TYPE_PODCAST;
		}
	}

	@Override
	public String getTextToShowInBubble(int position) {
		return getNameIndex(getItemForPosition(position).getName(), true);
	}
}
