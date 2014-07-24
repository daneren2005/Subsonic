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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.service.parser.SubsonicRESTException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UserAdapter;

public class AdminFragment extends SelectListFragment<User> {
	private static String TAG = AdminFragment.class.getSimpleName();

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_add_user:
				UserUtil.addNewUser(context, this);
				break;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();
		if(UserUtil.isCurrentAdmin()) {
			inflater.inflate(R.menu.admin_context, menu);
		} else if(UserUtil.isCurrentRole(User.SETTINGS)) {
			inflater.inflate(R.menu.admin_context_user, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		User user = objects.get(info.position);

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
	public ArrayAdapter getAdapter(List<User> objs) {
		return new UserAdapter(context, objs, getImageLoader());
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

			List<User> users = new ArrayList<User>();
			User user = musicService.getUser(refresh, UserUtil.getCurrentUsername(context), context, listener);
			if(user != null) {
				users.add(user);
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		User user = (User) parent.getItemAtPosition(position);

		SubsonicFragment fragment = new UserFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.INTENT_EXTRA_NAME_ID, user);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}
}
