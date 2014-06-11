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
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.SettingsAdapter;

public class UserFragment extends SubsonicFragment{
	private ListView listView;
	private LayoutInflater inflater;
	private User user;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		this.inflater = inflater;
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setEnabled(false);

		Bundle args = getArguments();
		user = (User) args.getSerializable(Constants.INTENT_EXTRA_NAME_ID);

		listView = (ListView)rootView.findViewById(R.id.fragment_list);
		listView.setAdapter(new SettingsAdapter(context, user.getSettings(), UserUtil.isCurrentAdmin(context)));

		setTitle(user.getUsername());

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(!primaryFragment) {
			return;
		}

		if(UserUtil.isCurrentAdmin(context)) {
			menuInflater.inflate(R.menu.user, menu);
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
				updateSettings();
				return true;
		}

		return false;
	}

	private void updateSettings() {
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.updateUser(user, context, null);
				return null;
			}

			@Override
			protected void done(Void v) {
				Util.toast(context, context.getResources().getString(R.string.admin_update_permissions_success, user.getUsername()));
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.admin_update_permissions_error, user.getUsername());
				}

				Util.toast(context, msg);
			}
		}.execute();
	}
}
