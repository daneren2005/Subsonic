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
package net.sourceforge.subsonic.androidapp.service.parser;

import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.util.Xml;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.Version;
import net.sourceforge.subsonic.androidapp.util.ProgressListener;
import net.sourceforge.subsonic.androidapp.util.Util;

/**
 * @author Sindre Mehus
 */
public abstract class AbstractParser {

    private final Context context;
    private XmlPullParser parser;
    private boolean rootElementFound;

    public AbstractParser(Context context) {
        this.context = context;
    }

    protected Context getContext() {
        return context;
    }

    protected void handleError() throws Exception {
        int code = getInteger("code");
        String message;
        switch (code) {
            case 20:
                message = context.getResources().getString(R.string.parser_upgrade_client);
                break;
            case 30:
                message = context.getResources().getString(R.string.parser_upgrade_server);
                break;
            case 40:
                message = context.getResources().getString(R.string.parser_not_authenticated);
                break;
            case 50:
                message = context.getResources().getString(R.string.parser_not_authorized);
                break;
            default:
                message = get("message");
                break;
        }
        throw new SubsonicRESTException(code, message);
    }

    protected void updateProgress(ProgressListener progressListener, int messageId) {
        if (progressListener != null) {
            progressListener.updateProgress(messageId);
        }
    }

    protected void updateProgress(ProgressListener progressListener, String message) {
        if (progressListener != null) {
            progressListener.updateProgress(message);
        }
    }

    protected String getText() {
        return parser.getText();
    }

    protected String get(String name) {
        return parser.getAttributeValue(null, name);
    }

    protected boolean getBoolean(String name) {
        return "true".equals(get(name));
    }

    protected Integer getInteger(String name) {
        String s = get(name);
        return s == null ? null : Integer.valueOf(s);
    }

    protected Long getLong(String name) {
        String s = get(name);
        return s == null ? null : Long.valueOf(s);
    }

    protected Float getFloat(String name) {
        String s = get(name);
        return s == null ? null : Float.valueOf(s);
    }

    protected void init(Reader reader) throws Exception {
        parser = Xml.newPullParser();
        parser.setInput(reader);
        rootElementFound = false;
    }

    protected int nextParseEvent() throws Exception {
        return parser.next();
    }

    protected String getElementName() {
        String name = parser.getName();
        if ("subsonic-response".equals(name)) {
            rootElementFound = true;
            String version = get("version");
            if (version != null) {
                Util.setServerRestVersion(context, new Version(version));
            }
        }
        return name;
    }

    protected void validate() throws Exception {
        if (!rootElementFound) {
            throw new Exception(context.getResources().getString(R.string.background_task_parse_error));
        }
    }
}