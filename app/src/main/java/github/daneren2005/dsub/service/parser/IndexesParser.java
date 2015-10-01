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

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.SharedPreferences;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ProgressListener;
import android.util.Log;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

/**
 * @author Sindre Mehus
 */
public class IndexesParser extends MusicDirectoryEntryParser {
    private static final String TAG = IndexesParser.class.getSimpleName();

    public IndexesParser(Context context, int instance) {
        super(context, instance);
    }

    public Indexes parse(Reader reader, ProgressListener progressListener) throws Exception {
        long t0 = System.currentTimeMillis();
        init(reader);

        List<Artist> artists = new ArrayList<Artist>();
        List<Artist> shortcuts = new ArrayList<Artist>();
		List<MusicDirectory.Entry> entries = new ArrayList<MusicDirectory.Entry>();
        Long lastModified = null;
        int eventType;
        String index = "#";
		String ignoredArticles = null;
        boolean changed = false;
		Map<String, Artist> artistList = new HashMap<String, Artist>();

        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("indexes".equals(name) || "artists".equals(name)) {
                    changed = true;
                    lastModified = getLong("lastModified");
					ignoredArticles = get("ignoredArticles");
                } else if ("index".equals(name)) {
                    index = get("name");

                } else if ("artist".equals(name)) {
                    Artist artist = new Artist();
                    artist.setId(get("id"));
                    artist.setName(get("name"));
                    artist.setIndex(index);
					artist.setStarred(get("starred") != null);
                    artist.setRating(getInteger("userRating"));

					// Combine the id's for the two artists
					if(artistList.containsKey(artist.getName())) {
						Artist originalArtist = artistList.get(artist.getName());
						if(originalArtist.isStarred()) {
							artist.setStarred(true);
						}
						originalArtist.setId(originalArtist.getId() + ";" + artist.getId());
					} else {
						artistList.put(artist.getName(), artist);
						artists.add(artist);
					}

                    if (artists.size() % 10 == 0) {
                        String msg = getContext().getResources().getString(R.string.parser_artist_count, artists.size());
                        updateProgress(progressListener, msg);
                    }
                } else if ("shortcut".equals(name)) {
                    Artist shortcut = new Artist();
                    shortcut.setId(get("id"));
                    shortcut.setName(get("name"));
                    shortcut.setIndex("*");
					shortcut.setStarred(get("starred") != null);
                    shortcuts.add(shortcut);
				} else if("child".equals(name)) {
					MusicDirectory.Entry entry = parseEntry("");
					entries.add(entry);
				} else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
		
		if(ignoredArticles != null) {
			SharedPreferences.Editor prefs = Util.getPreferences(context).edit();
			prefs.putString(Constants.CACHE_KEY_IGNORE, ignoredArticles);
			prefs.commit();
		}

        if (!changed) {
            return null;
        }

        long t1 = System.currentTimeMillis();
        Log.d(TAG, "Got " + artists.size() + " artist(s) in " + (t1 - t0) + "ms.");

        String msg = getContext().getResources().getString(R.string.parser_artist_count, artists.size());
        updateProgress(progressListener, msg);

        return new Indexes(lastModified == null ? 0L : lastModified, shortcuts, artists, entries);
    }
}