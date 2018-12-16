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
package github.vrih.xsub.service;

import android.content.Context;
import github.vrih.xsub.util.Util;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class MusicServiceFactory {

    private static final MusicService REST_MUSIC_SERVICE = new CachedMusicService(new RESTMusicService());
    private static final MusicService OFFLINE_MUSIC_SERVICE = new OfflineMusicService();

    public static MusicService getMusicService(Context context) {
        return Util.isOffline(context) ? OFFLINE_MUSIC_SERVICE : REST_MUSIC_SERVICE;
    }
}
