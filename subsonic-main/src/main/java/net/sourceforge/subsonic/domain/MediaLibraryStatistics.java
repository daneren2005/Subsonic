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
package net.sourceforge.subsonic.domain;

/**
 * Contains media libaray statistics, including the number of artists, albums and songs.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/11/17 18:29:03 $
 */
public class MediaLibraryStatistics {

    private int artistCount;
    private int albumCount;
    private int songCount;
    private long totalLengthInBytes;
    private long totalDurationInSeconds;

    public MediaLibraryStatistics(int artistCount, int albumCount, int songCount, long totalLengthInBytes, long totalDurationInSeconds) {
        this.artistCount = artistCount;
        this.albumCount = albumCount;
        this.songCount = songCount;
        this.totalLengthInBytes = totalLengthInBytes;
        this.totalDurationInSeconds = totalDurationInSeconds;
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

    public long getTotalLengthInBytes() {
        return totalLengthInBytes;
    }

    public long getTotalDurationInSeconds() {
        return totalDurationInSeconds;
    }
}
