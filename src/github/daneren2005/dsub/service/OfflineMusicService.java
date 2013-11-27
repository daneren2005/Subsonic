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

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Bookmark;
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
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import java.io.*;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * @author Sindre Mehus
 */
public class OfflineMusicService extends RESTMusicService {
	private static final String TAG = OfflineMusicService.class.getSimpleName();

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        return true;
    }

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<Artist> artists = new ArrayList<Artist>();
        File root = FileUtil.getMusicDirectory(context);
        for (File file : FileUtil.listFiles(root)) {
            if (file.isDirectory()) {
                Artist artist = new Artist();
                artist.setId(file.getPath());
                artist.setIndex(file.getName().substring(0, 1));
                artist.setName(file.getName());
                artists.add(artist);
            }
        }
		
		SharedPreferences prefs = Util.getPreferences(context);
		String ignoredArticlesString = prefs.getString(Constants.CACHE_KEY_IGNORE, "The El La Los Las Le Les");
		final String[] ignoredArticles = ignoredArticlesString.split(" ");
		
		Collections.sort(artists, new Comparator<Artist>() {
			public int compare(Artist lhsArtist, Artist rhsArtist) {
				String lhs = lhsArtist.getName().toLowerCase();
				String rhs = rhsArtist.getName().toLowerCase();
				
				char lhs1 = lhs.charAt(0);
				char rhs1 = rhs.charAt(0);
				
				if(Character.isDigit(lhs1) && !Character.isDigit(rhs1)) {
					return 1;
				} else if(Character.isDigit(rhs1) && !Character.isDigit(lhs1)) {
					return -1;
				}
				
				for(String article: ignoredArticles) {
					int index = lhs.indexOf(article.toLowerCase() + " ");
					if(index == 0) {
						lhs = lhs.substring(article.length() + 1);
					}
					index = rhs.indexOf(article.toLowerCase() + " ");
					if(index == 0) {
						rhs = rhs.substring(article.length() + 1);
					}
				}
				
				return lhs.compareTo(rhs);
			}
		});
		
        return new Indexes(0L, Collections.<Artist>emptyList(), artists);
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, String artistName, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        File dir = new File(id);
        MusicDirectory result = new MusicDirectory();
        result.setName(dir.getName());

        Set<String> names = new HashSet<String>();

        for (File file : FileUtil.listMediaFiles(dir)) {
            String name = getName(file);
            if (name != null & !names.contains(name)) {
                names.add(name);
                result.addChild(createEntry(context, file, name));
            }
        }
		result.sortChildren();
        return result;
    }

    private String getName(File file) {
        String name = file.getName();
        if (file.isDirectory()) {
            return name;
        }

        if (name.endsWith(".partial") || name.contains(".partial.") || name.equals(Constants.ALBUM_ART_FILE)) {
            return null;
        }

        name = name.replace(".complete", "");
        return FileUtil.getBaseName(name);
    }

	private MusicDirectory.Entry createEntry(Context context, File file, String name) {
		return createEntry(context, file, name, true);
	}
    private MusicDirectory.Entry createEntry(Context context, File file, String name, boolean load) {
        MusicDirectory.Entry entry = new MusicDirectory.Entry();
        entry.setDirectory(file.isDirectory());
        entry.setId(file.getPath());
        entry.setParent(file.getParent());
        entry.setSize(file.length());
        String root = FileUtil.getMusicDirectory(context).getPath();
		if(!file.getParentFile().getParentFile().getPath().equals(root)) {
			entry.setGrandParent(file.getParentFile().getParent());
		}
        entry.setPath(file.getPath().replaceFirst("^" + root + "/" , ""));
		String title = name;
        if (file.isFile()) {
			File artistFolder = file.getParentFile().getParentFile();
			File albumFolder = file.getParentFile();
			if(artistFolder.getPath().equals(root)) {
				entry.setArtist(albumFolder.getName());
			} else {
				entry.setArtist(artistFolder.getName());
			}
            entry.setAlbum(albumFolder.getName());
			
			int index = name.indexOf('-');
			if(index != -1) {
				try {
					entry.setTrack(Integer.parseInt(name.substring(0, index)));
					title = title.substring(index + 1);
				} catch(Exception e) {
					// Failed parseInt, just means track filled out
				}
			}
			
			if(load) {
				entry.loadMetadata(file);
			}
        }
		
        entry.setTitle(title);
        entry.setSuffix(FileUtil.getExtension(file.getName().replace(".complete", "")));

        File albumArt = FileUtil.getAlbumArtFile(context, entry);
        if (albumArt.exists()) {
            entry.setCoverArt(albumArt.getPath());
        }
		if(FileUtil.isVideoFile(file)) {
			entry.setVideo(true);
		}
        return entry;
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception {
		try {
			return FileUtil.getAlbumArtBitmap(context, entry, size);
		} catch(Exception e) {
			return null;
		}
    }

    @Override
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Music folders not available in offline mode");
    }

    @Override
    public SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception {
		List<Artist> artists = new ArrayList<Artist>();
		List<MusicDirectory.Entry> albums = new ArrayList<MusicDirectory.Entry>();
		List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>();
        File root = FileUtil.getMusicDirectory(context);
		int closeness = 0;
        for (File artistFile : FileUtil.listFiles(root)) {
			String artistName = artistFile.getName();
            if (artistFile.isDirectory()) {
				if((closeness = matchCriteria(criteria, artistName)) > 0) {
					Artist artist = new Artist();
					artist.setId(artistFile.getPath());
					artist.setIndex(artistFile.getName().substring(0, 1));
					artist.setName(artistName);
					artist.setCloseness(closeness);
					artists.add(artist);
				}
				
				recursiveAlbumSearch(artistName, artistFile, criteria, context, albums, songs);
            }
        }
		
		Collections.sort(artists, new Comparator<Artist>() {
			public int compare(Artist lhs, Artist rhs) {
				if(lhs.getCloseness() == rhs.getCloseness()) {
					return 0;
				}
				else if(lhs.getCloseness() > rhs.getCloseness()) {
					return -1;
				}
				else {
					return 1;
				}
			}
		});
		Collections.sort(albums, new Comparator<MusicDirectory.Entry>() {
			public int compare(MusicDirectory.Entry lhs, MusicDirectory.Entry rhs) {
				if(lhs.getCloseness() == rhs.getCloseness()) {
					return 0;
				}
				else if(lhs.getCloseness() > rhs.getCloseness()) {
					return -1;
				}
				else {
					return 1;
				}
			}
		});
		Collections.sort(songs, new Comparator<MusicDirectory.Entry>() {
			public int compare(MusicDirectory.Entry lhs, MusicDirectory.Entry rhs) {
				if(lhs.getCloseness() == rhs.getCloseness()) {
					return 0;
				}
				else if(lhs.getCloseness() > rhs.getCloseness()) {
					return -1;
				}
				else {
					return 1;
				}
			}
		});
		
		return new SearchResult(artists, albums, songs);
    }
	
	private void recursiveAlbumSearch(String artistName, File file, SearchCritera criteria, Context context, List<MusicDirectory.Entry> albums, List<MusicDirectory.Entry> songs) {
		int closeness;
		for(File albumFile : FileUtil.listMediaFiles(file)) {
			if(albumFile.isDirectory()) {
				String albumName = getName(albumFile);
				if((closeness = matchCriteria(criteria, albumName)) > 0) {
					MusicDirectory.Entry album = createEntry(context, albumFile, albumName);
					album.setArtist(artistName);
					album.setCloseness(closeness);
					albums.add(album);
				}

				for(File songFile : FileUtil.listMediaFiles(albumFile)) {
					String songName = getName(songFile);
					if(songFile.isDirectory()) {
						recursiveAlbumSearch(artistName, songFile, criteria, context, albums, songs);
					}
					else if((closeness = matchCriteria(criteria, songName)) > 0){
						MusicDirectory.Entry song = createEntry(context, albumFile, songName);
						song.setArtist(artistName);
						song.setAlbum(albumName);
						song.setCloseness(closeness);
						songs.add(song);
					}
				}
			}
			else {
				String songName = getName(albumFile);
				if((closeness = matchCriteria(criteria, songName)) > 0) {
					MusicDirectory.Entry song = createEntry(context, albumFile, songName);
					song.setArtist(artistName);
					song.setAlbum(songName);
					song.setCloseness(closeness);
					songs.add(song);
				}
			}
		}
	}
	private int matchCriteria(SearchCritera criteria, String name) {
		String query = criteria.getQuery().toLowerCase();
		String[] queryParts = query.split(" ");
		String[] nameParts = name.toLowerCase().split(" ");
		
		int closeness = 0;
		for(String queryPart : queryParts) {
			for(String namePart : nameParts) {
				if(namePart.equals(queryPart)) {
					closeness++;
				}
			}
		}
		
		return closeness;
	}

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<Playlist> playlists = new ArrayList<Playlist>();
        File root = FileUtil.getPlaylistDirectory();
		String lastServer = null;
		boolean removeServer = true;
        for (File folder : FileUtil.listFiles(root)) {
			if(folder.isDirectory()) {
				String server = folder.getName();
				SortedSet<File> fileList = FileUtil.listFiles(folder);
				for(File file: fileList) {
					if(FileUtil.isPlaylistFile(file)) {
						String id = file.getName();
						String filename = server + ": " + FileUtil.getBaseName(id);
						Playlist playlist = new Playlist(server, filename);
						playlists.add(playlist);
					}
				}
				
				if(!server.equals(lastServer) && fileList.size() > 0) {
					if(lastServer != null) {
						removeServer = false;
					}
					lastServer = server;
				}
			} else {
				// Delete legacy playlist files
				try {
					folder.delete();
				} catch(Exception e) {
					Log.w(TAG, "Failed to delete old playlist file: " + folder.getName());
				}
			}
        }
		
		if(removeServer) {
			for(Playlist playlist: playlists) {
				playlist.setName(playlist.getName().substring(playlist.getId().length() + 2));
			}
		}
        return playlists;
    }

    @Override
    public MusicDirectory getPlaylist(boolean refresh, String id, String name, Context context, ProgressListener progressListener) throws Exception {
		DownloadService downloadService = DownloadServiceImpl.getInstance();
        if (downloadService == null) {
            return new MusicDirectory();
        }
		
        Reader reader = null;
		BufferedReader buffer = null;
		try {
			int firstIndex = name.indexOf(id);
			if(firstIndex != -1) {
				name = name.substring(id.length() + 2);
			}
			
			File playlistFile = FileUtil.getPlaylistFile(id, name);
			reader = new FileReader(playlistFile);
			buffer = new BufferedReader(reader);
			
			MusicDirectory playlist = new MusicDirectory();
			String line = buffer.readLine();
	    	if(!"#EXTM3U".equals(line)) return playlist;
			
			while( (line = buffer.readLine()) != null ){
				File entryFile = new File(line);
				String entryName = getName(entryFile);
				if(entryFile.exists() && entryName != null){
					playlist.addChild(createEntry(context, entryFile, entryName, false));
				}
			}
			
			return playlist;
		} finally {
			Util.close(buffer);
			Util.close(reader);
		}
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Playlists not available in offline mode");
    }
	
	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Playlists not available in offline mode");
	}
	
	@Override
	public void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Updating playlist not available in offline mode");
	}
	
	@Override
	public void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Removing from playlist not available in offline mode");
	}
	
	@Override
	public void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Overwriting playlist not available in offline mode");
	}
	
	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Updating playlist not available in offline mode");
	}

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Lyrics not available in offline mode");
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
		if(!submission) {
			return;
		}

		SharedPreferences prefs = Util.getPreferences(context);
		String cacheLocn = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);

		SharedPreferences offline = Util.getOfflineSync(context);
		int scrobbles = offline.getInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		scrobbles++;
		SharedPreferences.Editor offlineEditor = offline.edit();
		
		if(id.indexOf(cacheLocn) != -1) {
			String scrobbleSearchCriteria = Util.parseOfflineIDSearch(context, id, cacheLocn);
			offlineEditor.putString(Constants.OFFLINE_SCROBBLE_SEARCH + scrobbles, scrobbleSearchCriteria);
			offlineEditor.remove(Constants.OFFLINE_SCROBBLE_ID + scrobbles);
		} else {
			offlineEditor.putString(Constants.OFFLINE_SCROBBLE_ID + scrobbles, id);
			offlineEditor.remove(Constants.OFFLINE_SCROBBLE_SEARCH + scrobbles);
		}
		
		offlineEditor.putLong(Constants.OFFLINE_SCROBBLE_TIME + scrobbles, System.currentTimeMillis());
		offlineEditor.putInt(Constants.OFFLINE_SCROBBLE_COUNT, scrobbles);
		offlineEditor.commit();
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Album lists not available in offline mode");
    }

    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        return null;
    }
	
	@Override
    public String getVideoStreamUrl(String format, int maxBitrate, Context context, String id) throws Exception {
        return null;
    }
	
	@Override
	public String getHlsUrl(String id, int bitRate, Context context) throws Exception {
		return null;
	}

    @Override
    public RemoteStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public RemoteStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public RemoteStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public RemoteStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public RemoteStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public RemoteStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }
	
	@Override
	public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
		SharedPreferences prefs = Util.getPreferences(context);
		String cacheLocn = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);

		SharedPreferences offline = Util.getOfflineSync(context);
		int stars = offline.getInt(Constants.OFFLINE_STAR_COUNT, 0);
		stars++;
		SharedPreferences.Editor offlineEditor = offline.edit();
		
		if(id.indexOf(cacheLocn) != -1) {
			String searchCriteria = Util.parseOfflineIDSearch(context, id, cacheLocn);
			offlineEditor.putString(Constants.OFFLINE_STAR_SEARCH + stars, searchCriteria);
			offlineEditor.remove(Constants.OFFLINE_STAR_ID + stars);
		} else {
			offlineEditor.putString(Constants.OFFLINE_STAR_ID + stars, id);
			offlineEditor.remove(Constants.OFFLINE_STAR_SEARCH + stars);
		}
		
		offlineEditor.putBoolean(Constants.OFFLINE_STAR_SETTING + stars, starred);
		offlineEditor.putInt(Constants.OFFLINE_STAR_COUNT, stars);
		offlineEditor.commit();
	}
	
	@Override
	public List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Getting Genres not available in offline mode");
	}
	
	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Getting Songs By Genre not available in offline mode");
	}

    @Override
    public MusicDirectory getRandomSongs(int size, String folder, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception {
        File root = FileUtil.getMusicDirectory(context);
        List<File> children = new LinkedList<File>();
        listFilesRecursively(root, children);
        MusicDirectory result = new MusicDirectory();

        if (children.isEmpty()) {
            return result;
        }
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            File file = children.get(random.nextInt(children.size()));
            result.addChild(createEntry(context, file, getName(file)));
        }

        return result;
    }
	
	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		List<PodcastChannel> channels = new ArrayList<PodcastChannel>();
		
		File dir = FileUtil.getPodcastDirectory(context);
		String line;
		for(File file: dir.listFiles()) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null && !"".equals(line)) {
				PodcastChannel channel = new PodcastChannel();
				channel.setId(line);
				channel.setName(line);
				channel.setStatus("completed");
				
				if(FileUtil.getPodcastDirectory(context, channel).exists()) { 
					channels.add(channel);
				}
			}
			br.close();
		}
		
		return channels;
	}
	
	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		return getMusicDirectory(FileUtil.getPodcastDirectory(context, id).getPath(), null, false, context, progressListener);
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}

	@Override
	public void setRating(String id, int rating, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Setting ratings not available in offline mode");
	}

	@Override
	public List<Bookmark> getBookmarks(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Getting bookmarks not available in offline mode");
	}

	@Override
	public void createBookmark(String id, int position, String comment, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Creating bookmarks not available in offline mode");
	}

	@Override
	public void deleteBookmark(String id, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Deleting bookmarks not available in offline mode");
	}
    
    @Override
    public int processOfflineSyncs(final Context context, final ProgressListener progressListener) throws Exception{
		throw new OfflineException("Offline scrobble cached can not be processes while in offline mode");
    }
    
    @Override
    public void setInstance(Integer instance) throws Exception{
    	throw new OfflineException("Offline servers only have one instance");
    }

    private void listFilesRecursively(File parent, List<File> children) {
        for (File file : FileUtil.listMediaFiles(parent)) {
            if (file.isFile()) {
                children.add(file);
            } else {
                listFilesRecursively(file, children);
            }
        }
    }
}
