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
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import github.daneren2005.dsub.fragments.DownloadFragment;

import android.widget.EditText;
import android.content.Intent;

import github.daneren2005.dsub.util.Constants;

public class DownloadActivity extends SubsonicActivity {
	private static final String TAG = DownloadActivity.class.getSimpleName();
	private EditText playlistNameView;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);

		if (findViewById(R.id.download_container) != null && savedInstanceState == null) {
			currentFragment = new DownloadFragment();
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW)) {
				Bundle args = new Bundle();
				args.putBoolean(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW, true);
				currentFragment.setArguments(args);
			}
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.download_container, currentFragment, currentFragment.getSupportTag() + "").commit();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if(currentFragment != null) {
			return ((DownloadFragment)currentFragment).getGestureDetector().onTouchEvent(me);
		} else {
			return false;
		}
	}
}
