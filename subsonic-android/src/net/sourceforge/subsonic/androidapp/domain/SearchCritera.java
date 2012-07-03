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

/**
 * The criteria for a music search.
 *
 * @author Sindre Mehus
 */
public class SearchCritera {

    private final String query;
    private final int artistCount;
    private final int albumCount;
    private final int songCount;

    public SearchCritera(String query, int artistCount, int albumCount, int songCount) {
        this.query = query;
        this.artistCount = artistCount;
        this.albumCount = albumCount;
        this.songCount = songCount;
    }

    public String getQuery() {
        return query;
    }

    public int getArtistCount() {
        return artistCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public int getSongCount() {
        return songCount;
    }
}