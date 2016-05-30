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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

public class ArtistRadioBuffer {
	private static final String TAG = ArtistRadioBuffer.class.getSimpleName();

	private ScheduledExecutorService executorService;
	private Runnable runnable;
	private final ArrayList<MusicDirectory.Entry> buffer = new ArrayList<MusicDirectory.Entry>();
	private int lastCount = -1;
	private DownloadService context;
	private boolean awaitingResults = false;
	private int capacity;
	private int refillThreshold;

	private String artistId;

	public ArtistRadioBuffer(DownloadService context) {
		this.context = context;
		runnable = new Runnable() {
			@Override
			public void run() {
				refill();
			}
		};
		
		// Calculate out the capacity and refill threshold based on the user's random size preference
		int shuffleListSize = Math.max(1, Integer.parseInt(Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_RANDOM_SIZE, "20")));
		// ex: default 20 -> 50
		capacity = shuffleListSize * 5 / 2;
		capacity = Math.min(500, capacity);
		
		// ex: default 20 -> 40
		refillThreshold = capacity * 4 / 5;
	}

	public void setArtist(String artistId) {
		if(!Util.equals(this.artistId, artistId)) {
			buffer.clear();
		}

		this.artistId = artistId;
		awaitingResults = true;
		refill();
	}
	public void restoreArtist(String artistId) {
		this.artistId = artistId;
		awaitingResults = false;
		restart();
	}

	public List<MusicDirectory.Entry> get(int size) {
		// Make sure fetcher is running if needed
		restart();

		List<MusicDirectory.Entry> result = new ArrayList<MusicDirectory.Entry>(size);
		synchronized (buffer) {
			while (!buffer.isEmpty() && result.size() < size) {
				result.add(buffer.remove(buffer.size() - 1));
			}
		}
		Log.i(TAG, "Taking " + result.size() + " songs from artist radio buffer. " + buffer.size() + " remaining.");
		if(result.isEmpty()) {
			awaitingResults = true;
		}
		return result;
	}

	public void shutdown() {
		executorService.shutdown();
	}

	private void restart() {
		synchronized(buffer) {
			if(buffer.size() <= refillThreshold && lastCount != 0 && (executorService == null || executorService.isShutdown())) {
				executorService = Executors.newSingleThreadScheduledExecutor();
				executorService.scheduleWithFixedDelay(runnable, 0, 10, TimeUnit.SECONDS);
			}
		}
	}

	private void refill() {
		if (buffer != null && executorService != null && (buffer.size() > refillThreshold || (!Util.isNetworkConnected(context) && !Util.isOffline(context)) || lastCount == 0)) {
			executorService.shutdown();
			return;
		}

		try {
			MusicService service = MusicServiceFactory.getMusicService(context);
			
			// Get capacity based 
			int n = capacity - buffer.size();
			MusicDirectory songs = service.getRandomSongs(n, artistId, context, null);

			synchronized (buffer) {
				lastCount = 0;
				for(MusicDirectory.Entry entry: songs.getChildren()) {
					if(!buffer.contains(entry) && entry.getRating() != 1) {
						buffer.add(entry);
						lastCount++;
					}
				}
				Log.i(TAG, "Refilled artist radio buffer with " + lastCount + " songs.");
			}
		} catch (Exception x) {
			// Give it one more try before quitting
			if(lastCount != -2) {
				lastCount = -2;
			} else if(lastCount == -2) {
				lastCount = 0;
			}
			Log.w(TAG, "Failed to refill artist radio buffer.", x);
		}
		
		if(awaitingResults) {
			awaitingResults = false;
			context.checkDownloads();
		}
	}
}
