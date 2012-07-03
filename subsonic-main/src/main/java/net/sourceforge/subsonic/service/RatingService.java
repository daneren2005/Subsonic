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
package net.sourceforge.subsonic.service;

import net.sourceforge.subsonic.dao.*;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.util.FileUtil;

import java.util.*;
import java.io.File;

/**
 * Provides services for user ratings.
 *
 * @author Sindre Mehus
 */
public class RatingService {

    private RatingDao ratingDao;
    private SecurityService securityService;
    private MediaFileService mediaFileService;

    /**
     * Returns the highest rated music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of files to return.
     * @return The highest rated music files.
     */
    public List<MediaFile> getHighestRated(int offset, int count) {
        List<String> highestRated = ratingDao.getHighestRated(offset, count);
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (String path : highestRated) {
            File file = new File(path);
            if (FileUtil.exists(file) && securityService.isReadAllowed(file)) {
                result.add(mediaFileService.getMediaFile(path));
            }
        }
        return result;
    }

    /**
     * Sets the rating for a music file and a given user.
     *
     * @param username  The user name.
     * @param mediaFile The music file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        ratingDao.setRatingForUser(username, mediaFile, rating);
    }

    /**
     * Returns the average rating for the given music file.
     *
     * @param mediaFile The music file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        return ratingDao.getAverageRating(mediaFile);
    }

    /**
     * Returns the rating for the given user and music file.
     *
     * @param username  The user name.
     * @param mediaFile The music file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        return ratingDao.getRatingForUser(username, mediaFile);
    }

    public void setRatingDao(RatingDao ratingDao) {
        this.ratingDao = ratingDao;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
