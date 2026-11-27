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
package github.paroj.dsub2000.service;

import android.content.Context;
import github.paroj.dsub2000.util.Util;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class MusicServiceFactory {

    private static final MusicService REST_MUSIC_SERVICE = new CachedMusicService(new RESTMusicService());
    private static final MusicService OFFLINE_MUSIC_SERVICE = new OfflineMusicService();

    public static MusicService getMusicService(Context context) {
        return getMusicService(context, false);
    }

    /**
     * @param forceOnline use the server service even while the app is in offline mode,
     *                    for operations that explicitly talk to the server
     */
    public static MusicService getMusicService(Context context, boolean forceOnline) {
        return !forceOnline && Util.isOffline(context) ? OFFLINE_MUSIC_SERVICE : REST_MUSIC_SERVICE;
    }
}
