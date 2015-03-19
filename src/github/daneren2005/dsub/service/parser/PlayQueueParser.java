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
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerQueue;
import github.daneren2005.dsub.util.ProgressListener;

public class PlayQueueParser extends MusicDirectoryEntryParser {
	private static final String TAG = PlayQueueParser.class.getSimpleName();

	public PlayQueueParser(Context context, int instance) {
		super(context, instance);
	}

	public PlayerQueue parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		PlayerQueue state = new PlayerQueue();
		String currentId = null;
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if("playQueue".equals(name)) {
					currentId = get("current");
					state.currentPlayingPosition = getInteger("position");
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
						dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
						state.changed = dateFormat.parse(get("changed"));
					} catch (ParseException e) {
						state.changed = null;
					}
				} else if ("entry".equals(name)) {
					MusicDirectory.Entry entry = parseEntry("");
					// Only add songs
					if(!entry.isVideo()) {
						state.songs.add(entry);
					}
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		if(currentId != null) {
			for (MusicDirectory.Entry entry : state.songs) {
				if (entry.getId().equals(currentId)) {
					state.currentPlayingIndex = state.songs.indexOf(entry);
				}
			}
		} else {
			state.currentPlayingIndex = 0;
			state.currentPlayingPosition = 0;
		}

		validate();
		return state;
	}
}
