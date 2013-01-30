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
package github.daneren2005.dsub.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott
 */
public class Updater {
	protected String TAG = Updater.class.getSimpleName();
	protected int version;
	protected Context context;
	
	public Updater(int version) {
		this.version = version;
	}
	
	public void checkUpdates(Context context) {
		this.context = context;
		List<Updater> updaters = new ArrayList<Updater>();
		
		SharedPreferences prefs = Util.getPreferences(context);
		int lastVersion = prefs.getInt(Constants.LAST_VERSION, 372);
		if(version > lastVersion) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.LAST_VERSION, version);
			
			Log.i(TAG, "Updating from version " + lastVersion + " to " + version);
			for(Updater updater: updaters) {
				if(updater.shouldUpdate(lastVersion)) {
					new BackgroundUpdate().execute(updater);
				}
			}
		}
	}
	
	private class BackgroundUpdate extends AsyncTask<Updater, Void, Void> {
		@Override
		protected Void doInBackground(Updater... params) {
			params[0].update(context);
			return null;
		}
	}
	
	public boolean shouldUpdate(int version) {
		return this.version > version;
	}
	public void update(Context context) {
		
	}
}
