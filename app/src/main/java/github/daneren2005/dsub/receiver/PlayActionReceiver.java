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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;

public class PlayActionReceiver extends BroadcastReceiver {
	private static final String TAG = PlayActionReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.hasExtra(Constants.TASKER_EXTRA_BUNDLE)) {
			Bundle data = intent.getBundleExtra(Constants.TASKER_EXTRA_BUNDLE);
			Boolean startShuffled = data.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE);

			Intent start = new Intent(context, DownloadService.class);
			start.setAction(DownloadService.START_PLAY);
			start.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, startShuffled);
			start.putExtra(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, data.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR));
			start.putExtra(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, data.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR));
			start.putExtra(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, data.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE));
			start.putExtra(Constants.PREFERENCES_KEY_OFFLINE, data.getInt(Constants.PREFERENCES_KEY_OFFLINE));
			DownloadService.startService(context, start);
		}
	}
}
