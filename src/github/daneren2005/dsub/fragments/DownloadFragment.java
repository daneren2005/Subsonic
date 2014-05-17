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

package github.daneren2005.dsub.fragments;

import android.content.DialogInterface;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.DownloadFileAdapter;

import static github.daneren2005.dsub.domain.MusicDirectory.Entry;

public class DownloadFragment extends SelectListFragment<DownloadFile> {
	private long currentRevision;
	private ScheduledExecutorService executorService;

	public DownloadFragment() {
		serialize = false;
	}

	@Override
	public void onResume() {
		super.onResume();

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		};

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onPause() {
		super.onPause();
		executorService.shutdown();
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.downloading;
	}

	@Override
	public ArrayAdapter getAdapter(List<DownloadFile> objs) {
		return new DownloadFileAdapter(context, objs);
	}

	@Override
	public List<DownloadFile> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		DownloadService downloadService = getDownloadService();
		if(downloadService == null) {
			return new ArrayList<DownloadFile>();
		}

		listView.setOnScrollListener(null);
		refreshLayout.setEnabled(false);

		List<DownloadFile> songList = new ArrayList<DownloadFile>();
		songList.addAll(downloadService.getBackgroundDownloads());
		currentRevision = downloadService.getDownloadListUpdateRevision();
		return songList;
	}

	@Override
	public int getTitleResource() {
		return R.string.button_bar_downloading;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if(super.onOptionsItemSelected(menuItem)) {
			return true;
		}

		switch (menuItem.getItemId()) {
			case R.id.menu_remove_all:
				Util.confirmDialog(context, R.string.download_menu_remove_all, "", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new SilentBackgroundTask<Void>(context) {
							@Override
							protected Void doInBackground() throws Throwable {
								getDownloadService().clearBackground();
								return null;
							}

							@Override
							protected void done(Void result) {
								update();
							}
						}.execute();
					}
				});
				return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Object selectedItem = ((DownloadFile) listView.getItemAtPosition(info.position)).getSong();
		onCreateContextMenu(menu, view, menuInfo, selectedItem);
		if(selectedItem instanceof MusicDirectory.Entry && !((MusicDirectory.Entry) selectedItem).isVideo() && !Util.isOffline(context)) {
			menu.removeItem(R.id.song_menu_remove_playlist);
		}

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Object selectedItem = ((DownloadFile) listView.getItemAtPosition(info.position)).getSong();

		if(onContextItemSelected(menuItem, selectedItem)) {
			return true;
		}

		return true;
	}

	private void update() {
		DownloadService downloadService = getDownloadService();
		if (downloadService == null) {
			return;
		}

		if (currentRevision != downloadService.getDownloadListUpdateRevision()) {
			List<DownloadFile> downloadFileList = downloadService.getBackgroundDownloads();
			objects.clear();
			objects.addAll(downloadFileList);
			adapter.notifyDataSetChanged();

			currentRevision = downloadService.getDownloadListUpdateRevision();
		}
	}
}
