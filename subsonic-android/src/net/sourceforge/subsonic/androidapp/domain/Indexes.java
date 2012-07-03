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
package net.sourceforge.subsonic.androidapp.domain;

import java.util.List;
import java.io.Serializable;

/**
 * @author Sindre Mehus
 */
public class Indexes implements Serializable {

    private final long lastModified;
    private final List<Artist> shortcuts;
    private final List<Artist> artists;

    public Indexes(long lastModified, List<Artist> shortcuts, List<Artist> artists) {
        this.lastModified = lastModified;
        this.shortcuts = shortcuts;
        this.artists = artists;
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
}