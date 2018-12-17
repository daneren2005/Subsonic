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

package github.vrih.xsub.fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import github.vrih.xsub.R;
import github.vrih.xsub.adapter.SectionAdapter;
import github.vrih.xsub.domain.User;
import github.vrih.xsub.service.MusicService;
import github.vrih.xsub.service.parser.SubsonicRESTException;
import github.vrih.xsub.util.Constants;
import github.vrih.xsub.util.ProgressListener;
import github.vrih.xsub.util.UserUtil;
import github.vrih.xsub.util.Util;
import github.vrih.xsub.adapter.UserAdapter;
import github.vrih.xsub.view.UpdateView;

public class AdminFragment extends SelectRecyclerFragment<User> {

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_add_user:
				UserUtil.addNewUser(context, this, (objects.size() > 0) ? objects.get(0) : null);
				break;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<User> updateView, User item) {
		if(UserUtil.isCurrentAdmin()) {
			menuInflater.inflate(R.menu.admin_context, menu);
		} else if(UserUtil.isCurrentRole(User.SETTINGS)) {
			menuInflater.inflate(R.menu.admin_context_user, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<User> updateView, User user) {
		switch(menuItem.getItemId()) {
			case R.id.admin_change_email:
				UserUtil.changeEmail(context, user);
				break;
			case R.id.admin_change_password:
				UserUtil.changePassword(context, user);
				break;
			case R.id.admin_delete_user:
				UserUtil.deleteUser(context, user, adapter);
				break;
		}

		return true;
	}

	@Override
	public int getOptionsMenu() {
		if(UserUtil.isCurrentAdmin()) {
			return R.menu.admin;
		} else {
			return R.menu.empty;
		}
	}

	@Override
	public SectionAdapter getAdapter(List<User> objs) {
		return new UserAdapter(context, objs, getImageLoader(), this);
	}

	@Override
	public List<User> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		try {
			// Will only work if user is admin
			List<User> users = musicService.getUsers(refresh, context, listener);
			if(refresh) {
				UserUtil.refreshCurrentUser(context, true);
			}
			return users;
		} catch(SubsonicRESTException e) {
			// Delete cached users if not allowed to get them
			String s = Util.getRestUrl(context, null, false);
			String cache = "users-" + s.hashCode() + ".ser";
			File file = new File(context.getCacheDir(), cache);
			file.delete();

			List<User> users = new ArrayList<>();
			User user = musicService.getUser(refresh, UserUtil.getCurrentUsername(context), context, listener);
			if(user != null) {
				SubsonicFragment fragment = new UserFragment();
				Bundle args = new Bundle();
				args.putSerializable(Constants.INTENT_EXTRA_NAME_ID, user);
				fragment.setArguments(args);

				replaceExistingFragment(fragment);
			}

			UserUtil.refreshCurrentUser(context, false);
			return users;
		}
	}

	@Override
	public int getTitleResource() {
		return R.string.button_bar_admin;
	}

	@Override
	public void onItemClicked(UpdateView<User> updateView, User user) {
		SubsonicFragment fragment = new UserFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.INTENT_EXTRA_NAME_ID, user);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}
}
