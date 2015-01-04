/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.domain;

import java.io.Serializable;
import java.util.List;

public class ArtistInfo implements Serializable {
	private String biography;
	private String musicBrainzId;
	private String lastFMUrl;
	private String imageUrl;
	private List<Artist> similarArtists;
	private List<String> missingArtists;

	public String getBiography() {
		return biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getMusicBrainzId() {
		return musicBrainzId;
	}

	public void setMusicBrainzId(String musicBrainzId) {
		this.musicBrainzId = musicBrainzId;
	}

	public String getLastFMUrl() {
		return lastFMUrl;
	}

	public void setLastFMUrl(String lastFMUrl) {
		this.lastFMUrl = lastFMUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public List<Artist> getSimilarArtists() {
		return similarArtists;
	}

	public void setSimilarArtists(List<Artist> similarArtists) {
		this.similarArtists = similarArtists;
	}

	public List<String> getMissingArtists() {
		return missingArtists;
	}

	public void setMissingArtists(List<String> missingArtists) {
		this.missingArtists = missingArtists;
	}
}
