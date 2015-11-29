/*
	This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Date;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;

public class SongDBHandler extends SQLiteOpenHelper {
	private static final String TAG = SongDBHandler.class.getSimpleName();
	private static SongDBHandler dbHandler;

	private static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "SongsDB";

	public static final String TABLE_SONGS = "RegisteredSongs";
	public static final String SONGS_ID = "id";
	public static final String SONGS_SERVER_ID = "serverId";
	public static final String SONGS_COMPLETE_PATH = "completePath";
	public static final String SONGS_LAST_PLAYED = "lastPlayed";
	public static final String SONGS_LAST_COMPLETED = "lastCompleted";

	private SongDBHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_SONGS + " ( " + SONGS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SONGS_SERVER_ID + " TEXT NOT NULL UNIQUE, " + SONGS_COMPLETE_PATH + " TEXT NOT NULL, " + SONGS_LAST_PLAYED + " INTEGER, " + SONGS_LAST_COMPLETED + " INTEGER )");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
		this.onCreate(db);
	}

	public synchronized void addSong(DownloadFile downloadFile) {
		SQLiteDatabase db = this.getWritableDatabase();
		addSong(db, downloadFile);
		db.close();
	}
	protected synchronized void addSong(SQLiteDatabase db, DownloadFile downloadFile) {
		ContentValues values = new ContentValues();
		values.put(SONGS_SERVER_ID, downloadFile.getSong().getId());
		values.put(SONGS_COMPLETE_PATH, downloadFile.getSaveFile().getAbsolutePath());

		db.insertWithOnConflict(TABLE_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public synchronized void setSongPlayed(DownloadFile downloadFile, boolean submission) {
		// Open and make sure song is in db
		SQLiteDatabase db = this.getWritableDatabase();
		addSong(db, downloadFile);

		// Update song's last played
		ContentValues values = new ContentValues();
		values.put(submission ? SONGS_LAST_COMPLETED : SONGS_LAST_PLAYED, System.currentTimeMillis());
		db.update(TABLE_SONGS, values, SONGS_SERVER_ID + " = ?", new String[]{downloadFile.getSong().getId()});
		db.close();
	}

	public Long[] getLastPlayed(String id) {
		SQLiteDatabase db = this.getReadableDatabase();

		String[] columns = {SONGS_LAST_PLAYED, SONGS_LAST_COMPLETED};
		Cursor cursor = db.query(TABLE_SONGS, columns, SONGS_SERVER_ID + " = ?", new String[] { id }, null, null, null, null);

		try {
			cursor.moveToFirst();

			Long[] dates = new Long[2];
			dates[0] = cursor.getLong(0);
			dates[1] = cursor.getLong(1);
			return dates;
		} catch(Exception e) {}

		return null;
	}

	public static SongDBHandler getHandler(Context context) {
		if(dbHandler == null) {
			dbHandler = new SongDBHandler(context);
		}

		return dbHandler;
	}
}
