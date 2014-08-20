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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.ChatMessage;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.domain.RemoteStatus;
import github.daneren2005.dsub.domain.Lyrics;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.TimeLimitedCache;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

import static github.daneren2005.dsub.domain.MusicDirectory.Entry;

/**
 * @author Sindre Mehus
 */
public class CachedMusicService implements MusicService {
	private static final String TAG = CachedMusicService.class.getSimpleName();

    private static final int MUSIC_DIR_CACHE_SIZE = 20;
    private static final int TTL_MUSIC_DIR = 5 * 60; // Five minutes

	private final RESTMusicService musicService;
    private final TimeLimitedCache<Boolean> cachedLicenseValid = new TimeLimitedCache<Boolean>(120, TimeUnit.SECONDS);
    private final TimeLimitedCache<Indexes> cachedIndexes = new TimeLimitedCache<Indexes>(60 * 60, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<Playlist>> cachedPlaylists = new TimeLimitedCache<List<Playlist>>(3600, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<MusicFolder>> cachedMusicFolders = new TimeLimitedCache<List<MusicFolder>>(10 * 3600, TimeUnit.SECONDS);
	private final TimeLimitedCache<List<PodcastChannel>> cachedPodcastChannels = new TimeLimitedCache<List<PodcastChannel>>(10 * 3600, TimeUnit.SECONDS);
    private String restUrl;
	private boolean isTagBrowsing = false;

    public CachedMusicService(RESTMusicService musicService) {
        this.musicService = musicService;
    }

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        musicService.ping(context, progressListener);
    }

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        Boolean result = cachedLicenseValid.get();
        if (result == null) {
			result = FileUtil.deserialize(context, getCacheName(context, "license"), Boolean.class);

			if(result == null) {
            	result = musicService.isLicenseValid(context, progressListener);

				// Only save a copy license is valid
				if(result) {
					FileUtil.serialize(context, (Boolean) result, getCacheName(context, "license"));
				}
			}
            cachedLicenseValid.set(result, result ? 30L * 60L : 2L * 60L, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedMusicFolders.clear();
        }
        List<MusicFolder> result = cachedMusicFolders.get();
        if (result == null) {
        	if(!refresh) {
        		result = FileUtil.deserialize(context, getCacheName(context, "musicFolders"), ArrayList.class);
        	}
        	
        	if(result == null) {
            	result = musicService.getMusicFolders(refresh, context, progressListener);
            	FileUtil.serialize(context, new ArrayList<MusicFolder>(result), getCacheName(context, "musicFolders"));
        	}
            cachedMusicFolders.set(result);
        }
        return result;
    }

	@Override
	public void startRescan(Context context, ProgressListener listener) throws Exception {
		musicService.startRescan(context, listener);
	}

	@Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedIndexes.clear();
            cachedMusicFolders.clear();
        }
        Indexes result = cachedIndexes.get();
        if (result == null) {
			String name = Util.isTagBrowsing(context, musicService.getInstance(context)) ? "artists" : "indexes";
			name = getCacheName(context, name, musicFolderId);
			if(!refresh) {
				result = FileUtil.deserialize(context, name, Indexes.class);
			}
        	
        	if(result == null) {
            	result = musicService.getIndexes(musicFolderId, refresh, context, progressListener);
            	FileUtil.serialize(context, result, name);
        	}
            cachedIndexes.set(result);
        }
        return result;
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		MusicDirectory dir = null;

		if(!refresh) {
			dir = FileUtil.deserialize(context, getCacheName(context, "directory", id), MusicDirectory.class);
		}

		if(dir == null) {
			dir = musicService.getMusicDirectory(id, name, refresh, context, progressListener);
			FileUtil.serialize(context, dir, getCacheName(context, "directory", id));
		}

		return dir;
    }

	@Override
	public MusicDirectory getArtist(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		MusicDirectory dir = null;

		if(!refresh) {
			dir = FileUtil.deserialize(context, getCacheName(context, "artist", id), MusicDirectory.class);
		}

		if(dir == null) {
			dir = musicService.getArtist(id, name, refresh, context, progressListener);
			FileUtil.serialize(context, dir, getCacheName(context, "artist", id));
		}

		return dir;
	}

	@Override
	public MusicDirectory getAlbum(String id, String name, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		MusicDirectory dir = null;

		if(!refresh) {
			dir = FileUtil.deserialize(context, getCacheName(context, "album", id), MusicDirectory.class);
		}

		if(dir == null) {
			dir = musicService.getAlbum(id, name, refresh, context, progressListener);
			FileUtil.serialize(context, dir, getCacheName(context, "album", id));
		}

		return dir;
	}

	@Override
    public SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception {
        return musicService.search(criteria, context, progressListener);
    }

    @Override
    public MusicDirectory getPlaylist(boolean refresh, String id, String name, Context context, ProgressListener progressListener) throws Exception {
		MusicDirectory dir = null;
		MusicDirectory cachedPlaylist = FileUtil.deserialize(context, getCacheName(context, "playlist", id), MusicDirectory.class);
		if(!refresh) {
			dir = cachedPlaylist;
		}
		if(dir == null) {
			dir = musicService.getPlaylist(refresh, id, name, context, progressListener);
			FileUtil.serialize(context, dir, getCacheName(context, "playlist", id));

			File playlistFile = FileUtil.getPlaylistFile(context, Util.getServerName(context, musicService.getInstance(context)), dir.getName());
			if(cachedPlaylist == null || !playlistFile.exists() || !cachedPlaylist.getChildren().equals(dir.getChildren())) {
				FileUtil.writePlaylistFile(context, playlistFile, dir);
			}
		}
        return dir;
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        List<Playlist> result = refresh ? null : cachedPlaylists.get();
        if (result == null) {
        	if(!refresh) {
        		result = FileUtil.deserialize(context, getCacheName(context, "playlist"), ArrayList.class);
        	}
        	
        	if(result == null) {
	        	result = musicService.getPlaylists(refresh, context, progressListener);
	        	FileUtil.serialize(context, new ArrayList<Playlist>(result), getCacheName(context, "playlist"));
        	}
            cachedPlaylists.set(result);
        }
        return result;
    }

    @Override
    public void createPlaylist(String id, String name, List<Entry> entries, Context context, ProgressListener progressListener) throws Exception {
		cachedPlaylists.clear();
		Util.delete(new File(context.getCacheDir(), getCacheName(context, "playlist")));
        musicService.createPlaylist(id, name, entries, context, progressListener);
    }
	
	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
		musicService.deletePlaylist(id, context, progressListener);

		new PlaylistUpdater(context, id) {
			@Override
			public void updateResult(List<Playlist> objects, Playlist result) {
				objects.remove(result);
				cachedPlaylists.set(objects);
			}
		}.execute();
	}
	
	@Override
	public void addToPlaylist(String id, final List<Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		musicService.addToPlaylist(id, toAdd, context, progressListener);

		new MusicDirectoryUpdater(context, "playlist", id) {
			@Override
			public boolean checkResult(Entry check) {
				return true;
			}

			@Override
			public void updateResult(List<Entry> objects, Entry result) {
				objects.addAll(toAdd);
			}
		}.execute();
	}
	
	@Override
	public void removeFromPlaylist(String id, final List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		musicService.removeFromPlaylist(id, toRemove, context, progressListener);

		new MusicDirectoryUpdater(context, "playlist", id) {
			@Override
			public boolean checkResult(Entry check) {
				return true;
			}

			@Override
			public void updateResult(List<Entry> objects, Entry result) {
				// Remove in reverse order so indexes are still correct as we iterate through
				for(ListIterator<Integer> iterator = toRemove.listIterator(toRemove.size()); iterator.hasPrevious(); ) {
					objects.remove((int) iterator.previous());
				}
			}
		}.execute();
	}
	
	@Override
	public void overwritePlaylist(String id, String name, int toRemove, final List<Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		musicService.overwritePlaylist(id, name, toRemove, toAdd, context, progressListener);

		new MusicDirectoryUpdater(context, "playlist", id) {
			@Override
			public boolean checkResult(Entry check) {
				return true;
			}

			@Override
			public void updateResult(List<Entry> objects, Entry result) {
				objects.clear();
				objects.addAll(toAdd);
			}
		}.execute();
	}
	
	@Override
	public void updatePlaylist(String id, final String name, final String comment, final boolean pub, Context context, ProgressListener progressListener) throws Exception {
		musicService.updatePlaylist(id, name, comment, pub, context, progressListener);

		new PlaylistUpdater(context, id) {
			@Override
			public void updateResult(List<Playlist> objects, Playlist result) {
				result.setName(name);
				result.setComment(comment);
				result.setPublic(pub);

				cachedPlaylists.set(objects);
			}
		}.execute();
	}

    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLyrics(artist, title, context, progressListener);
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        musicService.scrobble(id, submission, context, progressListener);
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getAlbumList(type, size, offset, context, progressListener);
    }

	@Override
	public MusicDirectory getAlbumList(String type, String extra, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getAlbumList(type, extra, size, offset, context, progressListener);
	}

	@Override
    public MusicDirectory getStarredList(Context context, ProgressListener progressListener) throws Exception {
        MusicDirectory dir = musicService.getStarredList(context, progressListener);

		MusicDirectory oldDir = FileUtil.deserialize(context, "starred", MusicDirectory.class);
		if(oldDir != null) {
			final List<Entry> newList = new ArrayList<Entry>();
			newList.addAll(dir.getChildren());
			final List<Entry> oldList = oldDir.getChildren();

			removeDuplicates(oldList, newList);

			// Left overs in newList need to be starred
			boolean isTagBrowsing = Util.isTagBrowsing(context, musicService.getInstance(context));
			updateStarredList(context, newList, true, isTagBrowsing);

			// Left overs in oldList need to be unstarred
			updateStarredList(context, oldList, false, isTagBrowsing);
			
			
			// Remove non-songs from lists before updating playlists
			for(Iterator<Entry> it = oldList.iterator(); it.hasNext(); ) {
				if(it.next().isDirectory()) {
					it.remove();
				}
			}
			for(Iterator<Entry> it = newList.iterator(); it.hasNext(); ) {
				if(it.next().isDirectory()) {
					it.remove();
				}
			}
			
			// Only try to update playlists if there was at least one song in new or old set
			if(newList.size() > 0 || oldList.size() > 0) {
				new PlaylistDirectoryUpdater(context) {
					@Override
					public boolean checkResult(Entry check) {
						for(Entry entry: oldList) {
							if(check.getId().equals(entry.getId()) && check.isStarred() != false) {
								check.setStarred(false);
								return true;
							}
						}
						
						for(Entry entry: newList) {
							if(check.getId().equals(entry.getId()) && check.isStarred() != true) {
								check.setStarred(true);
								return true;
							}
						}
						
						return false;
					}
					
					@Override
					public void updateResult(Entry result) {
						
					}
				}.execute();
			}
		}
		FileUtil.serialize(context, dir, "starred");

		return dir;
    }

	private void updateStarredList(Context context, List<Entry> list, final boolean starred, final boolean isTagBrowsing) {
		for(final Entry entry: list) {
			String cacheName, parent = null;
			boolean isArtist = false;
			if(isTagBrowsing) {
				if(entry.isDirectory()) {
					if(entry.isAlbum()) {
						cacheName = "artist";
						parent = entry.getArtistId();
					} else {
						isArtist = true;
						cacheName = "artists";
					}
				} else {
					cacheName = "album";
					parent = entry.getAlbumId();
				}
			} else {
				if(entry.isDirectory() && !entry.isAlbum()) {
					isArtist = true;
					cacheName = "indexes";
				} else {
					cacheName = "directory";
					parent = entry.getParent();
				}
			}

			if(isArtist) {
				new IndexesUpdater(context, isTagBrowsing ? "artists" : "indexes") {
					@Override
					public boolean checkResult(Artist check) {
						if(entry.getId().equals(check.getId()) && check.isStarred() != starred) {
							return true;
						}

						return false;
					}

					@Override
					public void updateResult(List<Artist> objects, Artist result) {
						result.setStarred(starred);
					}
				}.execute();
			} else {
				new MusicDirectoryUpdater(context, cacheName, parent) {
					@Override
					public boolean checkResult(Entry check) {
						if (entry.getId().equals(check.getId()) && check.isStarred() != starred) {
							return true;
						}

						return false;
					}

					@Override
					public void updateResult(List<Entry> objects, Entry result) {
						result.setStarred(starred);
					}
				}.execute();
			}
		}
	}

    @Override
    public MusicDirectory getRandomSongs(int size, String folder, String genre, String startYear, String endYear, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getRandomSongs(size, folder, genre, startYear, endYear, context, progressListener);
    }

	@Override
	public String getCoverArtUrl(Context context, Entry entry) throws Exception {
		return musicService.getCoverArtUrl(context, entry);
	}

	@Override
    public Bitmap getCoverArt(Context context, Entry entry, int size, ProgressListener progressListener) throws Exception {
        return musicService.getCoverArt(context, entry, size, progressListener);
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, Entry song, long offset, int maxBitrate, SilentBackgroundTask task) throws Exception {
        return musicService.getDownloadInputStream(context, song, offset, maxBitrate, task);
    }

	@Override
	public String getMusicUrl(Context context, Entry song, int maxBitrate) throws Exception {
		return musicService.getMusicUrl(context, song, maxBitrate);
	}

	@Override
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        return musicService.getVideoUrl(maxBitrate, context, id);
    }
	
	@Override
    public String getVideoStreamUrl(String format, int maxBitrate, Context context, String id) throws Exception {
        return musicService.getVideoStreamUrl(format, maxBitrate, context, id);
    }
	
	@Override
	public String getHlsUrl(String id, int bitRate, Context context) throws Exception {
		return musicService.getHlsUrl(id, bitRate, context);
	}

    @Override
    public RemoteStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        return musicService.updateJukeboxPlaylist(ids, context, progressListener);
    }

    @Override
    public RemoteStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        return musicService.skipJukebox(index, offsetSeconds, context, progressListener);
    }

    @Override
    public RemoteStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.stopJukebox(context, progressListener);
    }

    @Override
    public RemoteStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.startJukebox(context, progressListener);
    }

    @Override
    public RemoteStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getJukeboxStatus(context, progressListener);
    }

    @Override
    public RemoteStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        return musicService.setJukeboxGain(gain, context, progressListener);
    }
    
	@Override
	public void setStarred(List<String> id, List<String> artistId, List<String> albumId, List<String> parents, final boolean starred, ProgressListener progressListener, Context context) throws Exception {
		musicService.setStarred(id, artistId, albumId, parents, starred, progressListener, context);

		// Fuzzy logic to update parents serialization
		List<String> ids;
		if(artistId != null && artistId.size() > 0) {
			ids = artistId;
		} else if(albumId != null && albumId.size() > 0) {
			ids = albumId;
		} else {
			ids = id;
		}

		// Make sure list is not somehow null here
		if(ids == null) {
			Log.w(TAG, "There should never be no ids in setStarred");
			return;
		}

		// Define another variable final because Java is retarded
		final List<String> checkIds = ids;

		// If parents is null, or artist id's are set, then we are looking at artists
		if(parents != null && (artistId == null || artistId.size() == 0)) {
			String cacheName;
			
			// If using tag browsing, need to do lookup off of different criteria
			if(Util.isTagBrowsing(context, musicService.getInstance(context))) {
				// If using id's, we are starring songs and need to use album listings
				if(id != null && id.size() > 0) {
					cacheName = "album";
				} else {
					cacheName = "artist";
				}
			} else {
				cacheName = "directory";
			}
			
			for (String parent : parents) {
				new MusicDirectoryUpdater(context, cacheName, parent, checkIds.size() == 1) {
					@Override
					public boolean checkResult(Entry check) {
						for (String id : checkIds) {
							if(id.equals(check.getId())) {
								return true;
							}
						}

						return false;
					}

					@Override
					public void updateResult(List<Entry> objects, Entry result) {
						result.setStarred(starred);
					}
				}.execute();
			}
		} else {
			String name = Util.isTagBrowsing(context, musicService.getInstance(context)) ? "artists" : "indexes";
			new IndexesUpdater(context, name) {
				@Override
				public boolean checkResult(Artist check) {
					for (String id : checkIds) {
						if(id.equals(check.getId())) {
							return true;
						}
					}

					return false;
				}

				@Override
				public void updateResult(List<Artist> objects, Artist result) {
					result.setStarred(starred);
				}
			}.execute();
		}

		// Update playlist caches if there is at least one song to be starred
		if(ids != null && ids.size() > 0) {
			new PlaylistDirectoryUpdater(context) {
				@Override
				public boolean checkResult(Entry check) {
					for (String id : checkIds) {
						if (id.equals(check.getId())) {
							return true;
						}
					}

					return false;
				}

				@Override
				public void updateResult(Entry result) {
					result.setStarred(starred);
				}
			}.execute();
		}
	}
	
	@Override
	public List<Share> getShares(Context context, ProgressListener progressListener) throws Exception {
		return musicService.getShares(context, progressListener);	
	}

	@Override
	public List<Share> createShare(List<String> ids, String description, Long expires, Context context, ProgressListener progressListener) throws Exception {
		return musicService.createShare(ids, description, expires, context, progressListener);
	}

	@Override
	public void deleteShare(String id, Context context, ProgressListener progressListener) throws Exception {
		musicService.deleteShare(id, context, progressListener);
	}

	@Override
	public void updateShare(String id, String description, Long expires, Context context, ProgressListener progressListener) throws Exception {
		musicService.updateShare(id, description, expires, context, progressListener);
	}

	@Override
	public List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getChatMessages(since, context, progressListener);
	}

	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
		musicService.addChatMessage(message, context, progressListener);
	}
	
	@Override
	public List<Genre> getGenres(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		List<Genre> result = null;

		if(!refresh) {
			result = FileUtil.deserialize(context, getCacheName(context, "genre"), ArrayList.class);
		}

		if(result == null) {
			result = musicService.getGenres(refresh, context, progressListener);
			FileUtil.serialize(context, new ArrayList<Genre>(result), getCacheName(context, "genre"));
		}

		return result;
	}

	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getSongsByGenre(genre, count, offset, context, progressListener);
	}
	
	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkSettingsChanged(context);
		List<PodcastChannel> result = refresh ? null : cachedPodcastChannels.get();

		if (result == null) {
			if(!refresh) {
				result = FileUtil.deserialize(context, getCacheName(context, "podcast"), ArrayList.class);
			}
			
			if(result == null) {
				result = musicService.getPodcastChannels(refresh, context, progressListener);
				FileUtil.serialize(context, new ArrayList<PodcastChannel>(result), getCacheName(context, "podcast"));
			}
			cachedPodcastChannels.set(result);
		}

		return result;
	}
	
	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		String altId = "p-" + id;
		MusicDirectory result = null;

		if(!refresh) {
			result = FileUtil.deserialize(context, getCacheName(context, "directory", altId), MusicDirectory.class, 10);
		}

		if(result == null) {
			result = musicService.getPodcastEpisodes(refresh, id, context, progressListener);
			FileUtil.serialize(context, result, getCacheName(context, "directory", altId));
		}

		return result;
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		musicService.refreshPodcasts(context, progressListener);
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		musicService.createPodcastChannel(url, context, progressListener);
	}
	
	@Override
	public void deletePodcastChannel(final String id, Context context, ProgressListener progressListener) throws Exception{
		new SerializeUpdater<PodcastChannel>(context, "podcast") {
			@Override
			public boolean checkResult(PodcastChannel check) {
				return id.equals(check.getId());
			}

			@Override
			public void updateResult(List<PodcastChannel> objects, PodcastChannel result) {
				objects.remove(result);
				cachedPodcastChannels.set(objects);
			}
		}.execute();
		musicService.deletePodcastChannel(id, context, progressListener);
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		musicService.downloadPodcastEpisode(id, context, progressListener);
	}
	
	@Override
	public void deletePodcastEpisode(final String id, String parent, ProgressListener progressListener, Context context) throws Exception{
		musicService.deletePodcastEpisode(id, parent, progressListener, context);

		new MusicDirectoryUpdater(context, "directory", "p-" + parent) {
			@Override
			public boolean checkResult(Entry check) {
				return id.equals(((PodcastEpisode) check).getEpisodeId());
			}

			@Override
			public void updateResult(List<Entry> objects, Entry result) {
				objects.remove(result);
			}
		}.execute();
	}

	@Override
	public void setRating(String id, int rating, Context context, ProgressListener progressListener) throws Exception {
		musicService.setRating(id, rating, context, progressListener);
	}

	@Override
	public MusicDirectory getBookmarks(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		MusicDirectory bookmarks = musicService.getBookmarks(refresh, context, progressListener);
		
		MusicDirectory oldBookmarks = FileUtil.deserialize(context, "bookmarks", MusicDirectory.class);
		if(oldBookmarks != null) {
			final List<Entry> oldList = oldBookmarks.getChildren();
			final List<Entry> newList = new ArrayList<Entry>();
			newList.addAll(bookmarks.getChildren());
			
			removeDuplicates(oldList, newList);
			
			// Remove bookmarks from thinsg still in old list
			setBookmarkCache(context, oldList, true);
			// Add new bookmarks for things in new list
			setBookmarkCache(context, newList, false);
			
			if(oldList.size() > 0 || newList.size() > 0) {
				new PlaylistDirectoryUpdater(context) {
					@Override
					public boolean checkResult(Entry check) {
						for(Entry entry: oldList) {
							if(entry.getId().equals(check.getId()) && check.getBookmark() != null) {
								check.setBookmark(null);
								return true;
							}
						}
						for(Entry entry: newList) {
							if(entry.getId().equals(check.getId())) {
								int newPosition = entry.getBookmark().getPosition();
								if(check.getBookmark() == null || check.getBookmark().getPosition() != newPosition) {
									setBookmarkCache(check, newPosition);
									return true;
								}
							}
						}
						
						return false;
					}
					
					@Override
					public void updateResult(Entry result) {
						
					}
				}.execute();
			}
		}
		FileUtil.serialize(context, bookmarks, "bookmarks");
		
		return bookmarks;
	}

	@Override
	public void createBookmark(String id, String parent, int position, String comment, Context context, ProgressListener progressListener) throws Exception {
		musicService.createBookmark(id, null, position, comment, context, progressListener);
		// Add to directory cache
		setBookmarkCache(context, id, parent, position);
		// Add to playlist cache
		setBookmarkCache(context, id, position);
	}

	@Override
	public void deleteBookmark(String id, String parent, Context context, ProgressListener progressListener) throws Exception {
		musicService.deleteBookmark(id, null, context, progressListener);
		// Delete from directory cache
		setBookmarkCache(context, id, parent, -1);
		// Delete from playlist cache
		setBookmarkCache(context, id, -1);
	}
	
	private void setBookmarkCache(Context context, List<Entry> entries, boolean remove) {
		for(final Entry entry: entries) {
			if(remove) {
				setBookmarkCache(context, entry.getId(), Util.getParentFromEntry(context, entry), -1);
			} else {
				setBookmarkCache(context, entry.getId(), Util.getParentFromEntry(context, entry), entry.getBookmark().getPosition());
			}
		}
	}
	private void setBookmarkCache(Context context, final String id, final String parent, final int position) {
		String cacheName;
		if(isTagBrowsing) {
			cacheName = "album";
		} else {
			cacheName = "directory";
		}

		// Update the parent directory with bookmark data
		new MusicDirectoryUpdater(context, cacheName, parent) {
			@Override
			public boolean checkResult(Entry check) {
				return shouldBookmarkUpdate(check, id, position);
			}
			
			@Override
			public void updateResult(List<Entry> objects, Entry result) {
				setBookmarkCache(result, position);
			}
		}.execute();
	}
	private void setBookmarkCache(Context context, final String id, final int position) {
		// Update playlists with bookmark data
		new PlaylistDirectoryUpdater(context) {
			@Override
			public boolean checkResult(Entry check) {
				return shouldBookmarkUpdate(check, id, position);
			}
			
			@Override
			public void updateResult(Entry result) {
				setBookmarkCache(result, position);
			}
		}.execute();
	}
	
	private boolean shouldBookmarkUpdate(Entry check, String id, int position) {
		if(id.equals(check.getId())) {
			if(position == -1 && check.getBookmark() != null) {
				return true;
			} else if(position >= 0 && (check.getBookmark() == null || check.getBookmark().getPosition() != position)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void setBookmarkCache(Entry result, int position) {
		// If position == -1, then it is a delete
		if(result.getBookmark() != null && position == -1) {
			result.setBookmark(null);
		} else if(position >= 0) {
			Bookmark bookmark = result.getBookmark();
			
			// Create one if empty
			if(bookmark == null) {
				bookmark = new Bookmark();
				result.setBookmark(bookmark);
			}
			
			// Update bookmark position no matter what
			bookmark.setPosition(position);
		}
	}

	@Override
	public User getUser(boolean refresh, String username, Context context, ProgressListener progressListener) throws Exception {
		User result = null;

		try {
			result = musicService.getUser(refresh, username, context, progressListener);
			FileUtil.serialize(context, result, getCacheName(context, "user-" + username));
		} catch(Exception e) {
			// Don't care
		}
		
		if(result == null && !refresh) {
			result = FileUtil.deserialize(context, getCacheName(context, "user-" + username), User.class);
		}

		return result;
	}

	@Override
	public List<User> getUsers(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		List<User> result = null;

		if(!refresh) {
			result = FileUtil.deserialize(context, getCacheName(context, "users"), ArrayList.class);
		}

		if(result == null) {
			result = musicService.getUsers(refresh, context, progressListener);
			FileUtil.serialize(context, new ArrayList<User>(result), getCacheName(context, "users"));
		}

		return result;
	}

	@Override
	public void createUser(final User user, Context context, ProgressListener progressListener) throws Exception {
		musicService.createUser(user, context, progressListener);
		
		new UserUpdater(context, "") {
			@Override
			public boolean checkResult(User check) {
				return true;
			}
			
			@Override
			public void updateResult(List<User> users, User result) {
				users.add(user);
			}
		}.execute();
	}

	@Override
	public void updateUser(final User user, Context context, ProgressListener progressListener) throws Exception {
		musicService.updateUser(user, context, progressListener);

		new UserUpdater(context, user.getUsername()) {
			@Override
			public void updateResult(List<User> users, User result) {
				result.setEmail(user.getEmail());
				result.setSettings(user.getSettings());
			}
		}.execute();
	}

	@Override
	public void deleteUser(String username, Context context, ProgressListener progressListener) throws Exception {
		musicService.deleteUser(username, context, progressListener);

		new UserUpdater(context, username) {
			@Override
			public void updateResult(List<User> users, User result) {
				users.remove(result);
			}
		}.execute();
	}

	@Override
	public void changeEmail(String username, final String email, Context context, ProgressListener progressListener) throws Exception {
		musicService.changeEmail(username, email, context, progressListener);
		
		// Update cached email for user
		new UserUpdater(context, username) {
			@Override
			public void updateResult(List<User> users, User result) {
				result.setEmail(email);
			}
		}.execute();
	}

	@Override
	public void changePassword(String username, String password, Context context, ProgressListener progressListener) throws Exception {
		musicService.changePassword(username, password, context, progressListener);
	}

	@Override
	public Bitmap getAvatar(String username, int size, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getAvatar(username, size, context, progressListener);
	}

	@Override
	public int processOfflineSyncs(final Context context, final ProgressListener progressListener) throws Exception{
		return musicService.processOfflineSyncs(context, progressListener);
	}
	
	@Override
    public void setInstance(Integer instance) throws Exception {
    	musicService.setInstance(instance);
    }
  
  	private String getCacheName(Context context, String name, String id) {
  		String s = musicService.getRestUrl(context, null, false) + id;
  		return name + "-" + s.hashCode() + ".ser";
  	}
  	private String getCacheName(Context context, String name) {
  		String s = musicService.getRestUrl(context, null, false);
  		return name + "-" + s.hashCode() + ".ser";
  	}
  	
  	private void removeDuplicates(List<Entry> oldList, List<Entry> newList) {
  		for(Iterator<Entry> it = oldList.iterator(); it.hasNext(); ) {
  			// Remove entries from newList
  			if(newList.remove(it.next())) {
  				// If it was removed, then remove it from old list as well
  				it.remove();
  			}
  		}
  	}
  	
  	private abstract class SerializeUpdater<T> {
  		final Context context;
  		final String cacheName;
  		final boolean singleUpdate;
  		
  		public SerializeUpdater(Context context, String cacheName) {
  			this(context, cacheName, true);
  		}
  		public SerializeUpdater(Context context, String cacheName, boolean singleUpdate) {
  			this.context = context;
  			this.cacheName = getCacheName(context, cacheName);
  			this.singleUpdate = singleUpdate;
  		}
  		public SerializeUpdater(Context context, String cacheName, String id) {
  			this(context, cacheName, id, true);
  		}
		public SerializeUpdater(Context context, String cacheName, String id, boolean singleUpdate) {
			this.context = context;
			this.cacheName = getCacheName(context, cacheName, id);
			this.singleUpdate = singleUpdate;
		}

		public ArrayList<T> getArrayList() {
			return FileUtil.deserialize(context, cacheName, ArrayList.class);
		}
  		public abstract boolean checkResult(T check);
  		public abstract void updateResult(List<T> objects, T result);
		public void save(ArrayList<T> objects) {
			FileUtil.serialize(context, objects, cacheName);
		}
  		
  		public void execute() {
  			ArrayList<T> objects = getArrayList();
  			
  			// Only execute if something to check against
  			if(objects != null) {
  				List<T> results = new ArrayList<T>();
  				for(T check: objects) {
  					if(checkResult(check)) {
						results.add(check);
  						if(singleUpdate) {
  							break;
  						}
  					}
  				}
  				
  				// Iterate through and update each object matched
  				for(T result: results) {
  					updateResult(objects, result);
  				}
  				
  				// Only reserialize if at least one match was found
  				if(results.size() > 0) {
  					save(objects);
  				}
  			}
  		}
  	}
  	private abstract class UserUpdater extends SerializeUpdater<User> {
  		String username;
  		
  		public UserUpdater(Context context, String username) {
  			super(context, "users");
  			this.username = username;
  		}
  		
  		@Override
  		public boolean checkResult(User check) {
  			return username.equals(check.getUsername());
  		}
  	}
	private abstract class PlaylistUpdater extends SerializeUpdater<Playlist> {
		String id;

		public PlaylistUpdater(Context context, String id) {
			super(context, "playlist");
			this.id = id;
		}

		@Override
		public boolean checkResult(Playlist check) {
			return id.equals(check.getId());
		}
	}
	private abstract class MusicDirectoryUpdater extends SerializeUpdater<Entry> {
		private MusicDirectory musicDirectory;

		public MusicDirectoryUpdater(Context context, String cacheName, String id) {
			super(context, cacheName, id, true);
		}
		public MusicDirectoryUpdater(Context context, String cacheName, String id, boolean singleUpdate) {
			super(context, cacheName, id, singleUpdate);
		}

		@Override
		public ArrayList<Entry> getArrayList() {
			musicDirectory = FileUtil.deserialize(context, cacheName, MusicDirectory.class);
			if(musicDirectory != null) {
				return new ArrayList<Entry>(musicDirectory.getChildren());
			} else {
				return null;
			}
		}
		public void save(ArrayList<Entry> objects) {
			musicDirectory.replaceChildren(objects);
			FileUtil.serialize(context, musicDirectory, cacheName);
		}
	}
	private abstract class PlaylistDirectoryUpdater {
		Context context;
		
		public PlaylistDirectoryUpdater(Context context) {
			this.context = context;
		}
		
		public abstract boolean checkResult(Entry check);
		public abstract void updateResult(Entry result);
		
		public void execute() {
			List<Playlist> playlists = FileUtil.deserialize(context, getCacheName(context, "playlist"), ArrayList.class);
			if(playlists == null) {
				// No playlist list cache, nothing to update!
				return;
			}
			
			for(Playlist playlist: playlists) {
				new MusicDirectoryUpdater(context, "playlist", playlist.getId(), false) {
					@Override
					public boolean checkResult(Entry check) {
						return PlaylistDirectoryUpdater.this.checkResult(check);
					}
					
					@Override
					public void updateResult(List<Entry> objects, Entry result) {
						PlaylistDirectoryUpdater.this.updateResult(result);
					}
				}.execute();
			}
		}
	}
	private abstract class IndexesUpdater extends SerializeUpdater<Artist> {
		Indexes indexes;

		IndexesUpdater(Context context, String name) {
			super(context, name, Util.getSelectedMusicFolderId(context, musicService.getInstance(context)));
		}

		@Override
		public ArrayList<Artist> getArrayList() {
			indexes = FileUtil.deserialize(context, cacheName, Indexes.class);
			if(indexes == null) {
				return null;
			}

			ArrayList<Artist> artists = new ArrayList<Artist>();
			artists.addAll(indexes.getArtists());
			artists.addAll(indexes.getShortcuts());
			return artists;
		}

		public void save(ArrayList<Artist> objects) {
			indexes.setArtists(objects);
			FileUtil.serialize(context, indexes, cacheName);
			cachedIndexes.set(indexes);
		}
	}

    private void checkSettingsChanged(Context context) {
        String newUrl = musicService.getRestUrl(context, null, false);
		boolean newIsTagBrowsing = Util.isTagBrowsing(context);
        if (!Util.equals(newUrl, restUrl) || isTagBrowsing != newIsTagBrowsing) {
            cachedMusicFolders.clear();
            cachedLicenseValid.clear();
            cachedIndexes.clear();
            cachedPlaylists.clear();
			cachedPodcastChannels.clear();
            restUrl = newUrl;
			isTagBrowsing = newIsTagBrowsing;
        }
    }
}
