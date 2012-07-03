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

import net.sourceforge.subsonic.androidapp.domain.Version;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sindre Mehus
 */
public class VersionParser {

    public Version parse(Reader reader) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(reader);
        Pattern pattern = Pattern.compile("SUBSONIC_ANDROID_VERSION_BEGIN(.*)SUBSONIC_ANDROID_VERSION_END");
        String line = bufferedReader.readLine();
        while (line != null) {
            Matcher finalMatcher = pattern.matcher(line);
            if (finalMatcher.find()) {
                return new Version(finalMatcher.group(1));
            }
            line = bufferedReader.readLine();
        }
        return null;
    }
}