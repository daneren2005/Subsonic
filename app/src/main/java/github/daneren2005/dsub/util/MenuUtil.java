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
import android.util.Log;
import android.view.Menu;

import java.io.File;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.view.AlbumView;
import github.daneren2005.dsub.view.ArtistEntryView;
import github.daneren2005.dsub.view.ArtistView;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.view.UpdateView;

public final class MenuUtil {
	private final static String TAG = MenuUtil.class.getSimpleName();

	public static void hideMenuItems(Context context, Menu menu, UpdateView updateView) {
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
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_NOW, true)) {
			menu.setGroupVisible(R.id.hide_play_now, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_SHUFFLED, true)) {
			menu.setGroupVisible(R.id.hide_play_shuffled, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_NEXT, false)) {
			menu.setGroupVisible(R.id.hide_play_next, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_LAST, true)) {
			menu.setGroupVisible(R.id.hide_play_last, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_DOWNLOAD, false)) {
			menu.setGroupVisible(R.id.hide_download, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PIN, false)) {
			menu.setGroupVisible(R.id.hide_pin, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_DELETE, false)) {
			menu.setGroupVisible(R.id.hide_delete, false);
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

		if(!Util.isOffline(context)) {
			// If we are looking at a standard song view, get downloadFile to cache what options to show
			if(updateView instanceof SongView) {
				SongView songView = (SongView) updateView;
				DownloadFile downloadFile = songView.getDownloadFile();

				try {
					if(downloadFile != null) {
						if(downloadFile.isWorkDone()) {
							// Remove permanent cache menu if already perma cached
							if(downloadFile.isSaved()) {
								menu.setGroupVisible(R.id.hide_pin, false);
							}

							// Remove cache option no matter what if already downloaded
							menu.setGroupVisible(R.id.hide_download, false);
						} else {
							// Remove delete option if nothing to delete
							menu.setGroupVisible(R.id.hide_delete, false);
						}
					}
				} catch(Exception e) {
					Log.w(TAG, "Failed to lookup downloadFile info", e);
				}
			}
			// Apply similar logic to album views
			else if(updateView instanceof AlbumView || updateView instanceof ArtistView || updateView instanceof ArtistEntryView) {
				File folder = null;
				if(updateView instanceof AlbumView) {
					folder = ((AlbumView) updateView).getFile();
				} else if(updateView instanceof ArtistView) {
					folder = ((ArtistView) updateView).getFile();
				} else if(updateView instanceof ArtistEntryView) {
					folder = ((ArtistEntryView) updateView).getFile();
				}

				try {
					if(folder != null && !folder.exists()) {
						menu.setGroupVisible(R.id.hide_delete, false);
					}
				} catch(Exception e) {
					Log.w(TAG, "Failed to lookup album directory info", e);
				}
			}
		}
	}
}
