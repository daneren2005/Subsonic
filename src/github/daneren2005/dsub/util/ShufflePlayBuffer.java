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
    private static final int CAPACITY = 50;
    private static final int REFILL_THRESHOLD = 40;

    private final ScheduledExecutorService executorService;
	private boolean firstRun = true;
    private final ArrayList<MusicDirectory.Entry> buffer = new ArrayList<MusicDirectory.Entry>();
	private int lastCount = -1;
    private Context context;
    private int currentServer;
	private String currentFolder = "";
	
	private String genre = "";
	private String startYear = "";
	private String endYear = "";

    public ShufflePlayBuffer(Context context) {
        this.context = context;
        
        executorService = Executors.newSingleThreadScheduledExecutor();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
				refill();
			}
        };
        executorService.scheduleWithFixedDelay(runnable, 1, 10, TimeUnit.SECONDS);
    }

    public List<MusicDirectory.Entry> get(int size) {
        clearBufferIfnecessary();

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
        return result;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private void refill() {

        // Check if active server has changed.
        clearBufferIfnecessary();

        if (buffer != null && (buffer.size() > REFILL_THRESHOLD || (!Util.isNetworkConnected(context) && !Util.isOffline(context)) || lastCount == 0)) {
            return;
        }

        try {
            MusicService service = MusicServiceFactory.getMusicService(context);
            int n = CAPACITY - buffer.size();
			String folder = Util.getSelectedMusicFolderId(context);
            MusicDirectory songs = service.getRandomSongs(n, folder, genre, startYear, endYear, context, null);

            synchronized (buffer) {
                buffer.addAll(songs.getChildren());
                Log.i(TAG, "Refilled shuffle play buffer with " + songs.getChildrenSize() + " songs.");
				lastCount = songs.getChildrenSize();
				
				// Cache buffer
				FileUtil.serialize(context, buffer, CACHE_FILENAME);
            }
        } catch (Exception x) {
            Log.w(TAG, "Failed to refill shuffle play buffer.", x);
        }
    }

    private void clearBufferIfnecessary() {
        synchronized (buffer) {
			final SharedPreferences prefs = Util.getPreferences(context);
            if (currentServer != Util.getActiveServer(context)
				|| (currentFolder != null && !currentFolder.equals(Util.getSelectedMusicFolderId(context)))
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
