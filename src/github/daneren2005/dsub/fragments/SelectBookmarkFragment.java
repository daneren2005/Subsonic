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
	
	Copyright 2010 (C) Sindre Mehus
*/
package github.daneren2005.dsub.fragments;

import android.content.DialogInterface;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.BookmarkAdapter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class SelectBookmarkFragment extends SelectListFragment<MusicDirectory.Entry> {
	private static final String TAG = SelectBookmarkFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();
		inflater.inflate(R.menu.select_bookmark_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		MusicDirectory.Entry bookmark = objects.get(info.position);
		
		switch(menuItem.getItemId()) {
			case R.id.bookmark_menu_info:
				displayBookmarkInfo(bookmark);
				return true;
			case R.id.bookmark_menu_delete:
				deleteBookmark(bookmark, adapter);
				return true;
		}
		
		if(onContextItemSelected(menuItem, bookmark)) {
			return true;
		}

		return true;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public ArrayAdapter getAdapter(List<MusicDirectory.Entry> bookmarks) {
		return new BookmarkAdapter(context, bookmarks);
	}

	@Override
	public List<MusicDirectory.Entry> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return musicService.getBookmarks(refresh, context, listener).getChildren();
	}

	@Override
	public int getTitleResource() {
		return R.string.button_bar_bookmarks;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final DownloadService downloadService = getDownloadService();
		if(downloadService == null) {
			return;
		}

		final MusicDirectory.Entry bookmark = (MusicDirectory.Entry) parent.getItemAtPosition(position);
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				downloadService.download(Arrays.asList(bookmark), false, true, false, false, 0, bookmark.getBookmark().getPosition());
				return null;
			}
			
			@Override
			protected void done(Void result) {
				Util.startActivityWithoutTransition(context, DownloadActivity.class);
			}
		}.execute();
	}
	
	private void displayBookmarkInfo(final MusicDirectory.Entry entry) {
		Bookmark bookmark = entry.getBookmark();
		Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String comment = bookmark.getComment();
		if(comment == null) {
			comment = "";
		}

		String msg = context.getResources().getString(R.string.bookmark_details,
			entry.getTitle(), Util.formatDuration(bookmark.getPosition() / 1000),
			formatter.format(bookmark.getCreated()), formatter.format(bookmark.getChanged()), comment);
		
		Util.info(context, R.string.bookmark_details_title, msg, false);
	}
}
