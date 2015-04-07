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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.adapter.SettingsAdapter;

public class UserFragment extends SubsonicFragment{
	private ListView listView;
	private User user;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setEnabled(false);

		Bundle args = getArguments();
		user = (User) args.getSerializable(Constants.INTENT_EXTRA_NAME_ID);

		listView = (ListView)rootView.findViewById(R.id.fragment_list);
		createHeader();
		listView.setAdapter(new SettingsAdapter(context, user.getSettings(), UserUtil.isCurrentAdmin() && ServerInfo.checkServerVersion(context, "1.10")));

		setTitle(user.getUsername());

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((SubsonicActivity) activity).supportInvalidateOptionsMenu();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		// For some reason this is called before onAttach
		if(!primaryFragment || context == null) {
			return;
		}

		if(UserUtil.isCurrentAdmin() && ServerInfo.checkServerVersion(context, "1.10")) {
			menuInflater.inflate(R.menu.user, menu);
		} else if(UserUtil.isCurrentRole(User.SETTINGS)) {
			menuInflater.inflate(R.menu.user_user, menu);
		} else {
			menuInflater.inflate(R.menu.empty, menu);
		}
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

	private void createHeader() {
		View header = LayoutInflater.from(context).inflate(R.layout.user_header, listView, false);

		final ImageLoader imageLoader = getImageLoader();
		ImageView coverArtView = (ImageView) header.findViewById(R.id.user_avatar);
		imageLoader.loadAvatar(context, coverArtView, user.getUsername());

		TextView usernameView = (TextView) header.findViewById(R.id.user_username);
		usernameView.setText(user.getUsername());

		final TextView emailView = (TextView) header.findViewById(R.id.user_email);
		if(user.getEmail() != null) {
			emailView.setText(user.getEmail());
		} else {
			emailView.setVisibility(View.GONE);
		}

		listView.addHeaderView(header);
	}
}
