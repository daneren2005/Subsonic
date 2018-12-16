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
package github.vrih.xsub.adapter;

import android.content.Context;
import android.view.ViewGroup;

import java.util.List;

import github.vrih.xsub.domain.InternetRadioStation;
import github.vrih.xsub.view.FastScroller;
import github.vrih.xsub.view.InternetRadioStationView;
import github.vrih.xsub.view.UpdateView;

public class InternetRadioStationAdapter extends SectionAdapter<InternetRadioStation> implements FastScroller.BubbleTextGetter {
	public static final int VIEW_TYPE_INTERNET_RADIO_STATION = 1;

	public InternetRadioStationAdapter(Context context, List<InternetRadioStation> stations, OnItemClickedListener listener) {
		super(context, stations);
		this.onItemClickedListener = listener;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new InternetRadioStationView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, InternetRadioStation station, int viewType) {
		holder.getUpdateView().setObject(station);
		holder.setItem(station);
	}

	@Override
	public int getItemViewType(InternetRadioStation station) {
		return VIEW_TYPE_INTERNET_RADIO_STATION;
	}

	@Override
	public String getTextToShowInBubble(int position) {
		InternetRadioStation item = getItemForPosition(position);
		return item.getTitle();
	}
}
