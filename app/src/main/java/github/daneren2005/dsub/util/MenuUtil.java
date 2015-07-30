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

package github.daneren2005.dsub.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;

public final class MenuUtil {
	public static void hideMenuItems(Context context, Menu menu) {
		if(!ServerInfo.checkServerVersion(context, "1.8")) {
			menu.setGroupVisible(R.id.server_1_8, false);
			menu.setGroupVisible(R.id.hide_star, false);
		}
		if(!ServerInfo.checkServerVersion(context, "1.9")) {
			menu.setGroupVisible(R.id.server_1_9, false);
		}
		if(!ServerInfo.checkServerVersion(context, "1.10.1")) {
			menu.setGroupVisible(R.id.server_1_10, false);
		}

		SharedPreferences prefs = Util.getPreferences(context);
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_NEXT, true)) {
			menu.setGroupVisible(R.id.hide_play_next, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_LAST, true)) {
			menu.setGroupVisible(R.id.hide_play_last, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_STAR, true)) {
			menu.setGroupVisible(R.id.hide_star, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_SHARED, true) || !UserUtil.canShare()) {
			menu.setGroupVisible(R.id.hide_share, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_RATING, true)) {
			menu.setGroupVisible(R.id.hide_rating, false);
		}
	}
}
