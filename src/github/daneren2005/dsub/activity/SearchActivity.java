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

import github.daneren2005.dsub.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import github.daneren2005.dsub.fragments.SearchFragment;
import github.daneren2005.dsub.util.Constants;

public class SearchActivity extends SubsonicActivity {
	private static final String TAG = SearchActivity.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);

		if (findViewById(R.id.download_container) != null && savedInstanceState == null) {
			currentFragment = new SearchFragment();
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.download_container, currentFragment, currentFragment.getSupportTag() + "").commit();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		if(currentFragment != null && currentFragment instanceof SearchFragment) {
			String query = intent.getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY);
			boolean autoplay = intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
			boolean requestsearch = intent.getBooleanExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, false);

			if (query != null) {
				((SearchFragment)currentFragment).search(query, autoplay);
			} else {
				((SearchFragment)currentFragment).populateList();
				if (requestsearch) {
					onSearchRequested();
				}
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			Intent i = new Intent();
			i.setClass(this, SubsonicFragmentActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void onSupportNewIntent(Intent intent) {
		onNewIntent(intent);
	}
	
	@Override
	public void onBackPressed() {
		if(onBackPressedSupport()) {
			super.onBackPressed();
		}
	}
}