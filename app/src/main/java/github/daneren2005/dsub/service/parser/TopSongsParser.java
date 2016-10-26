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
	Copyright 2016 (C) Scott Jackson
*/
package github.daneren2005.dsub.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ProgressListener;

public class TopSongsParser extends MusicDirectoryEntryParser {

	public TopSongsParser(Context context, int instance) {
		super(context, instance);
	}

	public MusicDirectory parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		MusicDirectory dir = new MusicDirectory();
		int eventType;
		int customOrder = 1;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("song".equals(name)) {
					MusicDirectory.Entry entry = parseEntry("");
					entry.setCustomOrder(customOrder);
					dir.addChild(entry);

					customOrder++;
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();

		return dir;
	}
}