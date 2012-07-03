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
package net.sourceforge.subsonic.androidapp.service;

import net.sourceforge.subsonic.androidapp.domain.Version;

/**
 * Thrown if the REST API version implemented by the server is too old.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class ServerTooOldException extends Exception {

    private final String text;
    private final Version serverVersion;
    private final Version requiredVersion;

    public ServerTooOldException(String text, Version serverVersion, Version requiredVersion) {
        this.text = text;
        this.serverVersion = serverVersion;
        this.requiredVersion = requiredVersion;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (text != null) {
            builder.append(text).append(" ");
        }
        builder.append("Server API version too old. ");
        builder.append("Requires ").append(requiredVersion).append(" but is ").append(serverVersion).append(".");
        return builder.toString();
    }
}
