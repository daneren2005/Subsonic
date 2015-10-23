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
import android.util.Log;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;

import org.xmlpull.v1.XmlPullParser;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Joshua Bahnsen
 */
public class ShareParser extends MusicDirectoryEntryParser {
	private static final String TAG = ShareParser.class.getSimpleName();

	public ShareParser(Context context, int instance) {
		super(context, instance);
	}

    public List<Share> parse(Reader reader, ProgressListener progressListener) throws Exception {
        init(reader);

        List<Share> dir = new ArrayList<Share>();
        Share share = null;
        int eventType;

		SharedPreferences prefs = Util.getPreferences(context);
		String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
		if(serverUrl.charAt(serverUrl.length() - 1) != '/') {
			serverUrl += '/';
		}
		serverUrl += "share/";

		boolean isDateNormalized = ServerInfo.checkServerVersion(context, "1.11");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		if(isDateNormalized) {
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
        
        do {
            eventType = nextParseEvent();
            
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                
                if ("share".equals(name)) {
                	share = new Share();

					try {
						share.setCreated(dateFormat.parse(get("created")));
					} catch (Exception e) {
						share.setCreated((Date) null);
					}

					String url = get("url");
					if(url != null && url.indexOf(".php") == -1) {
						url = url.replaceFirst(".*/([^/?]+).*", serverUrl + "$1");
					}
					share.setUrl(url);

                	share.setDescription(get("description"));

					try {
						share.setExpires(dateFormat.parse(get("expires")));
					} catch (Exception e) {
						share.setExpires((Date) null);
					}
                	share.setId(get("id"));

					try {
						share.setLastVisited(dateFormat.parse(get("lastVisited")));
					} catch (Exception e) {
						share.setLastVisited((Date) null);
					}

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

        return dir;
    }
}