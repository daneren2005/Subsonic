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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.*;
import github.daneren2005.dsub.service.parser.AlbumListParser;
import github.daneren2005.dsub.service.parser.BookmarkParser;
import github.daneren2005.dsub.service.parser.ChatMessageParser;
import github.daneren2005.dsub.service.parser.ErrorParser;
import github.daneren2005.dsub.service.parser.GenreParser;
import github.daneren2005.dsub.service.parser.IndexesParser;
import github.daneren2005.dsub.service.parser.JukeboxStatusParser;
import github.daneren2005.dsub.service.parser.LicenseParser;
import github.daneren2005.dsub.service.parser.LyricsParser;
import github.daneren2005.dsub.service.parser.MusicDirectoryParser;
import github.daneren2005.dsub.service.parser.MusicFoldersParser;
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
import github.daneren2005.dsub.service.parser.UserParser;
import github.daneren2005.dsub.service.ssl.SSLSocketFactory;
import github.daneren2005.dsub.service.ssl.TrustSelfSignedStrategy;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * @author Sindre Mehus
 */
public class RESTMusicService implements MusicService {

    private static final String TAG = RESTMusicService.class.getSimpleName();

    private static final int SOCKET_CONNECT_TIMEOUT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DEFAULT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DOWNLOAD = 30 * 1000;
    private static final int SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS = 60 * 1000;
    private static final int SOCKET_READ_TIMEOUT_GET_PLAYLIST = 60 * 1000;

    // Allow 20 seconds extra timeout per MB offset.
    private static final double TIMEOUT_MILLIS_PER_OFFSET_BYTE = 20000.0 / 1000000.0;

    private static final int HTTP_REQUEST_MAX_ATTEMPTS = 5;
    private static final long REDIRECTION_CHECK_INTERVAL_MILLIS = 60L * 60L * 1000L;

    private final DefaultHttpClient httpClient;
    private long redirectionLastChecked;
    private int redirectionNetworkType = -1;
    private String redirectFrom;
    private String redirectTo;
    private final ThreadSafeClientConnManager connManager;
	private Integer instance;

    public RESTMusicService() {

        // Create and initialize default HTTP parameters
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20));
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_DEFAULT);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Create and initialize scheme registry
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", createSSLSocketFactory(), 443));

        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
        httpClient = new DefaultHttpClient(connManager, params);
    }

    private SocketFactory createSSLSocketFactory() {
        try {
            return new SSLSocketFactory(new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Throwable x) {
            Log.e(TAG, "Failed to create custom SSL socket factory, using default.", x);
            return org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
        }
    }    

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "ping", null);
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
      
      Reader reader = getReader(context, progressListener, "getLicense", null);
        try {
            ServerInfo serverInfo = new LicenseParser(context, getInstance(context)).parse(reader);
            return serverInfo.isLicenseValid();
        } finally {
            Util.close(reader);
        }
    }

    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getMusicFolders", null);
        try {
            return new MusicFoldersParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }
    
	@Override
	public void startRescan(Context context, ProgressListener listener) throws Exception {
		Reader reader = getReader(context, listener, "startRescan", null);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
		
		// Now check if still running
		boolean done = false;
		while(!done) {
			reader = getReader(context, null, "scanstatus", null);
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

        Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "getArtists" : "getIndexes", null, parameterNames, parameterValues);
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
		if(id.indexOf(cacheLocn) != -1) {
			String search = Util.parseOfflineIDSearch(context, id, cacheLocn);
			SearchCritera critera = new SearchCritera(search, 1, 1, 0);
			SearchResult result = searchNew(critera, context, progressListener);
			if(result.getArtists().size() == 1) {
				id = result.getArtists().get(0).getId();
			} else if(result.getAlbums().size() == 1) {
				id = result.getAlbums().get(0).getId();
			}
		}
		
        Reader reader = getReader(context, progressListener, "getMusicDirectory", null, "id", id);
        try {
            return new MusicDirectoryParser(context, getInstance(context)).parse(name, reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public MusicDirectory getArtist(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getArtist", null, "id", id);
		try {
			return new MusicDirectoryParser(context, getInstance(context)).parse(name, reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getAlbum(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getAlbum", null, "id", id);
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
        Reader reader = getReader(context, progressListener, "search", null, parameterNames, parameterValues);
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
        List<Object> parameterValues = Arrays.<Object>asList(critera.getQuery(), critera.getArtistCount(),
                                                             critera.getAlbumCount(), critera.getSongCount());
        Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "search3" : "search2", null, parameterNames, parameterValues);
        try {
            return new SearchResult2Parser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getPlaylist(boolean refresh, String id, String name, Context context, ProgressListener progressListener) throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_PLAYLIST);

        Reader reader = getReader(context, progressListener, "getPlaylist", params, "id", id);
        try {
			return new PlaylistParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getPlaylists", null);
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

        Reader reader = getReader(context, progressListener, "createPlaylist", null, parameterNames, parameterValues);
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
	
	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {		
		Reader reader = getReader(context, progressListener, "deletePlaylist", null, "id", id);
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
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
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
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
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
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, Arrays.asList("playlistId", "name", "comment", "public"), Arrays.<Object>asList(id, name, comment, pub));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getLyrics", null, Arrays.asList("artist", "title"), Arrays.<Object>asList(artist, title));
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
        	reader = getReader(context, progressListener, "scrobble", null, Arrays.asList("id", "submission", "time"), Arrays.<Object>asList(id, submission, time));
        }
        else
        	reader = getReader(context, progressListener, "scrobble", null, Arrays.asList("id", "submission"), Arrays.<Object>asList(id, submission));
        try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "getAlbumList2" : "getAlbumList",
                                  null, Arrays.asList("type", "size", "offset"), Arrays.<Object>asList(type, size, offset));
        try {
            return new AlbumListParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	@Override
	public MusicDirectory getAlbumList(String type, String extra, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.10.1", "This type of album list is not supported");

		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		names.add("size");
		names.add("offset");

		values.add(size);
		values.add(offset);

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
			values.add(decade);
			values.add(decade + 10);
		}

		Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "getAlbumList2" : "getAlbumList", null, names, values);
		try {
			return new AlbumListParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
    public MusicDirectory getStarredList(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, Util.isTagBrowsing(context, getInstance(context)) ? "getStarred2" : "getStarred", null);
        try {
            return new StarredListParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getRandomSongs(int size, String musicFolderId, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);
		
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
			names.add("fromYear");
			values.add(startYear);
		}
		if(endYear != null && !"".equals(endYear)) {
			names.add("toYear");
			values.add(endYear);
		}

        Reader reader = getReader(context, progressListener, "getRandomSongs", params, names, values);
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
		builder.append("&id=").append(entry.getId());
		return builder.toString();
	}

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, ProgressListener progressListener) throws Exception {

        // Synchronize on the entry so that we don't download concurrently for the same song.
        synchronized (entry) {

            // Use cached file, if existing.
            Bitmap bitmap = FileUtil.getAlbumArtBitmap(context, entry, size);
            if (bitmap != null) {
                return bitmap;
            }

            String url = getRestUrl(context, "getCoverArt");

            InputStream in = null;
            try {
                List<String> parameterNames = Arrays.asList("id");
                List<Object> parameterValues = Arrays.<Object>asList(entry.getCoverArt());
                HttpEntity entity = getEntityForURL(context, url, null, parameterNames, parameterValues, progressListener);

				in = entity.getContent();
				Header contentEncoding = entity.getContentEncoding();
				if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
					in = new GZIPInputStream(in);
				}

                // If content type is XML, an error occured.  Get it.
                String contentType = Util.getContentType(entity);
                if (contentType != null && contentType.startsWith("text/xml")) {
                    new ErrorParser(context, getInstance(context)).parse(new InputStreamReader(in, Constants.UTF_8));
                    return null; // Never reached.
                }

                byte[] bytes = Util.toByteArray(in);
				OutputStream out = null;
				try {
					out = new FileOutputStream(FileUtil.getAlbumArtFile(context, entry));
					out.write(bytes);
				} finally {
					Util.close(out);
				}

				// Size == 0 -> only want to download
				if(size == 0) {
					return null;
				} else {
					return FileUtil.getSampledBitmap(bytes, size);
				}
            } finally {
                Util.close(in);
            }
        }
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, SilentBackgroundTask task) throws Exception {

        String url = getRestUrl(context, "stream");

        // Set socket read timeout. Note: The timeout increases as the offset gets larger. This is
        // to avoid the thrashing effect seen when offset is combined with transcoding/downsampling on the server.
        // In that case, the server uses a long time before sending any data, causing the client to time out.
        HttpParams params = new BasicHttpParams();
        int timeout = (int) (SOCKET_READ_TIMEOUT_DOWNLOAD + offset * TIMEOUT_MILLIS_PER_OFFSET_BYTE);
        HttpConnectionParams.setSoTimeout(params, timeout);

        // Add "Range" header if offset is given.
        List<Header> headers = new ArrayList<Header>();
        if (offset > 0) {
            headers.add(new BasicHeader("Range", "bytes=" + offset + "-"));
        }

		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("id");
		parameterNames.add("maxBitRate");

		List<Object> parameterValues = new ArrayList<Object>();
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
        HttpResponse response = getResponseForURL(context, url, params, parameterNames, parameterValues, headers, null, task);

        // If content type is XML, an error occurred.  Get it.
        String contentType = Util.getContentType(response.getEntity());
        if (contentType != null && contentType.startsWith("text/xml")) {
            InputStream in = response.getEntity().getContent();
			Header contentEncoding = response.getEntity().getContentEncoding();
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				in = new GZIPInputStream(in);
			}
            try {
                new ErrorParser(context, getInstance(context)).parse(new InputStreamReader(in, Constants.UTF_8));
            } finally {
                Util.close(in);
            }
        }

        return response;
    }

	@Override
	public String getMusicUrl(Context context, MusicDirectory.Entry song, int maxBitrate) throws Exception {
		StringBuilder builder = new StringBuilder(getRestUrl(context, "stream"));
		builder.append("&id=").append(song.getId());
		builder.append("&maxBitRate=").append(maxBitrate);

		String url = builder.toString();
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
        Reader reader = getReader(context, progressListener, "jukeboxControl", null, parameterNames, parameterValues);
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
		
		Reader reader = getReader(context, progressListener, starred ? "star" : "unstar", null, names, values);
    	try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
	
	@Override
	public List<Share> getShares(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Shares not supported.");

		Reader reader = getReader(context, progressListener, "getShares", null);
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

		Reader reader = getReader(context, progressListener, "createShare", null, parameterNames, parameterValues);
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

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("id");
		parameterValues.add(id);

		Reader reader = getReader(context, progressListener, "deleteShare", params, parameterNames, parameterValues);

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

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

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

		Reader reader = getReader(context, progressListener, "updateShare", params, parameterNames, parameterValues);
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

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("since");
		parameterValues.add(since);

		Reader reader = getReader(context, progressListener, "getChatMessages", params, parameterNames, parameterValues);

		try {
			return new ChatMessageParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.2", "Chat not supported.");

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("message");
		parameterValues.add(message);

		Reader reader = getReader(context, progressListener, "addChatMessage", params, parameterNames, parameterValues);

		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}
	
	@Override
	public List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Genres not supported.");
		
        Reader reader = getReader(context, progressListener, "getGenres", null);
        try {
            return new GenreParser(context, getInstance(context)).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Genres not supported.");

		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

		List<String> parameterNames = new ArrayList<String>();
		List<Object> parameterValues = new ArrayList<Object>();

		parameterNames.add("genre");
		parameterValues.add(genre);
		parameterNames.add("count");
		parameterValues.add(count);
		parameterNames.add("offset");
		parameterValues.add(offset);

		Reader reader = getReader(context, progressListener, "getSongsByGenre", params, parameterNames, parameterValues);

		try {
			return new RandomSongsParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}
	
	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "getPodcasts", null, Arrays.asList("includeEpisodes"), Arrays.<Object>asList("false"));
        try {
            List<PodcastChannel> channels = new PodcastChannelParser(context, getInstance(context)).parse(reader, progressListener);
			
			String content = "";
			for(PodcastChannel channel: channels) {
				content += channel.getName() + "\n";
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
		Reader reader = getReader(context, progressListener, "getPodcasts", null, Arrays.asList("id"), Arrays.<Object>asList(id));
        try {
            return new PodcastEntryParser(context, getInstance(context)).parse(id, reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Refresh podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "refreshPodcasts", null);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Creating podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "createPodcastChannel", null, "url", url);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "deletePodcastChannel", null, "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Downloading podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "downloadPodcastEpisode", null, "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void deletePodcastEpisode(String id, String parent, ProgressListener progressListener, Context context) throws Exception{
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "deletePodcastEpisode", null, "id", id);
		try {
            new ErrorParser(context, getInstance(context)).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public void setRating(MusicDirectory.Entry entry, int rating, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Setting ratings not supported.");
		
		Reader reader = getReader(context, progressListener, "setRating", null, Arrays.asList("id", "rating"), Arrays.<Object>asList(entry.getId(), rating));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public MusicDirectory getBookmarks(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Bookmarks not supported.");
		
		Reader reader = getReader(context, progressListener, "getBookmarks", null);
		try {
			return new BookmarkParser(context, getInstance(context)).parse(reader, progressListener);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void createBookmark(MusicDirectory.Entry entry, int position, String comment, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Creating bookmarks not supported.");
		
		Reader reader = getReader(context, progressListener, "createBookmark", null, Arrays.asList("id", "position", "comment"), Arrays.<Object>asList(entry.getId(), position, comment));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void deleteBookmark(MusicDirectory.Entry entry, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Deleting bookmarks not supported.");
		
		Reader reader = getReader(context, progressListener, "deleteBookmark", null, Arrays.asList("id"), Arrays.<Object>asList(entry.getId()));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public User getUser(boolean refresh, String username, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "getUser", null, Arrays.asList("username"), Arrays.<Object>asList(username));
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

		Reader reader = getReader(context, progressListener, "getUsers", null);
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

		Reader reader = getReader(context, progressListener, "createUser", null, names, values);
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

		Reader reader = getReader(context, progressListener, "updateUser", null, names, values);
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void deleteUser(String username, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "deleteUser", null, Arrays.asList("username"), Arrays.<Object>asList(username));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void changeEmail(String username, String email, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "updateUser", null, Arrays.asList("username", "email"), Arrays.<Object>asList(username, email));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public void changePassword(String username, String password, Context context, ProgressListener progressListener) throws Exception {
		Reader reader = getReader(context, progressListener, "changePassword", null, Arrays.asList("username", "password"), Arrays.<Object>asList(username, password));
		try {
			new ErrorParser(context, getInstance(context)).parse(reader);
		} finally {
			Util.close(reader);
		}
	}

	@Override
	public Bitmap getAvatar(String username, int size, Context context, ProgressListener progressListener) throws Exception {
		// Return silently if server is too old
		if (!ServerInfo.checkServerVersion(context, "1.8")) {
			return null;
		}

		// Synchronize on the username so that we don't download concurrently for
		// the same user.
		synchronized (username) {
			// Use cached file, if existing.
			Bitmap bitmap = FileUtil.getAvatarBitmap(context, username, size);
			if(bitmap != null) {
				return bitmap;
			}

			String url = Util.getRestUrl(context, "getAvatar");
			InputStream in = null;
			try
			{
				List<String> parameterNames;
				List<Object> parameterValues;

				parameterNames = Collections.singletonList("username");
				parameterValues = Arrays.<Object>asList(username);

				HttpEntity entity = getEntityForURL(context, url, null, parameterNames, parameterValues, progressListener);
				in = entity.getContent();
				Header contentEncoding = entity.getContentEncoding();
				if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
					in = new GZIPInputStream(in);
				}

				// If content type is XML, an error occurred. Get it.
				String contentType = Util.getContentType(entity);
				if (contentType != null && contentType.startsWith("text/xml"))
				{
					new ErrorParser(context, getInstance(context)).parse(new InputStreamReader(in, Constants.UTF_8));
					return null; // Never reached.
				}

				byte[] bytes = Util.toByteArray(in);
				OutputStream out = null;
				try {
					out = new FileOutputStream(FileUtil.getAvatarFile(context, username));
					out.write(bytes);
				} finally {
					Util.close(out);
				}

				return FileUtil.getSampledBitmap(bytes, size);
			}
			finally {
				Util.close(in);
			}
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
			String id = offline.getString(Constants.OFFLINE_SCROBBLE_ID + i, null);
			long time = offline.getLong(Constants.OFFLINE_SCROBBLE_TIME + i, 0);
			if(id != null) {
				scrobble(id, true, time, context, progressListener);
			} else {
				String search = offline.getString(Constants.OFFLINE_SCROBBLE_SEARCH + i, "");
				try{
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
				catch(Exception e){
					Log.e(TAG, e.toString());
					retry++;
				}
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
					if(result.getSongs().size() == 1){
						Log.i(TAG, "Query '" + search + "' returned song " + result.getSongs().get(0).getTitle() + " by " + result.getSongs().get(0).getArtist() + " with id " + result.getSongs().get(0).getId());
						setStarred(Arrays.asList(result.getSongs().get(0)), null, null, starred, progressListener, context);
					} else if(result.getAlbums().size() == 1){
						Log.i(TAG, "Query '" + search + "' returned song " + result.getAlbums().get(0).getTitle() + " by " + result.getAlbums().get(0).getArtist() + " with id " + result.getAlbums().get(0).getId());
						setStarred(Arrays.asList(result.getAlbums().get(0)), null, null, starred, progressListener, context);
					}
					else{
						throw new Exception("Song not found on server");
					}
				}
				catch(Exception e){
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
		if(id.indexOf(cacheLocn) != -1) {
			String searchCriteria = Util.parseOfflineIDSearch(context, id, cacheLocn);
			SearchCritera critera = new SearchCritera(searchCriteria, 0, 0, 1);
			SearchResult result = searchNew(critera, context, progressListener);
			if(result.getSongs().size() == 1){
				id = result.getSongs().get(0).getId();
			}
		}
		
		return id;
	}
	
	@Override
	public void setInstance(Integer instance)  throws Exception {
		this.instance = instance;
	}

    private Reader getReader(Context context, ProgressListener progressListener, String method, HttpParams requestParams) throws Exception {
        return getReader(context, progressListener, method, requestParams, Collections.<String>emptyList(), Collections.emptyList());
    }

    private Reader getReader(Context context, ProgressListener progressListener, String method,
                             HttpParams requestParams, String parameterName, Object parameterValue) throws Exception {
        return getReader(context, progressListener, method, requestParams, Arrays.asList(parameterName), Arrays.<Object>asList(parameterValue));
    }

    private Reader getReader(Context context, ProgressListener progressListener, String method,
                             HttpParams requestParams, List<String> parameterNames, List<Object> parameterValues) throws Exception {

        if (progressListener != null) {
            progressListener.updateProgress(R.string.service_connecting);
        }

        String url = getRestUrl(context, method);
        return getReaderForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
    }

    private Reader getReaderForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                   List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        HttpEntity entity = getEntityForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
        if (entity == null) {
            throw new RuntimeException("No entity received for URL " + url);
        }

        InputStream in = entity.getContent();
		Header contentEncoding = entity.getContentEncoding();
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			in = new GZIPInputStream(in);
		}
        return new InputStreamReader(in, Constants.UTF_8);
    }

    private HttpEntity getEntityForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                       List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        return getResponseForURL(context, url, requestParams, parameterNames, parameterValues, null, progressListener, null).getEntity();
    }

    private HttpResponse getResponseForURL(Context context, String url, HttpParams requestParams,
                                           List<String> parameterNames, List<Object> parameterValues,
                                           List<Header> headers, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
	// If not too many parameters, extract them to the URL rather than relying on the HTTP POST request being
        // received intact. Remember, HTTP POST requests are converted to GET requests during HTTP redirects, thus
        // loosing its entity.
        if (parameterNames != null && parameterNames.size() < 10) {
            StringBuilder builder = new StringBuilder(url);
            for (int i = 0; i < parameterNames.size(); i++) {
                builder.append("&").append(parameterNames.get(i)).append("=");
                builder.append(URLEncoder.encode(String.valueOf(parameterValues.get(i)), "UTF-8"));
            }
            url = builder.toString();
            parameterNames = null;
            parameterValues = null;
        }

        String rewrittenUrl = rewriteUrlWithRedirect(context, url);
        return executeWithRetry(context, rewrittenUrl, url, requestParams, parameterNames, parameterValues, headers, progressListener, task);
    }

    private HttpResponse executeWithRetry(Context context, String url, String originalUrl, HttpParams requestParams,
                                          List<String> parameterNames, List<Object> parameterValues,
                                          List<Header> headers, ProgressListener progressListener, SilentBackgroundTask task) throws Exception {
		// Strip out sensitive information from log
		if(url.indexOf("scanstatus") == -1) {
			Log.i(TAG, stripUrlInfo(url));
		}

		SharedPreferences prefs = Util.getPreferences(context);
		int networkTimeout = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT, "15000"));
		HttpParams newParams = httpClient.getParams();
		HttpConnectionParams.setSoTimeout(newParams, networkTimeout);
		httpClient.setParams(newParams);

        final AtomicReference<Boolean> isCancelled = new AtomicReference<Boolean>(false);
        int attempts = 0;
        while (true) {
            attempts++;
            HttpContext httpContext = new BasicHttpContext();
            final HttpPost request = new HttpPost(url);

            if (task != null) {
                // Attempt to abort the HTTP request if the task is cancelled.
                task.setOnCancelListener(new BackgroundTask.OnCancelListener() {
                    @Override
                    public void onCancel() {
						try {
							isCancelled.set(true);
							request.abort();
						} catch(Exception e) {
							Log.e(TAG, "Failed to stop http task");
						}
                    }
                });
            }

            if (parameterNames != null) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (int i = 0; i < parameterNames.size(); i++) {
                    params.add(new BasicNameValuePair(parameterNames.get(i), String.valueOf(parameterValues.get(i))));
                }
                request.setEntity(new UrlEncodedFormEntity(params, Constants.UTF_8));
            }

            if (requestParams != null) {
                request.setParams(requestParams);
            }

            if (headers != null) {
                for (Header header : headers) {
                    request.addHeader(header);
                }
            }
			request.addHeader("Accept-Encoding", "gzip");

            // Set credentials to get through apache proxies that require authentication.
            int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
            String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
            String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(username, password));

            try {
                HttpResponse response = httpClient.execute(request, httpContext);
                detectRedirect(originalUrl, context, httpContext);
                return response;
            } catch (IOException x) {
                request.abort();
                if (attempts >= HTTP_REQUEST_MAX_ATTEMPTS || isCancelled.get()) {
                    throw x;
                }
                if (progressListener != null) {
                    String msg = context.getResources().getString(R.string.music_service_retry, attempts, HTTP_REQUEST_MAX_ATTEMPTS - 1);
                    progressListener.updateProgress(msg);
                }
                Log.w(TAG, "Got IOException " + x + " (" + attempts + "), will retry");
                increaseTimeouts(requestParams);
				Thread.sleep(2000L);
            }
        }
    }

    private void increaseTimeouts(HttpParams requestParams) {
        if (requestParams != null) {
            int connectTimeout = HttpConnectionParams.getConnectionTimeout(requestParams);
            if (connectTimeout != 0) {
                HttpConnectionParams.setConnectionTimeout(requestParams, (int) (connectTimeout * 1.3F));
            }
            int readTimeout = HttpConnectionParams.getSoTimeout(requestParams);
            if (readTimeout != 0) {
                HttpConnectionParams.setSoTimeout(requestParams, (int) (readTimeout * 1.5F));
            }
        }
    }

    private void detectRedirect(String originalUrl, Context context, HttpContext httpContext) {
        HttpUriRequest request = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		
		// Sometimes the request doesn't contain the "http://host" part
		String redirectedUrl;
		if (request.getURI().getScheme() == null) {
			redirectedUrl = host.toURI() + request.getURI();
		} else {
			redirectedUrl = request.getURI().toString();
		}

        redirectFrom = originalUrl.substring(0, originalUrl.indexOf("/rest/"));
        redirectTo = redirectedUrl.substring(0, redirectedUrl.indexOf("/rest/"));

		if(redirectFrom.compareTo(redirectTo) != 0) {
        	Log.i(TAG, redirectFrom + " redirects to " + redirectTo);
		}
        redirectionLastChecked = System.currentTimeMillis();
        redirectionNetworkType = getCurrentNetworkType(context);
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
}
