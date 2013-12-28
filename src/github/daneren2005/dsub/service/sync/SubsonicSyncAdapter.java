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

package github.daneren2005.dsub.service.sync;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.BatteryManager;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.CachedMusicService;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.RESTMusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 9/6/13.
 */

public class SubsonicSyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = SubsonicSyncAdapter.class.getSimpleName();
	protected CachedMusicService musicService = new CachedMusicService(new RESTMusicService());
	private Context context;

	public SubsonicSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
	}
	@TargetApi(14)
	public SubsonicSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		this.context = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		
		// Don't try to sync if no network!
		if(networkInfo == null || !networkInfo.isConnected() || Util.isOffline(context)) {
			Log.w(TAG, "Not running sync, not connected to network");
			return;
		}
		
		// Make sure battery > x% or is charging
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, intentFilter);
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		if(status != BatteryManager.BATTERY_STATUS_CHARGING && status != BatteryManager.BATTERY_STATUS_FULL) {
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			
			if((level / (float)scale) < 0.15) {
				Log.w(TAG, "Not running sync, battery too low");
				return;
			}
		}

		// Check if user wants to only sync on wifi
		SharedPreferences prefs = Util.getPreferences(context);
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_SYNC_WIFI, true)) {
			if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				executeSync(context);
			} else {
				Log.w(TAG, "Not running sync, not connected to wifi");
			}
		} else {
			executeSync(context);
		}
	}
	
	private void executeSync(Context context) {
		String className = this.getClass().getSimpleName();
		Log.i(TAG, "Running sync for " + className);
		long start = System.currentTimeMillis();
		int servers = Util.getServerCount(context);
		for(int i = 1; i <= servers; i++) {
			try {
				musicService.setInstance(i);
				onExecuteSync(context, i);
			} catch(Exception e) {
				Log.e(TAG, "Failed sync for " + className + "(" + i + ")", e);
			}
		}
		
		Log.i(TAG, className + " executed in " + (System.currentTimeMillis() - start) + " ms");
	}
	public void onExecuteSync(Context context, int instance) {
	
	}
	
	protected boolean downloadRecursively(List<String> paths, MusicDirectory parent, Context context, boolean save) throws Exception {
		boolean downloaded = false;
		for (MusicDirectory.Entry song: parent.getChildren(false, true)) {
			if (!song.isVideo()) {
				DownloadFile file = new DownloadFile(context, song, save);
				while(!(save && file.isSaved() || !save && file.isCompleteFileAvailable()) && !file.isFailedMax()) {
					file.downloadNow(musicService);
					downloaded = true;
				}

				if(paths != null && file.isCompleteFileAvailable()) {
					paths.add(file.getCompleteFile().getPath());
				}
			}
		}
		
		for (MusicDirectory.Entry dir: parent.getChildren(true, false)) {
			if(downloadRecursively(paths, musicService.getMusicDirectory(dir.getId(), dir.getTitle(), true, context, null), context, save)) {
				downloaded = true;
			}
		}

		return downloaded;
	}
}
