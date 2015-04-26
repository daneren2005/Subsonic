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

import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Share implements Serializable {
	private String id;
    private String url;
    private String description;
    private String username;
    private Date created;
    private Date lastVisited;
    private Date expires;
    private Long visitCount;
    private List<Entry> entries;
    
    public Share() {
    	entries = new ArrayList<Entry>();
    }

	public String getName() {
		if(description != null && !"".equals(description)) {
			return description;
		} else {
			return url.replaceFirst(".*/([^/?]+).*", "$1");
		}
	}
	
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
    
    public Date getLastVisited() {
        return lastVisited;
    }

    public void setLastVisited(String lastVisited) {
    	if (lastVisited != null) {
    		try {
				this.lastVisited = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).parse(lastVisited);
			} catch (ParseException e) { 
				this.lastVisited = null;
			}
    	} else {
    		this.lastVisited = null;
    	}
    }
	public void setLastVisited(Date lastVisited) {
		this.lastVisited = lastVisited;
	}
    
    public Date getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
    	if (expires != null) {
    		try {
				this.expires = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).parse(expires);
			} catch (ParseException e) { 
				this.expires = null;
			}
    	} else {
    		this.expires = null;
    	}
    }
	public void setExpires(Date expires) {
		this.expires = expires;
	}

    public Long getVisitCount() {
    	return visitCount;
    }
    
    public void setVisitCount(Long visitCount) {
    	this.visitCount = visitCount;
    }

	public MusicDirectory getMusicDirectory() {
		MusicDirectory dir = new MusicDirectory();
		dir.addChildren(entries);
		dir.setId(getId());
		dir.setName(getName());
		return dir;
	}

    public List<Entry> getEntries() {
    	return this.entries;
    }
    
    public void addEntry(Entry entry) {
		entries.add(entry);
    }
 }
