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

package github.paroj.dsub2000.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlayerQueue implements Serializable {
	public List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>();
	public List<MusicDirectory.Entry> toDelete = new ArrayList<MusicDirectory.Entry>();
	public int currentPlayingIndex;
	public int currentPlayingPosition;
	public boolean renameCurrent = false;
	public Date changed = null;

	/**
	 * Whether this queue can safely be handed to {@link github.paroj.dsub2000.service.DownloadService#restore}.
	 * Kryo does not run field initializers, so a stale or corrupt state file can leave fields at their default
	 * (null) even though they are initialized above.
	 */
	public boolean isRestorable() {
		if(songs == null || toDelete == null) {
			return false;
		}
		if(currentPlayingIndex < -1 || currentPlayingIndex >= songs.size()) {
			return false;
		}
		for(MusicDirectory.Entry song: songs) {
			if(song == null) {
				return false;
			}
		}
		for(MusicDirectory.Entry song: toDelete) {
			if(song == null) {
				return false;
			}
		}
		return true;
	}
}
