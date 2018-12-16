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

package github.vrih.xsub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import github.vrih.xsub.service.HeadphoneListenerService;
import github.vrih.xsub.util.Util;

public class BootReceiver  extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(Util.shouldStartOnHeadphones(context)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setClassName(context.getPackageName(), HeadphoneListenerService.class.getName());
			context.startService(serviceIntent);
		}
	}
}
