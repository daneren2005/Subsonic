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

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

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
	private String coverArt;
	
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

	public String getCoverArt() {
		return coverArt;
	}
	public void setCoverArt(String coverArt) {
		this.coverArt = coverArt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		PodcastChannel entry = (PodcastChannel) o;
		return id.equals(entry.id);
	}
	
	public static class PodcastComparator implements Comparator<PodcastChannel> {
		private static String[] ignoredArticles;

		@Override
		public int compare(PodcastChannel podcast1, PodcastChannel podcast2) {
			String lhs = podcast1.getName();
			String rhs = podcast2.getName();
			if(lhs == null && rhs == null) {
				return 0;
			} else if(lhs == null) {
				return 1;
			} else if(rhs == null) {
				return -1;
			}
			
			lhs = lhs.toLowerCase();
			rhs = rhs.toLowerCase();

			for(String article: ignoredArticles) {
				int index = lhs.indexOf(article.toLowerCase() + " ");
				if(index == 0) {
					lhs = lhs.substring(article.length() + 1);
				}
				index = rhs.indexOf(article.toLowerCase() + " ");
				if(index == 0) {
					rhs = rhs.substring(article.length() + 1);
				}
			}

			return lhs.compareToIgnoreCase(rhs);
		}

		public static List<PodcastChannel> sort(List<PodcastChannel> podcasts, Context context) {
			SharedPreferences prefs = Util.getPreferences(context);
			String ignoredArticlesString = prefs.getString(Constants.CACHE_KEY_IGNORE, "The El La Los Las Le Les");
			ignoredArticles = ignoredArticlesString.split(" ");

			Collections.sort(podcasts, new PodcastComparator());
			return podcasts;
		}

	}
}
