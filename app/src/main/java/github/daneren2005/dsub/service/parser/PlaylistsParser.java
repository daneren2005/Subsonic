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
package github.daneren2005.dsub.service.parser;

import android.content.Context;

import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sindre Mehus
 */
public class PlaylistsParser extends AbstractParser {

    public PlaylistsParser(Context context, int instance) {
		super(context, instance);
	}

    public List<Playlist> parse(Reader reader, ProgressListener progressListener) throws Exception {
        init(reader);

        List<Playlist> result = new ArrayList<Playlist>();
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String tag = getElementName();
                if ("playlist".equals(tag)) {
                    String id = get("id");
                    String name = get("name");
					String owner = get("owner");
					String comment = get("comment");
					String songCount = get("songCount");
					String pub = get("public");
					String created = get("created");
					String changed = get("changed");
					Integer duration = getInteger("duration");
                    result.add(new Playlist(id, name, owner, comment, songCount, pub, created, changed, duration));
                } else if ("error".equals(tag)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        return Playlist.PlaylistComparator.sort(result);
    }

}