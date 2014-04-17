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
import android.content.SharedPreferences;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;

import org.xmlpull.v1.XmlPullParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Joshua Bahnsen
 */
public class ShareParser extends MusicDirectoryEntryParser {
	private static final String TAG = ShareParser.class.getSimpleName();

	public ShareParser(Context context) {
        super(context);
    }

    public List<Share> parse(Reader reader, ProgressListener progressListener) throws Exception {

        updateProgress(progressListener, R.string.parser_reading);
        init(reader);

        List<Share> dir = new ArrayList<Share>();
        Share share = null;
        int eventType;

		SharedPreferences prefs = Util.getPreferences(context);
		int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
		String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
		if(serverUrl.charAt(serverUrl.length() - 1) != '/') {
			serverUrl += '/';
		}
		serverUrl += "share/";
        
        do {
            eventType = nextParseEvent();
            
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                
                if ("share".equals(name)) {
                	share = new Share();
                	share.setCreated(get("created"));
					share.setUrl(get("url").replaceFirst(".*/([^/?]+).*", serverUrl + "$1"));
                	share.setDescription(get("description"));
                	share.setExpires(get("expires"));
                	share.setId(get("id"));
                	share.setLastVisited(get("lastVisited"));
                	share.setUsername(get("username"));
                	share.setVisitCount(getLong("visitCount"));
					dir.add(share);
                } else if ("entry".equals(name)) {
					if(share != null) {
                		share.addEntry(parseEntry(null));
					}
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
        updateProgress(progressListener, R.string.parser_reading_done);

        return dir;
    }
}