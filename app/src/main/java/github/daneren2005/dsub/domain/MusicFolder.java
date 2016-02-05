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
package github.daneren2005.dsub.domain;

import android.util.Log;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a top level directory in which music or other media is stored.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class MusicFolder implements Serializable {
	private static final String TAG = MusicFolder.class.getSimpleName();
	private String id;
	private String name;
	private boolean enabled;

	public MusicFolder() {

	}
	public MusicFolder(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean getEnabled() {
		return enabled;
	}

	public static class MusicFolderComparator implements Comparator<MusicFolder> {
		public int compare(MusicFolder lhsMusicFolder, MusicFolder rhsMusicFolder) {
			if(lhsMusicFolder == rhsMusicFolder || lhsMusicFolder.getName().equals(rhsMusicFolder.getName())) {
				return 0;
			} else {
				return lhsMusicFolder.getName().compareToIgnoreCase(rhsMusicFolder.getName());
			}
		}
	}

	public static void sort(List<MusicFolder> musicFolders) {
		try {
			Collections.sort(musicFolders, new MusicFolderComparator());
		} catch (Exception e) {
			Log.w(TAG, "Failed to sort music folders", e);
		}
	}
}
