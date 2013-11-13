/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.provider.DSubSearchProvider;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.view.ErrorDialog;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private final Map<String, ServerSettings> serverSettings = new LinkedHashMap<String, ServerSettings>();
    private boolean testingConnection;
    private ListPreference theme;
    private ListPreference maxBitrateWifi;
    private ListPreference maxBitrateMobile;
	private ListPreference maxVideoBitrateWifi;
    private ListPreference maxVideoBitrateMobile;
	private ListPreference networkTimeout;
    private EditTextPreference cacheSize;
    private EditTextPreference cacheLocation;
    private ListPreference preloadCountWifi;
	private ListPreference preloadCountMobile;
	private EditTextPreference randomSize;
	private ListPreference tempLoss;
	private ListPreference pauseDisconnect;
	private EditTextPreference bufferLength;
	private Preference addServerPreference;
	private PreferenceCategory serversCategory;
	private EditTextPreference chatRefreshRate;
	private ListPreference videoPlayer;
	
	private int serverCount = 3;
	private SharedPreferences settings;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public void onCreate(Bundle savedInstanceState) {
		applyTheme();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        theme = (ListPreference) findPreference(Constants.PREFERENCES_KEY_THEME);
        maxBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI);
        maxBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE);
		maxVideoBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI);
        maxVideoBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE);
		networkTimeout = (ListPreference) findPreference(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT);
        cacheSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
        cacheLocation = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
        preloadCountWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT_WIFI);
		preloadCountMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT_MOBILE);
		randomSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_RANDOM_SIZE);
		tempLoss = (ListPreference) findPreference(Constants.PREFERENCES_KEY_TEMP_LOSS);
		pauseDisconnect = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PAUSE_DISCONNECT);
		bufferLength = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_BUFFER_LENGTH);
		serversCategory = (PreferenceCategory) findPreference(Constants.PREFERENCES_KEY_SERVER_KEY);
		addServerPreference = (Preference) findPreference(Constants.PREFERENCES_KEY_SERVER_ADD);
		chatRefreshRate = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CHAT_REFRESH);
		videoPlayer = (ListPreference) findPreference(Constants.PREFERENCES_KEY_VIDEO_PLAYER);
		
		settings = Util.getPreferences(this);
		serverCount = settings.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 3);

        findPreference("clearSearchHistory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SettingsActivity.this, DSubSearchProvider.AUTHORITY, DSubSearchProvider.MODE);
                suggestions.clearHistory();
                Util.toast(SettingsActivity.this, R.string.settings_search_history_cleared);
                return false;
            }
        });
		findPreference("clearCache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Util.confirmDialog(SettingsActivity.this, R.string.common_delete, "cache", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new LoadingTask<Void>(SettingsActivity.this, false) {
							@Override
							protected Void doInBackground() throws Throwable {
								FileUtil.deleteMusicDirectory(SettingsActivity.this);
								return null;
							}

							@Override
							protected void done(Void result) {
								Util.toast(SettingsActivity.this, R.string.settings_cache_clear_complete);
							}

							@Override
							protected void error(Throwable error) {
								Util.toast(SettingsActivity.this, getErrorMessage(error), false);
							}
						}.execute();
					}
				});
				return false;
			}
		});

		addServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				serverCount++;
				String instance = String.valueOf(serverCount);

				Preference addServerPreference = findPreference(Constants.PREFERENCES_KEY_SERVER_ADD);
				serversCategory.addPreference(addServer(serverCount));

				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, serverCount);
				editor.commit();

				serverSettings.put(instance, new ServerSettings(instance));

				return true;
			}
		});

		serversCategory.setOrderingAsAdded(false);
        for (int i = 1; i <= serverCount; i++) {
            String instance = String.valueOf(i);
			serversCategory.addPreference(addServer(i));
            serverSettings.put(instance, new ServerSettings(instance));
        }

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        update();

		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeButtonEnabled(true);
		}
    }
	
	@Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}

		return false;
	}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference changed: " + key);
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
			DownloadService downloadService = DownloadServiceImpl.getInstance();
			downloadService.setSleepTimerDuration(Integer.parseInt(sharedPreferences.getString(key, "60")));
		}
		
		scheduleBackup();
    }
    
    private void scheduleBackup() {
		try {
			Class managerClass = Class.forName("android.app.backup.BackupManager");
			Constructor managerConstructor = managerClass.getConstructor(Context.class);
			Object manager = managerConstructor.newInstance(this);
			Method m = managerClass.getMethod("dataChanged");
			m.invoke(manager);
			Log.d(TAG, "Backup requested");
		} catch(ClassNotFoundException e) {
			Log.d(TAG, "No backup manager found");
		} catch(Throwable t) {
			Log.d(TAG, "Scheduling backup failed " + t);
			t.printStackTrace();
		}
    }

    private void update() {
        if (testingConnection) {
            return;
        }

        theme.setSummary(theme.getEntry());
        maxBitrateWifi.setSummary(maxBitrateWifi.getEntry());
        maxBitrateMobile.setSummary(maxBitrateMobile.getEntry());
		maxVideoBitrateWifi.setSummary(maxVideoBitrateWifi.getEntry());
        maxVideoBitrateMobile.setSummary(maxVideoBitrateMobile.getEntry());
		networkTimeout.setSummary(networkTimeout.getEntry());
        cacheSize.setSummary(cacheSize.getText());
        cacheLocation.setSummary(cacheLocation.getText());
        preloadCountWifi.setSummary(preloadCountWifi.getEntry());
		preloadCountMobile.setSummary(preloadCountMobile.getEntry());
		randomSize.setSummary(randomSize.getText());
		tempLoss.setSummary(tempLoss.getEntry());
		pauseDisconnect.setSummary(pauseDisconnect.getEntry());
		bufferLength.setSummary(bufferLength.getText() + " seconds");
		chatRefreshRate.setSummary(chatRefreshRate.getText());
		videoPlayer.setSummary(videoPlayer.getEntry());
        for (ServerSettings ss : serverSettings.values()) {
            ss.update();
        }
    }
	
	private PreferenceScreen addServer(final int instance) {
		final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		screen.setTitle(R.string.settings_server_unused);
		screen.setKey(Constants.PREFERENCES_KEY_SERVER_KEY + instance);

		final EditTextPreference serverNamePreference = new EditTextPreference(this);
		serverNamePreference.setKey(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
		serverNamePreference.setDefaultValue(getResources().getString(R.string.settings_server_unused));
		serverNamePreference.setTitle(R.string.settings_server_name);

		if (serverNamePreference.getText() == null) {
			serverNamePreference.setText(getResources().getString(R.string.settings_server_unused));
		}

		serverNamePreference.setSummary(serverNamePreference.getText());

		final EditTextPreference serverUrlPreference = new EditTextPreference(this);
		serverUrlPreference.setKey(Constants.PREFERENCES_KEY_SERVER_URL + instance);
		serverUrlPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		serverUrlPreference.setDefaultValue("http://yourhost");
		serverUrlPreference.setTitle(R.string.settings_server_address);

		if (serverUrlPreference.getText() == null) {
			serverUrlPreference.setText("http://yourhost");
		}

		serverUrlPreference.setSummary(serverUrlPreference.getText());

		screen.setSummary(serverUrlPreference.getText());

		final EditTextPreference serverUsernamePreference = new EditTextPreference(this);
		serverUsernamePreference.setKey(Constants.PREFERENCES_KEY_USERNAME + instance);
		serverUsernamePreference.setTitle(R.string.settings_server_username);

		final EditTextPreference serverPasswordPreference = new EditTextPreference(this);
		serverPasswordPreference.setKey(Constants.PREFERENCES_KEY_PASSWORD + instance);
		serverPasswordPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		serverPasswordPreference.setSummary("***");
		serverPasswordPreference.setTitle(R.string.settings_server_password);
		
		final Preference serverOpenBrowser = new Preference(this);
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

		Preference serverRemoveServerPreference = new Preference(this);
		serverRemoveServerPreference.setKey(Constants.PREFERENCES_KEY_SERVER_REMOVE + instance);
		serverRemoveServerPreference.setPersistent(false);
		serverRemoveServerPreference.setTitle(R.string.settings_servers_remove);

		serverRemoveServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Util.confirmDialog(SettingsActivity.this, R.string.common_delete, screen.getTitle().toString(), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Reset values to null so when we ask for them again they are new
						serverNamePreference.setText(null);
						serverUrlPreference.setText(null);
						serverUsernamePreference.setText(null);
						serverPasswordPreference.setText(null);

						int activeServer = Util.getActiveServer(SettingsActivity.this);
						for (int i = instance; i <= serverCount; i++) {
							Util.removeInstanceName(SettingsActivity.this, i, activeServer);
						}

						serverCount--;
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, serverCount);
						editor.commit();

						serversCategory.removePreference(screen);
						screen.getDialog().dismiss();
					}
				});
				
				return true;
			}
		});

		Preference serverTestConnectionPreference = new Preference(this);
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
		screen.addPreference(serverUsernamePreference);
		screen.addPreference(serverPasswordPreference);
		screen.addPreference(serverRemoveServerPreference);
		screen.addPreference(serverTestConnectionPreference);
		screen.addPreference(serverOpenBrowser);

		screen.setOrder(instance);

		return screen;
	}
	
	private void applyTheme() {
		String activeTheme = Util.getTheme(this);
		if ("dark".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Dark);
		} else if ("black".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Black);
		} else if ("light".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Light);
		} else if ("dark_fullscreen".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Dark_Fullscreen);
		} else if ("black_fullscreen".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Black_Fullscreen);
		} else if ("light_fullscreen".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Light_Fullscreen);
		} else if("holo".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Holo);
		} else if("holo_fullscreen".equals(activeTheme)) {
			setTheme(R.style.Theme_DSub_Holo_Fullscreen);
		}else {
			setTheme(R.style.Theme_DSub_Holo);
		}
	}

    private void setHideMedia(boolean hide) {
        File nomediaDir = new File(FileUtil.getSubsonicDirectory(), ".nomedia");
        if (hide && !nomediaDir.exists()) {
			try {
				if (!nomediaDir.createNewFile()) {
					Log.w(TAG, "Failed to create " + nomediaDir);
				}
			} catch(Exception e) {
				Log.w(TAG, "Failed to create " + nomediaDir);
			}
        } else if (nomediaDir.exists()) {
            if (!nomediaDir.delete()) {
                Log.w(TAG, "Failed to delete " + nomediaDir);
            }
        }
        Util.toast(this, R.string.settings_hide_media_toast, false);
    }

    private void setMediaButtonsEnabled(boolean enabled) {
        if (enabled) {
            Util.registerMediaButtonEventReceiver(this);
        } else {
            Util.unregisterMediaButtonEventReceiver(this);
        }
    }

    private void setCacheLocation(String path) {
        File dir = new File(path);
        if (!FileUtil.ensureDirectoryExistsAndIsReadWritable(dir)) {
            Util.toast(this, R.string.settings_cache_location_error, false);

            // Reset it to the default.
            String defaultPath = FileUtil.getDefaultMusicDirectory().getPath();
            if (!defaultPath.equals(path)) {
                SharedPreferences prefs = Util.getPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, defaultPath);
                editor.commit();
                cacheLocation.setSummary(defaultPath);
                cacheLocation.setText(defaultPath);
            }

            // Clear download queue.
            DownloadService downloadService = DownloadServiceImpl.getInstance();
            downloadService.clear();
        }
    }

    private void testConnection(final int instance) {
		LoadingTask<Boolean> task = new LoadingTask<Boolean>(this) {
            private int previousInstance;

            @Override
            protected Boolean doInBackground() throws Throwable {
                updateProgress(R.string.settings_testing_connection);

                previousInstance = Util.getActiveServer(SettingsActivity.this);
                testingConnection = true;
                Util.setActiveServer(SettingsActivity.this, instance);
                try {
                    MusicService musicService = MusicServiceFactory.getMusicService(SettingsActivity.this);
                    musicService.ping(SettingsActivity.this, this);
                    return musicService.isLicenseValid(SettingsActivity.this, null);
                } finally {
                    Util.setActiveServer(SettingsActivity.this, previousInstance);
                    testingConnection = false;
                }
            }

            @Override
            protected void done(Boolean licenseValid) {
                if (licenseValid) {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_ok);
                } else {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_unlicensed);
                }
            }

            @Override
            protected void cancel() {
                super.cancel();
                Util.setActiveServer(SettingsActivity.this, previousInstance);
            }

            @Override
            protected void error(Throwable error) {
                Log.w(TAG, error.toString(), error);
                new ErrorDialog(SettingsActivity.this, getResources().getString(R.string.settings_connection_failure) +
                        " " + getErrorMessage(error), false);
            }
        };
        task.execute();
    }
	
	private void openInBrowser(final int instance) {
    	SharedPreferences prefs = Util.getPreferences(this);  
    	String url = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
    	Uri uriServer = Uri.parse(url);
    	
    	Intent browserIntent = new Intent(Intent.ACTION_VIEW, uriServer);            	
    	startActivity(browserIntent);
    }

    private class ServerSettings {
        private EditTextPreference serverName;
        private EditTextPreference serverUrl;
        private EditTextPreference username;
        private PreferenceScreen screen;

        private ServerSettings(String instance) {

            screen = (PreferenceScreen) findPreference("server" + instance);
            serverName = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
            serverUrl = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_URL + instance);
            username = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_USERNAME + instance);

            serverUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    try {
                        String url = (String) value;
                        new URL(url);
                        if (url.contains(" ") || url.contains("@") || url.contains("_")) {
                            throw new Exception();
                        }
                    } catch (Exception x) {
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_url, false);
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
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_username, false);
                        return false;
                    }
                    return true;
                }
            });
        }

        public void update() {
            serverName.setSummary(serverName.getText());
            serverUrl.setSummary(serverUrl.getText());
            username.setSummary(username.getText());
            screen.setSummary(serverUrl.getText());
            screen.setTitle(serverName.getText());
        }
    }
}
