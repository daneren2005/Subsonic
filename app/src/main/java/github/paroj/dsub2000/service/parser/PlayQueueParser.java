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

package github.paroj.dsub2000.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerQueue;
import github.paroj.dsub2000.util.ProgressListener;

public class PlayQueueParser extends MusicDirectoryEntryParser {
	private static final String TAG = PlayQueueParser.class.getSimpleName();

	public PlayQueueParser(Context context, int instance) {
		super(context, instance);
	}

	/**
	 * Parses the {@code changed} attribute of the play queue.
	 *
	 * Servers do not agree on the format: some send a UTC timestamp without a zone
	 * ({@code 2020-01-02T03:04:05}), others append a zone ({@code ...Z}) and some
	 * send a numeric offset ({@code ...+02:00}), optionally with fractional
	 * seconds. A pattern without a zone silently ignores the offset, which moves
	 * the resulting instant by up to a day and makes the remote queue look newer
	 * than it is, so the offset has to be part of the pattern.
	 */
	private static Date parseChanged(String value) {
		if(value == null || value.isEmpty()) {
			return null;
		}

		String changed = value.trim();
		int dot = changed.indexOf('.');
		if(dot >= 0) {
			// SimpleDateFormat handles milliseconds at most; some servers send microseconds or nanoseconds.
			int fractionEnd = dot + 1;
			while(fractionEnd < changed.length() && Character.isDigit(changed.charAt(fractionEnd))) {
				fractionEnd++;
			}
			String fraction = changed.substring(dot + 1, fractionEnd);
			if(fraction.length() > 3) {
				fraction = fraction.substring(0, 3);
			}
			changed = changed.substring(0, dot) + '.' + fraction + changed.substring(fractionEnd);
		}

		boolean hasZone = changed.endsWith("Z") || changed.matches(".*[+-]\\d{2}:?\\d{2}$");
		String pattern = "yyyy-MM-dd'T'HH:mm:ss" + (changed.indexOf('.') >= 0 ? ".SSS" : "") + (hasZone ? "XXX" : "");
		SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
		if(!hasZone) {
			// The Subsonic API documents the zone-less form as UTC.
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		try {
			return dateFormat.parse(changed);
		} catch(ParseException e) {
			return null;
		}
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
				if (handleNestedArtist(name)) {
					continue;
				}
				if("playQueue".equals(name)) {
					currentId = get("current");
					Integer position = getInteger("position");
					if(position != null) {
						state.currentPlayingPosition = position;
					}
					state.changed = parseChanged(get("changed"));
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
