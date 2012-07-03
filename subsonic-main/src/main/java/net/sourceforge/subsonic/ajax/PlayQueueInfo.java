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
package net.sourceforge.subsonic.ajax;

import java.util.List;

/**
 * The playlist of a player.
 *
 * @author Sindre Mehus
 */
public class PlayQueueInfo {

    private final List<Entry> entries;
    private final int index;
    private final boolean stopEnabled;
    private final boolean repeatEnabled;
    private final boolean sendM3U;
    private final float gain;

    public PlayQueueInfo(List<Entry> entries, int index, boolean stopEnabled, boolean repeatEnabled, boolean sendM3U, float gain) {
        this.entries = entries;
        this.index = index;
        this.stopEnabled = stopEnabled;
        this.repeatEnabled = repeatEnabled;
        this.sendM3U = sendM3U;
        this.gain = gain;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getIndex() {
        return index;
    }

    public boolean isStopEnabled() {
        return stopEnabled;
    }

    public boolean isSendM3U() {
        return sendM3U;
    }

    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }

    public float getGain() {
        return gain;
    }

    public static class Entry {
        private final int id;
        private final Integer trackNumber;
        private final String title;
        private final String artist;
        private final String album;
        private final String genre;
        private final Integer year;
        private final String bitRate;
        private final Integer duration;
        private final String durationAsString;
        private final String format;
        private final String contentType;
        private final String fileSize;
        private final boolean starred;
        private final String albumUrl;
        private final String streamUrl;

        public Entry(int id, Integer trackNumber, String title, String artist, String album, String genre, Integer year,
                     String bitRate, Integer duration, String durationAsString, String format, String contentType, String fileSize,
                     boolean starred, String albumUrl, String streamUrl) {
            this.id = id;
            this.trackNumber = trackNumber;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.genre = genre;
            this.year = year;
            this.bitRate = bitRate;
            this.duration = duration;
            this.durationAsString = durationAsString;
            this.format = format;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.starred = starred;
            this.albumUrl = albumUrl;
            this.streamUrl = streamUrl;
        }

        public int getId() {
            return id;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public String getGenre() {
            return genre;
        }

        public Integer getYear() {
            return year;
        }

        public String getBitRate() {
            return bitRate;
        }

        public String getDurationAsString() {
            return durationAsString;
        }

        public Integer getDuration() {
            return duration;
        }

        public String getFormat() {
            return format;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileSize() {
            return fileSize;
        }

        public boolean isStarred() {
            return starred;
        }

        public String getAlbumUrl() {
            return albumUrl;
        }

        public String getStreamUrl() {
            return streamUrl;
        }
    }

}