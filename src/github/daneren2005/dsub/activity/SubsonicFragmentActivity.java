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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.ChatFragment;
import github.daneren2005.dsub.fragments.SelectPodcastsFragment;
import github.daneren2005.dsub.util.Constants;

/**
 * Created by Scott on 10/14/13.
 */
public class SubsonicFragmentActivity extends SubsonicActivity {
	private static String TAG = SubsonicFragmentActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);

		if (findViewById(R.id.download_container) != null && savedInstanceState == null) {
			String fragmentType = getIntent().getStringExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE);
			if("Chat".equals(fragmentType)) {
				currentFragment = new ChatFragment();
			} else if("Podcast".equals(fragmentType)) {
				currentFragment = new SelectPodcastsFragment();
			} else {
				finish();
				return;
			}
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.download_container, currentFragment, currentFragment.getSupportTag() + "").commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			startActivity(MainActivity.class);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if(onBackPressedSupport()) {
			super.onBackPressed();
		}
	}
}
