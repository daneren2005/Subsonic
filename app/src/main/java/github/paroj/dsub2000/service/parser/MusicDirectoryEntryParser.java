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
package github.paroj.dsub2000.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import github.paroj.dsub2000.domain.Artist;
import github.paroj.dsub2000.domain.Bookmark;
import github.paroj.dsub2000.domain.MusicDirectory;

/**
 * @author Sindre Mehus
 */
public class MusicDirectoryEntryParser extends AbstractParser {
    private MusicDirectory.Entry currentEntry;
    private int currentEntryDepth = -1;

    public MusicDirectoryEntryParser(Context context, int instance) {
        super(context, instance);
    }

    @Override
    protected int nextParseEvent() throws Exception {
        int eventType = super.nextParseEvent();
        if (eventType == XmlPullParser.END_TAG && currentEntry != null && getDepth() == currentEntryDepth) {
            currentEntry = null;
            currentEntryDepth = -1;
        }
        return eventType;
    }

    /**
     * Collects an artist nested in an entry's artists container. Such elements
     * must not be mistaken for top-level artists by the owning parser.
     *
     * @return true if the element was consumed as a nested artist
     */
    protected boolean handleNestedArtist(String name) {
        if (currentEntry == null || currentEntryDepth <= 0 || getDepth() <= currentEntryDepth
                || !"artist".equals(name)) {
            return false;
        }
        if (isInside("artists")) {
            currentEntry.addArtist(new Artist(get("id"), get("name")));
            return true;
        }
        // albumArtists and contributors also contain artist elements, but they
        // are not this entry's display artists and must not be read as
        // structural tags by the owning parser.
        return isInside("albumArtists") || isInside("contributors");
    }

    protected MusicDirectory.Entry parseEntry(String artist) {
        MusicDirectory.Entry entry = new MusicDirectory.Entry();
        entry.setId(get("id"));
		entry.setParent(get("parent"));
		entry.setArtistId(get("artistId"));
        entry.setTitle(get("title"));
		if(entry.getTitle() == null) {
			entry.setTitle(get("name"));
		}
        entry.setDirectory(getBoolean("isDir"));
        entry.setCoverArt(get("coverArt"));
        entry.setArtist(get("artist"));
        entry.setStarred(get("starred") != null);
        entry.setYear(getInteger("year"));
        entry.setGenre(get("genre"));
		entry.setAlbum(get("album"));
		entry.setRating(getInteger("userRating"));
		entry.setDisplayArtist(get("displayArtist"));

        if (!entry.isDirectory()) {
			entry.setAlbumId(get("albumId"));
            entry.setTrack(getInteger("track"));
            entry.setContentType(get("contentType"));
            entry.setSuffix(get("suffix"));
            entry.setTranscodedContentType(get("transcodedContentType"));
            entry.setTranscodedSuffix(get("transcodedSuffix"));
            entry.setSize(getLong("size"));
            entry.setDuration(getInteger("duration"));
            entry.setBitRate(getInteger("bitRate"));
            entry.setPath(get("path"));
            entry.setVideo(getBoolean("isVideo"));
			entry.setDiscNumber(getInteger("discNumber"));

			Integer bookmark = getInteger("bookmarkPosition");
			if(bookmark != null) {
				entry.setBookmark(new Bookmark(bookmark));
			}

			String type = get("type");
			if("podcast".equals(type)) {
				entry.setType(MusicDirectory.Entry.TYPE_PODCAST);
			} else if("audiobook".equals(type) || (entry.getGenre() != null && "audiobook".equals(entry.getGenre().toLowerCase()))) {
				entry.setType(MusicDirectory.Entry.TYPE_AUDIO_BOOK);
			}
        } else if(!"".equals(artist)) {
			entry.setPath(artist + "/" + entry.getTitle());
		}
		currentEntry = entry;
		currentEntryDepth = getDepth();
        return entry;
    }
	
	protected MusicDirectory.Entry parseArtist() {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		
		entry.setId(get("id"));
		entry.setTitle(get("name"));
		entry.setPath(entry.getTitle());
		entry.setStarred(true);
		entry.setDirectory(true);
		
		return entry;
	}
}
