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
package github.vrih.xsub.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import github.vrih.xsub.R;
import github.vrih.xsub.fragments.PreferenceCompatFragment;
import github.vrih.xsub.fragments.SettingsFragment;
import github.vrih.xsub.util.Constants;

public class SettingsActivity extends SubsonicActivity {
	private static final String TAG = SettingsActivity.class.getSimpleName();
	private PreferenceCompatFragment fragment;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
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

		Toolbar mainToolbar = findViewById(R.id.main_toolbar);
		setSupportActionBar(mainToolbar);
	}
}
