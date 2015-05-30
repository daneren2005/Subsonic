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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.view.UserView;

public class UserAdapter extends SectionAdapter<User> {
	public static int VIEW_TYPE_USER = 1;

	private final ImageLoader imageLoader;

	public UserAdapter(Context context, List<User> users, ImageLoader imageLoader, OnItemClickedListener listener) {
		super(context, users);
		this.imageLoader = imageLoader;
		this.onItemClickedListener = listener;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new UserView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, User item, int viewType) {
		holder.getUpdateView().setObject(item, imageLoader);
	}

	@Override
	public int getItemViewType(User item) {
		return VIEW_TYPE_USER;
	}
}