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
package net.sourceforge.subsonic.service.metadata;

import java.io.File;

import net.sourceforge.subsonic.domain.MediaFile;

/**
 * Parses meta data by guessing artist, album and song title based on the path of the file.
 *
 * @author Sindre Mehus
 */
public class DefaultMetaDataParser extends MetaDataParser {

    /**
     * Parses meta data for the given file. No guessing or reformatting is done.
     *
     * @param file The file to parse.
     * @return Meta data for the file.
     */
    public MetaData getRawMetaData(File file) {
        MetaData metaData = new MetaData();
        metaData.setArtist(guessArtist(file));
        metaData.setAlbumName(guessAlbum(file, metaData.getArtist()));
        metaData.setTitle(guessTitle(file));
        return metaData;
    }

    /**
     * Updates the given file with the given meta data.
     * This method has no effect.
     *
     * @param file     The file to update.
     * @param metaData The new meta data.
     */
    public void setMetaData(MediaFile file, MetaData metaData) {
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    public boolean isEditingSupported() {
        return false;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file The file in question.
     * @return Whether this parser is applicable to the given file.
     */
    public boolean isApplicable(File file) {
        return file.isFile();
    }
}