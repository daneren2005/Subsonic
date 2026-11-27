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
 */

package github.paroj.dsub2000.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LibrarySyncService extends Service {
	private static LibrarySyncAdapter librarySyncAdapter;
	private static final Object syncLock = new Object();

	@Override
	public void onCreate() {
		synchronized (syncLock) {
			if (librarySyncAdapter == null) {
				librarySyncAdapter = new LibrarySyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return librarySyncAdapter.getSyncAdapterBinder();
	}
}
