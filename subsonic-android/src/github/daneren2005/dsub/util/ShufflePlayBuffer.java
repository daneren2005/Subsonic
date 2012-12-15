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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class ShufflePlayBuffer {

    private static final String TAG = ShufflePlayBuffer.class.getSimpleName();
    private static final int CAPACITY = 50;
    private static final int REFILL_THRESHOLD = 40;

    private final ScheduledExecutorService executorService;
    private final List<MusicDirectory.Entry> buffer = new ArrayList<MusicDirectory.Entry>();
    private Context context;
    private Context appContext;
    private int currentServer;
	private String currentFolder;
	private String genre;
	private String startYear;
	private String endYear;
	private SharedPreferences prefs;

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
        prefs = Util.getPreferences(context.getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (key.equals(Constants.PREFERENCES_BUILD_RANDOM_FROM_STAR)){
					synchronized (buffer) {
						buffer.clear();
					}
				}
				
			}
		});
    }

    public List<MusicDirectory.Entry> get(int size) {
        clearBufferIfnecessary();

        List<MusicDirectory.Entry> result = new ArrayList<MusicDirectory.Entry>(size);
        synchronized (buffer) {
            while (!buffer.isEmpty() && result.size() < size) {
                result.add(buffer.remove(buffer.size() - 1));
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

        if (buffer.size() > REFILL_THRESHOLD || (!Util.isNetworkConnected(context) && !Util.isOffline(context))) {
            return;
        }

        try {
        	
            MusicService service = MusicServiceFactory.getMusicService(context);
            
      
            if (prefs.getBoolean(Constants.PREFERENCES_BUILD_RANDOM_FROM_STAR, false)){
            	
            	MusicDirectory songs = service.getStarredList(context, new ProgressListener() {
     				@Override
     				public void updateProgress(int messageId) {
     					// TODO Auto-generated method stub	
     				}
     				@Override
     				public void updateProgress(String message) {
     					// TODO Auto-generated method stub
     				}
     				});
            	 
                 //Only get music files
            	 List<Entry> starlist = songs.getChildren(false, true);
                 //shuffle the list
                 Collections.shuffle(starlist);
                 
                 synchronized (buffer) {
                    int i;
     				for (i=1;i<=200;i++){
                     	buffer.add(starlist.get(i));
                     	
                     }
                     Log.i(TAG, "Refilled shuffle play buffer with 200 starred songs.");
                 }
            	
            	
            }else{
            	
            	int n = CAPACITY - buffer.size();
    			String folder = Util.getSelectedMusicFolderId(context);
                MusicDirectory songs = service.getRandomSongs(n, folder, genre, startYear, endYear, context, null);
                
                synchronized (buffer) {
                    buffer.addAll(songs.getChildren());
                    Log.i(TAG, "Refilled shuffle play buffer with " + songs.getChildren().size() + " songs.");
                }
            }
        } catch (Exception x) {
            Log.w(TAG, "Failed to refill shuffle play buffer.", x);
        }
    }

	private void clearBufferIfnecessary() {
        synchronized (buffer) {
        	
        	if (currentServer != Util.getActiveServer(context) || currentFolder != Util.getSelectedMusicFolderId(context)) {
                currentServer = Util.getActiveServer(context);
				currentFolder = Util.getSelectedMusicFolderId(context);
                buffer.clear();
            }
        }
    }
	
	public void setOptions(String genre, String startYear, String endYear) {
		this.genre = genre;
		this.startYear = startYear;
		this.endYear = endYear;
		
		synchronized (buffer) {
			buffer.clear();
		}
	}

}
