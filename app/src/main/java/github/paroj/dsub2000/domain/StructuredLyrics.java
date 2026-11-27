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
 */
package github.paroj.dsub2000.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One set of song lyrics as returned by the OpenSubsonic getLyricsBySongId
 * endpoint.  A song may have several sets, for instance a synced and an
 * unsynced one, or one per language.
 */
public class StructuredLyrics implements Serializable {

	private String displayArtist;
	private String displayTitle;
	private String lang;
	private Long offset;
	private boolean synced;
	private List<LyricsLine> lines = new ArrayList<LyricsLine>();

	public String getDisplayArtist() {
		return displayArtist;
	}

	public void setDisplayArtist(String displayArtist) {
		this.displayArtist = displayArtist;
	}

	public String getDisplayTitle() {
		return displayTitle;
	}

	public void setDisplayTitle(String displayTitle) {
		this.displayTitle = displayTitle;
	}

	/**
	 * ISO 639 language code of these lyrics, "xxx" when unknown.
	 */
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * Offset in milliseconds to add to every line's start time.
	 */
	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	/**
	 * Whether the lines carry timestamps.
	 */
	public boolean isSynced() {
		return synced;
	}

	public void setSynced(boolean synced) {
		this.synced = synced;
	}

	public List<LyricsLine> getLines() {
		return lines;
	}

	public void setLines(List<LyricsLine> lines) {
		this.lines = lines;
	}

	public void addLine(LyricsLine line) {
		this.lines.add(line);
	}
}
