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
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Util;

public class HeadphonePlugReceiver extends BroadcastReceiver {
	private static final String TAG = HeadphonePlugReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
			int headphoneState = intent.getIntExtra("state", -1);
			if(headphoneState == 1 && Util.shouldStartOnHeadphones(context)) {
				Intent start = new Intent(context, DownloadService.class);
				start.setAction(DownloadService.START_PLAY);
				DownloadService.startService(context, start);
			}
		}
	}
}
