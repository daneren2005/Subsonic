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

package github.vrih.xsub.adapter;

import android.content.Context;
import android.view.ViewGroup;

import java.util.List;

import github.vrih.xsub.domain.Share;
import github.vrih.xsub.view.ShareView;
import github.vrih.xsub.view.UpdateView;

public class ShareAdapter extends SectionAdapter<Share>{
	public static final int VIEW_TYPE_SHARE = 1;

	public ShareAdapter(Context context, List<Share> shares, OnItemClickedListener listener) {
        super(context, shares);
		this.onItemClickedListener = listener;
    }

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new ShareView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Share item, int viewType) {
		holder.getUpdateView().setObject(item);
	}

	@Override
	public int getItemViewType(Share item) {
		return VIEW_TYPE_SHARE;
	}
}
