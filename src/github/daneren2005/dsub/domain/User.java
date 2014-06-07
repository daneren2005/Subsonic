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

public class User implements Serializable {
	private String username;
	private String password;
	private String email;
	private Boolean scrobblingEnabled;

	private Boolean adminRole;
	private Boolean settingsRole;
	private Boolean downloadRole;
	private Boolean uploadRole;
	private Boolean playlistRole;
	private Boolean coverArtRole;
	private Boolean commentRole;
	private Boolean podcastRole;
	private Boolean streamRole;
	private Boolean jukeboxRole;
	private Boolean shareRole;

	public User() {

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getScrobblingEnabled() {
		return scrobblingEnabled;
	}

	public void setScrobblingEnabled(Boolean scrobblingEnabled) {
		this.scrobblingEnabled = scrobblingEnabled;
	}

	public Boolean getAdminRole() {
		return adminRole;
	}

	public void setAdminRole(Boolean adminRole) {
		this.adminRole = adminRole;
	}

	public Boolean getSettingsRole() {
		return settingsRole;
	}

	public void setSettingsRole(Boolean settingsRole) {
		this.settingsRole = settingsRole;
	}

	public Boolean getDownloadRole() {
		return downloadRole;
	}

	public void setDownloadRole(Boolean downloadRole) {
		this.downloadRole = downloadRole;
	}

	public Boolean getUploadRole() {
		return uploadRole;
	}

	public void setUploadRole(Boolean uploadRole) {
		this.uploadRole = uploadRole;
	}

	public Boolean getPlaylistRole() {
		return playlistRole;
	}

	public void setPlaylistRole(Boolean playlistRole) {
		this.playlistRole = playlistRole;
	}

	public Boolean getCoverArtRole() {
		return coverArtRole;
	}

	public void setCoverArtRole(Boolean coverArtRole) {
		this.coverArtRole = coverArtRole;
	}

	public Boolean getCommentRole() {
		return commentRole;
	}

	public void setCommentRole(Boolean commentRole) {
		this.commentRole = commentRole;
	}

	public Boolean getPodcastRole() {
		return podcastRole;
	}

	public void setPodcastRole(Boolean podcastRole) {
		this.podcastRole = podcastRole;
	}

	public Boolean getStreamRole() {
		return streamRole;
	}

	public void setStreamRole(Boolean streamRole) {
		this.streamRole = streamRole;
	}

	public Boolean getJukeboxRole() {
		return jukeboxRole;
	}

	public void setJukeboxRole(Boolean jukeboxRole) {
		this.jukeboxRole = jukeboxRole;
	}

	public Boolean getShareRole() {
		return shareRole;
	}

	public void setShareRole(Boolean shareRole) {
		this.shareRole = shareRole;
	}
}
