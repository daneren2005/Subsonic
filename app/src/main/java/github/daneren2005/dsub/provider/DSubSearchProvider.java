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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Util;

/**
 * Provides search suggestions based on recent searches.
 *
 * @author Sindre Mehus
 */
public class DSubSearchProvider extends ContentProvider {
	private static final String TAG = DSubSearchProvider.class.getSimpleName();

	private static final String RESOURCE_PREFIX = "android.resource://github.daneren2005.dsub/";
	private static final String[] COLUMNS = {"_id",
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_INTENT_DATA,
			SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
			SearchManager.SUGGEST_COLUMN_ICON_1};

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if(selectionArgs[0].isEmpty()) {
			return null;
		}

		String query = selectionArgs[0] + "*";
		SearchResult searchResult = search(query);
		return createCursor(selectionArgs[0], searchResult);
	}

	private SearchResult search(String query) {
		MusicService musicService = MusicServiceFactory.getMusicService(getContext());
		if (musicService == null) {
			return null;
		}

		try {
			return musicService.search(new SearchCritera(query, 5, 10, 10), getContext(), null);
		} catch (Exception e) {
			return null;
		}
	}

	private Cursor createCursor(String query, SearchResult searchResult) {
		MatrixCursor cursor = new MatrixCursor(COLUMNS);
		if (searchResult == null) {
			return cursor;
		}
		
		// Add all results into one pot
		List<Object> results = new ArrayList<Object>();
		results.addAll(searchResult.getArtists());
		results.addAll(searchResult.getAlbums());
		results.addAll(searchResult.getSongs());
		
		// For each, calculate its string distance to the query
		for(Object obj: results) {
			if(obj instanceof Artist) {
				Artist artist = (Artist) obj;
				artist.setCloseness(Util.getStringDistance(query, artist.getName()));
			} else {
				MusicDirectory.Entry entry = (MusicDirectory.Entry) obj;
				entry.setCloseness(Util.getStringDistance(query, entry.getTitle()));
			}
		}
		
		// Sort based on the closeness paramater
		Collections.sort(results, new Comparator<Object>() {
			@Override
			public int compare(Object lhs, Object rhs) {
				// Get the closeness of the two objects
				int left, right;
				boolean leftArtist = lhs instanceof Artist;
				boolean rightArtist = rhs instanceof Artist;
				if (leftArtist) {
					left = ((Artist) lhs).getCloseness();
				} else {
					left = ((MusicDirectory.Entry) lhs).getCloseness();
				}
				if (rightArtist) {
					right = ((Artist) rhs).getCloseness();
				} else {
					right = ((MusicDirectory.Entry) rhs).getCloseness();
				}

				if (left == right) {
					if(leftArtist && rightArtist) {
						return 0;
					} else if(leftArtist) {
						return -1;
					} else if(rightArtist) {
						return 1;
					} else {
						return 0;
					}
				} else if (left > right) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		
		// Done sorting, add results to cursor
		for(Object obj: results) {
			if(obj instanceof Artist) {
				Artist artist = (Artist) obj;
				String icon = RESOURCE_PREFIX + R.drawable.ic_action_artist;
				cursor.addRow(new Object[]{artist.getId().hashCode(), artist.getName(), null, "ar-" + artist.getId(), artist.getName(), icon});
			} else {
				MusicDirectory.Entry entry = (MusicDirectory.Entry) obj;
				
				if(entry.isDirectory()) {
					String icon = RESOURCE_PREFIX + R.drawable.ic_action_album;
					cursor.addRow(new Object[]{entry.getId().hashCode(), entry.getTitle(), entry.getArtist(), entry.getId(), entry.getTitle(), icon});
				} else {
					String icon = RESOURCE_PREFIX + R.drawable.ic_action_song;
					String id;
					if(Util.isTagBrowsing(getContext())) {
						id = entry.getAlbumId();
					} else {
						id = entry.getParent();
					}

					String artistDisplay;
					if(entry.getArtist() == null) {
						if(entry.getAlbum() != null) {
							artistDisplay = entry.getAlbumDisplay();
						} else {
							artistDisplay = "";
						}
					} else if(entry.getAlbum() != null) {
						artistDisplay = entry.getArtist() + " - " + entry.getAlbumDisplay();
					} else {
						artistDisplay = entry.getArtist();
					}

					cursor.addRow(new Object[]{entry.getId().hashCode(), entry.getTitle(), artistDisplay, "so-" + id, entry.getTitle(), icon});
				}
			}
		}
		return cursor;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) {
		return null;
	}

	@Override
	public int delete(Uri uri, String s, String[] strings) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
		return 0;
	}

}
