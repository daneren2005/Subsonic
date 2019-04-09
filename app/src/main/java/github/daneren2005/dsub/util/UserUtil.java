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

package github.daneren2005.dsub.util;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.adapter.SettingsAdapter;
import github.daneren2005.dsub.view.UpdateView;

public final class UserUtil {
	private static final String TAG = UserUtil.class.getSimpleName();
	private static final long MIN_VERIFY_DURATION = 1000L * 60L * 60L;
	
	private static int instance = -1;
	private static int instanceHash = -1;
	private static User currentUser;
	private static long lastVerifiedTime = 0;

	public static void refreshCurrentUser(Context context, boolean forceRefresh) {
		refreshCurrentUser(context, forceRefresh, false);
	}
	public static void refreshCurrentUser(Context context, boolean forceRefresh, boolean unAuth) {
		currentUser = null;
		if(unAuth) {
			lastVerifiedTime = 0;
		}
		seedCurrentUser(context, forceRefresh);
	}

	public static void seedCurrentUser(Context context) {
		seedCurrentUser(context, false);
	}
	public static void seedCurrentUser(final Context context, final boolean refresh) {
		// Only try to seed if online
		if(Util.isOffline(context)) {
			currentUser = null;
			return;
		}
		
		final int instance = Util.getActiveServer(context);
		final int instanceHash = (instance == 0) ? 0 : Util.getRestUrl(context, null).hashCode();
		if(UserUtil.instance == instance && UserUtil.instanceHash == instanceHash && currentUser != null) {
			return;
		} else {
			UserUtil.instance = instance;
			UserUtil.instanceHash = instanceHash;
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				currentUser = MusicServiceFactory.getMusicService(context).getUser(refresh, getCurrentUsername(context, instance), context, null);

				// If running, redo cast selector
				DownloadService downloadService = DownloadService.getInstance();
				if(downloadService != null) {
					downloadService.userSettingsChanged();
				}

				return null;
			}

			@Override
			protected void done(Void result) {
				if(context instanceof AppCompatActivity) {
					((AppCompatActivity) context).supportInvalidateOptionsMenu();
				}
			}

			@Override
			protected void error(Throwable error) {
				// Don't do anything, supposed to be background pull
				Log.e(TAG, "Failed to seed user information");
			}
		}.execute();
	}

	public static User getCurrentUser() {
		return currentUser;
	}

	public static String getCurrentUsername(Context context, int instance) {
		SharedPreferences prefs = Util.getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
	}

	public static String getCurrentUsername(Context context) {
		return getCurrentUsername(context, Util.getActiveServer(context));
	}

	public static boolean isCurrentAdmin() {
		return isCurrentRole(User.ADMIN);
	}
	
	public static boolean canPodcast() {
		return isCurrentRole(User.PODCAST);
	}
	public static boolean canShare() {
		return isCurrentRole(User.SHARE);
	}
	public static boolean canJukebox() {
		return isCurrentRole(User.JUKEBOX);
	}
	public static boolean canScrobble() {
		return isCurrentRole(User.SCROBBLING, true);
	}

	public static boolean isCurrentRole(String role) {
		return isCurrentRole(role, false);
	}
	public static boolean isCurrentRole(String role, boolean defaultValue) {
		if(currentUser == null) {
			return defaultValue;
		}

		for(User.Setting setting: currentUser.getSettings()) {
			if(setting.getName().equals(role)) {
				return setting.getValue() == true;
			}
		}

		return defaultValue;
	}

	public static void confirmCredentials(Activity context, Runnable onSuccess) {
		confirmCredentials(context, onSuccess, null);
	}
	public static void confirmCredentials(final Activity context, final Runnable onSuccess, final Runnable onCancel) {
		final long currentTime = System.currentTimeMillis();
		// If already ran this check within last x time, just go ahead and auth
		if((currentTime - lastVerifiedTime) < MIN_VERIFY_DURATION) {
			onSuccess.run();
		} else {
			View layout = context.getLayoutInflater().inflate(R.layout.confirm_password, null);
			final TextView passwordView = (TextView) layout.findViewById(R.id.password);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.admin_confirm_password)
				.setView(layout)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if(isPasswordCorrect(context, passwordView)) {
							lastVerifiedTime = currentTime;
							onSuccess.run();
						} else {
							Util.toast(context, R.string.admin_confirm_password_bad);
						}
					}
				})
				.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(onCancel != null) {
							onCancel.run();
						}
					}
				})
				.setCancelable(true);
			
			AlertDialog dialog = builder.create();
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			dialog.show();
		}
	}

	public static void changePassword(final Activity context, final User user) {
		View layout = context.getLayoutInflater().inflate(R.layout.change_password, null);
		View currentPasswordLayout = layout.findViewById(R.id.current_password_layout);
		final TextView currentPasswordView = (TextView) layout.findViewById(R.id.current_password);
		final TextView passwordView = (TextView) layout.findViewById(R.id.new_password);

		if(isCurrentAdmin()) {
			currentPasswordLayout.setVisibility(View.GONE);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.admin_change_password)
				.setView(layout)
				.setPositiveButton(R.string.common_save, null)
				.setNegativeButton(R.string.common_cancel, null)
				.setCancelable(true);

		final AlertDialog dialog = builder.create();
		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String password = passwordView.getText().toString();
				if(!isCurrentAdmin() && !isPasswordCorrect(context, currentPasswordView)) {
					Util.toast(context, R.string.admin_confirm_password_bad);
					return;
				}
				// Don't allow blank passwords
				else if ("".equals(password)) {
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

				dialog.dismiss();
			}
		});
	}

	private static boolean isPasswordCorrect(Context context, TextView passwordView) {
		return isPasswordCorrect(context, passwordView.getText().toString());
	}
	private static boolean isPasswordCorrect(Context context, String password) {
		SharedPreferences prefs = Util.getPreferences(context);
		String correctPassword = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + Util.getActiveServer(context), null);

		if (prefs.getBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + instance, false)) {
			correctPassword = KeyStoreUtil.decrypt(correctPassword);
		}

		return password != null && password.equals(correctPassword);
	}

	public static void updateSettings(final Context context, final User user) {
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

	public static void changeEmail(final Activity context, final User user) {
		View layout = context.getLayoutInflater().inflate(R.layout.change_email, null);
		final TextView emailView = (TextView) layout.findViewById(R.id.new_email);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.admin_change_email)
				.setView(layout)
				.setPositiveButton(R.string.common_save, null)
				.setNegativeButton(R.string.common_cancel, null)
				.setCancelable(true);

		final AlertDialog dialog = builder.create();
		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String email = emailView.getText().toString();
				// Don't allow blank emails
				if ("".equals(email)) {
					Util.toast(context, R.string.admin_change_email_invalid);
					return;
				}

				new SilentBackgroundTask<Void>(context) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.changeEmail(user.getUsername(), email, context, null);
						user.setEmail(email);
						return null;
					}

					@Override
					protected void done(Void v) {
						Util.toast(context, context.getResources().getString(R.string.admin_change_email_success, user.getUsername()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.admin_change_email_error, user.getUsername());
						}

						Util.toast(context, msg);
					}
				}.execute();

				dialog.dismiss();
			}
		});
	}

	public static void deleteUser(final Context context, final User user, final SectionAdapter adapter) {
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
						if(adapter != null) {
							adapter.removeItem(user);
						}

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

	public static void addNewUser(final Activity context, final SubsonicFragment fragment, User sampleUser) {
		final User user = new User();
		for(String role: User.ROLES) {
			if(role.equals(User.SETTINGS) || role.equals(User.STREAM)) {
				user.addSetting(role, true);
			} else {
				user.addSetting(role, false);
			}
		}

		if(sampleUser != null && sampleUser.getMusicFolderSettings() != null) {
			for(User.Setting setting: sampleUser.getMusicFolderSettings()) {
				User.MusicFolderSetting musicFolderSetting = (User.MusicFolderSetting) setting;
				user.addMusicFolder(musicFolderSetting, true);
			}
		}

		View layout = context.getLayoutInflater().inflate(R.layout.create_user, null);
		final TextView usernameView = (TextView) layout.findViewById(R.id.username);
		final TextView emailView = (TextView) layout.findViewById(R.id.email);
		final TextView passwordView = (TextView) layout.findViewById(R.id.password);
		final RecyclerView recyclerView = (RecyclerView) layout.findViewById(R.id.settings_list);
		LinearLayoutManager layoutManager = new LinearLayoutManager(context);
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(SettingsAdapter.getSettingsAdapter(context, user, null, true, new SectionAdapter.OnItemClickedListener<User.Setting>() {
			@Override
			public void onItemClicked(UpdateView<User.Setting> updateView, User.Setting item) {
				if(updateView.isCheckable()) {
					boolean newValue = !item.getValue();
					item.setValue(newValue);
					updateView.setChecked(newValue);
				}
			}

			@Override
			public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<User.Setting> updateView, User.Setting item) {

			}

			@Override
			public boolean onContextItemSelected(MenuItem menuItem, UpdateView<User.Setting> updateView, User.Setting item) {
				return false;
			}
		}));

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.menu_add_user)
				.setView(layout)
				.setPositiveButton(R.string.common_save, null)
				.setNegativeButton(R.string.common_cancel, null)
				.setCancelable(true);

		final AlertDialog dialog = builder.create();
		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String username = usernameView.getText().toString();
				// Don't allow blank emails
				if ("".equals(username)) {
					Util.toast(context, R.string.admin_change_username_invalid);
					return;
				}

				final String email = emailView.getText().toString();
				// Don't allow blank emails
				if ("".equals(email)) {
					Util.toast(context, R.string.admin_change_email_invalid);
					return;
				}

				final String password = passwordView.getText().toString();
				if ("".equals(password)) {
					Util.toast(context, R.string.admin_change_password_invalid);
					return;
				}

				user.setUsername(username);
				user.setEmail(email);
				user.setPassword(password);

				new SilentBackgroundTask<Void>(context) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.createUser(user, context, null);
						return null;
					}

					@Override
					protected void done(Void v) {
						fragment.onRefresh();
						Util.toast(context, context.getResources().getString(R.string.admin_create_user_success));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.admin_create_user_error);
						}

						Util.toast(context, msg);
					}
				}.execute();

				dialog.dismiss();
			}
		});
	}
}
