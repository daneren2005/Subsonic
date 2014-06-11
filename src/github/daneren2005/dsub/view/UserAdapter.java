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

package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.util.ImageLoader;

public class UserAdapter extends ArrayAdapter<User> {
	private final Context activity;
	private final ImageLoader imageLoader;

	public UserAdapter(Context activity, List<User> users, ImageLoader imageLoader) {
		super(activity, R.layout.basic_list_item, users);
		this.activity = activity;
		this.imageLoader = imageLoader;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		User entry = getItem(position);
		UserView view;
		if (convertView != null && convertView instanceof UserView) {
			view = (UserView) convertView;
		} else {
			view = new UserView(activity);
		}
		view.setObject(entry, imageLoader);
		return view;
	}
}