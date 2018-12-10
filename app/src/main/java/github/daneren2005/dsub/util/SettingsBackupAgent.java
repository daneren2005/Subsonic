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
package github.daneren2005.dsub.util;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class SettingsBackupAgent extends BackupAgentHelper {
	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, Constants.PREFERENCES_FILE_NAME);
		addHelper("mypreferences", helper);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException{
		super.onRestore(data, appVersionCode, newState);

		SharedPreferences.Editor editor = Util.getPreferences(this).edit();
		editor.remove(Constants.PREFERENCES_KEY_CACHE_LOCATION);
		editor.remove(Constants.CACHE_AUDIO_SESSION_ID);
		editor.apply();
	}
 }
