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

import android.content.Context;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;

import net.sourceforge.subsonic.androidapp.domain.ServerInfo;
import net.sourceforge.subsonic.androidapp.domain.Version;

/**
 * @author Sindre Mehus
 */
public class LicenseParser extends AbstractParser {

    public LicenseParser(Context context) {
        super(context);
    }

    public ServerInfo parse(Reader reader) throws Exception {

        init(reader);

        ServerInfo serverInfo = new ServerInfo();
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("subsonic-response".equals(name)) {
                    serverInfo.setRestVersion(new Version(get("version")));
                } else if ("license".equals(name)) {
                    serverInfo.setLicenseValid(getBoolean("valid"));
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        return serverInfo;
    }
}