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
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();
		if(UserUtil.isCurrentAdmin(context) && Util.checkServerVersion(context, "1.10")) {
			inflater.inflate(R.menu.admin_context, menu);
		} else {
			inflater.inflate(R.menu.admin_context_user, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		User user = objects.get(info.position);

		switch(menuItem.getItemId()) {
			case R.id.admin_change_password:
				changePassword(user);
				break;
			case R.id.admin_delete_user:
				deleteUser(user);
				break;
		}

		return true;
	}

	@Override
	public int getOptionsMenu() {
		if(UserUtil.isCurrentAdmin(context)) {
			return R.menu.admin;
		} else {
			return R.menu.empty;
		}
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
			// Delete cached users if not allowed to get them
			String s = Util.getRestUrl(context, null, false);
			String cache = "users-" + s.hashCode() + ".ser";
			File file = new File(context.getCacheDir(), cache);
			file.delete();

			List<User> users = new ArrayList<User>();
			users.add(musicService.getUser(refresh, UserUtil.getCurrentUsername(context), context, listener));
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

	private void changePassword(final User user) {
		View layout = context.getLayoutInflater().inflate(R.layout.change_password, null);
		final TextView passwordView = (TextView) layout.findViewById(R.id.new_password);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.admin_change_password)
				.setView(layout)
				.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						final String password = passwordView.getText().toString();
						// Don't allow blank passwords
						if ("".equals(password)) {
							Util.toast(context, R.string.admin_change_password_invalid);
							return;
						}

						new SilentBackgroundTask<Void>(context) {
							@Override
							protected Void doInBackground() throws Throwable {
								MusicService musicService = MusicServiceFactory.getMusicService(context);
								musicService.changePassword(user.getUsername(), password, context, null);
								return null;
							}

							@Override
							protected void done(Void v) {
								Util.toast(context, context.getResources().getString(R.string.admin_change_password_success, user.getUsername()));
							}

							@Override
							protected void error(Throwable error) {
								String msg;
								if (error instanceof OfflineException || error instanceof ServerTooOldException) {
									msg = getErrorMessage(error);
								} else {
									msg = context.getResources().getString(R.string.admin_change_password_error, user.getUsername());
								}

								Util.toast(context, msg);
							}
						}.execute();
					}
				})
				.setNegativeButton(R.string.common_cancel, null)
				.setCancelable(true);

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void deleteUser(final User user) {
		Util.confirmDialog(context, R.string.common_delete, user.getUsername(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new SilentBackgroundTask<Void>(context) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deleteUser(user.getUsername(), context, null);
						return null;
					}

					@Override
					protected void done(Void v) {
						adapter.remove(user);
						adapter.notifyDataSetChanged();

						Util.toast(context, context.getResources().getString(R.string.admin_delete_user_success, user.getUsername()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.admin_delete_user_error, user.getUsername());
						}

						Util.toast(context, msg);
					}
				}.execute();
			}
		});
	}
}
