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
import android.util.Log;
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
import java.util.List;

public class SelectBookmarkFragment extends SelectListFragment<Bookmark> {
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
		Bookmark bookmark = objects.get(info.position);
		
		switch(menuItem.getItemId()) {
			case R.id.bookmark_menu_info:
				displayBookmarkInfo(bookmark);
				break;
			case R.id.bookmark_menu_delete:
				deleteBookmark(bookmark);
				break;
		}
		
		if(onContextItemSelected(menuItem, bookmark.getEntry())) {
			return true;
		}

		return true;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public ArrayAdapter getAdapter(List<Bookmark> bookmarks) {
		return new BookmarkAdapter(context, bookmarks);
	}

	@Override
	public List<Bookmark> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return musicService.getBookmarks(refresh, context, listener);
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

		final Bookmark bookmark = (Bookmark) parent.getItemAtPosition(position);
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				downloadService.download(bookmark);
				return null;
			}
			
			@Override
			protected void done(Void result) {
				Util.startActivityWithoutTransition(context, DownloadActivity.class);
			}
		}.execute();
	}
	
	private void displayBookmarkInfo(final Bookmark bookmark) {
		Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String comment = bookmark.getComment();
		if(comment == null) {
			comment = "";
		}

		String msg = context.getResources().getString(R.string.bookmark_details,
			bookmark.getEntry().getTitle(), Util.formatDuration(bookmark.getPosition() / 1000),
			formatter.format(bookmark.getCreated()), formatter.format(bookmark.getChanged()), comment);
		
		Util.info(context, R.string.bookmark_details_title, msg, false);
	}
	private void deleteBookmark(final Bookmark bookmark) {
		final MusicDirectory.Entry entry = bookmark.getEntry();
		Util.confirmDialog(context, R.string.bookmark_delete_title, entry.getTitle(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(context, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deleteBookmark(entry.getId(), context, null);
						return null;
					}
					
					@Override
					protected void done(Void result) {
						adapter.remove(bookmark);
						adapter.notifyDataSetChanged();
						Util.toast(context, context.getResources().getString(R.string.bookmark_deleted, entry.getTitle()));
					}
					
					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.bookmark_deleted_error, entry.getTitle()) + " " + getErrorMessage(error);
						}
						
						Util.toast(context, msg, false);
					}
				}.execute();
			}
		});
	}
}
