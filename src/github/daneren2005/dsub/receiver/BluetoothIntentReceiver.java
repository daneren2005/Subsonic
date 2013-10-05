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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

/**
 * Request media button focus when connected to Bluetooth A2DP.
 *
 * @author Sindre Mehus
 */
public class BluetoothIntentReceiver extends BroadcastReceiver {
	private static final String TAG = BluetoothIntentReceiver.class.getSimpleName();
	// Same as constants in android.bluetooth.BluetoothProfile, which is API level 11.
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTED = 2;
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "GOT INTENT " + intent);
		if (isConnected(intent)) {
			Log.i(TAG, "Connected to Bluetooth A2DP, requesting media button focus.");
			Util.registerMediaButtonEventReceiver(context);
		} else if (isDisconnected(intent)) {
			Log.i(TAG, "Disconnected from Bluetooth A2DP, requesting pause.");
			SharedPreferences prefs = Util.getPreferences(context);
			int pausePref = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_PAUSE_DISCONNECT, "0"));
			if(pausePref == 0 || pausePref == 2) {
				context.sendBroadcast(new Intent(DownloadServiceImpl.CMD_PAUSE));
			}
		}
	}
	private boolean isConnected(Intent intent) {
		if ("android.bluetooth.a2dp.action.SINK_STATE_CHANGED".equals(intent.getAction()) &&
			intent.getIntExtra("android.bluetooth.a2dp.extra.SINK_STATE", -1) == STATE_CONNECTED) {
			return true;
		}
		else if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction()) &&
			intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1) == STATE_CONNECTED) {
			return true;
		}
		else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
			return true;
		}
		return false;
	}
	private boolean isDisconnected(Intent intent) {
		if ("android.bluetooth.a2dp.action.SINK_STATE_CHANGED".equals(intent.getAction()) &&
			intent.getIntExtra("android.bluetooth.a2dp.extra.SINK_STATE", -1) == STATE_DISCONNECTED) {
			return true;
		}
		else if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction()) &&
			intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1) == STATE_DISCONNECTED) {
			return true;
		}
		else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
			return true;
		}
		return false;
	}
} 
