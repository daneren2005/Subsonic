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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.*;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.service.parser.EntryListParser;
import github.daneren2005.dsub.service.parser.ArtistInfoParser;
import github.daneren2005.dsub.service.parser.BookmarkParser;
import github.daneren2005.dsub.service.parser.ChatMessageParser;
import github.daneren2005.dsub.service.parser.ErrorParser;
import github.daneren2005.dsub.service.parser.GenreParser;
import github.daneren2005.dsub.service.parser.IndexesParser;
import github.daneren2005.dsub.service.parser.InternetRadioStationParser;
import github.daneren2005.dsub.service.parser.JukeboxStatusParser;
import github.daneren2005.dsub.service.parser.LicenseParser;
import github.daneren2005.dsub.service.parser.LyricsParser;
import github.daneren2005.dsub.service.parser.MusicDirectoryParser;
import github.daneren2005.dsub.service.parser.MusicFoldersParser;
import github.daneren2005.dsub.service.parser.PlayQueueParser;
import github.daneren2005.dsub.service.parser.PlaylistParser;
import github.daneren2005.dsub.service.parser.PlaylistsParser;
import github.daneren2005.dsub.service.parser.PodcastChannelParser;
import github.daneren2005.dsub.service.parser.PodcastEntryParser;
import github.daneren2005.dsub.service.parser.RandomSongsParser;
import github.daneren2005.dsub.service.parser.ScanStatusParser;
import github.daneren2005.dsub.service.parser.SearchResult2Parser;
import github.daneren2005.dsub.service.parser.SearchResultParser;
import github.daneren2005.dsub.service.parser.ShareParser;
import github.daneren2005.dsub.service.parser.StarredListParser;
import github.daneren2005.dsub.service.parser.TopSongsParser;
import github.daneren2005.dsub.service.parser.UserParser;
import github.daneren2005.dsub.service.parser.VideosParser;
import github.daneren2005.dsub.util.KeyStoreUtil;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SongDBHandler;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.util.compat.GoogleCompat;

import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RESTMusicService implements MusicService {

    private static final String TAG = RESTMusicService.class.getSimpleName();

    private static final int SOCKET_READ_TIMEOUT_DEFAULT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DOWNLOAD = 30 * 1000;
    private static final int SOCKET_READ_TIMEOUT_GET_PLAYLIST = 60 * 1000;

    // Allow 20 seconds extra timeout per MB offset.
    private static final double TIMEOUT_MILLIS_PER_OFFSET_BYTE = 20000.0 / 1000000.0;

    private static final int HTTP_REQUEST_MAX_ATTEMPTS = 5;
    private static final long REDIRECTION_CHECK_INTERVAL_MILLIS = 60L * 60L * 1000L;

	private SSLSocketFactory sslSocketFactory;
	private HostnameVerifier selfSignedHostnameVerifier;
    private long redirectionLastChecked;
    private int redirectionNetworkType = -1;
    private String redirectFrom;
    private String redirectTo;
	private Integer instance;
	private boolean hasInstalledGoogleSSL = false;

    public RESTMusicService() {
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] certs, String authType) {
				}
			}
		};
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (Exception e) {
		}

		selfSignedHostnameVerifier = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
    }

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "ping");
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {

      Reader reader = getReader(context, progressListener, "getLicense");
        try {
            ServerInfo serverInfo = new LicenseParser(context, getInstance(context)).parse(reader);
            return serverInfo.isLicenseValid();
        } finally {
            Util.close(reader);
        }
    }

    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getMusicFolders");
        try {
            return new MusicFoldersParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public void startRescan(Context context, ProgressListener listener) throws Exception {
		String startMethod = ServerInfo.isMadsonic(context, getInstance(context)) ? "startRescan" : "startScan";
		String refreshMethod = null;
		if(ServerInfo.isMadsonic(context, getInstance(context))) {
			startMethod = "startRescan";
			refreshMethod = "scanstatus";
		} else {
			startMethod = "startScan";
			refreshMethod = "getScanStatus";
		}

		Reader reader = getReader(context, listener, startMethod);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}

		// Now check if still running
		boolean done = false;
		while(!done) {
			reader = getReader(context, null, refreshMethod);
			try {
				boolean running = new ScanStatusParser(context, getInstance(context)).parse(reader, listener);
				if(running) {
					// Don't run system ragged trying to query too much
					Thread.sleep(100L);
				} else {
					done = true;
				}
			} catch(Exception e) {
				done = true;
			} finally {
				Util.close(reader);
			}
		}
	}

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

	if (musicFolderId != null) {
            parameterNames.add("musicFolderId");
            parameterValues.add(musicFolderId);
        }

        Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "getArtists" : "getIndexes", parameterNames, parameterValues);
        try {
            return new IndexesParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		SharedPreferences prefs = Util.getPreferences(context);
		String cacheLocn = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
		if(cacheLocn != null && id.indexOf(cacheLocn) != -1) {
			String search = Util.parseOfflineIDSearch(context, id, cacheLocn);
			SearchCritera critera = new SearchCritera(search, 1, 1, 0);
			SearchResult result = searchNew(critera, context, progressListener);
			if(result.getArtists().size() == 1) {
				id = result.getArtists().get(0).getId();
			} else if(result.getAlbums().size() == 1) {
				id = result.getAlbums().get(0).getId();
			}
		}

		MusicDirectory dir = null;
		int index, start = 0;
		while((index = id.indexOf(';', start)) != -1) {
			MusicDirectory extra = getMusicDirectoryImpl(id.substring(start, index), name, refresh, context, progressListener);
			if(dir == null) {
				dir = extra;
			} else {
				dir.addChildren(extra.getChildren());
			}

			start = index + 1;
		}
		MusicDirectory extra = getMusicDirectoryImpl(id.substring(start), name, refresh, context, progressListener);
		if(dir == null) {
			dir = extra;
		} else {
			dir.addChildren(extra.getChildren());
		}

		return dir;
    }

	private MusicDirectory getMusicDirectoryImpl(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getMusicDirectory", "id", id);
		try {
			return new MusicDirectoryParser(context, getInstance(context)).parse(name, reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getArtist(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getArtist", "id", id);
		try {
			return new MusicDirectoryParser(context, getInstance(context)).parse(name, reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getAlbum(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getAlbum", "id", id);
		try {
			return new MusicDirectoryParser(context, getInstance(context)).parse(name, reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
    public SearchResult search(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        try {
            return searchNew(critera, context, progressListener);
        } catch (ServerTooOldException x) {
            // Ensure backward compatibility with REST 1.3.
            return searchOld(critera, context, progressListener);
        }
    }

    /**
     * Search using the "search" REST method.
     */
    private SearchResult searchOld(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("any", "songCount");
        List<Object> parameterValues = Arrays.<Object>asList(critera.getQuery(), critera.getSongCount());
        Reader reader = getReader(context, progressListener, "search", parameterNames, parameterValues);
        try {
            return new SearchResultParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    /**
     * Search using the "search2" REST method, available in 1.4.0 and later.
     */
    private SearchResult searchNew(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.4", null);

        List<String> parameterNames = Arrays.asList("query", "artistCount", "albumCount", "songCount");
        List<Object> parameterValues = Arrays.<Object>asList(critera.getQuery(), critera.getArtistCount(), critera.getAlbumCount(), critera.getSongCount());

		int instance = getInstance(context);
		String method;
		if(ServerInfo.isMadsonic(context, instance) && ServerInfo.checkServerVersion(context, "2.0", instance)) {
			if(Util.isTagBrowsing(context, instance)) {
				method = "searchID3";
			} else {
				method = "search";
			}
		} else {
			if(Util.isTagBrowsing(context, instance)) {
				method = "search3";
			} else {
				method = "search2";
			}
		}
        Reader reader = getReader(context, progressListener, method, parameterNames, parameterValues);
        try {
            return new SearchResult2Parser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getPlaylist(boolean refresh, String id, String name, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getPlaylist", "id", id, SOCKET_READ_TIMEOUT_GET_PLAYLIST);
        try {
			return new PlaylistParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getPlaylists");
        try {
            return new PlaylistsParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = new LinkedList<String>();
        List<Object> parameterValues = new LinkedList<Object>();

        if (id != null) {
            parameterNames.add("playlistId");
            parameterValues.add(id);
        }
        if (name != null) {
            parameterNames.add("name");
            parameterValues.add(name);
        }
        for (MusicDirectory.Entry entry : entries) {
            parameterNames.add("songId");
            parameterValues.add(getOfflineSongId(entry.getId(), context, progressListener));
        }

        Reader reader = getReader(context, progressListener, "createPlaylist", parameterNames, parameterValues);
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "deletePlaylist", "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		for(MusicDirectory.Entry song: toAdd) {
			names.add("songIdToAdd");
			values.add(getOfflineSongId(song.getId(), context, progressListener));
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		for(Integer song: toRemove) {
			names.add("songIndexToRemove");
			values.add(song);
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		names.add("name");
		values.add(name);
		for(MusicDirectory.Entry song: toAdd) {
			names.add("songIdToAdd");
			values.add(song.getId());
		}
		for(int i = 0; i < toRemove; i++) {
			names.add("songIndexToRemove");
			values.add(i);
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		Reader reader = getReader(context, progressListener, "updatePlaylist", Arrays.asList("playlistId", "name", "comment", "public"), Arrays.<Object>asList(id, name, comment, pub));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getLyrics", Arrays.asList("artist", "title"), Arrays.<Object>asList(artist, title));
        try {
            return new LyricsParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
		id = getOfflineSongId(id, context, progressListener);
		scrobble(id, submission, 0, context, progressListener);
    }

    public void scrobble(String id, boolean submission, long time, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.5", "Scrobbling not supported.");
        Reader reader;
        if(time > 0){
        	checkServerVersion(context, "1.8", "Scrobbling with a time not supported.");
        	reader = getReader(context, progressListener, "scrobble", Arrays.asList("id", "submission", "time"), Arrays.<Object>asList(id, submission, time));
        }
        else
        	reader = getReader(context, progressListener, "scrobble", Arrays.asList("id", "submission"), Arrays.<Object>asList(id, submission));
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("type");
		values.add(type);
		names.add("size");
		values.add(size);
		names.add("offset");
		values.add(offset);

		// Add folder if it was set and is non null
		int instance = getInstance(context);
		if(Util.getAlbumListsPerFolder(context, instance)) {
			String folderId = Util.getSelectedMusicFolderId(context, instance);
			if(folderId != null) {
				names.add("musicFolderId");
				values.add(folderId);
			}
		}

		String method;
		if(Util.isTagBrowsing(context, instance)) {
			if(ServerInfo.isMadsonic6(context, instance)) {
				method = "getAlbumListID3";
			} else {
				method = "getAlbumList2";
			}
		} else {
			method = "getAlbumList";
		}

        Reader reader = getReader(context, progressListener, method, names, values, true);
        try {
            return new EntryListParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public MusicDirectory getAlbumList(String type, String extra, int size, int offset, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.10.1", "This type of album list is not supported");

		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("size");
		names.add("offset");

		values.add(size);
		values.add(offset);

		int instance = getInstance(context);
		if("genres".equals(type)) {
			names.add("type");
			values.add("byGenre");

			names.add("genre");
			values.add(extra);
		} else if("years".equals(type)) {
			names.add("type");
			values.add("byYear");

			names.add("fromYear");
			names.add("toYear");

			int decade = Integer.parseInt(extra);
			// Reverse chronological order only supported in 5.3+
			if(ServerInfo.checkServerVersion(context, "1.13", instance) && !ServerInfo.isMadsonic(context, instance)) {
				values.add(decade + 9);
				values.add(decade);
			} else {
				values.add(decade);
				values.add(decade + 9);
			}
		}

		// Add folder if it was set and is non null
		if(Util.getAlbumListsPerFolder(context, instance)) {
			String folderId = Util.getSelectedMusicFolderId(context, instance);
			if(folderId != null) {
				names.add("musicFolderId");
				values.add(folderId);
			}
		}

		String method;
		if(Util.isTagBrowsing(context, instance)) {
			if(ServerInfo.isMadsonic6(context, instance)) {
				method = "getAlbumListID3";
			} else {
				method = "getAlbumList2";
			}
		} else {
			method = "getAlbumList";
		}

		Reader reader = getReader(context, progressListener, method, names, values, true);
		try {
			return new EntryListParser(context, instance).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

    @Override
    public MusicDirectory getSongList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        names.add("size");
        values.add(size);
        names.add("offset");
        values.add(offset);

        String method;
        switch(type) {
			case MainFragment.SONGS_NEWEST:
				method = "getNewaddedSongs";
				break;
			case MainFragment.SONGS_TOP_PLAYED:
				method = "getTopplayedSongs";
				break;
			case MainFragment.SONGS_RECENT:
				method = "getLastplayedSongs";
				break;
			case MainFragment.SONGS_FREQUENT:
				method = "getMostplayedSongs";
				break;
			default:
				method = "getNewaddedSongs";
		}

        Reader reader = getReader(context, progressListener, method, names, values, true);
        try {
            return new EntryListParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
	public MusicDirectory getRandomSongs(int size, String artistId, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.11", "Artist radio is not supported");

		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("id");
		names.add("count");

		values.add(artistId);
		values.add(size);

		int instance = getInstance(context);
		String method;
		if(ServerInfo.isMadsonic6(context, instance)) {
			if (Util.isTagBrowsing(context, instance)) {
				method = "getSimilarSongsID3";
			} else {
				method = "getSimilarSongs";
			}
		} else if(ServerInfo.isMadsonic(context, instance)) {
			method = "getPandoraSongs";
		} else {
			if (Util.isTagBrowsing(context, instance)) {
				method = "getSimilarSongs2";
			} else {
				method = "getSimilarSongs";
			}
		}

		Reader reader = getReader(context, progressListener, method, names, values);
		try {
			return new RandomSongsParser(context, instance).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
    public MusicDirectory getStarredList(Context context, ProgressListener progressListener) throws Exception {
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		// Add folder if it was set and is non null
		int instance = getInstance(context);
		if(Util.getAlbumListsPerFolder(context, instance)) {
			String folderId = Util.getSelectedMusicFolderId(context, instance);
			if(folderId != null) {
				names.add("musicFolderId");
				values.add(folderId);
			}
		}

		String method;
		if(Util.isTagBrowsing(context, instance)) {
			if(ServerInfo.isMadsonic6(context, instance)) {
				method = "getStarredID3";
			} else {
				method = "getStarred2";
			}
		} else {
			method = "getStarred";
		}

        Reader reader = getReader(context, progressListener, method, names, values, true);
        try {
            return new StarredListParser(context, instance).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getRandomSongs(int size, String musicFolderId, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception {
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        names.add("size");
        values.add(size);

        if (musicFolderId != null && !"".equals(musicFolderId) && !Util.isTagBrowsing(context, getInstance(context))) {
            names.add("musicFolderId");
            values.add(musicFolderId);
        }
		if(genre != null && !"".equals(genre)) {
			names.add("genre");
			values.add(genre);
		}
		if(startYear != null && !"".equals(startYear)) {
			// Check to make sure user isn't doing 2015 -> 2010 since Subsonic will return no results
			if(endYear != null && !"".equals(endYear)) {
				try {
					int startYearInt = Integer.parseInt(startYear);
					int endYearInt = Integer.parseInt(endYear);

					if(startYearInt > endYearInt) {
						String tmp = startYear;
						startYear = endYear;
						endYear = tmp;
					}
				} catch(Exception e) {
					Log.w(TAG, "Failed to convert start/end year into ints", e);
				}
			}

			names.add("fromYear");
			values.add(startYear);
		}
		if(endYear != null && !"".equals(endYear)) {
			names.add("toYear");
			values.add(endYear);
		}

        Reader reader = getReader(context, progressListener, "getRandomSongs", names, values);
        try {
            return new RandomSongsParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	private void checkServerVersion(Context context, String version, String text) throws ServerTooOldException {
        Version serverVersion = ServerInfo.getServerVersion(context);
        Version requiredVersion = new Version(version);
        boolean ok = serverVersion == null || serverVersion.compareTo(requiredVersion) >= 0;

        if (!ok) {
            throw new ServerTooOldException(text, serverVersion, requiredVersion);
        }
    }

	@Override
	public String getCoverArtUrl(Context context, MusicDirectory.Entry entry) throws Exception {
		StringBuilder builder = new StringBuilder(getRestUrl(context, "getCoverArt"));
		builder.append("&id=").append(entry.getCoverArt());
		String url = builder.toString();
		url = Util.replaceInternalUrl(context, url);
		url = rewriteUrlWithRedirect(context, url);
		return url;
	}

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
        // Synchronize on the entry so that we don't download concurrently for the same song.
        synchronized (entry) {
			String url = getRestUrl(context, "getCoverArt");
			List<String> parameterNames = Arrays.asList("id");
			List<Object> parameterValues = Arrays.<Object>asList(entry.getCoverArt());

			return getBitmapFromUrl(context, url, parameterNames, parameterValues, size, FileUtil.getAlbumArtFile(context, entry), true, progressListener, task);
        }
    }

    @Override
    public HttpURLConnection getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, SilentBackgroundTask task) throws Exception {
        String url = getRestUrl(context, "stream");
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("id");
		parameterNames.add("maxBitRate");

		List<Object> parameterValues = new ArrayList<>();
		parameterValues.add(song.getId());
		parameterValues.add(maxBitrate);

		// If video specify what format to download
		if(song.isVideo()) {
			String videoPlayerType = Util.getVideoPlayerType(context);
			if("hls".equals(videoPlayerType)) {
				// HLS should be able to transcode to mp4 automatically
				parameterNames.add("format");
				parameterValues.add("mp4");

				parameterNames.add("hls");
				parameterValues.add("true");
			} else if("raw".equals(videoPlayerType)) {
				// Download the original video without any transcoding
				parameterNames.add("format");
				parameterValues.add("raw");
			}
		}

		// Add "Range" header if offset is given
		Map<String, String> headers = new HashMap<>();
		if (offset > 0) {
			headers.put("Range", "bytes=" + offset + "-");
		}

		// Set socket read timeout. Note: The timeout increases as the offset gets larger. This is
		// to avoid the thrashing effect seen when offset is combined with transcoding/downsampling on the server.
		// In that case, the server uses a long time before sending any data, causing the client to time out.
		int timeout = (int) (SOCKET_READ_TIMEOUT_DOWNLOAD + offset * TIMEOUT_MILLIS_PER_OFFSET_BYTE);
		HttpURLConnection connection = getConnection(context, url, parameterNames, parameterValues, headers, timeout);

		// If content type is XML, an error occurred.  Get it.
		String contentType = connection.getContentType();
		if (contentType != null && (contentType.startsWith("text/xml") || contentType.startsWith("text/html"))) {
			InputStream in = getInputStreamFromConnection(connection);

			try {
				new ErrorParser(context, getInstance(context)).parse(new InputStreamReader(in, Constants.UTF_8));
			} finally {
				Util.close(in);
			}
		}

		return connection;
    }

	@Override
	public String getMusicUrl(Context context, MusicDirectory.Entry song, int maxBitrate) throws Exception {
		StringBuilder builder = new StringBuilder(getRestUrl(context, "stream"));
		builder.append("&id=").append(song.getId());

		// Allow user to specify to stream raw formats if available
		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_CAST_STREAM_ORIGINAL, true) && ("mp3".equals(song.getSuffix()) || "flac".equals(song.getSuffix()) || "wav".equals(song.getSuffix()) || "aac".equals(song.getSuffix())) && ServerInfo.checkServerVersion(context, "1.9", getInstance(context))) {
			builder.append("&format=raw");
		} else {
			builder.append("&maxBitRate=").append(maxBitrate);
		}

		String url = builder.toString();
		url = Util.replaceInternalUrl(context, url);
		url = rewriteUrlWithRedirect(context, url);
		Log.i(TAG, "Using music URL: " + stripUrlInfo(url));
		return url;
	}

	@Override
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        StringBuilder builder = new StringBuilder(getRestUrl(context, "videoPlayer"));
        builder.append("&id=").append(id);
        builder.append("&maxBitRate=").append(maxBitrate);
        builder.append("&autoplay=true");

        String url = rewriteUrlWithRedirect(context, builder.toString());
        Log.i(TAG, "Using video URL: " + stripUrlInfo(url));
        return url;
    }

	@Override
	public String getVideoStreamUrl(String format, int maxBitrate, Context context, String id) throws Exception {
		StringBuilder builder = new StringBuilder(getRestUrl(context, "stream"));
        builder.append("&id=").append(id);
		if(!"raw".equals(format)) {
			checkServerVersion(context, "1.9", "Video streaming not supported.");
			builder.append("&maxBitRate=").append(maxBitrate);
		}
		builder.append("&format=").append(format);

        String url = rewriteUrlWithRedirect(context, builder.toString());
        Log.i(TAG, "Using video URL: " + stripUrlInfo(url));
        return url;
	}

	@Override
	public String getHlsUrl(String id, int bitRate, Context context) throws Exception {
		checkServerVersion(context, "1.9", "HLS video streaming not supported.");

		StringBuilder builder = new StringBuilder(getRestUrl(context, "hls"));
        builder.append("&id=").append(id);
		if(bitRate > 0) {
			builder.append("&bitRate=").append(bitRate);
		}

        String url = rewriteUrlWithRedirect(context, builder.toString());
        Log.i(TAG, "Using hls URL: " + stripUrlInfo(url));
        return url;
	}

    @Override
    public RemoteStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        int n = ids.size();
        List<String> parameterNames = new ArrayList<String>(n + 1);
        parameterNames.add("action");
        for (int i = 0; i < n; i++) {
            parameterNames.add("id");
        }
        List<Object> parameterValues = new ArrayList<Object>();
        parameterValues.add("set");
        parameterValues.addAll(ids);

        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);
    }

    @Override
    public RemoteStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("action", "index", "offset");
        List<Object> parameterValues = Arrays.<Object>asList("skip", index, offsetSeconds);
        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);
    }

    @Override
    public RemoteStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("stop"));
    }

    @Override
    public RemoteStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("start"));
    }

    @Override
    public RemoteStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("status"));
    }

    @Override
    public RemoteStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("action", "gain");
        List<Object> parameterValues = Arrays.<Object>asList("setGain", gain);
        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);

    }

    private RemoteStatus executeJukeboxCommand(Context context, ProgressListener progressListener, List<String> parameterNames, List<Object> parameterValues) throws Exception {
        checkServerVersion(context, "1.7", "Jukebox not supported.");
        Reader reader = getReader(context, progressListener, "jukeboxControl", parameterNames, parameterValues);
        try {
            return new JukeboxStatusParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public void setStarred(List<MusicDirectory.Entry> entries, List<MusicDirectory.Entry> artists, List<MusicDirectory.Entry> albums, boolean starred, ProgressListener progressListener, Context context) throws Exception {
    	checkServerVersion(context, "1.8", "Starring is not supported.");

		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		if(entries != null && entries.size() > 0) {
			if(entries.size() > 1) {
				for (MusicDirectory.Entry entry : entries) {
					names.add("id");
					values.add(entry.getId());
				}
			} else {
				names.add("id");
				values.add(getOfflineSongId(entries.get(0).getId(), context, progressListener));
			}
		}
		if(artists != null && artists.size() > 0) {
			for (MusicDirectory.Entry artist : artists) {
				names.add("artistId");
				values.add(artist.getId());
			}
		}
		if(albums != null && albums.size() > 0) {
			for (MusicDirectory.Entry album : albums) {
				names.add("albumId");
				values.add(album.getId());
			}
		}

		Reader reader = getReader(context, progressListener, starred ? "star" : "unstar", names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public List<Share> getShares(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Shares not supported.");

		Reader reader = getReader(context, progressListener, "getShares");
		try {
			return new ShareParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public List<Share> createShare(List<String> ids, String description, Long expires, Context context, ProgressListener progressListener) throws Exception {
		List<String> parameterNames = new LinkedList<String>();
		List<Object> parameterValues = new LinkedList<Object>();

		for (String id : ids) {
			parameterNames.add("id");
			parameterValues.add(id);
		}

		if (description != null) {
			parameterNames.add("description");
			parameterValues.add(description);
		}

		if (expires > 0) {
			parameterNames.add("expires");
			parameterValues.add(expires);
		}

		Reader reader = getReader(context, progressListener, "createShare", parameterNames, parameterValues);
		try {
			return new ShareParser(context, getInstance(context)).parse(reader, progressListener);
		}
		finally {
			Util.close(reader);
		}
	}

	@Override
	public void deleteShare(String id, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Shares not supported.");

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("id");
		parameterValues.add(id);

		Reader reader = getReader(context, progressListener, "deleteShare", parameterNames, parameterValues);

		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		}
		finally {
			Util.close(reader);
		}
	}

	@Override
	public void updateShare(String id, String description, Long expires, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Updating share not supported.");

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("id");
		parameterValues.add(id);

		if (description != null) {
			parameterNames.add("description");
			parameterValues.add(description);
		}

		parameterNames.add("expires");
		parameterValues.add(expires);

		Reader reader = getReader(context, progressListener, "updateShare", parameterNames, parameterValues);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		}
		finally {
			Util.close(reader);
		}
	}

	@Override
	public List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.2", "Chat not supported.");

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("since");
		parameterValues.add(since);

		Reader reader = getReader(context, progressListener, "getChatMessages", parameterNames, parameterValues);

		try {
			return new ChatMessageParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.2", "Chat not supported.");

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("message");
		parameterValues.add(message);

		Reader reader = getReader(context, progressListener, "addChatMessage", parameterNames, parameterValues);

		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Genres not supported.");

        Reader reader = getReader(context, progressListener, "getGenres");
        try {
            return new GenreParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Genres not supported.");

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("genre");
		parameterValues.add(genre);
		parameterNames.add("count");
		parameterValues.add(count);
		parameterNames.add("offset");
		parameterValues.add(offset);

		// Add folder if it was set and is non null
		int instance = getInstance(context);
		if(Util.getAlbumListsPerFolder(context, instance)) {
			String folderId = Util.getSelectedMusicFolderId(context, instance);
			if(folderId != null) {
				parameterNames.add("musicFolderId");
				parameterValues.add(folderId);
			}
		}

		Reader reader = getReader(context, progressListener, "getSongsByGenre", parameterNames, parameterValues, true);
		try {
			return new RandomSongsParser(context, instance).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getTopTrackSongs(String artist, int size, Context context, ProgressListener progressListener) throws Exception {
		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("artist");
		parameterValues.add(artist);
		parameterNames.add("size");
		parameterValues.add(size);

		String method = ServerInfo.isMadsonic(context, getInstance(context)) ? "getTopTrackSongs" : "getTopSongs";
		Reader reader = getReader(context, progressListener, method, parameterNames, parameterValues);
		try {
			return new TopSongsParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Podcasts not supported.");

		Reader reader = getReader(context, progressListener, "getPodcasts", Arrays.asList("includeEpisodes"), Arrays.<Object>asList("false"));
        try {
            List<PodcastChannel> channels = new PodcastChannelParser(context, getInstance(context)).parse(reader, progressListener);

			String content = "";
			for(PodcastChannel channel: channels) {
				content += channel.getName() + "\t" + channel.getUrl() + "\n";
			}

			File file = FileUtil.getPodcastFile(context, Util.getServerName(context, getInstance(context)));
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(content);
			bw.close();

			return channels;
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getPodcasts", Arrays.asList("id"), Arrays.<Object>asList(id));
        try {
            return new PodcastEntryParser(context, getInstance(context)).parse(id, reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getNewestPodcastEpisodes(boolean refresh, Context context, ProgressListener progressListener, int count) throws Exception {
		Reader reader = getReader(context, progressListener, "getNewestPodcasts", Arrays.asList("count"), Arrays.<Object>asList(count), true);

		try {
			return new PodcastEntryParser(context, getInstance(context)).parse(null, reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

    @Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Refresh podcasts not supported.");

		Reader reader = getReader(context, progressListener, "refreshPodcasts");
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Creating podcasts not supported.");

		Reader reader = getReader(context, progressListener, "createPodcastChannel", "url", url);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");

		Reader reader = getReader(context, progressListener, "deletePodcastChannel", "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Downloading podcasts not supported.");

		Reader reader = getReader(context, progressListener, "downloadPodcastEpisode", "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void deletePodcastEpisode(String id, String parent, ProgressListener progressListener, Context context) throws Exception{
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");

		Reader reader = getReader(context, progressListener, "deletePodcastEpisode", "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void setRating(MusicDirectory.Entry entry, int rating, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Setting ratings not supported.");

		Reader reader = getReader(context, progressListener, "setRating", Arrays.asList("id", "rating"), Arrays.<Object>asList(entry.getId(), rating));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getBookmarks(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Bookmarks not supported.");

		Reader reader = getReader(context, progressListener, "getBookmarks");
		try {
			return new BookmarkParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void createBookmark(MusicDirectory.Entry entry, int position, String comment, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Creating bookmarks not supported.");

		Reader reader = getReader(context, progressListener, "createBookmark", Arrays.asList("id", "position", "comment"), Arrays.<Object>asList(entry.getId(), position, comment));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void deleteBookmark(MusicDirectory.Entry entry, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Deleting bookmarks not supported.");

		Reader reader = getReader(context, progressListener, "deleteBookmark", Arrays.asList("id"), Arrays.<Object>asList(entry.getId()));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public User getUser(boolean refresh, String username, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getUser", Arrays.asList("username"), Arrays.<Object>asList(username));
		try {
			List<User> users = new UserParser(context, getInstance(context)).parse(reader, progressListener);
			if(users.size() > 0) {
				// Should only have returned one anyways
				return users.get(0);
			} else {
				return null;
			}
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public List<User> getUsers(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Getting user list is not supported");

		Reader reader = getReader(context, progressListener, "getUsers");
		try {
			return new UserParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void createUser(User user, Context context, ProgressListener progressListener) throws Exception {
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("username");
		values.add(user.getUsername());
		names.add("email");
		values.add(user.getEmail());
		names.add("password");
		values.add(user.getPassword());

		for(User.Setting setting: user.getSettings()) {
			names.add(setting.getName());
			values.add(setting.getValue());
		}

		if(user.getMusicFolderSettings() != null) {
			for(User.Setting setting: user.getMusicFolderSettings()) {
				if(setting.getValue()) {
					names.add("musicFolderId");
					values.add(setting.getName());
				}
			}
		}

		Reader reader = getReader(context, progressListener, "createUser", names, values);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void updateUser(User user, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.10", "Updating user is not supported");

		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("username");
		values.add(user.getUsername());

		for(User.Setting setting: user.getSettings()) {
			if(setting.getName().indexOf("Role") != -1) {
				names.add(setting.getName());
				values.add(setting.getValue());
			}
		}

		if(user.getMusicFolderSettings() != null) {
			for(User.Setting setting: user.getMusicFolderSettings()) {
				if(setting.getValue()) {
					names.add("musicFolderId");
					values.add(setting.getName());
				}
			}
		}

		Reader reader = getReader(context, progressListener, "updateUser", names, values);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void deleteUser(String username, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "deleteUser", Arrays.asList("username"), Arrays.<Object>asList(username));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void changeEmail(String username, String email, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "updateUser", Arrays.asList("username", "email"), Arrays.<Object>asList(username, email));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void changePassword(String username, String password, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "changePassword", Arrays.asList("username", "password"), Arrays.<Object>asList(username, password));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public Bitmap getAvatar(String username, int size, Context context, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
		// Return silently if server is too old
		if (!ServerInfo.checkServerVersion(context, "1.8")) {
			return null;
		}

		// Synchronize on the username so that we don't download concurrently for
		// the same user.
		synchronized (username) {
			String url = Util.getRestUrl(context, "getAvatar");
			List<String> parameterNames = Collections.singletonList("username");
			List<Object> parameterValues = Arrays.<Object>asList(username);

			return getBitmapFromUrl(context, url, parameterNames, parameterValues, size, FileUtil.getAvatarFile(context, username), false, progressListener, task);
		}
	}

	@Override
	public ArtistInfo getArtistInfo(String id, boolean refresh, boolean allowNetwork, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.11", "Getting artist info is not supported");

		int instance = getInstance(context);
		String method;
		if(Util.isTagBrowsing(context, instance)) {
			if(ServerInfo.isMadsonic6(context, instance)) {
				method = "getArtistInfoID3";
			} else {
				method = "getArtistInfo2";
			}
		} else {
			method = "getArtistInfo";
		}

		Reader reader = getReader(context, progressListener, method, Arrays.asList("id", "includeNotPresent"), Arrays.<Object>asList(id, "true"));
		try {
			return new ArtistInfoParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public Bitmap getBitmap(String url, int size, Context context, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
		// Synchronize on the url so that we don't download concurrently
		synchronized (url) {
			return getBitmapFromUrl(context, url, null, null, size, FileUtil.getMiscFile(context, url), false, progressListener, task);
		}
	}

	@Override
	public MusicDirectory getVideos(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getVideos");
		try {
			return new VideosParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void savePlayQueue(List<MusicDirectory.Entry> songs, MusicDirectory.Entry currentPlaying, int position, Context context, ProgressListener progressListener) throws Exception {
		List<String> parameterNames = new LinkedList<String>();
		List<Object> parameterValues = new LinkedList<Object>();

		for(MusicDirectory.Entry song: songs) {
			parameterNames.add("id");
			parameterValues.add(song.getId());
		}

		parameterNames.add("current");
		parameterValues.add(currentPlaying.getId());

		parameterNames.add("position");
		parameterValues.add(position);

		Reader reader = getReader(context, progressListener, "savePlayQueue", parameterNames, parameterValues);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public PlayerQueue getPlayQueue(Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getPlayQueue");
		try {
			return new PlayQueueParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public List<InternetRadioStation> getInternetRadioStations(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", null);

		Reader reader = getReader(context, progressListener, "getInternetRadioStations");
		try {
			return new InternetRadioStationParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public int processOfflineSyncs(final Context context, final ProgressListener progressListener) throws Exception{
		return processOfflineScrobbles(context, progressListener) + processOfflineStars(context, progressListener);
	}

	public int processOfflineScrobbles(final Context context, final ProgressListener progressListener) throws Exception {
		SharedPreferences offline = Util.getOfflineSync(context);
		SharedPreferences.Editor offlineEditor = offline.edit();
		int count = offline.getInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		int retry = 0;
		for(int i = 1; i <= count; i++) {
			try {
				String id = offline.getString(Constants.OFFLINE_SCROBBLE_ID + i, null);
				long time = offline.getLong(Constants.OFFLINE_SCROBBLE_TIME + i, 0);
				if(id != null) {
					scrobble(id, true, time, context, progressListener);
				} else {
					String search = offline.getString(Constants.OFFLINE_SCROBBLE_SEARCH + i, "");
					SearchCritera critera = new SearchCritera(search, 0, 0, 1);
					SearchResult result = searchNew(critera, context, progressListener);
					if(result.getSongs().size() == 1){
						Log.i(TAG, "Query '" + search + "' returned song " + result.getSongs().get(0).getTitle() + " by " + result.getSongs().get(0).getArtist() + " with id " + result.getSongs().get(0).getId());
						Log.i(TAG, "Scrobbling " + result.getSongs().get(0).getId() + " with time " + time);
						scrobble(result.getSongs().get(0).getId(), true, time, context, progressListener);
					}
					else{
						throw new Exception("Song not found on server");
					}
				}
			}
			catch(Exception e){
				Log.e(TAG, e.toString());
				retry++;
			}
		}

		offlineEditor.putInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		offlineEditor.commit();

		return count - retry;
	}

	public int processOfflineStars(final Context context, final ProgressListener progressListener) throws Exception {
		SharedPreferences offline = Util.getOfflineSync(context);
		SharedPreferences.Editor offlineEditor = offline.edit();
		int count = offline.getInt(Constants.OFFLINE_STAR_COUNT, 0);
		int retry = 0;
		for(int i = 1; i <= count; i++) {
			String id = offline.getString(Constants.OFFLINE_STAR_ID + i, null);
			boolean starred = offline.getBoolean(Constants.OFFLINE_STAR_SETTING + i, false);
			if(id != null) {
				setStarred(Arrays.asList(new MusicDirectory.Entry(id)), null, null, starred, progressListener, context);
			} else {
				String search = offline.getString(Constants.OFFLINE_STAR_SEARCH + i, "");
				try{
					SearchCritera critera = new SearchCritera(search, 0, 1, 1);
					SearchResult result = searchNew(critera, context, progressListener);
					if(result.getSongs().size() == 1) {
						MusicDirectory.Entry song = result.getSongs().get(0);
						Log.i(TAG, "Query '" + search + "' returned song " + song.getTitle() + " by " + song.getArtist() + " with id " + song.getId());
						setStarred(Arrays.asList(song), null, null, starred, progressListener, context);
					} else if(result.getAlbums().size() == 1) {
						MusicDirectory.Entry album = result.getAlbums().get(0);
						Log.i(TAG, "Query '" + search + "' returned album " + album.getTitle() + " by " + album.getArtist() + " with id " + album.getId());
						if(Util.isTagBrowsing(context, getInstance(context))) {
							setStarred(null, null, Arrays.asList(album), starred, progressListener, context);
						} else {
							setStarred(Arrays.asList(album), null, null, starred, progressListener, context);
						}
					}
					else {
						throw new Exception("Song not found on server");
					}
				}
				catch(Exception e) {
					Log.e(TAG, e.toString());
					retry++;
				}
			}
		}

		offlineEditor.putInt(Constants.OFFLINE_STAR_COUNT, 0);
		offlineEditor.commit();

		return count - retry;
	}

	private String getOfflineSongId(String id, Context context, ProgressListener progressListener) throws Exception {
		SharedPreferences prefs = Util.getPreferences(context);
		String cacheLocn = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
		if(cacheLocn != null && id.indexOf(cacheLocn) != -1) {
			Pair<Integer, String> cachedSongId = SongDBHandler.getHandler(context).getIdFromPath(Util.getRestUrlHash(context, getInstance(context)), id);
			if(cachedSongId != null) {
				id = cachedSongId.getSecond();
			} else {
				String searchCriteria = Util.parseOfflineIDSearch(context, id, cacheLocn);
				SearchCritera critera = new SearchCritera(searchCriteria, 0, 0, 1);
				SearchResult result = searchNew(critera, context, progressListener);
				if (result.getSongs().size() == 1) {
					id = result.getSongs().get(0).getId();
				}
			}
		}

		return id;
	}

	@Override
	public void setInstance(Integer instance)  throws Exception {
		this.instance = instance;
	}

	protected Bitmap getBitmapFromUrl(Context context, String url, List<String> parameterNames, List<Object> parameterValues, int size, File saveToFile, boolean allowUnscaled, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
		InputStream in = null;
		try {
			HttpURLConnection connection = getConnection(context, url, parameterNames, parameterValues, progressListener, true);
			in = getInputStreamFromConnection(connection);

			String contentType = connection.getContentType();
			if (contentType != null && (contentType.startsWith("text/xml") || contentType.startsWith("text/html"))) {
				new ErrorParser(context, getInstance(context)).parse(new InputStreamReader(in, Constants.UTF_8));
			}

			byte[] bytes = Util.toByteArray(in);

			// Handle case where partial was downloaded before being cancelled
			if(task != null && task.isCancelled()) {
				return null;
			}

			OutputStream out = null;
			try {
				out = new FileOutputStream(saveToFile);
				out.write(bytes);
			} finally {
				Util.close(out);
			}

			// Size == 0 -> only want to download
			if(size == 0) {
				return null;
			} else {
				return FileUtil.getSampledBitmap(bytes, size, allowUnscaled);
			}
		} finally {
			Util.close(in);
		}
	}

	// Helper classes to get a reader for the request
	private Reader getReader(Context context, ProgressListener progressListener, String method) throws Exception {
		return getReader(context, progressListener, method, (List<String>)null, null);
	}

	private Reader getReader(Context context, ProgressListener progressListener, String method, String parameterName, Object parameterValue) throws Exception {
		return getReader(context, progressListener, method, parameterName, parameterValue, 0);
	}
    private Reader getReader(Context context, ProgressListener progressListener, String method, String parameterName, Object parameterValue, int minNetworkTimeout) throws Exception {
        return getReader(context, progressListener, method, Arrays.asList(parameterName), Arrays.asList(parameterValue), minNetworkTimeout);
    }

	private Reader getReader(Context context, ProgressListener progressListener, String method, List<String> parameterNames, List<Object> parameterValues) throws Exception {
		return getReader(context, progressListener, method, parameterNames, parameterValues, 0);
	}
	private Reader getReader(Context context, ProgressListener progressListener, String method, List<String> parameterNames, List<Object> parameterValues, int minNetworkTimeout) throws Exception {
		return getReader(context, progressListener, method, parameterNames, parameterValues, minNetworkTimeout, false);
	}
	private Reader getReader(Context context, ProgressListener progressListener, String method, List<String> parameterNames, List<Object> parameterValues, boolean throwErrors) throws Exception {
		return getReader(context, progressListener, method, parameterNames, parameterValues, 0, throwErrors);
	}
    private Reader getReader(Context context, ProgressListener progressListener, String method, List<String> parameterNames, List<Object> parameterValues, int minNetworkTimeout, boolean throwErrors) throws Exception {
        if (progressListener != null) {
            progressListener.updateProgress(R.string.service_connecting);
        }

        String url = getRestUrl(context, method);
        return getReaderForURL(context, url, parameterNames, parameterValues, minNetworkTimeout, progressListener, throwErrors);
    }

	private Reader getReaderForURL(Context context, String url, List<String> parameterNames, List<Object> parameterValues, ProgressListener progressListener) throws Exception {
		return getReaderForURL(context, url, parameterNames, parameterValues, progressListener, true);
	}
	private Reader getReaderForURL(Context context, String url, List<String> parameterNames, List<Object> parameterValues, ProgressListener progressListener, boolean throwErrors) throws Exception {
		return getReaderForURL(context, url, parameterNames, parameterValues, 0, progressListener, throwErrors);
	}
    private Reader getReaderForURL(Context context, String url, List<String> parameterNames, List<Object> parameterValues, int minNetworkTimeout, ProgressListener progressListener, boolean throwErrors) throws Exception {
		InputStream in = getInputStream(context, url, parameterNames, parameterValues, minNetworkTimeout, progressListener, throwErrors);
		return new InputStreamReader(in, Constants.UTF_8);
    }

	// Helper classes to open a connection to a server
	private InputStream getInputStream(Context context, String url, List<String> parameterNames, List<Object> parameterValues, ProgressListener progressListener, boolean throwsErrors) throws Exception {
		return getInputStream(context, url, parameterNames, parameterValues, 0, progressListener, throwsErrors);
	}
	private InputStream getInputStream(Context context, String url, List<String> parameterNames, List<Object> parameterValues, int minNetworkTimeout, ProgressListener progressListener, boolean throwsErrors) throws Exception {
		HttpURLConnection connection = getConnection(context, url, parameterNames, parameterValues, minNetworkTimeout, progressListener, throwsErrors);
		return getInputStreamFromConnection(connection);
	}
	private InputStream getInputStreamFromConnection(HttpURLConnection connection) throws Exception {
		InputStream in = connection.getInputStream();
		if("gzip".equals(connection.getContentEncoding())) {
			in = new GZIPInputStream(in);
		}

		return in;
	}

	private HttpURLConnection getConnection(Context context, String url, List<String> parameterNames, List<Object> parameterValues, Map<String, String> headers, int minNetworkTimeout) throws Exception {
		return getConnection(context, url, parameterNames, parameterValues, headers, minNetworkTimeout, null, true);
	}
	private HttpURLConnection getConnection(Context context, String url, List<String> parameterNames, List<Object> parameterValues, ProgressListener progressListener, boolean throwErrors) throws Exception {
		return getConnection(context, url, parameterNames, parameterValues, 0, progressListener, throwErrors);
	}
	private HttpURLConnection getConnection(Context context, String url, List<String> parameterNames, List<Object> parameterValues, int minNetworkTimeout, ProgressListener progressListener, boolean throwErrors) throws Exception {
		return getConnection(context, url, parameterNames, parameterValues, null, minNetworkTimeout, progressListener, throwErrors);
	}
	private HttpURLConnection getConnection(Context context, String url, List<String> parameterNames, List<Object> parameterValues, Map<String, String> headers, int minNetworkTimeout, ProgressListener progressListener, boolean throwErrors) throws Exception {
		if(throwErrors) {
			SharedPreferences prefs = Util.getPreferences(context);
			int networkTimeout = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT, SOCKET_READ_TIMEOUT_DEFAULT + ""));
			return getConnectionDirect(context, url, parameterNames, parameterValues, headers, Math.max(minNetworkTimeout, networkTimeout));
		} else {
			return getConnection(context, url, parameterNames, parameterValues, headers, minNetworkTimeout, progressListener, HTTP_REQUEST_MAX_ATTEMPTS, 0);
		}
	}

	private HttpURLConnection getConnection(Context context, String url, List<String> parameterNames, List<Object> parameterValues, Map<String, String> headers, int minNetworkTimeout, ProgressListener progressListener, int retriesLeft, int attempts) throws Exception {
		SharedPreferences prefs = Util.getPreferences(context);
		int networkTimeout = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT, SOCKET_READ_TIMEOUT_DEFAULT + ""));
		minNetworkTimeout = Math.max(minNetworkTimeout, networkTimeout);
		attempts++;
		retriesLeft--;

		try {
			return getConnectionDirect(context, url, parameterNames, parameterValues, headers, minNetworkTimeout);
		} catch (IOException x) {
			if(retriesLeft > 0) {
				if (progressListener != null) {
					String msg = context.getResources().getString(R.string.music_service_retry, attempts, HTTP_REQUEST_MAX_ATTEMPTS - 1);
					progressListener.updateProgress(msg);
				}

				Log.w(TAG, "Got IOException " + x + " (" + attempts + "), will retry");
				Thread.sleep(2000L);

				minNetworkTimeout = (int) (minNetworkTimeout * 1.3);
				return getConnection(context, url, parameterNames, parameterValues, headers, minNetworkTimeout, progressListener, retriesLeft, attempts);
			} else {
				throw x;
			}
		}
	}

	private HttpURLConnection getConnectionDirect(Context context, String url, List<String> parameterNames, List<Object> parameterValues, Map<String, String> headers, int minNetworkTimeout) throws Exception {
		// Add params to query
		if (parameterNames != null) {
			StringBuilder builder = new StringBuilder(url);
			for (int i = 0; i < parameterNames.size(); i++) {
				builder.append("&").append(parameterNames.get(i)).append("=");
				String part = URLEncoder.encode(String.valueOf(parameterValues.get(i)), "UTF-8");
				part = part.replaceAll("\\%27", "'");
				builder.append(part);
			}
			url = builder.toString();
		}

		// Rewrite url based on redirects
		String rewrittenUrl = rewriteUrlWithRedirect(context, url);
		if(rewrittenUrl.indexOf("scanstatus") == -1) {
			Log.i(TAG, stripUrlInfo(rewrittenUrl));
		}

		return getConnectionDirect(context, rewrittenUrl, headers, minNetworkTimeout);
	}

	private HttpURLConnection getConnectionDirect(Context context, String url, Map<String, String> headers, int minNetworkTimeout) throws Exception {
		if(!hasInstalledGoogleSSL) {
			try {
				GoogleCompat.installProvider(context);
			} catch(Exception e) {
				// Just continue on anyways, doesn't really harm anything if this fails
				Log.w(TAG, "Failed to update to use Google Play SSL", e);
			}
			hasInstalledGoogleSSL = true;
		}

		// Connect and add headers
		URL urlObj = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
		if(url.indexOf("getCoverArt") == -1 && url.indexOf("stream") == -1 && url.indexOf("getAvatar") == -1) {
			connection.addRequestProperty("Accept-Encoding", "gzip");
		}
		connection.addRequestProperty("User-Agent", Constants.REST_CLIENT_ID);

		// Set timeout
		connection.setConnectTimeout(minNetworkTimeout);
		connection.setReadTimeout(minNetworkTimeout);

		// Add headers
		if(headers != null) {
			for(Map.Entry<String, String> header: headers.entrySet()) {
				connection.setRequestProperty(header.getKey(), header.getValue());
			}
		}

		if(connection instanceof HttpsURLConnection) {
			HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
			sslConnection.setSSLSocketFactory(sslSocketFactory);
			sslConnection.setHostnameVerifier(selfSignedHostnameVerifier);
		}

		SharedPreferences prefs = Util.getPreferences(context);
		int instance = getInstance(context);
		String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
		String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
		if (prefs.getBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + instance, false)) password = KeyStoreUtil.decrypt(password);
		String encoded = Base64.encodeToString((username + ":" + password).getBytes("UTF-8"), Base64.NO_WRAP);;
		connection.setRequestProperty("Authorization", "Basic " + encoded);

		// Force the connection to initiate
		if(connection.getResponseCode() >= 500) {
			throw new IOException("Error code: " + connection.getResponseCode());
		}
		if(detectRedirect(context, urlObj, connection)) {
			String rewrittenUrl = rewriteUrlWithRedirect(context, url);
			if(!rewrittenUrl.equals(url)) {
				connection.disconnect();
				return getConnectionDirect(context, rewrittenUrl, headers, minNetworkTimeout);
			}
		}

		return connection;
	}

	// Returns true when we should immediately retry with the redirect
	private boolean detectRedirect(Context context, URL originalUrl, HttpURLConnection connection) throws Exception {
		if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
			String redirectLocation = connection.getHeaderField("Location");
			if(redirectLocation != null) {
				detectRedirect(context, originalUrl.toExternalForm(), redirectLocation);
				return true;
			}
		}

		detectRedirect(context, originalUrl, connection.getURL());
		return false;
	}
	private void detectRedirect(Context context, URL originalUrl, URL redirectedUrl) throws Exception {
		detectRedirect(context, originalUrl.toExternalForm(), redirectedUrl.toExternalForm());
	}
	private void detectRedirect(Context context, String originalUrl, String redirectedUrl) throws Exception {
		if(redirectedUrl != null && "http://subsonic.org/pages/".equals(redirectedUrl)) {
			throw new Exception("Invalid url, redirects to http://subsonic.org/pages/");
		}

		int fromIndex = originalUrl.indexOf("/rest/");
		int toIndex = redirectedUrl.indexOf("/rest/");
		if(fromIndex != -1 && toIndex != -1 && !Util.equals(originalUrl, redirectedUrl)) {
			redirectFrom = originalUrl.substring(0, fromIndex);
			redirectTo = redirectedUrl.substring(0, toIndex);

			if (redirectFrom.compareTo(redirectTo) != 0) {
				Log.i(TAG, redirectFrom + " redirects to " + redirectTo);
			}
			redirectionLastChecked = System.currentTimeMillis();
			redirectionNetworkType = getCurrentNetworkType(context);
		}
	}

    private String rewriteUrlWithRedirect(Context context, String url) {

        // Only cache for a certain time.
        if (System.currentTimeMillis() - redirectionLastChecked > REDIRECTION_CHECK_INTERVAL_MILLIS) {
            return url;
        }

        // Ignore cache if network type has changed.
        if (redirectionNetworkType != getCurrentNetworkType(context)) {
            return url;
        }

        if (redirectFrom == null || redirectTo == null) {
            return url;
        }

        return url.replace(redirectFrom, redirectTo);
    }

	private String stripUrlInfo(String url) {
		return url.substring(0, url.indexOf("?u=") + 1) + url.substring(url.indexOf("&v=") + 1);
	}

    private int getCurrentNetworkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo == null ? -1 : networkInfo.getType();
    }

	public int getInstance(Context context) {
		if(instance == null) {
			return Util.getActiveServer(context);
		} else {
			return instance;
		}
	}
	public String getRestUrl(Context context, String method) {
		return getRestUrl(context, method, true);
	}
	public String getRestUrl(Context context, String method, boolean allowAltAddress) {
		if(instance == null) {
			return Util.getRestUrl(context, method, allowAltAddress);
		} else {
			return Util.getRestUrl(context, method, instance, allowAltAddress);
		}
	}

	public SSLSocketFactory getSSLSocketFactory() {
		return sslSocketFactory;
	}
	public HostnameVerifier getHostNameVerifier() {
		return selfSignedHostnameVerifier;
	}
}
