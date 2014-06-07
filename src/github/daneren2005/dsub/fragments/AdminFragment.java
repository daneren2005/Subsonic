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

package github.daneren2005.dsub.fragments;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.view.UserAdapter;

public class AdminFragment extends SelectListFragment<User> {
	@Override
	public int getOptionsMenu() {
		return R.menu.admin;
	}

	@Override
	public ArrayAdapter getAdapter(List<User> objs) {
		return new UserAdapter(context, objs);
	}

	@Override
	public List<User> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		try {
			// Will only work if user is admin
			return musicService.getUsers(refresh, context, listener);
		} catch(SubsonicRESTException e) {
			List<User> users = new ArrayList<User>();
			users.add(musicService.getUser(refresh, "scott", context, listener));
			return users;
		}
	}

	@Override
	public int getTitleResource() {
		return R.string.button_bar_admin;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}
}
