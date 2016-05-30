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

import android.util.Log;

import java.io.Serializable;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author Sindre Mehus
 */
public class Artist implements Serializable {
	private static final String TAG = Artist.class.getSimpleName();
	public static final String ROOT_ID = "-1";
	public static final String MISSING_ID = "-2";

    private String id;
    private String name;
    private String index;
	private boolean starred;
	private Integer rating;
	private int closeness;

	public Artist() {

	}
	public Artist(String id, String name) {
		this.id = id;
		this.name = name;
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

    public String getIndex() {
        return index;
    }
    public void setIndex(String index) {
        this.index = index;
    }
	
	public boolean isStarred() {
		return starred;
	}
	public void setStarred(boolean starred) {
		this.starred = starred;
	}

	public int getRating() {
		return rating == null ? 0 : rating;
	}
	public void setRating(Integer rating) {
		if(rating == null || rating == 0) {
			this.rating = null;
		} else {
			this.rating = rating;
		}
	}
	
	public int getCloseness() {
		return closeness;
	}
	public void setCloseness(int closeness) {
		this.closeness = closeness;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Artist entry = (Artist) o;
		return id.equals(entry.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

    @Override
    public String toString() {
        return name;
    }

	public static class ArtistComparator implements Comparator<Artist> {
		private String[] ignoredArticles;
		private Collator collator;

		public ArtistComparator(String[] ignoredArticles) {
			this.ignoredArticles = ignoredArticles;
			this.collator = Collator.getInstance(Locale.US);
			this.collator.setStrength(Collator.PRIMARY);
		}

		public int compare(Artist lhsArtist, Artist rhsArtist) {
			String lhs = lhsArtist.getName().toLowerCase();
			String rhs = rhsArtist.getName().toLowerCase();

			for (String article : ignoredArticles) {
				int index = lhs.indexOf(article.toLowerCase() + " ");
				if (index == 0) {
					lhs = lhs.substring(article.length() + 1);
				}
				index = rhs.indexOf(article.toLowerCase() + " ");
				if (index == 0) {
					rhs = rhs.substring(article.length() + 1);
				}
			}

			return collator.compare(lhs, rhs);
		}
	}

	public static void sort(List<Artist> artists, String[] ignoredArticles) {
		try {
			Collections.sort(artists, new ArtistComparator(ignoredArticles));
		} catch (Exception e) {
			Log.w(TAG, "Failed to sort artists", e);
		}
	}
}