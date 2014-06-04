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

import android.media.RemoteControlClient;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public enum PlayerState {
	IDLE(RemoteControlClient.PLAYSTATE_STOPPED),
    DOWNLOADING(RemoteControlClient.PLAYSTATE_BUFFERING),
    PREPARING(RemoteControlClient.PLAYSTATE_BUFFERING),
    PREPARED(RemoteControlClient.PLAYSTATE_STOPPED),
    STARTED(RemoteControlClient.PLAYSTATE_PLAYING),
    STOPPED(RemoteControlClient.PLAYSTATE_STOPPED),
    PAUSED(RemoteControlClient.PLAYSTATE_PAUSED),
	PAUSED_TEMP(RemoteControlClient.PLAYSTATE_PAUSED),
    COMPLETED(RemoteControlClient.PLAYSTATE_STOPPED);
    
    private final int mRemoteControlClientPlayState;
    
    private PlayerState(int playState) {
    	mRemoteControlClientPlayState = playState;
    }
    
    public int getRemoteControlClientPlayState() {
    	return mRemoteControlClientPlayState;
    }
}
