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
import github.daneren2005.dsub.view.PodcastChannelView;
import github.daneren2005.dsub.view.UpdateView;

import java.util.List;

public class PodcastChannelAdapter extends SectionAdapter<PodcastChannel>{
    public static int VIEW_TYPE_PODCAST = 1;

	public PodcastChannelAdapter(Context context, List<PodcastChannel> podcasts, OnItemClickedListener listener) {
        super(context, podcasts);
		this.onItemClickedListener = listener;
    }

    @Override
    public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
        return new UpdateView.UpdateViewHolder(new PodcastChannelView(context));
    }

    @Override
    public void onBindViewHolder(UpdateView.UpdateViewHolder holder, PodcastChannel item, int viewType) {
        holder.getUpdateView().setObject(item);
    }

    @Override
    public int getItemViewType(PodcastChannel item) {
        return VIEW_TYPE_PODCAST;
    }
}
