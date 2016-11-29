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
import android.util.Log;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.SilentBackgroundTask;
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
		// 5.2 should show as 520 instead of 52
		if(version < 100) {
			version *= 10;
		}
		this.version = version;
	}
	
	public void checkUpdates(Context context) {
		this.context = context;
		List<Updater> updaters = new ArrayList<Updater>();
		updaters.add(new UpdaterSongPress());
		updaters.add(new UpdaterNoDLNA());
		
		SharedPreferences prefs = Util.getPreferences(context);
		int lastVersion = prefs.getInt(Constants.LAST_VERSION, 0);
		if(lastVersion == 0) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.LAST_VERSION, version);
			editor.commit();
		}
		else if(version > lastVersion) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.LAST_VERSION, version);
			editor.commit();
			
			Log.i(TAG, "Updating from version " + lastVersion + " to " + version);
			for(Updater updater: updaters) {
				if(updater.shouldUpdate(lastVersion)) {
					new BackgroundUpdate(context, updater).execute();
				}
			}
		}
	}
	
	public String getName() {
		return this.TAG;
	}
	
	private class BackgroundUpdate extends SilentBackgroundTask<Void> {
		private final Updater updater;

		public BackgroundUpdate(Context context, Updater updater) {
			super(context);
			this.updater = updater;
		}

		@Override
		protected Void doInBackground() {
			try {
				updater.update(context);
			} catch(Exception e) {
				Log.w(TAG, "Failed to run update for " + updater.getName());
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
