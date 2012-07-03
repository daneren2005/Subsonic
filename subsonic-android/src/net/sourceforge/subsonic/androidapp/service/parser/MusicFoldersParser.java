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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.MusicFolder;
import net.sourceforge.subsonic.androidapp.domain.Playlist;
import net.sourceforge.subsonic.androidapp.util.ProgressListener;

/**
 * @author Sindre Mehus
 */
public class MusicFoldersParser extends AbstractParser {

    public MusicFoldersParser(Context context) {
        super(context);
    }

    public List<MusicFolder> parse(Reader reader, ProgressListener progressListener) throws Exception {

        updateProgress(progressListener, R.string.parser_reading);
        init(reader);

        List<MusicFolder> result = new ArrayList<MusicFolder>();
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String tag = getElementName();
                if ("musicFolder".equals(tag)) {
                    String id = get("id");
                    String name = get("name");
                    result.add(new MusicFolder(id, name));
                } else if ("error".equals(tag)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        updateProgress(progressListener, R.string.parser_reading_done);

        return result;
    }

}