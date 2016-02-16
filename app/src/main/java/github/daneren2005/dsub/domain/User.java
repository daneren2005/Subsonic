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

package github.daneren2005.dsub.domain;

import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
	public static final String SCROBBLING = "scrobblingEnabled";
	public static final String ADMIN = "adminRole";
	public static final String SETTINGS = "settingsRole";
	public static final String DOWNLOAD = "downloadRole";
	public static final String UPLOAD = "uploadRole";
	public static final String COVERART = "coverArtRole";
	public static final String COMMENT = "commentRole";
	public static final String PODCAST = "podcastRole";
	public static final String STREAM = "streamRole";
	public static final String JUKEBOX = "jukeboxRole";
	public static final String SHARE = "shareRole";
	public static final String VIDEO_CONVERSION = "videoConversionRole";
	public static final String LASTFM = "lastFMRole";
	public static final List<String> ROLES = new ArrayList<>();
	
	static {
		ROLES.add(ADMIN);
		ROLES.add(SETTINGS);
		ROLES.add(STREAM);
		ROLES.add(DOWNLOAD);
		ROLES.add(UPLOAD);
		ROLES.add(COVERART);
		ROLES.add(COMMENT);
		ROLES.add(PODCAST);
		ROLES.add(JUKEBOX);
		ROLES.add(SHARE);
		ROLES.add(VIDEO_CONVERSION);
	}
	
	private String username;
	private String password;
	private String email;

	private List<Setting> settings = new ArrayList<Setting>();
	private List<Setting> musicFolders;

	public User() {

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public List<Setting> getSettings() {
		return settings;
	}
	public void setSettings(List<Setting> settings) {
		this.settings.clear();
		this.settings.addAll(settings);
	}
	public void addSetting(String name, Boolean value) {
		settings.add(new Setting(name, value));
	}

	public void addMusicFolder(MusicFolder musicFolder) {
		if(musicFolders == null) {
			musicFolders = new ArrayList<>();
		}

		musicFolders.add(new MusicFolderSetting(musicFolder.getId(), musicFolder.getName(), false));
	}
	public void addMusicFolder(MusicFolderSetting musicFolderSetting, boolean defaultValue) {
		if(musicFolders == null) {
			musicFolders = new ArrayList<>();
		}

		musicFolders.add(new MusicFolderSetting(musicFolderSetting.getName(), musicFolderSetting.getLabel(), defaultValue));
	}
	public List<Setting> getMusicFolderSettings() {
		return musicFolders;
	}

	public static class Setting implements Serializable {
		private String name;
		private Boolean value;

		public Setting() {
			
		}
		public Setting(String name, Boolean value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}
		public Boolean getValue() {
			return value;
		}
		public void setValue(Boolean value) {
			this.value = value;
		}
	}

	public static class MusicFolderSetting extends Setting {
		private String label;

		public MusicFolderSetting() {

		}
		public MusicFolderSetting(String name, String label, Boolean value) {
			super(name, value);
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}
}
