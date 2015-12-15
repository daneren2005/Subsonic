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
import android.util.Log;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static github.daneren2005.dsub.domain.MusicDirectory.*;

/**
 * @author Sindre Mehus
 */
public class MusicDirectoryParser extends MusicDirectoryEntryParser {

    private static final String TAG = MusicDirectoryParser.class.getSimpleName();

    public MusicDirectoryParser(Context context, int instance) {
        super(context, instance);
    }

    public MusicDirectory parse(String artist, Reader reader, ProgressListener progressListener) throws Exception {
        init(reader);

        MusicDirectory dir = new MusicDirectory();
        int eventType;
		boolean isArtist = false;
		boolean checkForDuplicates = Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_RENAME_DUPLICATES, true);
		Map<String, Entry> titleMap = new HashMap<String, Entry>();
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("child".equals(name) || "song".equals(name) || "video".equals(name)) {
					Entry entry = parseEntry(artist);
					entry.setGrandParent(dir.getParent());

					// Only check for songs
					if(checkForDuplicates && !entry.isDirectory()) {
						// Check if duplicates
						String disc = (entry.getDiscNumber() != null) ? Integer.toString(entry.getDiscNumber()) : "";
						String track = (entry.getTrack() != null) ? Integer.toString(entry.getTrack()) : "";
						String duplicateId = disc + "-" + track + "-" + entry.getTitle();

						Entry duplicate = titleMap.get(duplicateId);
						if (duplicate != null) {
							// Check if the first already has been rebased or not
							if (duplicate.getTitle().equals(entry.getTitle())) {
								duplicate.rebaseTitleOffPath();
							}

							// Rebase if this is the second instance of this title found
							entry.rebaseTitleOffPath();
						} else {
							titleMap.put(duplicateId, entry);
						}
					}

                    dir.addChild(entry);
                } else if ("directory".equals(name) || "artist".equals(name) || ("album".equals(name) && !isArtist)) {
                    dir.setName(get("name"));
					dir.setId(get("id"));
					if(Util.isTagBrowsing(context, instance)) {
						dir.setParent(get("artistId"));
					} else {
						dir.setParent(get("parent"));
					}
					isArtist = true;
                } else if("album".equals(name)) {
					Entry entry = parseEntry(artist);
					entry.setDirectory(true);
					dir.addChild(entry);
				} else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        return dir;
    }
}
