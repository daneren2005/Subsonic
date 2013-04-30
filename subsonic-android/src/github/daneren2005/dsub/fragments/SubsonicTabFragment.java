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
package github.daneren2005.dsub.fragments;

import android.util.Log;
import android.view.MenuItem;

public class SubsonicTabFragment extends SubsonicFragment {
	private static final String TAG = SubsonicTabFragment.class.getSimpleName();
	protected boolean primaryFragment = false;
	protected boolean invalidated = false;
	
	public void replaceFragment(SubsonicTabFragment fragment, int id) {
		context.getPagerAdapter().replaceCurrent(fragment, id);
	}
	
	public void setPrimaryFragment(boolean primary) {
		primaryFragment = primary;
		if(primary) {
			if(context != null) {
				context.setTitle(title);
			}
			if(invalidated) {
				invalidated = false;
				refresh(false);
			}
		}
	}
	
	public void invalidate() {
		if(primaryFragment) {
			refresh(false);
		} else {
			invalidated = true;
		}
	}
}
