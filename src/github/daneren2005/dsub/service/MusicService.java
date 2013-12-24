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
package github.daneren2005.dsub.service;

import java.util.List;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;

import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.ChatMessage;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.RemoteStatus;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.domain.Version;
import github.daneren2005.dsub.util.CancellableTask;
import github.daneren2005.dsub.util.ProgressListener;

/**
 * @author Sindre Mehus
 */
public interface MusicService {

    void ping(Context context, ProgressListener progressListener) throws Exception;

    boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception;

    List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getMusicDirectory(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getStarredList(Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getPlaylist(boolean refresh, String id, String name, Context context, ProgressListener progressListener) throws Exception;

    List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception;
	
	void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception;
	
	void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception;
	
	void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception;
	
	void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception;

    Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception;

    void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getAlbumList(String type, String extra, int size, int offset, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getRandomSongs(int size, String folder, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception;

    Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception;

    HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception;

    Version getLocalVersion(Context context) throws Exception;

    Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception;

    String getVideoUrl(int maxBitrate, Context context, String id);
	
	String getVideoStreamUrl(String format, int Bitrate, Context context, String id) throws Exception;
	
	String getHlsUrl(String id, int bitRate, Context context) throws Exception;

    RemoteStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception;

    RemoteStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception;

    RemoteStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception;

    RemoteStatus startJukebox(Context context, ProgressListener progressListener) throws Exception;

    RemoteStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception;

    RemoteStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception;
    
    void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception;
	
	List<Share> getShares(Context context, ProgressListener progressListener) throws Exception;
    
    List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception;
    
    void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception;
	
	List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception;
	
	MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception;
	
	List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception;
	
	MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception;
	
	void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception;
	
	void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception;
	
	void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception;

	void setRating(String id, int rating, Context context, ProgressListener progressListener) throws Exception;

	List<Bookmark> getBookmarks(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

	void createBookmark(String id, int position, String comment, Context context, ProgressListener progressListener) throws Exception;

	void deleteBookmark(String id, Context context, ProgressListener progressListener) throws Exception;
	
	int processOfflineSyncs(final Context context, final ProgressListener progressListener) throws Exception;
	
	void setInstance(Integer instance) throws Exception;
}
