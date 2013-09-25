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
 * @author Sindre Mehus
 */
public class Playlist implements Serializable {

    private String id;
    private String name;
	private String owner;
	private String comment;
	private String songCount;
	private String created;
	private Boolean pub;

	public Playlist() {

	}
    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
    }
	public Playlist(String id, String name, String owner, String comment, String songCount, String created, String pub) {
        this.id = id;
        this.name = name;
		this.owner = (owner == null) ? "" : owner;
		this.comment = (comment == null) ? "" : comment;
		this.songCount = (songCount == null) ? "" : songCount;
		this.created = (created == null) ? "" : created;
		this.pub = (pub == null) ? null : (pub.equals("true"));
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
	
	public String getOwner() {
		return this.owner;
	}
	
	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getSongCount() {
		return this.songCount;
	}
	
	public void setSongCount(String songCount) {
		this.songCount = songCount;
	}
	
	public String getCreated() {
		return this.created;
	}
	
	public void setCreated(String created) {
		this.created = created;
	}
	
	public Boolean getPublic() {
		return this.pub;
	}
	public void setPublic(Boolean pub) {
		this.pub = pub;
	}

    @Override
    public String toString() {
        return name;
    }
}