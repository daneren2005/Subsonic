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
	
	Copyright 2009 (C) Sindre Mehus
*/

package github.daneren2005.dsub.domain;

public enum RemoteControlState {
	LOCAL(0),
	JUKEBOX_SERVER(1),
	CHROMECAST(2),
	REMOTE_CLIENT(3),
	DLNA(4);
	
	private final int mRemoteControlState;
	
	private RemoteControlState(int value) {
		mRemoteControlState = value;
	}
	
	public int getValue() {
		return mRemoteControlState;
	}
}
