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
import github.daneren2005.dsub.fragments.SearchFragment;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.MergeAdapter;

public class SearchActivity extends SubsonicActivity {
	SearchFragment fragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);

		if (findViewById(R.id.download_container) != null && savedInstanceState == null) {
			fragment = new SearchFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.download_container, fragment).commit();
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String query = intent.getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY);
		boolean autoplay = intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
		boolean requestsearch = intent.getBooleanExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, false);

		if (query != null) {
			fragment.search(query, autoplay);
		} else {
			fragment.populateList();
			if (requestsearch) {
				onSearchRequested();
			}
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