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

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * @author Sindre Mehus
 */
public class Indexes implements Serializable {

    private long lastModified;
    private List<Artist> shortcuts;
    private List<Artist> artists;
	private List<MusicDirectory.Entry> entries;

	public Indexes() {

	}
    public Indexes(long lastModified, List<Artist> shortcuts, List<Artist> artists) {
        this.lastModified = lastModified;
        this.shortcuts = shortcuts;
        this.artists = artists;
		this.entries = new ArrayList<MusicDirectory.Entry>();
    }
	public Indexes(long lastModified, List<Artist> shortcuts, List<Artist> artists, List<MusicDirectory.Entry> entries) {
		this.lastModified = lastModified;
		this.shortcuts = shortcuts;
		this.artists = artists;
		this.entries = entries;
		if(!entries.isEmpty()) {
			Artist root = new Artist();
			root.setId("root");
			root.setName("Root");
			root.setIndex("#");
			artists.add(root);
		}
	}

    public long getLastModified() {
        return lastModified;
    }

    public List<Artist> getShortcuts() {
        return shortcuts;
    }

    public List<Artist> getArtists() {
        return artists;
    }

	public List<MusicDirectory.Entry> getEntries() {
		return entries;
	}
}