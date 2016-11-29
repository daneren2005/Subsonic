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
	Copyright 2016 (C) Scott Jackson
*/

package github.daneren2005.dsub.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

public class UpdaterNoDLNA extends Updater {
	public UpdaterNoDLNA() {
		super(534);
		TAG = this.getClass().getSimpleName();
	}

	@Override
	public void update(Context context) {
		SharedPreferences prefs = Util.getPreferences(context);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCES_KEY_DLNA_CASTING_ENABLED, false);
			editor.commit();
		}
	}
}
