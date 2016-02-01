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

import java.util.regex.Pattern;

/**
 * The criteria for a music search.
 *
 * @author Sindre Mehus
 */
public class SearchCritera {

	private final String query;
	private final int artistCount;
	private final int albumCount;
	private final int songCount;
	private Pattern pattern;

	public SearchCritera(String query, int artistCount, int albumCount, int songCount) {
		this.query = query;
		this.artistCount = artistCount;
		this.albumCount = albumCount;
		this.songCount = songCount;
	}

	public String getQuery() {
		return query;
	}

	public int getArtistCount() {
		return artistCount;
	}

	public int getAlbumCount() {
		return albumCount;
	}

	public int getSongCount() {
		return songCount;
	}

	/**
	 * Returns and caches a pattern instance that can be used to check if a
	 * string matches the query.
	 */
	public Pattern getPattern() {

		// If the pattern wasn't already cached, create a new regular expression
		// from the search string :
		//  * Surround the search string with ".*" (match anything)
		//  * Replace spaces and wildcard '*' characters with ".*"
		//  * All other characters are properly quoted
		if (this.pattern == null) {
			String regex = ".*";
			String currentPart = "";
			for (int i = 0; i < query.length(); i++) {
				char c = query.charAt(i);
				if (c == '*' || c == ' ') {
					regex += Pattern.quote(currentPart);
					regex += ".*";
					currentPart = "";
				} else {
					currentPart += c;
				}
			}
			if (currentPart.length() > 0) {
				regex += Pattern.quote(currentPart);
			}

			regex += ".*";
			this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		}

		return this.pattern;
	}
}
