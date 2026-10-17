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
 */
package github.paroj.dsub2000.service.parser;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import github.paroj.dsub2000.domain.LyricsLine;
import github.paroj.dsub2000.domain.StructuredLyrics;
import github.paroj.dsub2000.util.ProgressListener;

/**
 * Parses the lyricsList response of the OpenSubsonic getLyricsBySongId
 * endpoint:
 *
 * <pre>
 * &lt;lyricsList&gt;
 *   &lt;structuredLyrics displayArtist=".." displayTitle=".." lang="eng" offset="0" synced="true"&gt;
 *     &lt;line start="18800"&gt;We're no strangers to love&lt;/line&gt;
 *   &lt;/structuredLyrics&gt;
 * &lt;/lyricsList&gt;
 * </pre>
 *
 * Elements are collected on START_TAG and flushed when the following element
 * (or the end of the document) is reached, so that END_TAG never has to be
 * inspected -- attribute access is only valid on START_TAG.
 */
public class LyricsListParser extends AbstractParser {
	private static final String TAG = LyricsListParser.class.getSimpleName();

	public LyricsListParser(Context context, int instance) {
		super(context, instance);
	}

	public List<StructuredLyrics> parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		List<StructuredLyrics> lyricsList = new ArrayList<StructuredLyrics>();
		StructuredLyrics current = null;
		LyricsLine currentLine = null;

		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("structuredLyrics".equals(name)) {
					current = addLine(current, currentLine);
					currentLine = null;
					current = addLyrics(lyricsList, current);

					current = new StructuredLyrics();
					current.setDisplayArtist(get("displayArtist"));
					current.setDisplayTitle(get("displayTitle"));
					current.setLang(get("lang"));
					current.setOffset(getLongSafe("offset"));
					current.setSynced(getBoolean("synced"));
				} else if ("line".equals(name)) {
					current = addLine(current, currentLine);

					currentLine = new LyricsLine();
					currentLine.setStart(getLongSafe("start"));
				} else if ("error".equals(name)) {
					handleError();
				}
			} else if (eventType == XmlPullParser.TEXT) {
				// The first text node after a <line> is its content.  Whitespace
				// between elements can also land here for empty lines, which
				// trims away to the empty string.
				if (currentLine != null && currentLine.getValue() == null) {
					String text = getText();
					currentLine.setValue(text == null ? "" : text.trim());
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		current = addLine(current, currentLine);
		addLyrics(lyricsList, current);

		validate();
		return lyricsList;
	}

	/**
	 * Like getLong, but a malformed value reads as absent instead of failing the
	 * parse: one bad timestamp should not cost the whole set of lyrics.
	 */
	private Long getLongSafe(String name) {
		try {
			return getLong(name);
		} catch(NumberFormatException e) {
			Log.w(TAG, "Ignoring malformed " + name + " attribute", e);
			return null;
		}
	}

	private StructuredLyrics addLine(StructuredLyrics current, LyricsLine line) {
		if (current != null && line != null) {
			if (line.getValue() == null) {
				line.setValue("");
			}
			current.addLine(line);
		}
		return current;
	}

	private StructuredLyrics addLyrics(List<StructuredLyrics> lyricsList, StructuredLyrics current) {
		if (current != null) {
			lyricsList.add(current);
		}
		return null;
	}
}
