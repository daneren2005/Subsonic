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
	
	Copyright 2013 (C) Scott Jackson
*/
package github.daneren2005.dsub.service.parser;

import android.content.Context;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Scott Jackson
 */
public class BookmarkParser extends MusicDirectoryEntryParser {
    public BookmarkParser(Context context, int instance) {
		super(context, instance);
	}

    public MusicDirectory parse(Reader reader, ProgressListener progressListener) throws Exception {
        init(reader);

		List<MusicDirectory.Entry> bookmarks = new ArrayList<MusicDirectory.Entry>();
        Bookmark bookmark = null;
        int eventType;

		boolean isDateNormalized = ServerInfo.checkServerVersion(context, "1.11");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		if(isDateNormalized) {
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		do {
            eventType = nextParseEvent();
            
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                
                if ("bookmark".equals(name)) {
                	bookmark = new Bookmark();

					try {
						bookmark.setCreated(dateFormat.parse(get("created")));
					} catch (Exception e) {
						bookmark.setCreated((Date) null);
					}

					try {
						bookmark.setChanged(dateFormat.parse(get("changed")));
					} catch (Exception e) {
						bookmark.setChanged((Date) null);
					}

                	bookmark.setComment(get("comment"));
                	bookmark.setPosition(getInteger("position"));
                	bookmark.setUsername(get("username"));
                } else if ("entry".equals(name)) {
					MusicDirectory.Entry entry = parseEntry(null);
					// Work around for bookmarks showing entry with a track when podcast listings don't
					if("podcast".equals(get("type"))) {
						entry.setTrack(null);
					}
					entry.setBookmark(bookmark);
                	bookmarks.add(entry);
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        return new MusicDirectory(bookmarks);
    }
}
