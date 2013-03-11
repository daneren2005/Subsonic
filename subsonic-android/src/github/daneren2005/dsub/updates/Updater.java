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
		updaters.add(new Updater373());
		
		SharedPreferences prefs = Util.getPreferences(context);
		int lastVersion = prefs.getInt(Constants.LAST_VERSION, 372);
		if(version > lastVersion) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.LAST_VERSION, version);
			editor.commit();
			
			Log.i(TAG, "Updating from version " + lastVersion + " to " + version);
			for(Updater updater: updaters) {
				if(updater.shouldUpdate(lastVersion)) {
					new BackgroundUpdate().execute(updater);
				}
			}
		}
	}
	
	public String getName() {
		return this.TAG;
	}
	
	private class BackgroundUpdate extends AsyncTask<Updater, Void, Void> {
		@Override
		protected Void doInBackground(Updater... params) {
			try {
				params[0].update(context);
			} catch(Exception e) {
				Log.w(TAG, "Failed to run update for " + params[0].getName());
			}
			return null;
		}
	}
	
	public boolean shouldUpdate(int version) {
		return this.version > version;
	}
	public void update(Context context) {
		
	}
}
