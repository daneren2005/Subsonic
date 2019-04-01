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

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.HeadphoneListenerService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.MediaRouteManager;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.CacheLocationPreference;
import github.daneren2005.dsub.view.ErrorDialog;
import github.daneren2005.dsub.view.EditPasswordPreference;

public class SettingsFragment extends PreferenceCompatFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = SettingsFragment.class.getSimpleName();

	private final Map<String, ServerSettings> serverSettings = new LinkedHashMap<String, ServerSettings>();
	private boolean testingConnection;
	private ListPreference theme;
	private ListPreference maxBitrateWifi;
	private ListPreference maxBitrateMobile;
	private ListPreference maxVideoBitrateWifi;
	private ListPreference maxVideoBitrateMobile;
	private ListPreference networkTimeout;
	private CacheLocationPreference cacheLocation;
	private ListPreference preloadCountWifi;
	private ListPreference preloadCountMobile;
	private ListPreference keepPlayedCount;
	private ListPreference tempLoss;
	private ListPreference pauseDisconnect;
	private Preference addServerPreference;
	private PreferenceCategory serversCategory;
	private ListPreference songPressAction;
	private ListPreference videoPlayer;
	private ListPreference syncInterval;
	private CheckBoxPreference syncEnabled;
	private CheckBoxPreference syncWifi;
	private CheckBoxPreference syncNotification;
	private CheckBoxPreference syncStarred;
	private CheckBoxPreference syncMostRecent;
	private CheckBoxPreference replayGain;
	private ListPreference replayGainType;
	private Preference replayGainBump;
	private Preference replayGainUntagged;
	private String internalSSID;
	private String internalSSIDDisplay;
	private EditTextPreference cacheSize;
	private ListPreference openToTab;

	private int serverCount = 3;
	private SharedPreferences settings;
	private DecimalFormat megabyteFromat;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(Build.VERSION.SDK_INT >= 21) {
			CheckBoxPreference mediaButtons = (CheckBoxPreference) findPreference("mediaButtons");
			if (mediaButtons != null) {
				PreferenceCategory otherCategory = (PreferenceCategory) findPreference("otherSettings");
				otherCategory.removePreference(mediaButtons);
			}
		}

		int instance = this.getArguments().getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, -1);
		if (instance != -1) {
			PreferenceScreen preferenceScreen = expandServer(instance);
			setPreferenceScreen(preferenceScreen);

			serverSettings.put(Integer.toString(instance), new ServerSettings(instance));
			onInitPreferences(preferenceScreen);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		SharedPreferences prefs = Util.getPreferences(context);
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStartNewFragment(String name) {
		SettingsFragment newFragment = new SettingsFragment();
		Bundle args = new Bundle();

		int xml = 0;
		if("appearance".equals(name)) {
			xml = R.xml.settings_appearance;
		} else if("drawer".equals(name)) {
			xml = R.xml.settings_drawer;
		} else if("cache".equals(name)) {
			xml = R.xml.settings_cache;
		} else if("sync".equals(name)) {
			xml = R.xml.settings_sync;
		} else if("playback".equals(name)) {
			xml = R.xml.settings_playback;
		} else if("servers".equals(name)) {
			xml = R.xml.settings_servers;
		} else if ("cast".equals(name)) {
			xml = R.xml.settings_cast;
		}

		if(xml != 0) {
			args.putInt(Constants.INTENT_EXTRA_FRAGMENT_TYPE, xml);
			newFragment.setArguments(args);
			replaceFragment(newFragment);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Random error I have no idea how to reproduce
		if(sharedPreferences == null) {
			return;
		}

		update();

		if (Constants.PREFERENCES_KEY_HIDE_MEDIA.equals(key)) {
			setHideMedia(sharedPreferences.getBoolean(key, false));
		}
		else if (Constants.PREFERENCES_KEY_MEDIA_BUTTONS.equals(key)) {
			setMediaButtonsEnabled(sharedPreferences.getBoolean(key, true));
		}
		else if (Constants.PREFERENCES_KEY_CACHE_LOCATION.equals(key)) {
			setCacheLocation(sharedPreferences.getString(key, ""));
		}
		else if (Constants.PREFERENCES_KEY_SLEEP_TIMER_DURATION.equals(key)){
			DownloadService downloadService = DownloadService.getInstance();
			downloadService.setSleepTimerDuration(Integer.parseInt(sharedPreferences.getString(key, "60")));
		}
		else if(Constants.PREFERENCES_KEY_SYNC_MOST_RECENT.equals(key)) {
			SyncUtil.removeMostRecentSyncFiles(context);
		} else if(Constants.PREFERENCES_KEY_REPLAY_GAIN.equals(key) || Constants.PREFERENCES_KEY_REPLAY_GAIN_BUMP.equals(key) || Constants.PREFERENCES_KEY_REPLAY_GAIN_UNTAGGED.equals(key)) {
			DownloadService downloadService = DownloadService.getInstance();
			if(downloadService != null) {
				downloadService.reapplyVolume();
			}
		} else if(Constants.PREFERENCES_KEY_START_ON_HEADPHONES.equals(key)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setClassName(context.getPackageName(), HeadphoneListenerService.class.getName());

			if(sharedPreferences.getBoolean(key, false)) {
				context.startService(serviceIntent);
			} else {
				context.stopService(serviceIntent);
			}
		} else if(Constants.PREFERENCES_KEY_THEME.equals(key)) {
			String value = sharedPreferences.getString(key, null);
			if("day/night".equals(value) || "day/black".equals(value)) {
				if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(context, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, SubsonicActivity.PERMISSIONS_REQUEST_LOCATION);
				}
			}
		} else if(Constants.PREFERENCES_KEY_DLNA_CASTING_ENABLED.equals(key)) {
			DownloadService downloadService = DownloadService.getInstance();
			if(downloadService != null) {
				MediaRouteManager mediaRouter = downloadService.getMediaRouter();

				Boolean enabled = sharedPreferences.getBoolean(key, true);
				if (enabled) {
					mediaRouter.addDLNAProvider();
				} else {
					mediaRouter.removeDLNAProvider();
				}
			}
		}

		scheduleBackup();
	}

	@Override
	protected void onInitPreferences(PreferenceScreen preferenceScreen) {
		this.setTitle(preferenceScreen.getTitle());

		internalSSID = Util.getSSID(context);
		if (internalSSID == null) {
			internalSSID = "";
		}
		internalSSIDDisplay = context.getResources().getString(R.string.settings_server_local_network_ssid_hint, internalSSID);

		theme = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_THEME);
		maxBitrateWifi = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI);
		maxBitrateMobile = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE);
		maxVideoBitrateWifi = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI);
		maxVideoBitrateMobile = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE);
		networkTimeout = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT);
		cacheLocation = (CacheLocationPreference) this.findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
		preloadCountWifi = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT_WIFI);
		preloadCountMobile = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT_MOBILE);
		keepPlayedCount = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_KEEP_PLAYED_CNT);
		tempLoss = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_TEMP_LOSS);
		pauseDisconnect = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_PAUSE_DISCONNECT);
		serversCategory = (PreferenceCategory) this.findPreference(Constants.PREFERENCES_KEY_SERVER_KEY);
		addServerPreference = this.findPreference(Constants.PREFERENCES_KEY_SERVER_ADD);
		videoPlayer = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_VIDEO_PLAYER);
		songPressAction = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_SONG_PRESS_ACTION);
		syncInterval = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_INTERVAL);
		syncEnabled = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_ENABLED);
		syncWifi = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_WIFI);
		syncNotification = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_NOTIFICATION);
		syncStarred = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_STARRED);
		syncMostRecent = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_SYNC_MOST_RECENT);
		replayGain = (CheckBoxPreference) this.findPreference(Constants.PREFERENCES_KEY_REPLAY_GAIN);
		replayGainType = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_REPLAY_GAIN_TYPE);
		replayGainBump = this.findPreference(Constants.PREFERENCES_KEY_REPLAY_GAIN_BUMP);
		replayGainUntagged = this.findPreference(Constants.PREFERENCES_KEY_REPLAY_GAIN_UNTAGGED);
		cacheSize = (EditTextPreference) this.findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
		openToTab = (ListPreference) this.findPreference(Constants.PREFERENCES_KEY_OPEN_TO_TAB);

		settings = Util.getPreferences(context);
		serverCount = settings.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 1);

		if(cacheSize != null) {
			this.findPreference("clearCache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Util.confirmDialog(context, R.string.common_delete, R.string.common_confirm_message_cache, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new LoadingTask<Void>(context, false) {
								@Override
								protected Void doInBackground() throws Throwable {
									FileUtil.deleteMusicDirectory(context);
									FileUtil.deleteSerializedCache(context);
									FileUtil.deleteArtworkCache(context);
									FileUtil.deleteAvatarCache(context);
									return null;
								}

								@Override
								protected void done(Void result) {
									Util.toast(context, R.string.settings_cache_clear_complete);
								}

								@Override
								protected void error(Throwable error) {
									Util.toast(context, getErrorMessage(error), false);
								}
							}.execute();
						}
					});
					return false;
				}
			});
		}

		if(syncEnabled != null) {
			this.findPreference(Constants.PREFERENCES_KEY_SYNC_ENABLED).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean syncEnabled = (Boolean) newValue;

					Account account = new Account(Constants.SYNC_ACCOUNT_NAME, Constants.SYNC_ACCOUNT_TYPE);
					ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_PLAYLIST_AUTHORITY, syncEnabled);
					ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_PODCAST_AUTHORITY, syncEnabled);

					return true;
				}
			});
			syncInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Integer syncInterval = Integer.parseInt(((String) newValue));

					Account account = new Account(Constants.SYNC_ACCOUNT_NAME, Constants.SYNC_ACCOUNT_TYPE);
					ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_PLAYLIST_AUTHORITY, new Bundle(), 60L * syncInterval);
					ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_PODCAST_AUTHORITY, new Bundle(), 60L * syncInterval);

					return true;
				}
			});
		}

		if(serversCategory != null) {
			addServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					serverCount++;
					int instance = serverCount;
					serversCategory.addPreference(addServer(serverCount));

					SharedPreferences.Editor editor = settings.edit();
					editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, serverCount);
					// Reset set folder ID
					editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, null);
					editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + instance, "http://yourhost");
					editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, getResources().getString(R.string.settings_server_unused));
					editor.commit();

					ServerSettings ss = new ServerSettings(instance);
					serverSettings.put(String.valueOf(instance), ss);
					ss.update();

					return true;
				}
			});

			serversCategory.setOrderingAsAdded(false);
			for (int i = 1; i <= serverCount; i++) {
				serversCategory.addPreference(addServer(i));
				serverSettings.put(String.valueOf(i), new ServerSettings(i));
			}
		}

		SharedPreferences prefs = Util.getPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);

		update();
	}

	private void scheduleBackup() {
		try {
			Class managerClass = Class.forName("android.app.backup.BackupManager");
			Constructor managerConstructor = managerClass.getConstructor(Context.class);
			Object manager = managerConstructor.newInstance(context);
			Method m = managerClass.getMethod("dataChanged");
			m.invoke(manager);
		} catch(ClassNotFoundException e) {
			Log.e(TAG, "No backup manager found");
		} catch(Throwable t) {
			Log.e(TAG, "Scheduling backup failed " + t);
			t.printStackTrace();
		}
	}

	private void update() {
		if (testingConnection) {
			return;
		}

		if(theme != null) {
			theme.setSummary(theme.getEntry());
		}
		if(openToTab != null) {
			openToTab.setSummary(openToTab.getEntry());
		}

		if(cacheSize != null) {
			maxBitrateWifi.setSummary(maxBitrateWifi.getEntry());
			maxBitrateMobile.setSummary(maxBitrateMobile.getEntry());
			maxVideoBitrateWifi.setSummary(maxVideoBitrateWifi.getEntry());
			maxVideoBitrateMobile.setSummary(maxVideoBitrateMobile.getEntry());
			networkTimeout.setSummary(networkTimeout.getEntry());
			cacheLocation.setSummary(cacheLocation.getText());
			preloadCountWifi.setSummary(preloadCountWifi.getEntry());
			preloadCountMobile.setSummary(preloadCountMobile.getEntry());

			try {
				if(megabyteFromat == null) {
					megabyteFromat = new DecimalFormat(getResources().getString(R.string.util_bytes_format_megabyte));
				}

				cacheSize.setSummary(megabyteFromat.format((double) Integer.parseInt(cacheSize.getText())).replace(".00", ""));
			} catch(Exception e) {
				Log.e(TAG, "Failed to format cache size", e);
				cacheSize.setSummary(cacheSize.getText());
			}
		}

		if(keepPlayedCount != null) {
			keepPlayedCount.setSummary(keepPlayedCount.getEntry());
			tempLoss.setSummary(tempLoss.getEntry());
			pauseDisconnect.setSummary(pauseDisconnect.getEntry());
			songPressAction.setSummary(songPressAction.getEntry());
			videoPlayer.setSummary(videoPlayer.getEntry());

			if(replayGain.isChecked()) {
				replayGainType.setEnabled(true);
				replayGainBump.setEnabled(true);
				replayGainUntagged.setEnabled(true);
			} else {
				replayGainType.setEnabled(false);
				replayGainBump.setEnabled(false);
				replayGainUntagged.setEnabled(false);
			}
			replayGainType.setSummary(replayGainType.getEntry());
		}

		if(syncEnabled != null) {
			syncInterval.setSummary(syncInterval.getEntry());

			if(syncEnabled.isChecked()) {
				if(!syncInterval.isEnabled()) {
					syncInterval.setEnabled(true);
					syncWifi.setEnabled(true);
					syncNotification.setEnabled(true);
					syncStarred.setEnabled(true);
					syncMostRecent.setEnabled(true);
				}
			} else {
				if(syncInterval.isEnabled()) {
					syncInterval.setEnabled(false);
					syncWifi.setEnabled(false);
					syncNotification.setEnabled(false);
					syncStarred.setEnabled(false);
					syncMostRecent.setEnabled(false);
				}
			}
		}

		for (ServerSettings ss : serverSettings.values()) {
			ss.update();
		}
	}
	public void checkForRemoved() {
		for (ServerSettings ss : serverSettings.values()) {
			if(!ss.update()) {
				serversCategory.removePreference(ss.getScreen());
				serverCount--;
			}
		}
	}

	private PreferenceScreen addServer(final int instance) {
		final PreferenceScreen screen = this.getPreferenceManager().createPreferenceScreen(context);
		screen.setKey(Constants.PREFERENCES_KEY_SERVER_KEY + instance);
		screen.setOrder(instance);

		screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SettingsFragment newFragment = new SettingsFragment();

				Bundle args = new Bundle();
				args.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, instance);
				newFragment.setArguments(args);

				replaceFragment(newFragment);
				return false;
			}
		});

		return screen;
	}

	private PreferenceScreen expandServer(final int instance) {
		final PreferenceScreen screen = this.getPreferenceManager().createPreferenceScreen(context);
		screen.setTitle(R.string.settings_server_unused);
		screen.setKey(Constants.PREFERENCES_KEY_SERVER_KEY + instance);

		final EditTextPreference serverNamePreference = new EditTextPreference(context);
		serverNamePreference.setKey(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
		serverNamePreference.setDefaultValue(getResources().getString(R.string.settings_server_unused));
		serverNamePreference.setTitle(R.string.settings_server_name);
		serverNamePreference.setDialogTitle(R.string.settings_server_name);

		if (serverNamePreference.getText() == null) {
			serverNamePreference.setText(getResources().getString(R.string.settings_server_unused));
		}

		serverNamePreference.setSummary(serverNamePreference.getText());

		final EditTextPreference serverUrlPreference = new EditTextPreference(context);
		serverUrlPreference.setKey(Constants.PREFERENCES_KEY_SERVER_URL + instance);
		serverUrlPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		serverUrlPreference.setDefaultValue("http://yourhost");
		serverUrlPreference.setTitle(R.string.settings_server_address);
		serverUrlPreference.setDialogTitle(R.string.settings_server_address);

		if (serverUrlPreference.getText() == null) {
			serverUrlPreference.setText("http://yourhost");
		}

		serverUrlPreference.setSummary(serverUrlPreference.getText());
		screen.setSummary(serverUrlPreference.getText());

		final EditTextPreference serverLocalNetworkSSIDPreference = new EditTextPreference(context) {
			@Override
			protected void onAddEditTextToDialogView(View dialogView, final EditText editText) {
				super.onAddEditTextToDialogView(dialogView, editText);
				ViewGroup root = (ViewGroup) ((ViewGroup) dialogView).getChildAt(0);

				Button defaultButton = new Button(getContext());
				defaultButton.setText(internalSSIDDisplay);
				defaultButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						editText.setText(internalSSID);
					}
				});
				root.addView(defaultButton);
			}
		};
		serverLocalNetworkSSIDPreference.setKey(Constants.PREFERENCES_KEY_SERVER_LOCAL_NETWORK_SSID + instance);
		serverLocalNetworkSSIDPreference.setTitle(R.string.settings_server_local_network_ssid);
		serverLocalNetworkSSIDPreference.setDialogTitle(R.string.settings_server_local_network_ssid);

		final EditTextPreference serverInternalUrlPreference = new EditTextPreference(context);
		serverInternalUrlPreference.setKey(Constants.PREFERENCES_KEY_SERVER_INTERNAL_URL + instance);
		serverInternalUrlPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		serverInternalUrlPreference.setDefaultValue("");
		serverInternalUrlPreference.setTitle(R.string.settings_server_internal_address);
		serverInternalUrlPreference.setDialogTitle(R.string.settings_server_internal_address);
		serverInternalUrlPreference.setSummary(serverInternalUrlPreference.getText());

		final EditTextPreference serverUsernamePreference = new EditTextPreference(context);
		serverUsernamePreference.setKey(Constants.PREFERENCES_KEY_USERNAME + instance);
		serverUsernamePreference.setTitle(R.string.settings_server_username);
		serverUsernamePreference.setDialogTitle(R.string.settings_server_username);

		final EditTextPreference serverPasswordPreference = new EditPasswordPreference(context, instance);
		serverPasswordPreference.setKey(Constants.PREFERENCES_KEY_PASSWORD + instance);
		serverPasswordPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		serverPasswordPreference.setSummary("***");
		serverPasswordPreference.setTitle(R.string.settings_server_password);

		final CheckBoxPreference serverTagPreference = new CheckBoxPreference(context);
		serverTagPreference.setKey(Constants.PREFERENCES_KEY_BROWSE_TAGS + instance);
		serverTagPreference.setChecked(Util.isTagBrowsing(context, instance));
		serverTagPreference.setSummary(R.string.settings_browse_by_tags_summary);
		serverTagPreference.setTitle(R.string.settings_browse_by_tags);
		serverPasswordPreference.setDialogTitle(R.string.settings_server_password);

		final CheckBoxPreference serverSyncPreference = new CheckBoxPreference(context);
		serverSyncPreference.setKey(Constants.PREFERENCES_KEY_SERVER_SYNC + instance);
		serverSyncPreference.setChecked(Util.isSyncEnabled(context, instance));
		serverSyncPreference.setSummary(R.string.settings_server_sync_summary);
		serverSyncPreference.setTitle(R.string.settings_server_sync);

		final Preference serverOpenBrowser = new Preference(context);
		serverOpenBrowser.setKey(Constants.PREFERENCES_KEY_OPEN_BROWSER);
		serverOpenBrowser.setPersistent(false);
		serverOpenBrowser.setTitle(R.string.settings_server_open_browser);
		serverOpenBrowser.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				openInBrowser(instance);
				return true;
			}
		});

		Preference serverRemoveServerPreference = new Preference(context);
		serverRemoveServerPreference.setKey(Constants.PREFERENCES_KEY_SERVER_REMOVE + instance);
		serverRemoveServerPreference.setPersistent(false);
		serverRemoveServerPreference.setTitle(R.string.settings_servers_remove);

		serverRemoveServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Util.confirmDialog(context, R.string.common_delete, screen.getTitle().toString(), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Reset values to null so when we ask for them again they are new
						serverNamePreference.setText(null);
						serverUrlPreference.setText(null);
						serverUsernamePreference.setText(null);
						serverPasswordPreference.setText(null);

						// Don't use Util.getActiveServer since it is 0 if offline
						int activeServer = Util.getPreferences(context).getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
						for (int i = instance; i <= serverCount; i++) {
							Util.removeInstanceName(context, i, activeServer);
						}

						serverCount--;
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, serverCount);
						editor.commit();

						removeCurrent();

						SubsonicFragment parentFragment = context.getCurrentFragment();
						if(parentFragment instanceof SettingsFragment) {
							SettingsFragment serverSelectionFragment = (SettingsFragment) parentFragment;
							serverSelectionFragment.checkForRemoved();
						}
					}
				});

				return true;
			}
		});

		Preference serverTestConnectionPreference = new Preference(context);
		serverTestConnectionPreference.setKey(Constants.PREFERENCES_KEY_TEST_CONNECTION + instance);
		serverTestConnectionPreference.setPersistent(false);
		serverTestConnectionPreference.setTitle(R.string.settings_test_connection_title);
		serverTestConnectionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				testConnection(instance);
				return false;
			}
		});

		screen.addPreference(serverNamePreference);
		screen.addPreference(serverUrlPreference);
		screen.addPreference(serverInternalUrlPreference);
		screen.addPreference(serverLocalNetworkSSIDPreference);
		screen.addPreference(serverUsernamePreference);
		screen.addPreference(serverPasswordPreference);
		screen.addPreference(serverTagPreference);
		screen.addPreference(serverSyncPreference);
		screen.addPreference(serverTestConnectionPreference);
		screen.addPreference(serverOpenBrowser);
		screen.addPreference(serverRemoveServerPreference);

		return screen;
	}

	private void setHideMedia(boolean hide) {
		File nomediaDir = new File(FileUtil.getSubsonicDirectory(context), ".nomedia");
		File musicNoMedia = new File(FileUtil.getMusicDirectory(context), ".nomedia");
		if (hide && !nomediaDir.exists()) {
			try {
				if (!nomediaDir.createNewFile()) {
					Log.w(TAG, "Failed to create " + nomediaDir);
				}
			} catch(Exception e) {
				Log.w(TAG, "Failed to create " + nomediaDir, e);
			}

			try {
				if(!musicNoMedia.createNewFile()) {
					Log.w(TAG, "Failed to create " + musicNoMedia);
				}
			} catch(Exception e) {
				Log.w(TAG, "Failed to create " + musicNoMedia, e);
			}
		} else if (!hide && nomediaDir.exists()) {
			if (!nomediaDir.delete()) {
				Log.w(TAG, "Failed to delete " + nomediaDir);
			}
			if(!musicNoMedia.delete()) {
				Log.w(TAG, "Failed to delete " + musicNoMedia);
			}
		}
		Util.toast(context, R.string.settings_hide_media_toast, false);
	}

	private void setMediaButtonsEnabled(boolean enabled) {
		if (enabled) {
			Util.registerMediaButtonEventReceiver(context);
		} else {
			Util.unregisterMediaButtonEventReceiver(context);
		}
	}

	private void setCacheLocation(String path) {
		File dir = new File(path);
		if (!FileUtil.verifyCanWrite(dir)) {
			Util.toast(context, R.string.settings_cache_location_error, false);

			// Reset it to the default.
			String defaultPath = FileUtil.getDefaultMusicDirectory(context).getPath();
			if (!defaultPath.equals(path)) {
				SharedPreferences prefs = Util.getPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, defaultPath);
				editor.commit();

				if(cacheLocation != null) {
					cacheLocation.setSummary(defaultPath);
					cacheLocation.setText(defaultPath);
				}
			}

			// Clear download queue.
			DownloadService downloadService = DownloadService.getInstance();
			downloadService.clear();
		}
	}

	private void testConnection(final int instance) {
		LoadingTask<Boolean> task = new LoadingTask<Boolean>(context) {
			private int previousInstance;

			@Override
			protected Boolean doInBackground() throws Throwable {
				updateProgress(R.string.settings_testing_connection);

				previousInstance = Util.getActiveServer(context);
				testingConnection = true;
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				try {
					musicService.setInstance(instance);
					musicService.ping(context, this);
					return musicService.isLicenseValid(context, null);
				} finally {
					musicService.setInstance(null);
					testingConnection = false;
				}
			}

			@Override
			protected void done(Boolean licenseValid) {
				if (licenseValid) {
					Util.toast(context, R.string.settings_testing_ok);
				} else {
					Util.toast(context, R.string.settings_testing_unlicensed);
				}
			}

			@Override
			public void cancel() {
				super.cancel();
				Util.setActiveServer(context, previousInstance);
			}

			@Override
			protected void error(Throwable error) {
				Log.w(TAG, error.toString(), error);
				new ErrorDialog(context, getResources().getString(R.string.settings_connection_failure) +
						" " + getErrorMessage(error), false);
			}
		};
		task.execute();
	}

	private void openInBrowser(final int instance) {
		SharedPreferences prefs = Util.getPreferences(context);
		String url = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
		if(url == null) {
			new ErrorDialog(context, R.string.settings_invalid_url, false);
			return;
		}
		Uri uriServer = Uri.parse(url);

		Intent browserIntent = new Intent(Intent.ACTION_VIEW, uriServer);
		startActivity(browserIntent);
	}

	private class ServerSettings {
		private int instance;
		private EditTextPreference serverName;
		private EditTextPreference serverUrl;
		private EditTextPreference serverLocalNetworkSSID;
		private EditTextPreference serverInternalUrl;
		private EditTextPreference username;
		private PreferenceScreen screen;

		private ServerSettings(int instance) {
			this.instance = instance;
			screen = (PreferenceScreen) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_SERVER_KEY + instance);
			serverName = (EditTextPreference) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
			serverUrl = (EditTextPreference) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_SERVER_URL + instance);
			serverLocalNetworkSSID = (EditTextPreference) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_SERVER_LOCAL_NETWORK_SSID + instance);
			serverInternalUrl = (EditTextPreference) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_SERVER_INTERNAL_URL + instance);
			username = (EditTextPreference) SettingsFragment.this.findPreference(Constants.PREFERENCES_KEY_USERNAME + instance);

			if(serverName != null) {
				serverUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object value) {
						try {
							String url = (String) value;
							new URL(url);
							if (url.contains(" ") || url.contains("@")) {
								throw new Exception();
							}
						} catch (Exception x) {
							new ErrorDialog(context, R.string.settings_invalid_url, false);
							return false;
						}
						return true;
					}
				});
				serverInternalUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object value) {
						try {
							String url = (String) value;
							// Allow blank internal IP address
							if ("".equals(url) || url == null) {
								return true;
							}

							new URL(url);
							if (url.contains(" ") || url.contains("@")) {
								throw new Exception();
							}
						} catch (Exception x) {
							new ErrorDialog(context, R.string.settings_invalid_url, false);
							return false;
						}
						return true;
					}
				});

				username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object value) {
						String username = (String) value;
						if (username == null || !username.equals(username.trim())) {
							new ErrorDialog(context, R.string.settings_invalid_username, false);
							return false;
						}
						return true;
					}
				});
			}
		}

		public PreferenceScreen getScreen() {
			return screen;
		}

		public boolean update() {
			SharedPreferences prefs = Util.getPreferences(context);

			if(prefs.contains(Constants.PREFERENCES_KEY_SERVER_NAME + instance)) {
				if (serverName != null) {
					serverName.setSummary(serverName.getText());
					serverUrl.setSummary(serverUrl.getText());
					serverLocalNetworkSSID.setSummary(serverLocalNetworkSSID.getText());
					serverInternalUrl.setSummary(serverInternalUrl.getText());
					username.setSummary(username.getText());

					setTitle(serverName.getText());
				}


				String title = prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
				String summary = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);

				if (title != null) {
					screen.setTitle(title);
				} else {
					screen.setTitle(R.string.settings_server_unused);
				}
				if (summary != null) {
					screen.setSummary(summary);
				}

				return true;
			} else {
				return false;
			}
		}
	}
}
