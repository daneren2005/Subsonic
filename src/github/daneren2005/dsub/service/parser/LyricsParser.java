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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.service.parser;

import android.content.Context;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;

/**
 * @author Sindre Mehus
 */
public class LyricsParser extends AbstractParser {

    public LyricsParser(Context context, int instance) {
		super(context, instance);
	}

    public Lyrics parse(Reader reader, ProgressListener progressListener) throws Exception {
        init(reader);

        Lyrics lyrics = null;
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("lyrics".equals(name)) {
                    lyrics = new Lyrics();
                    lyrics.setArtist(get("artist"));
                    lyrics.setTitle(get("title"));
                } else if ("error".equals(name)) {
                    handleError();
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (lyrics != null && lyrics.getText() == null) {
                    lyrics.setText(getText());
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        return lyrics;
    }
}
