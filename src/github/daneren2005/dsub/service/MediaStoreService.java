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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

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
		if(!song.isVideo()) {
			values.put(MediaStore.MediaColumns.TITLE, song.getTitle());
			values.put(MediaStore.MediaColumns.DATA, songFile.getAbsolutePath());
			values.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
			values.put(MediaStore.Audio.AudioColumns.ALBUM, song.getAlbum());
			if (song.getDuration() != null) {
				values.put(MediaStore.Audio.AudioColumns.DURATION, song.getDuration() * 1000L);
			}
			if (song.getTrack() != null) {
				values.put(MediaStore.Audio.AudioColumns.TRACK, song.getTrack());
			}
			if (song.getYear() != null) {
				values.put(MediaStore.Audio.AudioColumns.YEAR, song.getYear());
			}
			if(song.getTranscodedContentType() != null) {
				values.put(MediaStore.MediaColumns.MIME_TYPE, song.getTranscodedContentType());
			} else {
				values.put(MediaStore.MediaColumns.MIME_TYPE, song.getContentType());
			}
			values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1);

			Uri uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

			// Look up album, and add cover art if found.
			Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.Audio.AudioColumns.ALBUM_ID}, null, null, null);
			if (cursor.moveToFirst()) {
				int albumId = cursor.getInt(0);
				insertAlbumArt(albumId, downloadFile);
			}

			cursor.close();
		} else {
			values.put(MediaStore.Video.VideoColumns.TITLE, song.getTitle());
			values.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, song.getTitle());
			values.put(MediaStore.Video.VideoColumns.ARTIST, song.getArtist());
			values.put(MediaStore.Video.VideoColumns.DATA, songFile.getAbsolutePath());
			if (song.getDuration() != null) {
				values.put(MediaStore.Video.VideoColumns.DURATION, song.getDuration() * 1000L);
			}

			String videoPlayerType = Util.getVideoPlayerType(context);
			if("hls".equals(videoPlayerType)) {
				// HLS should be able to transcode to mp4 automatically
				values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mpeg");
			} else if("raw".equals(videoPlayerType) || song.getTranscodedContentType() == null) {
				// Download the original video without any transcoding
				values.put(MediaStore.MediaColumns.MIME_TYPE, song.getContentType());
			} else {
				values.put(MediaStore.MediaColumns.MIME_TYPE, song.getTranscodedContentType());
			}

			Uri uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
			if(uri == null) {
				Log.e(TAG, "Failed to insert");
			}
		}
	}

	public void deleteFromMediaStore(DownloadFile downloadFile) {
		ContentResolver contentResolver = context.getContentResolver();
		MusicDirectory.Entry song = downloadFile.getSong();
		File file = downloadFile.getCompleteFile();

		Uri uri;
		if(song.isVideo()) {
			uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		} else {
			uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		}

		int n = contentResolver.delete(uri,
				MediaStore.MediaColumns.DATA + "=?",
				new String[]{file.getAbsolutePath()});
		if (n > 0) {
			Log.i(TAG, "Deleting media store row for " + song);
		}
	}

	public void deleteFromMediaStore(File file) {
		ContentResolver contentResolver = context.getContentResolver();

		Uri uri;
		if(FileUtil.isVideoFile(file)) {
			uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		} else {
			uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		}

		int n = contentResolver.delete(uri,
				MediaStore.MediaColumns.DATA + "=?",
				new String[]{file.getAbsolutePath()});
		if (n > 0) {
			Log.i(TAG, "Deleting media store row for " + file);
		}
	}

	public void renameInMediaStore(File start, File end) {
		ContentResolver contentResolver = context.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DATA, end.getAbsolutePath());

		int n = contentResolver.update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				values,
				MediaStore.MediaColumns.DATA + "=?",
				new String[]{start.getAbsolutePath()});
		if (n > 0) {
			Log.i(TAG, "Rename media store row for " + start + " to " + end);
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
