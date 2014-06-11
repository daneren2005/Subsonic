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

package github.daneren2005.dsub.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.service.MusicServiceFactory;

public final class UserUtil {
	private static User currentUser;

	public static void seedCurrentUser(final Context context) {
		// Only try to seed if online
		if(Util.isOffline(context)) {
			return;
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				currentUser = MusicServiceFactory.getMusicService(context).getUser(false, getCurrentUsername(context), context, null);
				return null;
			}
		}.execute();
	}

	public static User getCurrentUser(Context context) {
		return currentUser;
	}

	public static String getCurrentUsername(Context context, int instance) {
		SharedPreferences prefs = Util.getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
	}

	public static String getCurrentUsername(Context context) {
		return getCurrentUsername(context, Util.getActiveServer(context));
	}

	public static boolean isCurrentAdmin(Context context) {
		if(currentUser == null) {
			return false;
		} else {
			return isCurrentRole(context, "adminRole");
		}
	}

	public static boolean isCurrentRole(Context context, String role) {
		if(currentUser == null) {
			return false;
		}

		for(User.Setting setting: currentUser.getSettings()) {
			if(setting.getName().equals(role)) {
				return setting.getValue() == true;
			}
		}

		return false;
	}
}
