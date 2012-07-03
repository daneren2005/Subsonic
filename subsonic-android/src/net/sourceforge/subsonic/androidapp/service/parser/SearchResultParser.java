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
package net.sourceforge.subsonic.androidapp.service.parser;

import android.content.Context;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.domain.SearchResult;
import net.sourceforge.subsonic.androidapp.domain.Artist;
import net.sourceforge.subsonic.androidapp.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Sindre Mehus
 */
public class SearchResultParser extends MusicDirectoryEntryParser {

    public SearchResultParser(Context context) {
        super(context);
    }

    public SearchResult parse(Reader reader, ProgressListener progressListener) throws Exception {
        updateProgress(progressListener, R.string.parser_reading);
        init(reader);

        List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>();
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("match".equals(name)) {
                    songs.add(parseEntry());
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        updateProgress(progressListener, R.string.parser_reading_done);

        return new SearchResult(Collections.<Artist>emptyList(), Collections.<MusicDirectory.Entry>emptyList(), songs);
    }

}
