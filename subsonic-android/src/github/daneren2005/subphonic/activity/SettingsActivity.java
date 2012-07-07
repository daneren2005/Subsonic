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
package github.daneren2005.subphonic.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import github.daneren2005.subphonic.R;
import github.daneren2005.subphonic.provider.SearchSuggestionProvider1;
import github.daneren2005.subphonic.service.DownloadService;
import github.daneren2005.subphonic.service.DownloadServiceImpl;
import github.daneren2005.subphonic.service.MusicService;
import github.daneren2005.subphonic.service.MusicServiceFactory;
import github.daneren2005.subphonic.util.Constants;
import github.daneren2005.subphonic.util.ErrorDialog;
import github.daneren2005.subphonic.util.FileUtil;
import github.daneren2005.subphonic.util.ModalBackgroundTask;
import github.daneren2005.subphonic.util.Util;

import java.io.File;
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
    private EditTextPreference cacheSize;
    private EditTextPreference cacheLocation;
    private ListPreference preloadCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        theme = (ListPreference) findPreference(Constants.PREFERENCES_KEY_THEME);
        maxBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI);
        maxBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE);
        cacheSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
        cacheLocation = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
        preloadCount = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT);

        findPreference("testConnection1").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                testConnection(1);
                return false;
            }
        });

        findPreference("testConnection2").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                testConnection(2);
                return false;
            }
        });

        findPreference("testConnection3").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                testConnection(3);
                return false;
            }
        });

        findPreference("clearSearchHistory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SettingsActivity.this, SearchSuggestionProvider1.AUTHORITY, SearchSuggestionProvider1.MODE);
                suggestions.clearHistory();
                Util.toast(SettingsActivity.this, R.string.settings_search_history_cleared);
                return false;
            }
        });

        for (int i = 1; i <= 3; i++) {
            String instance = String.valueOf(i);
            serverSettings.put(instance, new ServerSettings(instance));
        }

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        update();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
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
    }

    private void update() {
        if (testingConnection) {
            return;
        }

        theme.setSummary(theme.getEntry());
        maxBitrateWifi.setSummary(maxBitrateWifi.getEntry());
        maxBitrateMobile.setSummary(maxBitrateMobile.getEntry());
        cacheSize.setSummary(cacheSize.getText());
        cacheLocation.setSummary(cacheLocation.getText());
        preloadCount.setSummary(preloadCount.getEntry());
        for (ServerSettings ss : serverSettings.values()) {
            ss.update();
        }
    }

    private void setHideMedia(boolean hide) {
        File nomediaDir = new File(FileUtil.getSubsonicDirectory(), ".nomedia");
        if (hide && !nomediaDir.exists()) {
            if (!nomediaDir.mkdir()) {
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
        ModalBackgroundTask<Boolean> task = new ModalBackgroundTask<Boolean>(this, false) {
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
                        if (!url.equals(url.trim()) || url.contains("@")) {
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