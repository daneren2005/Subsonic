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
package github.vrih.xsub.service.parser;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.Reader;

import github.vrih.xsub.R;
import github.vrih.xsub.domain.ServerInfo;
import github.vrih.xsub.domain.Version;
import github.vrih.xsub.util.ProgressListener;
import github.vrih.xsub.util.Util;

/**
 * @author Sindre Mehus
 */
abstract class AbstractParser {
    private static final String TAG = AbstractParser.class.getSimpleName();
	private static final String SUBSONIC_RESPONSE = "subsonic-response";
	private static final String MADSONIC_RESPONSE = "madsonic-response";
	private static final String SUBSONIC = "subsonic";
	private static final String MADSONIC = "madsonic";
	private static final String AMPACHE = "ampache";

    final Context context;
	final int instance;
    private XmlPullParser parser;
    private boolean rootElementFound;

    AbstractParser(Context context, int instance) {
        this.context = context;
		this.instance = instance;
    }

    Context getContext() {
        return context;
    }

    void handleError() throws Exception {
        int code = getInteger("code");
        String message;
        switch (code) {
			case 0:
				message = context.getResources().getString(R.string.parser_server_error, get("message"));
				break;
            case 20:
                message = context.getResources().getString(R.string.parser_upgrade_client);
                break;
            case 30:
                message = context.getResources().getString(R.string.parser_upgrade_server);
                break;
            case 40:
                message = context.getResources().getString(R.string.parser_not_authenticated);
                break;
			case 41:
				Util.setBlockTokenUse(context, instance, true);

				// Throw IOException so RESTMusicService knows to retry
				throw new IOException();
            case 50:
                message = context.getResources().getString(R.string.parser_not_authorized);
                break;
            default:
                message = get("message");
                break;
        }
        throw new SubsonicRESTException(code, message);
    }

    void updateProgress(ProgressListener progressListener, String message) {
        if (progressListener != null) {
            progressListener.updateProgress(message);
        }
    }

    String getText() {
        return parser.getText();
    }

    String get(String name) {
        return parser.getAttributeValue(null, name);
    }

    boolean getBoolean(String name) {
        return "true".equals(get(name));
    }

    Integer getInteger(String name) {
        String s = get(name);
        try {
            return (s == null || "".equals(s)) ? null : Integer.valueOf(s);
        } catch(Exception e) {
            Log.w(TAG, "Failed to parse " + s + " into integer");
            return null;
        }
    }

    Long getLong(String name) {
        String s = get(name);
        return s == null ? null : Long.valueOf(s);
    }

    Float getFloat() {
        String s = get("gain");
        return s == null ? null : Float.valueOf(s);
    }

    void init(Reader reader) throws Exception {
        parser = Xml.newPullParser();
        parser.setInput(reader);
        rootElementFound = false;
    }

    int nextParseEvent() {
		try {
			return parser.next();
		} catch(Exception e) {
		    ServerInfo overrideInfo = new ServerInfo();
		    overrideInfo.saveServerInfo(context, instance);
		}
		return 0;
    }


    String getElementName() {
        String name = parser.getName();
        if (SUBSONIC_RESPONSE.equals(name) || MADSONIC_RESPONSE.equals(name)) {
            rootElementFound = true;
            String version = get("version");
            if (version != null) {
            	ServerInfo server = new ServerInfo();
            	server.setRestVersion(new Version(version));

            	if(MADSONIC.equals(get("type")) || MADSONIC_RESPONSE.equals(name)) {
					server.setRestType(ServerInfo.TYPE_MADSONIC);
            	} if(AMPACHE.equals(get("type"))) {
                    server.setRestType(ServerInfo.TYPE_AMPACHE);
                } else if(SUBSONIC.equals(get("type")) && server.checkServerVersion(context, "1.13")) {
                    // Oh am I going to regret this
                    server.setRestType(ServerInfo.TYPE_MADSONIC);
                    server.setRestVersion(new Version("2.0.0"));
                }
            	server.saveServerInfo(context, instance);
            }
        }
        return name;
    }

    void validate() throws Exception {
        if (!rootElementFound) {
			if(ServerInfo.isMadsonic6(context, instance)) {
				ServerInfo overrideInfo = new ServerInfo();
				overrideInfo.saveServerInfo(context, instance);
			}

            throw new Exception(context.getResources().getString(R.string.background_task_parse_error));
        }
    }
}
