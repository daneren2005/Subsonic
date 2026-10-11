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
package github.paroj.dsub2000.activity;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.appcompat.widget.Toolbar;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.fragments.PreferenceCompatFragment;
import github.paroj.dsub2000.fragments.SettingsFragment;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Util;

public class SettingsActivity extends SubsonicActivity {
	private static final String TAG = SettingsActivity.class.getSimpleName();
	private PreferenceCompatFragment fragment;
    private OnBackInvokedCallback backCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		lastSelectedPosition = R.id.drawer_settings;
		setContentView(R.layout.settings_activity);

		if (savedInstanceState == null) {
			fragment = new SettingsFragment();
			Bundle args = new Bundle();
			args.putInt(Constants.INTENT_EXTRA_FRAGMENT_TYPE, R.xml.settings);

			fragment.setArguments(args);
			fragment.setRetainInstance(true);

			currentFragment = fragment;
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currentFragment, currentFragment.getSupportTag() + "").commit();
		}

		Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        SharedPreferences prefs = Util.getPreferences(this);
        if (prefs.getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true)) {
            int color = prefs.getInt(Constants.PREFERENCES_KEY_ACTION_BAR_COLOR, -1);
            if (color != -1) {
                mainToolbar.setBackgroundColor(color);
            }
        }
        setSupportActionBar(mainToolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    onBackPressed();
                }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    backCallback
            );
        }
	}

    @Override
    public void onBackPressed() {
        if (!backStack.isEmpty()) {
            removeCurrent();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        super.onDestroy();
    }
}
