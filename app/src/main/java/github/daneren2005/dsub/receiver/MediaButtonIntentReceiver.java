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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import github.daneren2005.dsub.service.DownloadService;

/**
 * @author Sindre Mehus
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final String TAG = MediaButtonIntentReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
		if(DownloadService.getInstance() == null && (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_STOP ||
			event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK)) {
			Log.w(TAG, "Ignore keycode event because downloadService is off");
			return;
		}
        Log.i(TAG, "Got MEDIA_BUTTON key event: " + event);

        Intent serviceIntent = new Intent(context, DownloadService.class);
        serviceIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        DownloadService.startService(context, serviceIntent);
        if (isOrderedBroadcast()) {
			try {
				abortBroadcast();
			} catch (Exception x) {
				// Ignored.
			}
		}
    }
}
