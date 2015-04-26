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

 Copyright 2013 (C) Scott Jackson
 */
package github.daneren2005.dsub.domain;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Scott on 11/4/13.
 */
public class Bookmark implements Serializable {
	private int position;
	private String username;
	private String comment;
	private Date created;
	private Date changed;

	public Bookmark() {

	}
	public Bookmark(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(String created) {
		if (created != null) {
			try {
				this.created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).parse(created);
			} catch (ParseException e) { 
				this.created = null;
			}
		} else {
			this.created = null;
		}
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public Date getChanged() {
		return changed;
	}
	
	public void setChanged(String changed) {
		if (changed != null) {
			try {
				this.changed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).parse(changed);
			} catch (ParseException e) { 
				this.changed = null;
			}
		} else {
			this.changed = null;
		}
	}
	public void setChanged(Date changed) {
		this.changed = changed;
	}
}
