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
package net.sourceforge.subsonic.domain;

import net.sourceforge.subsonic.util.StringUtil;

/**
 * A Podcast channel. Each channel contain several episodes.
 *
 * @author Sindre Mehus
 * @see PodcastEpisode
 */
public class PodcastChannel {

    private Integer id;
    private String url;
    private String title;
    private String description;
    private PodcastStatus status;
    private String errorMessage;

    public PodcastChannel(Integer id, String url, String title, String description,
                          PodcastStatus status, String errorMessage) {
        this.id = id;
        this.url = url;
        this.title = StringUtil.removeMarkup(title);
        this.description = StringUtil.removeMarkup(description);
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public PodcastChannel(String url) {
        this.url = url;
        status = PodcastStatus.NEW;
    }

    public Integer getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PodcastStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}