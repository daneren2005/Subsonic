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
package net.sourceforge.subsonic.androidapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;

/**
 * @author Sindre Mehus
 */
public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();
    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":", "\"", "?", "*", "<", ">"};
    private static final String[] FILE_SYSTEM_UNSAFE_DIR = {"\\", "..", ":", "\"", "?", "*", "<", ">"};
    private static final List<String> MUSIC_FILE_EXTENSIONS = Arrays.asList("mp3", "ogg", "aac", "flac", "m4a", "wav", "wma");
    private static final File DEFAULT_MUSIC_DIR = createDirectory("music");

    public static File getSongFile(Context context, MusicDirectory.Entry song) {
        File dir = getAlbumDirectory(context, song);

        StringBuilder fileName = new StringBuilder();
        Integer track = song.getTrack();
        if (track != null) {
            if (track < 10) {
                fileName.append("0");
            }
            fileName.append(track).append("-");
        }

        fileName.append(fileSystemSafe(song.getTitle())).append(".");

        if (song.getTranscodedSuffix() != null) {
            fileName.append(song.getTranscodedSuffix());
        } else {
            fileName.append(song.getSuffix());
        }

        return new File(dir, fileName.toString());
    }

    public static File getAlbumArtFile(Context context, MusicDirectory.Entry entry) {
        File albumDir = getAlbumDirectory(context, entry);
        return getAlbumArtFile(albumDir);
    }

    public static File getAlbumArtFile(File albumDir) {
        File albumArtDir = getAlbumArtDirectory();
        return new File(albumArtDir, Util.md5Hex(albumDir.getPath()) + ".jpeg");
    }

    public static Bitmap getAlbumArtBitmap(Context context, MusicDirectory.Entry entry, int size) {
        File albumArtFile = getAlbumArtFile(context, entry);
        if (albumArtFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(albumArtFile.getPath());
            return bitmap == null ? null : Bitmap.createScaledBitmap(bitmap, size, size, true);
        }
        return null;
    }

    public static File getAlbumArtDirectory() {
        File albumArtDir = new File(getSubsonicDirectory(), "artwork");
        ensureDirectoryExistsAndIsReadWritable(albumArtDir);
        ensureDirectoryExistsAndIsReadWritable(new File(albumArtDir, ".nomedia"));
        return albumArtDir;
    }

    private static File getAlbumDirectory(Context context, MusicDirectory.Entry entry) {
        File dir;
        if (entry.getPath() != null) {
            File f = new File(fileSystemSafeDir(entry.getPath()));
            dir = new File(getMusicDirectory(context).getPath() + "/" + (entry.isDirectory() ? f.getPath() : f.getParent()));
        } else {
            String artist = fileSystemSafe(entry.getArtist());
            String album = fileSystemSafe(entry.getAlbum());
            dir = new File(getMusicDirectory(context).getPath() + "/" + artist + "/" + album);
        }
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

    private static File createDirectory(String name) {
        File dir = new File(getSubsonicDirectory(), name);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create " + name);
        }
        return dir;
    }

    public static File getSubsonicDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "subsonic");
    }

    public static File getDefaultMusicDirectory() {
        return DEFAULT_MUSIC_DIR;
    }

    public static File getMusicDirectory(Context context) {
        String path = Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, DEFAULT_MUSIC_DIR.getPath());
        File dir = new File(path);
        return ensureDirectoryExistsAndIsReadWritable(dir) ? dir : getDefaultMusicDirectory();
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

    public static SortedSet<File> listMusicFiles(File dir) {
        SortedSet<File> files = listFiles(dir);
        Iterator<File> iterator = files.iterator();
        while (iterator.hasNext()) {
            File file = iterator.next();
            if (!file.isDirectory() && !isMusicFile(file)) {
                iterator.remove();
            }
        }
        return files;
    }

    private static boolean isMusicFile(File file) {
        String extension = getExtension(file.getName());
        return MUSIC_FILE_EXTENSIONS.contains(extension);
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

    public static <T extends Serializable> boolean serialize(Context context, T obj, String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(obj);
            Log.i(TAG, "Serialized object to " + file);
            return true;
        } catch (Throwable x) {
            Log.w(TAG, "Failed to serialize object to " + file);
            return false;
        } finally {
            Util.close(out);
        }
    }

    public static <T extends Serializable> T deserialize(Context context, String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            T result = (T) in.readObject();
            Log.i(TAG, "Deserialized object from " + file);
            return result;
        } catch (Throwable x) {
            Log.w(TAG, "Failed to deserialize object from " + file, x);
            return null;
        } finally {
            Util.close(in);
        }
    }
}
