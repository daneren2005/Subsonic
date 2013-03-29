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
import java.io.FileInputStream;
import java.io.InputStream;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.JukeboxStatus;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.service.parser.PlaylistParser;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import java.io.*;
import java.util.Comparator;

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
				
				int index = lhs.indexOf("The ");
				if(index == 0) {
					lhs = lhs.substring(4, 0);
				}
				index = rhs.indexOf("The ");
				if(index == 0) {
					rhs = rhs.substring(4, 0);
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
        MusicDirectory.Entry entry = new MusicDirectory.Entry();
        entry.setDirectory(file.isDirectory());
        entry.setId(file.getPath());
        entry.setParent(file.getParent());
        entry.setSize(file.length());
        String root = FileUtil.getMusicDirectory(context).getPath();
        entry.setPath(file.getPath().replaceFirst("^" + root + "/" , ""));
		String title = name;
        if (file.isFile()) {
            entry.setArtist(file.getParentFile().getParentFile().getName());
            entry.setAlbum(file.getParentFile().getName());
			
			int index = name.indexOf('-');
			if(index != -1) {
				try {
					entry.setTrack(Integer.parseInt(name.substring(0, index)));
					title = title.substring(index + 1);
				} catch(Exception e) {
					// Failed parseInt, just means track filled out
				}
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
        for (File file : FileUtil.listFiles(root)) {
			if(FileUtil.isPlaylistFile(file)) {
				String id = file.getName();
				String filename = FileUtil.getBaseName(id);
				Playlist playlist = new Playlist(id, filename);
				playlists.add(playlist);
			} else {
				// Delete legacy playlist files
				try {
					file.delete();
				} catch(Exception e) {
					Log.w(TAG, "Failed to delete old playlist file: " + file.getName());
				}
			}
        }
        return playlists;
    }

    @Override
    public MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception {
		DownloadService downloadService = DownloadServiceImpl.getInstance();
        if (downloadService == null) {
            return new MusicDirectory();
        }
		
        Reader reader = null;
		BufferedReader buffer = null;
		try {
			File playlistFile = FileUtil.getPlaylistFile(name);
			reader = new FileReader(playlistFile);
			buffer = new BufferedReader(reader);
			
			MusicDirectory playlist = new MusicDirectory();
			String line = buffer.readLine();
	    	if(!"#EXTM3U".equals(line)) return playlist;
			
			while( (line = buffer.readLine()) != null ){
				File entryFile = new File(line);
				String entryName = getName(entryFile);
				if(entryFile.exists() && entryName != null){
					playlist.addChild(createEntry(context, entryFile, entryName));
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
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Updating playlist not available in offline mode");
	}

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Lyrics not available in offline mode");
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Scrobbling not available in offline mode");
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
    public String getVideoStreamUrl(int maxBitrate, Context context, String id) {
        return null;
    }

    @Override
    public JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }
	
	@Override
	public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Starring not available in offline mode");
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
