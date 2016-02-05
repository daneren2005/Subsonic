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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.adapter.SettingsAdapter;
import github.daneren2005.dsub.view.UpdateView;

public class UserFragment extends SelectRecyclerFragment<User.Setting>{
	private User user;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Bundle args = getArguments();
		user = (User) args.getSerializable(Constants.INTENT_EXTRA_NAME_ID);
		pullToRefresh = false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_update_permissions:
				UserUtil.updateSettings(context, user);
				return true;
			case R.id.menu_change_password:
				UserUtil.changePassword(context, user);
				return true;
			case R.id.menu_change_email:
				UserUtil.changeEmail(context, user);
				return true;
		}

		return false;
	}

	@Override
	public int getOptionsMenu() {
		if(UserUtil.isCurrentAdmin() && ServerInfo.checkServerVersion(context, "1.10")) {
			return R.menu.user;
		} else if(UserUtil.isCurrentRole(User.SETTINGS)) {
			return R.menu.user_user;
		} else {
			return R.menu.empty;
		}
	}

	@Override
	public SectionAdapter<User.Setting> getAdapter(List<User.Setting> objs) {
		return SettingsAdapter.getSettingsAdapter(context, user, getImageLoader(), this);
	}

	@Override
	public List<User.Setting> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return user.getSettings();
	}

	@Override
	public int getTitleResource() {
		setTitle(user.getUsername());
		return 0;
	}

	@Override
	public void onItemClicked(UpdateView<User.Setting> updateView, User.Setting item) {
		if(updateView.isCheckable()) {
			boolean newValue = !item.getValue();
			item.setValue(newValue);
			updateView.setChecked(newValue);
		}
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<User.Setting> updateView, User.Setting item) {}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<User.Setting> updateView, User.Setting item) {
		return false;
	}
}
