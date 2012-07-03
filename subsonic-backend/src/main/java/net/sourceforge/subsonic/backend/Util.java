package net.sourceforge.subsonic.backend;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;/*
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

 Copyright 2010 (C) Sindre Mehus
 */

/**
 * @author Sindre Mehus
 */
public class Util {

    private static final File BACKEND_HOME = new File("/var/subsonic-backend");
    private static final Logger LOG = Logger.getLogger(Util.class);

    private Util() {
    }

    public static synchronized File getBackendHome() {
        if (!BACKEND_HOME.exists() || !BACKEND_HOME.isDirectory()) {
            boolean success = BACKEND_HOME.mkdirs();
            if (!success) {
                String message = "The directory " + BACKEND_HOME + " does not exist. Please create it and make it writable.";
                LOG.error(message);
                throw new RuntimeException(message);
            }
        }
        return BACKEND_HOME;
    }

    public static String getPassword(String filename) throws IOException {
        File pwdFile = new File(getBackendHome(), filename);
        Reader reader = new FileReader(pwdFile);
        try {
            return StringUtils.trimToNull(IOUtils.toString(reader));
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
