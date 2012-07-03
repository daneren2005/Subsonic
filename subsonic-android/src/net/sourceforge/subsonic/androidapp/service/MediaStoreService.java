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

import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.util.FileUtil;

/**
 * @author Sindre Mehus
 */
public class MediaStoreService {

    private static final String TAG = MediaStoreService.class.getSimpleName();
    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");

    private final Context context;

    public MediaStoreService(Context context) {
        this.context = context;
    }

    public void saveInMediaStore(DownloadFile downloadFile) {
        MusicDirectory.Entry song = downloadFile.getSong();
        File songFile = downloadFile.getCompleteFile();

        // Delete existing row in case the song has been downloaded before.
        deleteFromMediaStore(downloadFile);

        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, song.getTitle());
        values.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
        values.put(MediaStore.Audio.AudioColumns.ALBUM, song.getAlbum());
        values.put(MediaStore.Audio.AudioColumns.TRACK, song.getTrack());
        values.put(MediaStore.Audio.AudioColumns.YEAR, song.getYear());
        values.put(MediaStore.MediaColumns.DATA, songFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.MIME_TYPE, song.getContentType());
        values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1);

        Uri uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        // Look up album, and add cover art if found.
        Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.Audio.AudioColumns.ALBUM_ID}, null, null, null);
        if (cursor.moveToFirst()) {
            int albumId = cursor.getInt(0);
            insertAlbumArt(albumId, downloadFile);
        }
        cursor.close();
    }

    public void deleteFromMediaStore(DownloadFile downloadFile) {
        ContentResolver contentResolver = context.getContentResolver();
        MusicDirectory.Entry song = downloadFile.getSong();
        File file = downloadFile.getCompleteFile();

        int n = contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.AudioColumns.TITLE_KEY + "=? AND " +
                        MediaStore.MediaColumns.DATA + "=?",
                new String[]{MediaStore.Audio.keyFor(song.getTitle()), file.getAbsolutePath()});
        if (n > 0) {
            Log.i(TAG, "Deleting media store row for " + song);
        }
    }

    private void insertAlbumArt(int albumId, DownloadFile downloadFile) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(Uri.withAppendedPath(ALBUM_ART_URI, String.valueOf(albumId)), null, null, null, null);
        if (!cursor.moveToFirst()) {

            // No album art found, add it.
            File albumArtFile = FileUtil.getAlbumArtFile(context, downloadFile.getSong());
            if (albumArtFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, albumId);
                values.put(MediaStore.MediaColumns.DATA, albumArtFile.getPath());
                contentResolver.insert(ALBUM_ART_URI, values);
                Log.i(TAG, "Added album art: " + albumArtFile);
            }
        }
        cursor.close();
    }

}