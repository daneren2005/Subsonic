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

/**
 * The result of a search.  Contains matching artists, albums and songs.
 *
 * @author Sindre Mehus
 */
public class SearchResult {

    private final List<Artist> artists;
    private final List<MusicDirectory.Entry> albums;
    private final List<MusicDirectory.Entry> songs;

    public SearchResult(List<Artist> artists, List<MusicDirectory.Entry> albums, List<MusicDirectory.Entry> songs) {
        this.artists = artists;
        this.albums = albums;
        this.songs = songs;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public List<MusicDirectory.Entry> getAlbums() {
        return albums;
    }

    public List<MusicDirectory.Entry> getSongs() {
        return songs;
    }
}