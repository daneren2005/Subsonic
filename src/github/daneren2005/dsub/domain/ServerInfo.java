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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.domain;

/**
 * Information about the Subsonic server.
 *
 * @author Sindre Mehus
 */
public class ServerInfo implements Serializable {
	public static final int TYPE_SUBSONIC = 1;
	public static final int TYPE_MADSONIC = 2;
	
    private boolean isLicenseValid;
    private Version restVersion;
	private int type;
	
	public ServerInfo() {
		type = TYPE_SUBSONIC;
	}

    public boolean isLicenseValid() {
        return isLicenseValid;
    }

    public void setLicenseValid(boolean licenseValid) {
        isLicenseValid = licenseValid;
    }

    public Version getRestVersion() {
        return restVersion;
    }

    public void setRestVersion(Version restVersion) {
        this.restVersion = restVersion;
    }
    
    public int getRestType() {
    	return type;
    }
    public void setRestType(int type) {
    	this.type = type;
    }
    
    public boolean isStockSubsonic() {
    	return type == TYPE_SUBSONIC;
    }
    public boolean isMadsonic() {
    	return type == TYPE_MADSONIC;
    }
    
    @Override
    public boolean equals(Object o) {
    	if(this == 0) {
    		return true;
    	} else if(o == null || getClass() != o.getClass()) {
    		return false;
    	}
    	
    	final ServerInfo info = (ServerInfo) o;
    	
    	if(this.type != info.type) {
    		return false;
    	} else if(this.restVersion == null || info.restVersion == null) {
    		// Should never be null unless just starting up
    		return false;
    	} else {
    		return this.restVersion.equals(info.restVersion);
    	}
    }
}
