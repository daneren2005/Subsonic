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
package github.daneren2005.dsub.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.FileUtil;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class ShufflePlayBuffer {

	private static final String TAG = ShufflePlayBuffer.class.getSimpleName();
	private static final String CACHE_FILENAME = "shuffleBuffer.ser";

	private ScheduledExecutorService executorService;
	private Runnable runnable;
	private boolean firstRun = true;
	private final ArrayList<MusicDirectory.Entry> buffer = new ArrayList<MusicDirectory.Entry>();
	private int lastCount = -1;
	private DownloadService context;
	private boolean awaitingResults = false;
	private int capacity;
	private int refillThreshold;

	private SharedPreferences.OnSharedPreferenceChangeListener listener;
	private int currentServer;
	private String currentFolder = "";
	private String genre = "";
	private String startYear = "";
	private String endYear = "";

	public ShufflePlayBuffer(DownloadService context) {
		this.context = context;

		executorService = Executors.newSingleThreadScheduledExecutor();
		runnable = new Runnable() {
			@Override
			public void run() {
				refill();
			}
		};
		executorService.scheduleWithFixedDelay(runnable, 1, 10, TimeUnit.SECONDS);
		
		// Calculate out the capacity and refill threshold based on the user's random size preference
		int shuffleListSize = Math.max(1, Integer.parseInt(Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_RANDOM_SIZE, "20")));
		// ex: default 20 -> 50
		capacity = shuffleListSize * 5 / 2;
		capacity = Math.min(500, capacity);
		
		// ex: default 20 -> 40
		refillThreshold = capacity * 4 / 5;
	}

	public List<MusicDirectory.Entry> get(int size) {
		clearBufferIfnecessary();
		// Make sure fetcher is running if needed
		restart();

		List<MusicDirectory.Entry> result = new ArrayList<MusicDirectory.Entry>(size);
		synchronized (buffer) {
			boolean removed = false;
			while (!buffer.isEmpty() && result.size() < size) {
				result.add(buffer.remove(buffer.size() - 1));
				removed = true;
			}

			// Re-cache if anything is taken out
			if(removed) {
				FileUtil.serialize(context, buffer, CACHE_FILENAME);
			}
		}
		Log.i(TAG, "Taking " + result.size() + " songs from shuffle play buffer. " + buffer.size() + " remaining.");
		if(result.isEmpty()) {
			awaitingResults = true;
		}
		return result;
	}

	public void shutdown() {
		executorService.shutdown();
		Util.getPreferences(context).unregisterOnSharedPreferenceChangeListener(listener);
	}

	private void restart() {
		synchronized(buffer) {
			if(buffer.size() <= refillThreshold && lastCount != 0 && executorService.isShutdown()) {
				executorService = Executors.newSingleThreadScheduledExecutor();
				executorService.scheduleWithFixedDelay(runnable, 0, 10, TimeUnit.SECONDS);
			}
		}
	}

	private void refill() {
		// Check if active server has changed.
		clearBufferIfnecessary();

		if (buffer != null && (buffer.size() > refillThreshold || (!Util.isNetworkConnected(context) && !Util.isOffline(context)) || lastCount == 0)) {
			executorService.shutdown();
			return;
		}

		try {
			MusicService service = MusicServiceFactory.getMusicService(context);
			
			// Get capacity based 
			int n = capacity - buffer.size();
			String folder = null;
			if(!Util.isTagBrowsing(context)) {
				folder = Util.getSelectedMusicFolderId(context);
			}
			MusicDirectory songs = service.getRandomSongs(n, folder, genre, startYear, endYear, context, null);

			synchronized (buffer) {
				lastCount = 0;
				for(MusicDirectory.Entry entry: songs.getChildren()) {
					if(!buffer.contains(entry) && entry.getRating() != 1) {
						buffer.add(entry);
						lastCount++;
					}
				}
				Log.i(TAG, "Refilled shuffle play buffer with " + lastCount + " songs.");

				// Cache buffer
				FileUtil.serialize(context, buffer, CACHE_FILENAME);
			}
		} catch (Exception x) {
			// Give it one more try before quitting
			if(lastCount != -2) {
				lastCount = -2;
			} else if(lastCount == -2) {
				lastCount = 0;
			}
			Log.w(TAG, "Failed to refill shuffle play buffer.", x);
		}
		
		if(awaitingResults) {
			awaitingResults = false;
			context.checkDownloads();
		}
	}

	private void clearBufferIfnecessary() {
		synchronized (buffer) {
			final SharedPreferences prefs = Util.getPreferences(context);
			if (currentServer != Util.getActiveServer(context)
					|| !Util.equals(currentFolder, Util.getSelectedMusicFolderId(context))
					|| (genre != null && !genre.equals(prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, "")))
					|| (startYear != null && !startYear.equals(prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, "")))
					|| (endYear != null && !endYear.equals(prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, "")))) {
				lastCount = -1;
				currentServer = Util.getActiveServer(context);
				currentFolder = Util.getSelectedMusicFolderId(context);
				genre = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, "");
				startYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, "");
				endYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, "");
				buffer.clear();

				if(firstRun) {
					ArrayList cacheList = FileUtil.deserialize(context, CACHE_FILENAME, ArrayList.class);
					if(cacheList != null) {
						buffer.addAll(cacheList);
					}

					listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
						@Override
						public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
							clearBufferIfnecessary();
							restart();
						}
					};
					prefs.registerOnSharedPreferenceChangeListener(listener);
					firstRun = false;
				} else {
					// Clear cache
					File file = new File(context.getCacheDir(), CACHE_FILENAME);
					file.delete();
				}
			}
		}
	}
}
