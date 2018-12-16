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

package github.vrih.xsub.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Scott on 8/28/13.
 */

public class MostRecentSyncService extends Service {
	private static MostRecentSyncAdapter mostRecentSyncAdapter;
	private static final Object syncLock = new Object();

	@Override
	public void onCreate() {
		synchronized (syncLock) {
			if(mostRecentSyncAdapter == null) {
				mostRecentSyncAdapter = new MostRecentSyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mostRecentSyncAdapter.getSyncAdapterBinder();

	}
}
