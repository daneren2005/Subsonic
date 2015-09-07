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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Sindre Mehus
 */
public class Playlist implements Serializable {

    private String id;
    private String name;
	private String owner;
	private String comment;
	private String songCount;
	private Boolean pub;
	private Date created;
	private Date changed;
	private Integer duration;

	public Playlist() {

	}
    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
    }
	public Playlist(String id, String name, String owner, String comment, String songCount, String pub, String created, String changed, Integer duration) {
        this.id = id;
        this.name = name;
		this.owner = (owner == null) ? "" : owner;
		this.comment = (comment == null) ? "" : comment;
		this.songCount = (songCount == null) ? "" : songCount;
		this.pub = (pub == null) ? null : (pub.equals("true"));
		setCreated(created);
		setChanged(changed);
		this.duration = duration;
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
	
	public Boolean getPublic() {
		return this.pub;
	}
	public void setPublic(Boolean pub) {
		this.pub = pub;
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

	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		} else if(o == null) {
			return false;
		} else if(o instanceof String) {
			return o.equals(this.id);
		} else if(o.getClass() != getClass()) {
			return false;
		}
		
		Playlist playlist = (Playlist) o;
		return playlist.id.equals(this.id);
	}

	public static class PlaylistComparator implements Comparator<Playlist> {
		@Override
		public int compare(Playlist playlist1, Playlist playlist2) {
			return playlist1.getName().compareToIgnoreCase(playlist2.getName());
		}

		public static List<Playlist> sort(List<Playlist> playlists) {
			Collections.sort(playlists, new PlaylistComparator());
			return playlists;
		}
	}
}
