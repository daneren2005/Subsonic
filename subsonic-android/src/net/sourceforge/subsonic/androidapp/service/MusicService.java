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
package net.sourceforge.subsonic.androidapp.service;

import java.util.List;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import net.sourceforge.subsonic.androidapp.domain.Indexes;
import net.sourceforge.subsonic.androidapp.domain.JukeboxStatus;
import net.sourceforge.subsonic.androidapp.domain.Lyrics;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.domain.MusicFolder;
import net.sourceforge.subsonic.androidapp.domain.Playlist;
import net.sourceforge.subsonic.androidapp.domain.SearchCritera;
import net.sourceforge.subsonic.androidapp.domain.SearchResult;
import net.sourceforge.subsonic.androidapp.domain.Version;
import net.sourceforge.subsonic.androidapp.util.CancellableTask;
import net.sourceforge.subsonic.androidapp.util.ProgressListener;

/**
 * @author Sindre Mehus
 */
public interface MusicService {

    void ping(Context context, ProgressListener progressListener) throws Exception;

    boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception;

    List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getPlaylist(String id, Context context, ProgressListener progressListener) throws Exception;

    List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception;

    Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception;

    void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception;

    Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, boolean saveToFile, ProgressListener progressListener) throws Exception;

    HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception;

    Version getLocalVersion(Context context) throws Exception;

    Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception;

    String getVideoUrl(Context context, String id);

    JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception;
}