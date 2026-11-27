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

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses the openSubsonicExtensions response of the OpenSubsonic
 * getOpenSubsonicExtensions endpoint.
 */
public class OpenSubsonicExtensionsParser extends AbstractParser {

    private static final String EXTENSION = "openSubsonicExtension";

    public OpenSubsonicExtensionsParser(Context context, int instance) {
        super(context, instance);
    }

    public Set<String> parse(Reader reader) throws Exception {
        init(reader);

        Set<String> extensions = new HashSet<String>();
        int eventType;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                // Nextcloud Music currently emits the extension elements with the
                // plural wrapper name, so accept both spellings here.
                if (EXTENSION.equals(name) || "openSubsonicExtensions".equals(name)) {
                    String extensionName = get("name");
                    if (extensionName != null) {
                        extensions.add(extensionName);
                    }
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        return extensions;
    }
}
