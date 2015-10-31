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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.BasicHeaderView;
import github.daneren2005.dsub.view.FastScroller;
import github.daneren2005.dsub.view.PodcastChannelView;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.view.UpdateView;

import java.io.Serializable;
import java.util.List;

public class PodcastChannelAdapter extends SectionAdapter<Serializable> implements FastScroller.BubbleTextGetter {
	public static final int VIEW_TYPE_PODCAST_LEGACY = 1;
	public static final int VIEW_TYPE_PODCAST_LINE = 2;
	public static final int VIEW_TYPE_PODCAST_CELL = 3;
	public static final int VIEW_TYPE_PODCAST_EPISODE = 4;

	public static final String EPISODE_HEADER = "episodes";
	public static final String CHANNEL_HEADER = "channels";

	private ImageLoader imageLoader;
	private boolean largeCell;
	private int selectToggleAttr = R.attr.select_server;
	private List<Serializable> extraEpisodes;

	public PodcastChannelAdapter(Context context, List<Serializable> podcasts, ImageLoader imageLoader, OnItemClickedListener listener, boolean largeCell) {
		super(context, podcasts);
		this.imageLoader = imageLoader;
		this.onItemClickedListener = listener;
		this.largeCell = largeCell;
	}
	public PodcastChannelAdapter(Context context, List<String> headers, List<List<Serializable>> sections, List<Serializable> extraEpisodes, ImageLoader imageLoader, OnItemClickedListener listener, boolean largeCell) {
		super(context, headers, sections);
		this.extraEpisodes = extraEpisodes;
		this.imageLoader = imageLoader;
		this.onItemClickedListener = listener;
		this.largeCell = largeCell;
		checkable = true;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		UpdateView updateView;
		if(viewType == VIEW_TYPE_PODCAST_EPISODE) {
			updateView = new SongView(context);
		} else if(viewType == VIEW_TYPE_PODCAST_LEGACY) {
			updateView = new PodcastChannelView(context);
		} else {
			updateView = new PodcastChannelView(context, imageLoader, viewType == VIEW_TYPE_PODCAST_CELL);
		}

		return new UpdateView.UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Serializable item, int viewType) {
		if(viewType == VIEW_TYPE_PODCAST_EPISODE) {
			PodcastEpisode episode = (PodcastEpisode) item;
			holder.getUpdateView().setObject(item, !episode.isVideo());
		} else {
			holder.getUpdateView().setObject(item);
		}
	}

	@Override
	public int getItemViewType(Serializable item) {
		if(item instanceof PodcastChannel) {
			PodcastChannel channel = (PodcastChannel) item;

			if (imageLoader != null && channel.getCoverArt() != null) {
				return largeCell ? VIEW_TYPE_PODCAST_CELL : VIEW_TYPE_PODCAST_LINE;
			} else {
				return VIEW_TYPE_PODCAST_LEGACY;
			}
		} else {
			return VIEW_TYPE_PODCAST_EPISODE;
		}
	}

	@Override
	public String getTextToShowInBubble(int position) {
		Serializable item = getItemForPosition(position);
		if(item instanceof PodcastChannel) {
			PodcastChannel channel = (PodcastChannel) item;
			return getNameIndex(channel.getName(), true);
		} else {
			return null;
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
		menu.removeItem(R.id.menu_unstar);
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		return new UpdateView.UpdateViewHolder(new BasicHeaderView(context, R.layout.newest_episode_header));
	}

	@Override
	public void onBindHeaderHolder(UpdateView.UpdateViewHolder holder, String header) {
		UpdateView view = holder.getUpdateView();
		ImageView toggleSelectionView = (ImageView) view.findViewById(R.id.item_select);

		String display;
		if(EPISODE_HEADER.equals(header)) {
			display = context.getResources().getString(R.string.main_albums_newest);

			if(extraEpisodes != null && !extraEpisodes.isEmpty()) {
				toggleSelectionView.setVisibility(View.VISIBLE);
				toggleSelectionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Update icon
						if (selectToggleAttr == R.attr.select_server) {
							selectToggleAttr = R.attr.select_tabs;

							// Update how many are displayed
							sections.get(0).addAll(extraEpisodes);
							notifyItemRangeInserted(4, extraEpisodes.size());
						} else {
							selectToggleAttr = R.attr.select_server;

							// Update how many are displayed
							sections.get(0).removeAll(extraEpisodes);
							notifyItemRangeRemoved(4, extraEpisodes.size());
						}

						((ImageView) v).setImageResource(DrawableTint.getDrawableRes(context, selectToggleAttr));

					}
				});
				toggleSelectionView.setImageResource(DrawableTint.getDrawableRes(context, selectToggleAttr));
			}
		} else {
			display = context.getResources().getString(R.string.select_podcasts_channels);
			toggleSelectionView.setVisibility(View.GONE);
		}

		if(view != null) {
			view.setObject(display);
		}
	}
}
