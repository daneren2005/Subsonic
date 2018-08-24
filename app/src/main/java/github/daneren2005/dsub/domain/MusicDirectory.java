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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.UpdateHelper;
import github.daneren2005.dsub.util.Util;

/**
 * @author Sindre Mehus
 */
public class MusicDirectory implements Serializable {
	private static final String TAG = MusicDirectory.class.getSimpleName();

    private String name;
	private String id;
	private String parent;
    private List<Entry> children;

	public MusicDirectory() {
		children = new ArrayList<Entry>();
	}
	public MusicDirectory(List<Entry> children) {
		this.children = children;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
	
	 public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public void addChild(Entry child) {
		if(child != null) {
			children.add(child);
		}
	}
	public void addChildren(List<Entry> children) {
		this.children.addAll(children);
	}
    
	public void replaceChildren(List<Entry> children) {
		this.children = children;
	}

    public synchronized List<Entry> getChildren() {
        return getChildren(true, true);
    }

    public synchronized List<Entry> getChildren(boolean includeDirs, boolean includeFiles) {
        if (includeDirs && includeFiles) {
            return children;
        }

        List<Entry> result = new ArrayList<Entry>(children.size());
        for (Entry child : children) {
            if (child != null && child.isDirectory() && includeDirs || !child.isDirectory() && includeFiles) {
                result.add(child);
            }
        }
        return result;
    }
	public synchronized List<Entry> getSongs() {
		List<Entry> result = new ArrayList<Entry>();
		for (Entry child : children) {
			if (child != null && !child.isDirectory() && !child.isVideo()) {
				result.add(child);
			}
		}
		return result;
	}
	
	public synchronized int getChildrenSize() {
		return children.size();
	}

	public void shuffleChildren() {
		Collections.shuffle(this.children);
	}
	
	public void sortChildren(Context context, int instance) {
		// Only apply sorting on server version 4.7 and greater, where disc is supported
		if(ServerInfo.checkServerVersion(context, "1.8", instance)) {
			sortChildren(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_CUSTOM_SORT_ENABLED, true));
		}
	}
	public void sortChildren(boolean byYear) {
		EntryComparator.sort(children, byYear);
	}

	public synchronized boolean updateMetadata(MusicDirectory refreshedDirectory) {
		boolean metadataUpdated = false;
		Iterator<Entry> it = children.iterator();
		while(it.hasNext()) {
			Entry entry = it.next();
			int index = refreshedDirectory.children.indexOf(entry);
			if(index != -1) {
				final Entry refreshed = refreshedDirectory.children.get(index);

				entry.setTitle(refreshed.getTitle());
				entry.setAlbum(refreshed.getAlbum());
				entry.setArtist(refreshed.getArtist());
				entry.setTrack(refreshed.getTrack());
				entry.setYear(refreshed.getYear());
				entry.setGenre(refreshed.getGenre());
				entry.setTranscodedContentType(refreshed.getTranscodedContentType());
				entry.setTranscodedSuffix(refreshed.getTranscodedSuffix());
				entry.setDiscNumber(refreshed.getDiscNumber());
				entry.setStarred(refreshed.isStarred());
				entry.setRating(refreshed.getRating());
				entry.setType(refreshed.getType());
				if(!Util.equals(entry.getCoverArt(), refreshed.getCoverArt())) {
					metadataUpdated = true;
					entry.setCoverArt(refreshed.getCoverArt());
				}

				new UpdateHelper.EntryInstanceUpdater(entry) {
					@Override
					public void update(Entry found) {
						found.setTitle(refreshed.getTitle());
						found.setAlbum(refreshed.getAlbum());
						found.setArtist(refreshed.getArtist());
						found.setTrack(refreshed.getTrack());
						found.setYear(refreshed.getYear());
						found.setGenre(refreshed.getGenre());
						found.setTranscodedContentType(refreshed.getTranscodedContentType());
						found.setTranscodedSuffix(refreshed.getTranscodedSuffix());
						found.setDiscNumber(refreshed.getDiscNumber());
						found.setStarred(refreshed.isStarred());
						found.setRating(refreshed.getRating());
						found.setType(refreshed.getType());
						if(!Util.equals(found.getCoverArt(), refreshed.getCoverArt())) {
							found.setCoverArt(refreshed.getCoverArt());
							metadataUpdate = DownloadService.METADATA_UPDATED_COVER_ART;
						}
					}
				}.execute();
			}
		}

		return metadataUpdated;
	}
	public synchronized boolean updateEntriesList(Context context, int instance, MusicDirectory refreshedDirectory) {
		boolean changed = false;
		Iterator<Entry> it = children.iterator();
		while(it.hasNext()) {
			Entry entry = it.next();
			// No longer exists in here
			if(refreshedDirectory.children.indexOf(entry) == -1) {
				it.remove();
				changed = true;
			}
		}

		// Make sure we contain all children from refreshed set
		boolean resort = false;
		for(Entry refreshed: refreshedDirectory.children) {
			if(!this.children.contains(refreshed)) {
				this.children.add(refreshed);
				resort = true;
				changed = true;
			}
		}

		if(resort) {
			this.sortChildren(context, instance);
		}

		return changed;
	}

    public static class Entry implements Serializable {
		public static final int TYPE_SONG = 0;
		public static final int TYPE_PODCAST = 1;
		public static final int TYPE_AUDIO_BOOK = 2;

		private String id;
		private String parent;
		private String grandParent;
		private String albumId;
		private String artistId;
		private boolean directory;
		private String title;
		private String album;
		private String artist;
		private Integer track;
		private Integer customOrder;
		private Integer year;
		private String genre;
		private String contentType;
		private String suffix;
		private String transcodedContentType;
		private String transcodedSuffix;
		private String coverArt;
		private Long size;
		private Integer duration;
		private Integer bitRate;
		private String path;
		private boolean video;
		private Integer discNumber;
		private boolean starred;
		private Integer rating;
		private Bookmark bookmark;
		private int type = 0;
		private int closeness;
		private transient Artist linkedArtist;

		public Entry() {

		}
		public Entry(String id) {
			this.id = id;
		}
		public Entry(Artist artist) {
			this.id = artist.getId();
			this.title = artist.getName();
			this.directory = true;
			this.starred = artist.isStarred();
			this.rating = artist.getRating();
			this.linkedArtist = artist;
		}
		
		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public void loadMetadata(File file) {
			try {
				MediaMetadataRetriever metadata = new MediaMetadataRetriever();
				metadata.setDataSource(file.getAbsolutePath());
				String discNumber = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER);
				if(discNumber == null) {
					discNumber = "1/1";
				}
				int slashIndex = discNumber.indexOf("/");
				if(slashIndex > 0) {
					discNumber = discNumber.substring(0, slashIndex);
				}
				try {
					setDiscNumber(Integer.parseInt(discNumber));
				} catch(Exception e) {
					Log.w(TAG, "Non numbers in disc field!");
				}
				String bitrate = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
				setBitRate(Integer.parseInt((bitrate != null) ? bitrate : "0") / 1000);
				String length = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
				setDuration(Integer.parseInt(length) / 1000);
				String artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
				if(artist != null) {
					setArtist(artist);
				}
				String album = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
				if(album != null) {
					setAlbum(album);
				}
				metadata.release();
			} catch(Exception e) {
				Log.i(TAG, "Device doesn't properly support MediaMetadataRetreiver", e);
			}
		}
		public void rebaseTitleOffPath() {
			try {
				String filename = getPath();
				if(filename == null) {
					return;
				}

				int index = filename.lastIndexOf('/');
				if (index != -1) {
					filename = filename.substring(index + 1);
					if (getTrack() != null) {
						filename = filename.replace(String.format("%02d ", getTrack()), "");
					}

					index = filename.lastIndexOf('.');
					if(index != -1) {
						filename = filename.substring(0, index);
					}

					setTitle(filename);
				}
			} catch(Exception e) {
				Log.w(TAG, "Failed to update title based off of path", e);
			}
		}

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }
		
		public String getGrandParent() {
            return grandParent;
        }

        public void setGrandParent(String grandParent) {
            this.grandParent = grandParent;
        }

		public String getAlbumId() {
			return albumId;
		}

		public void setAlbumId(String albumId) {
			this.albumId = albumId;
		}

		public String getArtistId() {
			return artistId;
		}

		public void setArtistId(String artistId) {
			this.artistId = artistId;
		}

        public boolean isDirectory() {
            return directory;
        }

        public void setDirectory(boolean directory) {
            this.directory = directory;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAlbum() {
            return album;
        }

		public boolean isAlbum() {
			return getParent() != null || getArtist() != null;
		}

		public String getAlbumDisplay() {
			if(album != null && title.startsWith("Disc ")) {
				return album;
			} else {
				return title;
			}
		}

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public Integer getTrack() {
            return track;
        }

        public void setTrack(Integer track) {
            this.track = track;
        }

		public Integer getCustomOrder() {
			return customOrder;
		}
		public void setCustomOrder(Integer customOrder) {
			this.customOrder = customOrder;
		}

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public String getTranscodedContentType() {
            return transcodedContentType;
        }

        public void setTranscodedContentType(String transcodedContentType) {
            this.transcodedContentType = transcodedContentType;
        }

        public String getTranscodedSuffix() {
            return transcodedSuffix;
        }

        public void setTranscodedSuffix(String transcodedSuffix) {
            this.transcodedSuffix = transcodedSuffix;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public Integer getBitRate() {
            return bitRate;
        }

        public void setBitRate(Integer bitRate) {
            this.bitRate = bitRate;
        }

        public String getCoverArt() {
            return coverArt;
        }

        public void setCoverArt(String coverArt) {
            this.coverArt = coverArt;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isVideo() {
            return video;
        }

        public void setVideo(boolean video) {
            this.video = video;
        }
		
		public Integer getDiscNumber() {
			return discNumber;
		}
		
		public void setDiscNumber(Integer discNumber) {
			this.discNumber = discNumber;
		}
        
        public boolean isStarred() {
            return starred;
        }
        
        public void setStarred(boolean starred) {
            this.starred = starred;

			if(linkedArtist != null) {
				linkedArtist.setStarred(starred);
			}
        }
        
		public int getRating() {
			return rating == null ? 0 : rating;
		}
		public void setRating(Integer rating) {
			if(rating == null || rating == 0) {
				this.rating = null;
			} else {
				this.rating = rating;
			}

			if(linkedArtist != null) {
				linkedArtist.setRating(rating);
			}
		}
		
		public Bookmark getBookmark() {
			return bookmark;
		}
		public void setBookmark(Bookmark bookmark) {
			this.bookmark = bookmark;
		}

		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public boolean isSong() {
			return type == TYPE_SONG;
		}
		public boolean isPodcast() {
			return this instanceof PodcastEpisode || type == TYPE_PODCAST;
		}
		public boolean isAudioBook() {
			return type == TYPE_AUDIO_BOOK;
		}
		
		public int getCloseness() {
			return closeness;
		}

		public void setCloseness(int closeness) {
			this.closeness = closeness;
		}

		public boolean isOnlineId(Context context) {
			try {
				String cacheLocation = Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
				return cacheLocation == null || id == null || id.indexOf(cacheLocation) == -1;
			} catch(Exception e) {
				Log.w(TAG, "Failed to check online id validity");

				// Err on the side of default functionality
				return true;
			}
		}

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;
            return id.equals(entry.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return title;
        }

        public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(this);
				out.flush();
				return bos.toByteArray();
			} finally {
				try {
					bos.close();
				} catch (IOException ex) {
					// ignore close exception
				}
			}
		}

		public static Entry fromByteArray(byte[] byteArray) throws IOException, ClassNotFoundException {
			ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
			ObjectInput in = null;
			try {
				in = new ObjectInputStream(bis);
				return (Entry) in.readObject();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
					// ignore close exception
				}
			}
		}
	}
	
	public static class EntryComparator implements Comparator<Entry> {
		private boolean byYear;
		private Collator collator;
		
		public EntryComparator(boolean byYear) {
			this.byYear = byYear;
			this.collator = Collator.getInstance(Locale.US);
			this.collator.setStrength(Collator.PRIMARY);
		}
		
		public int compare(Entry lhs, Entry rhs) {
			if(lhs.isDirectory() && !rhs.isDirectory()) {
				return -1;
			} else if(!lhs.isDirectory() && rhs.isDirectory()) {
				return 1;
			} else if(lhs.isDirectory() && rhs.isDirectory()) {
				if(byYear) {
					Integer lhsYear = lhs.getYear();
					Integer rhsYear = rhs.getYear();
					if(lhsYear != null && rhsYear != null) {
						return lhsYear.compareTo(rhsYear);
					} else if(lhsYear != null) {
						return -1;
					} else if(rhsYear != null) {
						return 1;
					}
				}

				return collator.compare(lhs.getAlbumDisplay(), rhs.getAlbumDisplay());
			}
			
			Integer lhsDisc = lhs.getDiscNumber();
			Integer rhsDisc = rhs.getDiscNumber();
			
			if(lhsDisc != null && rhsDisc != null) {
				if(lhsDisc < rhsDisc) {
					return -1;
				} else if(lhsDisc > rhsDisc) {
					return 1;
				}
			}
			
			Integer lhsTrack = lhs.getTrack();
			Integer rhsTrack = rhs.getTrack();
			if(lhsTrack != null && rhsTrack != null && lhsTrack != rhsTrack) {
				return lhsTrack.compareTo(rhsTrack);
			} else if(lhsTrack != null) {
				return -1;
			} else if(rhsTrack != null) {
				return 1;
			}

			return collator.compare(lhs.getTitle(), rhs.getTitle());
		}
		
		public static void sort(List<Entry> entries) {
			sort(entries, true);
		}
		public static void sort(List<Entry> entries, boolean byYear) {
			try {
				Collections.sort(entries, new EntryComparator(byYear));
			} catch (Exception e) {
				Log.w(TAG, "Failed to sort MusicDirectory");
			}
		}
	}
}
