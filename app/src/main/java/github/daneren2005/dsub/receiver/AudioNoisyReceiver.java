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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

public class AudioNoisyReceiver extends BroadcastReceiver {
	private static final String TAG = AudioNoisyReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		DownloadService downloadService = DownloadService.getInstance();
		// Don't do anything if downloadService is not started
		if(downloadService == null) {
			return;
		}

		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals (intent.getAction ())) {
				if(!downloadService.isRemoteEnabled()  && (downloadService.getPlayerState() == PlayerState.STARTED || downloadService.getPlayerState() == PlayerState.PAUSED_TEMP)) {
					SharedPreferences prefs = Util.getPreferences(downloadService);
					int pausePref = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_PAUSE_DISCONNECT, "0"));
					if(pausePref == 0) {
						downloadService.pause();
					}
				}
		}
	}
}
