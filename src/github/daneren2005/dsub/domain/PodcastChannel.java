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
package github.daneren2005.dsub.domain;

import java.io.Serializable;

/**
 *
 * @author Scott
 */
public class PodcastChannel implements Serializable {
	private String id;
	private String name;
	private String url;
	private String description;
	private String status;
	private String errorMessage;
	
	public PodcastChannel() {
		
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public static class PodcastComparator implements Comparator<PodcastChannel> {
		@Override
		public int compare(PodcastChannel podcast1, PodcastChannel podcast2) {
			return podcast1.getName().compareToIgnoreCase(podcast2.getName());
		}

		public static List<PodcastChannel> sort(List<PodcastChannel> podcasts) {
			Collections.sort(podcasts, new PodcastComparator());
			return podcasts;
		}

	}
}
