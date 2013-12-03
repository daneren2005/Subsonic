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
import github.daneren2005.dsub.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Scott Jackson
 */
public class BookmarkParser extends MusicDirectoryEntryParser {
    public BookmarkParser(Context context) {
        super(context);
    }

    public List<Bookmark> parse(Reader reader, ProgressListener progressListener) throws Exception {
        updateProgress(progressListener, R.string.parser_reading);
        init(reader);

        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        Bookmark bookmark = null;
        int eventType;
        
        do {
            eventType = nextParseEvent();
            
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                
                if ("bookmark".equals(name)) {
                	bookmark = new Bookmark();
                	bookmark.setChanged(get("changed"));
                	bookmark.setCreated(get("created"));
                	bookmark.setComment(get("comment"));
                	bookmark.setPosition(getInteger("position"));
                	bookmark.setUsername(get("username"));
                } else if ("entry".equals(name)) {
					MusicDirectory.Entry entry = parseEntry(null);
					entry.setTrack(null);
                	bookmark.setEntry(entry);
                	bookmarks.add(bookmark);
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        updateProgress(progressListener, R.string.parser_reading_done);

        return bookmarks;
    }
}
