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
package github.paroj.dsub2000.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import github.paroj.dsub2000.domain.Artist;
import github.paroj.dsub2000.domain.Genre;
import github.paroj.dsub2000.domain.Indexes;
import github.paroj.dsub2000.domain.Playlist;
import github.paroj.dsub2000.domain.PodcastChannel;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.MusicFolder;
import github.paroj.dsub2000.domain.PodcastEpisode;
import github.paroj.dsub2000.service.MediaStoreService;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Sindre Mehus
 */
public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();
    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":", "\"", "?", "*", "<", ">", "|"};
    private static final String[] FILE_SYSTEM_UNSAFE_DIR = {"\\", "..", ":", "\"", "?", "*", "<", ">", "|"};
    private static final List<String> MUSIC_FILE_EXTENSIONS = Arrays.asList("mp3", "ogg", "aac", "flac", "m4a", "wav", "wma", "opus", "oga");
	private static final List<String> VIDEO_FILE_EXTENSIONS = Arrays.asList("flv", "mp4", "m4v", "wmv", "avi", "mov", "mpg", "mkv", "3gp", "webm");
	private static final List<String> PLAYLIST_FILE_EXTENSIONS = Arrays.asList("m3u");
    private static final int MAX_FILENAME_LENGTH_V1 = 254 - ".complete.mp3".length();
    private static final int MAX_FILENAME_LENGTH_V2 = 254 - "00-null-.complete.abcd".length();
    private static File DEFAULT_MUSIC_DIR;
	private static final Kryo kryo = new Kryo();
	private static HashMap<String, MusicDirectory.Entry> entryLookup;

	static {
		kryo.setRegistrationRequired(false);  // Every class will be registered at use time, with
		// hope their order of serialization will be consistent, before migration kryo 2.21 -> 5.6.0
		// this was default
		kryo.register(MusicDirectory.Entry.class);
		kryo.register(Indexes.class);
		kryo.register(Artist.class);
		kryo.register(MusicFolder.class);
		kryo.register(PodcastChannel.class);
		kryo.register(Playlist.class);
		kryo.register(Genre.class);
	}
	
	public static File getAnySong(Context context) {
		File dir = getMusicDirectory(context);
		return getAnySong(context, dir);
	}
	private static File getAnySong(Context context, File dir) {
		for(File file: dir.listFiles()) {
			if(file.isDirectory()) {
				return getAnySong(context, file);
			}
			
			String extension = getExtension(file.getName());
			if(MUSIC_FILE_EXTENSIONS.contains(extension)) {
				return file;
			}
		}
		
		return null;
	}
	
	public static File getEntryFile(Context context, MusicDirectory.Entry entry) {
		if(entry.isDirectory()) {
			return getAlbumDirectory(context, entry);
		} else {
			return getSongFiles(context, entry).save();
		}
	}

    /**
     * Resolves the save, partial and complete files for a song.
     */
    public static SongFiles getSongFiles(Context context, MusicDirectory.Entry song) {
        return getSongFiles(context, song, getMusicDirectory(context), File::exists);
    }

    private static SongFiles getSongFiles(Context context, MusicDirectory.Entry song, File musicDirectory, Predicate<File> exists) {
        File albumDirectory = getAlbumDirectory(context, song, musicDirectory);
        return new SongFiles(
                getSaveFile(context, song, musicDirectory, albumDirectory, exists),
                buildSongFile(context, song, "partial", albumDirectory, exists),
                buildSongFile(context, song, "complete", albumDirectory, exists));
    }

    private static File getSaveFile(Context context, MusicDirectory.Entry song, File musicDirectory, File albumDirectory, Predicate<File> exists) {
        // Offline entries store their original absolute filesystem path as their ID.
        // Prefer that path instead of reconstructing a filename from metadata.
        if (Util.isOffline(context) && song.getId() != null) {
            File original = new File(song.getId());
            if (original.isFile()
                    && original.toPath().normalize()
                        .startsWith(musicDirectory.toPath().normalize())) {
                return original;
            }
        }
        return buildSongFile(context, song, null, albumDirectory, exists);
    }

    private static File buildSongFile(Context context, MusicDirectory.Entry song, String stage, File albumDirectory, Predicate<File> exists) {
        Integer trackNumber = song.getTrack();
        String title = fileSystemSafe(song.getTitle());
        String extension = getSongFileNameExtension(context, song);

        String fileNameV1 = getSongFileNameV1(trackNumber, title);
        fileNameV1 = getSongFileNameFull(fileNameV1, stage, extension);
        File fileV1 = new File(albumDirectory, fileNameV1);
        if (exists.test(fileV1)) {
            return fileV1;
        }

        Integer discNumber = song.getDiscNumber();
        if (discNumber == null) {
            discNumber = 1;
        }
        String fileNameV2 = getSongFileNameV2(trackNumber, discNumber, title);
        fileNameV2 = getSongFileNameFull(fileNameV2, stage, extension);
        return new File(albumDirectory, fileNameV2);
    }

    /**
     * Resolves the files of many songs, for example while a playlist is queued.
     *
     * Checking whether each song still uses a legacy file name costs a filesystem
     * call per path, which made queueing large playlists slow on external storage
     * (issue #135). The batch validates the music directory once and lists each
     * album directory once instead. The listings are not refreshed, so a batch is
     * meant to be used for one operation and then dropped.
     */
    public static final class SongFileBatch {
        private final Context context;
        private final File musicDirectory;
        private final Map<File, Set<String>> listings = new HashMap<>();

        public SongFileBatch(Context context) {
            this.context = context;
            this.musicDirectory = getMusicDirectory(context);
        }

        public SongFiles get(MusicDirectory.Entry song) {
            return getSongFiles(context, song, musicDirectory, this::isListed);
        }

        private boolean isListed(File file) {
            return listings.computeIfAbsent(file.getParentFile(), SongFileBatch::list).contains(file.getName());
        }

        private static Set<String> list(File directory) {
            String[] names = directory == null ? null : directory.list();
            return names == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(names));
        }
    }

    public record SongFiles(File save, File partial, File complete) {}

    private static String getSongFileNameV1(Integer trackNumber, String title) {
        String fileName = trackNumber == null ? title : String.format(Locale.ROOT, "%02d-%s", trackNumber, title);
        if(fileName.length() >= MAX_FILENAME_LENGTH_V1) {
            fileName = fileName.substring(0, MAX_FILENAME_LENGTH_V1);
        }
        return fileName;
    }

    private static String getSongFileNameV2(Integer trackNumber, Integer discNumber, String title) {
        String fileName = String.format(Locale.ROOT, "%02d-%03d-%s", discNumber, trackNumber, title);
        if(fileName.length() >= MAX_FILENAME_LENGTH_V2) {
            fileName = fileName.substring(0, MAX_FILENAME_LENGTH_V2);
        }
        return fileName;
    }

    public static void parseFileNameIntoEntry(MusicDirectory.Entry entry, String fileName) {
        String discNumber = null;
        String trackNumber = null;
        String title = null;

		// tracknumber could be writte as null in v5.7.0 - dont think we can recover from this..
        Pattern fileNameV2 = Pattern.compile("(\\d{2})-(\\d{3}|null)-(.+)");
        Matcher matcher = fileNameV2.matcher(fileName);
        if (matcher.matches()) {
            discNumber = matcher.group(1);
            trackNumber = matcher.group(2);
            title = matcher.group(3);
        } else {
            Pattern fileNameV1 = Pattern.compile("(\\d{2})-(.+)");
            matcher = fileNameV1.matcher(fileName);
            if (matcher.matches()) {
                trackNumber = matcher.group(1);
                title = matcher.group(2);
            }
        }

        if (discNumber != null) {
            try {
                entry.setDiscNumber(Integer.parseInt(discNumber));
            } catch(Exception e) {
                entry.setDiscNumber(1);
            }
        }
        if (trackNumber != null) {
            try {
                entry.setTrack(Integer.parseInt(trackNumber));
            } catch(Exception e) {
            }
        }
        if (title != null) {
            entry.setTitle(title);
        }
    }

    private static String getSongFileNameFull(String fileName, String stage, String extension) {
        if (stage != null) {
            fileName = fileName + "." + stage;
        }
        return fileName + "." + extension;
    }

    /**
     * Returns whether the given file name denotes a partially downloaded file
     * ("name.partial.ext" or legacy "name.partial").
     */
    public static boolean isPartialFile(String name) {
        return name.endsWith(".partial") || name.contains(".partial.");
    }

    /**
     * Returns whether the given file name denotes a song which is fully
     * downloaded but not yet pinned ("name.complete.ext" or legacy "name.complete").
     */
    public static boolean isCompleteFile(String name) {
        return name.endsWith(".complete") || name.contains(".complete.");
    }

    /**
     * Removes the download stage marker (".partial" or ".complete") from the
     * given file name, yielding the name of the finalized file. V1 file names
     * append the marker as a bare suffix ("song.complete"), V2 insert it before
     * the extension ("song.complete.mp3").
     */
    public static String stripStage(String name) {
        for (String stage : new String[] {".complete", ".partial"}) {
            if (name.endsWith(stage)) {
                name = name.substring(0, name.length() - stage.length());
            } else {
                int index = name.lastIndexOf(stage + ".");
                if (index != -1) {
                    name = name.substring(0, index) + name.substring(index + stage.length());
                }
            }
        }
        return name;
    }

    private static String getSongFileNameExtension(Context context, MusicDirectory.Entry song) {
        if(song.isVideo()) {
            String videoPlayerType = Util.getVideoPlayerType(context);
            if("hls".equals(videoPlayerType)) {
                // HLS should be able to transcode to mp4 automatically
                return "mp4";
            } else if("raw".equals(videoPlayerType)) {
                // Download the original video without any transcoding
                return song.getSuffix();
            }
        } else {
            if (song.getTranscodedSuffix() != null) {
                return song.getTranscodedSuffix();
            }
        }
        return song.getSuffix();
    }

	public static File getPlaylistFile(Context context, String server, String name) {
		File playlistDir = getPlaylistDirectory(context, server);
		return new File(playlistDir, fileSystemSafe(name) + ".m3u");
	}
	public static void writePlaylistFile(Context context, File file, MusicDirectory playlist) throws IOException {
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		try {
			fw.write("#EXTM3U\n");
			for (MusicDirectory.Entry e : playlist.getChildren()) {
				String filePath = FileUtil.getSongFiles(context, e).save().getAbsolutePath();
				if(! new File(filePath).exists()){
					String ext = FileUtil.getExtension(filePath);
					String base = FileUtil.getBaseName(filePath);
					filePath = base + ".complete." + ext;
				}
				fw.write(filePath + "\n");
			}
		} catch(Exception e) {
			Log.w(TAG, "Failed to save playlist: " + playlist.getName());
		} finally {
			bw.close();
			fw.close();
		}
	}
	/**
	 * Checks whether the given playlist file still lists all songs of the playlist by
	 * comparing line counts. Reading the file sequentially is much cheaper than writing
	 * it, which resolves the path of every song on the (possibly slow) external storage.
	 * This catches stale or truncated files without rewriting unchanged playlists.
	 */
	public static boolean isPlaylistFileUpToDate(File file, MusicDirectory playlist) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			if(!"#EXTM3U".equals(reader.readLine())) {
				return false;
			}

			int count = 0;
			while(reader.readLine() != null) {
				count++;
			}
			return count == playlist.getChildrenSize();
		} catch(Exception e) {
			Log.w(TAG, "Failed to read playlist: " + file.getName(), e);
			return false;
		} finally {
			Util.close(reader);
		}
	}
	public static File getPlaylistDirectory(Context context) {
		File playlistDir = new File(getSubsonicDirectory(context), "playlists");
		ensureDirectoryExistsAndIsReadWritable(playlistDir);
		return playlistDir;
	}
	public static File getPlaylistDirectory(Context context, String server) {
		File playlistDir = new File(getPlaylistDirectory(context), server);
		ensureDirectoryExistsAndIsReadWritable(playlistDir);
		return playlistDir;
	}

	public static File getAlbumArtFile(Context context, PodcastChannel channel) {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		entry.setId(channel.getId());
		entry.setTitle(channel.getName());
		return getAlbumArtFile(context, entry);
	}
    public static File getAlbumArtFile(Context context, MusicDirectory.Entry entry) {
		String coverArt = entry.getCoverArt();
		if(entry.getId().indexOf(ImageLoader.PLAYLIST_PREFIX) != -1) {
			File dir = getAlbumArtDirectory(context);
			return  new File(dir, Util.md5Hex(ImageLoader.PLAYLIST_PREFIX + entry.getTitle()) + ".jpeg");
		} else if(entry.getId().indexOf(ImageLoader.PODCAST_PREFIX) != -1) {
			File dir = getAlbumArtDirectory(context);
			return  new File(dir, Util.md5Hex(ImageLoader.PODCAST_PREFIX + entry.getTitle()) + ".jpeg");
		} else if (coverArt != null && new File(coverArt).isAbsolute()) {
			// Offline entries store the location of the album art file as their cover art
			return new File(coverArt);
		} else if (coverArt != null && !coverArt.isEmpty()) {
			// Servers may return a different cover art ID for every song when the art is
			// embedded in the file. Key the cache by that ID instead of the album directory,
			// otherwise all songs of an album would end up sharing one image.
			String key = Util.getMostRecentActiveServer(context) + ":" + coverArt;
			return new File(getAlbumArtDirectory(context), Util.md5Hex(key) + ".jpeg");
		} else {
			return getOfflineAlbumArtFile(context, entry);
		}
    }

	/**
	 * The album art file kept for downloaded songs, one per album. Offline mode and
	 * MediaStore rely on it. Use {@link #getAlbumArtFile(Context, MusicDirectory.Entry)}
	 * for the art that is displayed while browsing online; servers with embedded art
	 * return a different cover art ID per song, which cannot be represented here.
	 */
	public static File getOfflineAlbumArtFile(Context context, MusicDirectory.Entry entry) {
		File albumDir = getAlbumDirectory(context, entry);
		File artFile;
		File albumFile = getAlbumArtFile(albumDir);
		File hexFile = getHexAlbumArtFile(context, albumDir);
		if (albumDir.exists()) {
			if (hexFile.exists()) {
				hexFile.renameTo(albumFile);
			}
			artFile = albumFile;
		} else {
			artFile = hexFile;
		}
		return artFile;
	}

    public static File getAlbumArtFile(File albumDir) {
        return new File(albumDir, Constants.ALBUM_ART_FILE);
    }
	public static File getHexAlbumArtFile(Context context, File albumDir) {
		return new File(getAlbumArtDirectory(context), Util.md5Hex(albumDir.getPath()) + ".jpeg");
	}

	/**
	 * Art file for a single downloaded song. Downloaded songs of a folder can share
	 * one album art file (see {@link #getOfflineAlbumArtFile}), so keep a per song
	 * copy for servers that embed different art in each track. Offline mode finds it
	 * again from the song's file path.
	 */
	public static File getOfflineSongArtFile(Context context, File songFile) {
		return new File(getAlbumArtDirectory(context), Util.md5Hex("song:" + songFile.getAbsolutePath()) + ".jpeg");
	}

	public static void copyFile(File from, File to) throws IOException {
		File parent = to.getParentFile();
		if (parent != null) {
			ensureDirectoryExistsAndIsReadWritable(parent);
		}

		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(from);
			out = new FileOutputStream(to);
			Util.copy(in, out);
		} finally {
			Util.close(in);
			Util.close(out);
		}
	}

    public static Bitmap getAlbumArtBitmap(Context context, MusicDirectory.Entry entry, int size) {
        File albumArtFile = getAlbumArtFile(context, entry);
        return getSampledBitmap(albumArtFile, size);
    }

	public static File getAvatarDirectory(Context context) {
		File avatarDir = new File(getSubsonicDirectory(context), "avatars");
		ensureDirectoryExistsAndIsReadWritable(avatarDir);
		ensureDirectoryExistsAndIsReadWritable(new File(avatarDir, ".nomedia"));
		return avatarDir;
	}

	public static File getAvatarFile(Context context, String username) {
		return new File(getAvatarDirectory(context), Util.md5Hex(username) + ".jpeg");
	}

	public static Bitmap getAvatarBitmap(Context context, String username, int size) {
		File avatarFile = getAvatarFile(context, username);
		return getSampledBitmap(avatarFile, size, false);
	}

	public static File getMiscDirectory(Context context) {
		File dir = new File(getSubsonicDirectory(context), "misc");
		ensureDirectoryExistsAndIsReadWritable(dir);
		ensureDirectoryExistsAndIsReadWritable(new File(dir, ".nomedia"));
		return dir;
	}

	public static File getMiscFile(Context context, String url) {
		return new File(getMiscDirectory(context), Util.md5Hex(url) + ".jpeg");
	}

	public static Bitmap getMiscBitmap(Context context, String url, int size) {
		File miscFile = getMiscFile(context, url);
		return getSampledBitmap(miscFile, size, false);
	}

	public static Bitmap getSampledBitmap(byte[] bytes, int size) {
		return getSampledBitmap(bytes, size, true);
	}
	public static Bitmap getSampledBitmap(byte[] bytes, int size, boolean allowUnscaled) {
		final BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
		opt.inPurgeable = true;
		opt.inSampleSize = Util.calculateInSampleSize(opt, size, Util.getScaledHeight(opt.outHeight, opt.outWidth, size));
		opt.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
		if(bitmap == null) {
			return null;
		} else {
			return getScaledBitmap(bitmap, size, allowUnscaled);
		}
	}
	public static Bitmap getSampledBitmap(File file, int size) {
		return getSampledBitmap(file, size, true);
	}
	public static Bitmap getSampledBitmap(File file, int size, boolean allowUnscaled) {
		if (!file.exists()) {
			return null;
		}

		final BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getPath(), opt);
		opt.inPurgeable = true;
		opt.inSampleSize = Util.calculateInSampleSize(opt, size, Util.getScaledHeight(opt.outHeight, opt.outWidth, size));
		opt.inJustDecodeBounds = false;

		Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), opt);
		return bitmap == null ? null : getScaledBitmap(bitmap, size, allowUnscaled);
	}
	public static Bitmap getScaledBitmap(Bitmap bitmap, int size) {
		return getScaledBitmap(bitmap, size, true);
	}
	public static Bitmap getScaledBitmap(Bitmap bitmap, int size, boolean allowUnscaled) {
		// Don't waste time scaling if the difference is minor
		// Large album arts still need to be scaled since displayed as is on now playing!
		if(allowUnscaled && size < 400 && bitmap.getWidth() < (size * 1.1)) {
			return bitmap;
		} else {
			return Bitmap.createScaledBitmap(bitmap, size, Util.getScaledHeight(bitmap, size), true);
		}
	}

	public static File getAlbumArtDirectory(Context context) {
		File albumArtDir = new File(getSubsonicDirectory(context), "artwork");
		ensureDirectoryExistsAndIsReadWritable(albumArtDir);
		ensureDirectoryExistsAndIsReadWritable(new File(albumArtDir, ".nomedia"));
		return albumArtDir;
	}

	public static File getArtistDirectory(Context context, Artist artist) {
		File dir = new File(getMusicDirectory(context).getPath() + "/" + fileSystemSafe(artist.getName()));
		return dir;
	}
	public static File getArtistDirectory(Context context, MusicDirectory.Entry artist) {
		File dir = new File(getMusicDirectory(context).getPath() + "/" + fileSystemSafe(artist.getTitle()));
		return dir;
	}

    public static File getAlbumDirectory(Context context, MusicDirectory.Entry entry) {
        return getAlbumDirectory(context, entry, getMusicDirectory(context));
    }

    private static File getAlbumDirectory(Context context, MusicDirectory.Entry entry, File musicDirectory) {
        File dir = null;
        if (entry.getPath() != null) {
            File f = new File(fileSystemSafeDir(entry.getPath()));
			String folder = musicDirectory.getPath();
			if(entry.isDirectory()) {
				folder += "/" + f.getPath();
			} else if(f.getParent() != null) {
				folder += "/" + f.getParent();
			}
            dir = new File(folder);
        } else {
			MusicDirectory.Entry firstSong;
			if(!Util.isOffline(context)) {
				firstSong = lookupChild(context, entry, false);
				if(firstSong != null) {
					File songFile = getSongFiles(context, firstSong).save();
					dir = songFile.getParentFile();
				}
			}

			if(dir == null) {
				String artist = fileSystemSafe(entry.getArtist());
				String album = fileSystemSafe(entry.getAlbum());
				if("unnamed".equals(album)) {
					album = fileSystemSafe(entry.getTitle());
				}
				dir = new File(musicDirectory.getPath() + "/" + artist + "/" + album);
			}
        }
        return dir;
    }

	public static MusicDirectory.Entry lookupChild(Context context, MusicDirectory.Entry entry, boolean allowDir) {
		// Initialize lookupMap if first time called
		String lookupName = Util.getCacheName(context, "entryLookup");
		if(entryLookup == null) {
			entryLookup = deserialize(context, lookupName, HashMap.class);
			
			// Create it if 
			if(entryLookup == null) {
				entryLookup = new HashMap<String, MusicDirectory.Entry>();
			}
		}
		
		// Check if this lookup has already been done before
		MusicDirectory.Entry child = entryLookup.get(entry.getId());
		if(child != null) {
			return child;
		}
		
		// Do a special lookup since 4.7+ doesn't match artist/album to entry.getPath
		String s = Util.getRestUrl(context, null, false) + entry.getId();
		String cacheName = (Util.isTagBrowsing(context) ? "album-" : "directory-") + s.hashCode() + ".ser";
		MusicDirectory entryDir = FileUtil.deserialize(context, cacheName, MusicDirectory.class);

		if(entryDir != null) {
			List<MusicDirectory.Entry> songs = entryDir.getChildren(allowDir, true);
			if(songs.size() > 0) {
				child = songs.get(0);
				entryLookup.put(entry.getId(), child);
				serialize(context, entryLookup, lookupName);
				return child;
			}
		}

		return null;
	}
	
	public static String getPodcastPath(Context context, PodcastEpisode episode) {
		return fileSystemSafe(episode.getArtist()) + "/" + fileSystemSafe(episode.getTitle());
	}
	public static File getPodcastFile(Context context, String server) {
		File dir = getPodcastDirectory(context);
		return new File(dir.getPath() + "/" +  fileSystemSafe(server));
	}
	public static File getPodcastDirectory(Context context) {
		File dir = new File(context.getCacheDir(), "podcasts");
		ensureDirectoryExistsAndIsReadWritable(dir);
		return dir;
	}
	public static File getPodcastDirectory(Context context, PodcastChannel channel) {
		File dir = new File(getMusicDirectory(context).getPath() + "/" + fileSystemSafe(channel.getName()));
		return dir;
	}
	public static File getPodcastDirectory(Context context, String channel) {
		File dir = new File(getMusicDirectory(context).getPath() + "/" + fileSystemSafe(channel));
		return dir;
	}

    public static void createDirectoryForParent(File file) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory " + dir);
            }
        }
    }

    private static File createDirectory(Context context, String name) {
        File dir = new File(getSubsonicDirectory(context), name);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create " + name);
        }
        return dir;
    }

    public static File getSubsonicDirectory(Context context) {
        return context.getExternalFilesDir(null);
    }

    public static File getDefaultMusicDirectory(Context context) {
		if(DEFAULT_MUSIC_DIR == null) {
			File[] dirs;
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
				dirs = context.getExternalMediaDirs();
			} else {
				dirs = ContextCompat.getExternalFilesDirs(context, null);
			}

			DEFAULT_MUSIC_DIR = new File(getBestDir(dirs), "music");

			if (!DEFAULT_MUSIC_DIR.exists() && !DEFAULT_MUSIC_DIR.mkdirs()) {
				Log.e(TAG, "Failed to create default dir " + DEFAULT_MUSIC_DIR);

				// Some devices seem to have screwed up the new media directory API.  Go figure.  Try again with standard locations
				dirs = ContextCompat.getExternalFilesDirs(context, null);

				DEFAULT_MUSIC_DIR = new File(getBestDir(dirs), "music");
				if (!DEFAULT_MUSIC_DIR.exists() && !DEFAULT_MUSIC_DIR.mkdirs()) {
					Log.e(TAG, "Failed to create default dir " + DEFAULT_MUSIC_DIR);
				} else {
					Log.w(TAG, "Stupid OEM's messed up media dir API added in 5.0");
				}
			}
		}

        return DEFAULT_MUSIC_DIR;
    }
	private static File getBestDir(File[] dirs) {
		// Past 5.0 we can query directly for SD Card
		for(int i = 0; i < dirs.length; i++) {
			try {
				if (dirs[i] != null && Environment.isExternalStorageRemovable(dirs[i])) {
					return dirs[i];
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to check if is external", e);
			}
		}

		// Before 5.0, we have to guess.  Most of the time the SD card is last
		for(int i = dirs.length - 1; i >= 0; i--) {
			if(dirs[i] != null) {
				return dirs[i];
			}
		}

		// Should be impossible to be reached
		return dirs[0];
	}

    public static File getMusicDirectory(Context context) {
        String path = Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, getDefaultMusicDirectory(context).getPath());
        File dir = new File(path);
        return ensureDirectoryExistsAndIsReadWritable(dir) ? dir : getDefaultMusicDirectory(context);
    }
	public static boolean deleteMusicDirectory(Context context) {
		File musicDirectory = FileUtil.getMusicDirectory(context);
		MediaStoreService mediaStore = new MediaStoreService(context);
		return recursiveDelete(musicDirectory, mediaStore);
	}
	public static void deleteSerializedCache(Context context) {
		for(File file: context.getCacheDir().listFiles()) {
			if(file.getName().indexOf(".ser") != -1) {
				file.delete();
			}
		}
	}
	public static boolean deleteArtworkCache(Context context) {
		File artDirectory = FileUtil.getAlbumArtDirectory(context);
		return recursiveDelete(artDirectory);
	}
	public static boolean deleteAvatarCache(Context context) {
		File artDirectory = FileUtil.getAvatarDirectory(context);
		return recursiveDelete(artDirectory);
	}

	public static boolean recursiveDelete(File dir) {
		return recursiveDelete(dir, null);
	}
	public static boolean recursiveDelete(File dir, MediaStoreService mediaStore) {
		if (dir != null && dir.exists()) {
			File[] list = dir.listFiles();
			if(list != null) {
				for(File file: list) {
					if(file.isDirectory()) {
						if(!recursiveDelete(file, mediaStore)) {
							return false;
						}
					} else if(file.exists()) {
						if(!file.delete()) {
							return false;
						} else if(mediaStore != null) {
							mediaStore.deleteFromMediaStore(file);
						}
					}
				}
			}
			return dir.delete();
		}
		return false;
	}

	public static void deleteEmptyDir(File dir) {
		try {
			File[] children = dir.listFiles();
			if(children == null) {
				return;
			}

			// No songs left in the folder
			if (children.length == 1 && children[0].getPath().equals(FileUtil.getAlbumArtFile(dir).getPath())) {
				Util.delete(children[0]);
				children = dir.listFiles();
			}

			// Delete empty directory
			if (children.length == 0) {
				Util.delete(dir);
			}
		} catch(Exception e) {
			Log.w(TAG, "Error while trying to delete empty dir", e);
		}
	}

	public static void unpinSong(Context context, File saveFile) {
		// Don't try to unpin a song which isn't actually pinned
		if(isCompleteFile(saveFile.getName())) {
			return;
		}

		// Unpin file, rename to .complete
		File completeFile = new File(saveFile.getParent(), FileUtil.getBaseName(saveFile.getName()) +
				".complete." + FileUtil.getExtension(saveFile.getName()));

		if(!saveFile.renameTo(completeFile)) {
			Log.w(TAG, "Failed to upin " + saveFile + " to " + completeFile);
		} else {
			try {
				new MediaStoreService(context).renameInMediaStore(saveFile, completeFile);
			} catch(Exception e) {
				Log.w(TAG, "Failed to write to media store");
			}
		}
	}

    public static boolean ensureDirectoryExistsAndIsReadWritable(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.exists()) {
            if (!dir.isDirectory()) {
                Log.w(TAG, dir + " exists but is not a directory.");
                return false;
            }
        } else {
            if (dir.mkdirs()) {
                Log.i(TAG, "Created directory " + dir);
            } else {
                Log.w(TAG, "Failed to create directory " + dir);
                return false;
            }
        }

        if (!dir.canRead()) {
            Log.w(TAG, "No read permission for directory " + dir);
            return false;
        }

        if (!dir.canWrite()) {
            Log.w(TAG, "No write permission for directory " + dir);
            return false;
        }
        return true;
    }
	public static boolean verifyCanWrite(File dir) {
		if(ensureDirectoryExistsAndIsReadWritable(dir)) {
			try {
				File tmp = new File(dir, "checkWrite");
				tmp.createNewFile();
				if(tmp.exists()) {
					if(tmp.delete()) {
						return true;
					} else {
						Log.w(TAG, "Failed to delete temp file, retrying");
						
						// This should never be reached since this is a file DSub created!
						Thread.sleep(100L);
						tmp = new File(dir, "checkWrite");
						if(tmp.delete()) {
							return true;
						} else {
							Log.w(TAG, "Failed retry to delete temp file");
							return false;
						}
					}
				} else {
					Log.w(TAG, "Temp file does not actually exist");
					return false;
				}
			} catch(Exception e) {
				Log.w(TAG, "Failed to create tmp file", e);
				return false;
			}
		} else {
			return false;
		}
	}

    /**
    * Makes a given filename safe by replacing special characters like slashes ("/" and "\")
    * with dashes ("-").
    *
    * @param filename The filename in question.
    * @return The filename with special characters replaced by hyphens.
    */
    private static String fileSystemSafe(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return "unnamed";
        }

        for (String s : FILE_SYSTEM_UNSAFE) {
            filename = filename.replace(s, "-");
        }
        return filename;
    }

    /**
     * Makes a given filename safe by replacing special characters like colons (":")
     * with dashes ("-").
     *
     * @param path The path of the directory in question.
     * @return The the directory name with special characters replaced by hyphens.
     */
    private static String fileSystemSafeDir(String path) {
        if (path == null || path.trim().length() == 0) {
            return "";
        }

		// Airsonic 11 appears to be returning backslashes instead of always forcing path with forward slashes
		if(!path.contains("/")) {
			path = path.replace("\\", "/");
		}
        for (String s : FILE_SYSTEM_UNSAFE_DIR) {
            path = path.replace(s, "-");
        }
        return path;
    }

    /**
     * Similar to {@link File#listFiles()}, but returns a sorted set.
     * Never returns {@code null}, instead a warning is logged, and an empty set is returned.
     */
    public static SortedSet<File> listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            Log.w(TAG, "Failed to list children for " + dir.getPath());
            return new TreeSet<File>();
        }

        return new TreeSet<File>(Arrays.asList(files));
    }

    public static SortedSet<File> listMediaFiles(File dir) {
        SortedSet<File> files = listFiles(dir);
        Iterator<File> iterator = files.iterator();
        while (iterator.hasNext()) {
            File file = iterator.next();
            if (!file.isDirectory() && !isMediaFile(file)) {
                iterator.remove();
            }
        }
        return files;
    }

    private static boolean isMediaFile(File file) {
        String extension = getExtension(file.getName());
        return MUSIC_FILE_EXTENSIONS.contains(extension) || VIDEO_FILE_EXTENSIONS.contains(extension);
    }
	
	public static boolean isMusicFile(File file) {
		String extension = getExtension(file.getName());
        return MUSIC_FILE_EXTENSIONS.contains(extension);
	}
	public static boolean isVideoFile(File file) {
		String extension = getExtension(file.getName());
        return VIDEO_FILE_EXTENSIONS.contains(extension);
	}
	
	public static boolean isPlaylistFile(File file) {
		String extension = getExtension(file.getName());
		return PLAYLIST_FILE_EXTENSIONS.contains(extension);
	}

    /**
     * Returns the extension (the substring after the last dot) of the given file. The dot
     * is not included in the returned extension.
     *
     * @param name The filename in question.
     * @return The extension, or an empty string if no extension is found.
     */
    public static String getExtension(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? "" : name.substring(index + 1).toLowerCase();
    }

    /**
     * Returns the base name (the substring before the last dot) of the given file. The dot
     * is not included in the returned basename.
     *
     * @param name The filename in question.
     * @return The base name, or an empty string if no basename is found.
     */
    public static String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(0, index);
    }
	
	public static Long[] getUsedSize(Context context, File file) {
		long number = 0L;
		long permanent = 0L;
		long size = 0L;
		
		if(file.isFile()) {
			if(isMediaFile(file)) {
				if(!isCompleteFile(file.getName())) {
					permanent++;
				}
				return new Long[] {1L, permanent, file.length()};
			} else {
				return new Long[] {0L, 0L, 0L};
			}
		} else {
			for (File child : FileUtil.listFiles(file)) {
				Long[] pair = getUsedSize(context, child);
				number += pair[0];
				permanent += pair[1];
				size += pair[2];
			}
			return new Long[] {number, permanent, size};
		}
	}

	/**
	 * Serialized objects are written to a temporary file first so that a killed process cannot leave a
	 * partially written cache file behind.
	 */
	private static File createTempFile(Context context, String fileName) {
		return new File(context.getCacheDir(), fileName + "." + UUID.randomUUID() + ".tmp");
	}

    public static <T extends Serializable> boolean serialize(Context context, T obj, String fileName) {
		Output out = null;
		File tmpFile = createTempFile(context, fileName);
		try {
			out = new Output(new FileOutputStream(tmpFile));
			synchronized (kryo) {
				kryo.writeObject(out, obj);
			}
			out.close();
			out = null;
			Util.renameFile(tmpFile, new File(context.getCacheDir(), fileName));
			return true;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to serialize object to " + fileName, x);
			return false;
		} finally {
			Util.close(out);
			Util.delete(tmpFile);
		}
    }

	public static <T extends Serializable> T deserialize(Context context, String fileName, Class<T> tClass) {
		return deserialize(context, fileName, tClass, 0);
	}

    public static <T extends Serializable> T deserialize(Context context, String fileName, Class<T> tClass, int hoursOld) {
		Input in = null;
		try {
			File file = new File(context.getCacheDir(), fileName);
			if(!file.exists()) {
				return null;
			}

			if(hoursOld != 0) {
				Date fileDate = new Date(file.lastModified());
				// Convert into hours
				long age = (new Date().getTime() - fileDate.getTime()) / 1000 / 3600;
				if(age > hoursOld) {
					return null;
				}
			}

			in = new Input(new FileInputStream(file));
			synchronized (kryo) {
				T result = kryo.readObject(in, tClass);
				return result;
			}
		} catch(FileNotFoundException e) {
			// Different error message
			Log.w(TAG, "No serialization for object from " + fileName, e);
			return null;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to deserialize object from " + fileName, x);
			return null;
		} finally {
			Util.close(in);
		}
    }

	public static <T extends Serializable> boolean serializeCompressed(Context context, T obj, String fileName) {
		Output out = null;
		File tmpFile = createTempFile(context, fileName);
		try {
			out = new Output(new DeflaterOutputStream(new FileOutputStream(tmpFile)));
			synchronized (kryo) {
				kryo.writeObject(out, obj);
			}
			out.close();
			out = null;
			Util.renameFile(tmpFile, new File(context.getCacheDir(), fileName));
			return true;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to serialize compressed object to " + fileName);
			return false;
		} finally {
			Util.close(out);
			Util.delete(tmpFile);
		}
	}

	public static <T extends Serializable> T deserializeCompressed(Context context, String fileName, Class<T> tClass) {
		Input in = null;
		try {
			File file = new File(context.getCacheDir(), fileName);

			in = new Input(new InflaterInputStream(new FileInputStream(file)));
			synchronized (kryo) {
				T result = kryo.readObject(in, tClass);
				return result;
			}
		} catch(FileNotFoundException e) {
			// Different error message
			Log.w(TAG, "No serialization compressed for object from " + fileName);
			return null;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to deserialize compressed object from " + fileName);
			return null;
		} finally {
			Util.close(in);
		}
	}
}
