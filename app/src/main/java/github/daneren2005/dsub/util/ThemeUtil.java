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
	Copyright 2016 (C) Scott Jackson
*/

package github.daneren2005.dsub.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SettingsActivity;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;

public final class ThemeUtil {
	public static final String THEME_DARK = "dark";
	public static final String THEME_BLACK = "black";
	public static final String THEME_LIGHT = "light";
	public static final String THEME_HOLO = "holo";
	public static final String THEME_DAY_NIGHT = "day/night";
	public static final String THEME_DAY_BLACK_NIGHT = "day/black";

	public static String getTheme(Context context) {
		SharedPreferences prefs = Util.getPreferences(context);
		String theme = prefs.getString(Constants.PREFERENCES_KEY_THEME, null);

		if(THEME_DAY_NIGHT.equals(theme)) {
			int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
			if(currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
				theme = THEME_DARK;
			} else {
				theme = THEME_LIGHT;
			}
		} else if(THEME_DAY_BLACK_NIGHT.equals(theme)) {
			int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
			if(currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
				theme = THEME_BLACK;
			} else {
				theme = THEME_LIGHT;
			}
		}

		return theme;
	}
	public static int getThemeRes(Context context) {
		return getThemeRes(context, getTheme(context));
	}
	public static int getThemeRes(Context context, String theme) {
		if(context instanceof SubsonicFragmentActivity || context instanceof SettingsActivity) {
			if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true)) {
				if (THEME_DARK.equals(theme)) {
					return R.style.Theme_DSub_Dark_No_Actionbar;
				} else if (THEME_BLACK.equals(theme)) {
					return R.style.Theme_DSub_Black_No_Actionbar;
				} else if (THEME_HOLO.equals(theme)) {
					return R.style.Theme_DSub_Holo_No_Actionbar;
				} else {
					return R.style.Theme_DSub_Light_No_Actionbar;
				}
			} else {
				if (THEME_DARK.equals(theme)) {
					return R.style.Theme_DSub_Dark_No_Color;
				} else if (THEME_BLACK.equals(theme)) {
					return R.style.Theme_DSub_Black_No_Color;
				} else if (THEME_HOLO.equals(theme)) {
					return R.style.Theme_DSub_Holo_No_Color;
				} else {
					return R.style.Theme_DSub_Light_No_Color;
				}
			}
		} else {
			if (THEME_DARK.equals(theme)) {
				return R.style.Theme_DSub_Dark;
			} else if (THEME_BLACK.equals(theme)) {
				return R.style.Theme_DSub_Black;
			} else if (THEME_HOLO.equals(theme)) {
				return R.style.Theme_DSub_Holo;
			} else {
				return R.style.Theme_DSub_Light;
			}
		}
	}
	public static void setTheme(Context context, String theme) {
		SharedPreferences.Editor editor = Util.getPreferences(context).edit();
		editor.putString(Constants.PREFERENCES_KEY_THEME, theme);
		editor.commit();
	}

	public static void applyTheme(Context context, String theme) {
		context.setTheme(getThemeRes(context, theme));

		SharedPreferences prefs = Util.getPreferences(context);
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_OVERRIDE_SYSTEM_LANGUAGE, false)) {
			Configuration config = new Configuration();
			config.locale = Locale.ENGLISH;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		}
	}
}
