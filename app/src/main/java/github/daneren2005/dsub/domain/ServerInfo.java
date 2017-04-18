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

import android.content.Context;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

/**
 * Information about the Subsonic server.
 *
 * @author Sindre Mehus
 */
public class ServerInfo implements Serializable {
	public static final int TYPE_SUBSONIC = 1;
	public static final int TYPE_MADSONIC = 2;
	public static final int TYPE_AMPACHE = 3;
	private static final Map<Integer, ServerInfo> SERVERS = new ConcurrentHashMap<Integer, ServerInfo>();
	
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
		if(this == o) {
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

	// Stub to make sure this is never used, too easy to screw up
	private void saveServerInfo(Context context) {

	}
	public void saveServerInfo(Context context, int instance) {
		ServerInfo current = SERVERS.get(instance);
		if(!this.equals(current)) {
			SERVERS.put(instance, this);
			FileUtil.serialize(context, this, getCacheName(context, instance));
		}
	}
	
	public static ServerInfo getServerInfo(Context context) {
		return getServerInfo(context, Util.getActiveServer(context));
	}
	public static ServerInfo getServerInfo(Context context, int instance) {
		ServerInfo current = SERVERS.get(instance);
		if(current != null) {
			return current;
		}
		
		current = FileUtil.deserialize(context, getCacheName(context, instance), ServerInfo.class);
		if(current != null) {
			SERVERS.put(instance, current);
		}
		
		return current;
	}

	public static Version getServerVersion(Context context) {
		return getServerVersion(context, Util.getActiveServer(context));
	}
	public static Version getServerVersion(Context context, int instance) {
		ServerInfo server = getServerInfo(context, instance);
		if(server == null) {
			return null;
		}

		return server.getRestVersion();
	}

	public static boolean checkServerVersion(Context context, String requiredVersion) {
		return checkServerVersion(context, requiredVersion, Util.getActiveServer(context));
	}
	public static boolean checkServerVersion(Context context, String requiredVersion, int instance) {
		ServerInfo server = getServerInfo(context, instance);
		if(server == null) {
			return false;
		}
		
		Version version = server.getRestVersion();
		if(version == null) {
			return false;
		}
		
		Version required = new Version(requiredVersion);
		return version.compareTo(required) >= 0;
	}

	public static int getServerType(Context context) {
		return getServerType(context, Util.getActiveServer(context));
	}
	public static int getServerType(Context context, int instance) {
		if(Util.isOffline(context)) {
			return 0;
		}

		ServerInfo server = getServerInfo(context, instance);
		if(server == null) {
			return 0;
		}

		return server.getRestType();
	}

	public static boolean isStockSubsonic(Context context) {
		return isStockSubsonic(context, Util.getActiveServer(context));
	}
	public static boolean isStockSubsonic(Context context, int instance) {
		return getServerType(context, instance) == TYPE_SUBSONIC;
	}

	public static boolean isMadsonic(Context context) {
		return isMadsonic(context, Util.getActiveServer(context));
	}
	public static boolean isMadsonic(Context context, int instance) {
		return getServerType(context, instance) == TYPE_MADSONIC;
	}
	public static boolean isMadsonic6(Context context) {
		return isMadsonic6(context, Util.getActiveServer(context));
	}
	public static boolean isMadsonic6(Context context, int instance) {
		return getServerType(context, instance) == TYPE_MADSONIC && checkServerVersion(context, "2.0", instance);
	}

	public static boolean isAmpache(Context context) {
		return isAmpache(context, Util.getActiveServer(context));
	}
	public static boolean isAmpache(Context context, int instance) {
		return getServerType(context, instance) == TYPE_AMPACHE;
	}
	
	private static String getCacheName(Context context, int instance) {
		return "server-" + Util.getRestUrl(context, null, instance, false).hashCode() + ".ser";
	}

	public static boolean hasArtistInfo(Context context) {
		if(!isMadsonic(context) && ServerInfo.checkServerVersion(context, "1.11")) {
			return true;
		} else if(isMadsonic(context)) {
			return checkServerVersion(context, "2.0");
		} else {
			return false;
		}
	}
	
	public static boolean canBookmark(Context context) {
		return checkServerVersion(context, "1.9");
	}
	public static boolean canInternetRadio(Context context) {
		return checkServerVersion(context, "1.9");
	}

	public static boolean canSavePlayQueue(Context context) {
		return ServerInfo.checkServerVersion(context, "1.12") && (!ServerInfo.isMadsonic(context) || checkServerVersion(context, "2.0"));
	}

	public static boolean canAlbumListPerFolder(Context context) {
		return ServerInfo.checkServerVersion(context, "1.11") && (!ServerInfo.isMadsonic(context) || checkServerVersion(context, "2.0")) && !Util.isTagBrowsing(context);
	}
	public static boolean hasTopSongs(Context context) {
		return ServerInfo.isMadsonic(context) || ServerInfo.checkServerVersion(context, "1.13");
	}

	public static boolean canUseToken(Context context) {
		return canUseToken(context, Util.getActiveServer(context));
	}
	public static boolean canUseToken(Context context, int instance) {
		if(isStockSubsonic(context, instance) && checkServerVersion(context, "1.14", instance)) {
			if(Util.getBlockTokenUse(context, instance)) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
	public static boolean hasSimilarArtists(Context context) {
		return !ServerInfo.isMadsonic(context) || ServerInfo.checkServerVersion(context, "2.0");
	}
	public static boolean hasNewestPodcastEpisodes(Context context) {
		return ServerInfo.checkServerVersion(context, "1.13");
	}

	public static boolean canRescanServer(Context context) {
		return ServerInfo.isMadsonic(context) ||
			(ServerInfo.isStockSubsonic(context) && ServerInfo.checkServerVersion(context, "1.15"));
	}
}
