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

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.MenuUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.BookmarkAdapter;
import github.daneren2005.dsub.view.UpdateView;

import java.util.Arrays;
import java.util.List;

public class SelectBookmarkFragment extends SelectRecyclerFragment<MusicDirectory.Entry> {
	private static final String TAG = SelectBookmarkFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry item) {
		menuInflater.inflate(R.menu.select_bookmark_context, menu);
		MenuUtil.hideMenuItems(context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry bookmark) {
		switch(menuItem.getItemId()) {
			case R.id.bookmark_menu_info:
				displayBookmarkInfo(bookmark);
				return true;
			case R.id.bookmark_menu_delete:
				deleteBookmark(bookmark, adapter);
				return true;
		}

		return onContextItemSelected(menuItem, bookmark);
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public SectionAdapter getAdapter(List<MusicDirectory.Entry> bookmarks) {
		return new BookmarkAdapter(context, bookmarks, this);
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
	public void onItemClicked(final MusicDirectory.Entry bookmark) {
		final DownloadService downloadService = getDownloadService();
		if(downloadService == null) {
			return;
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				downloadService.clear();
				downloadService.download(Arrays.asList(bookmark), false, true, false, false, 0, bookmark.getBookmark().getPosition());
				return null;
			}
			
			@Override
			protected void done(Void result) {
				context.openNowPlaying();
			}
		}.execute();
	}

	private void displayBookmarkInfo(final MusicDirectory.Entry entry) {
		Bookmark bookmark = entry.getBookmark();
		String comment = bookmark.getComment();
		if(comment == null) {
			comment = "";
		}

		String msg = context.getResources().getString(R.string.bookmark_details,
			entry.getTitle(), Util.formatDuration(bookmark.getPosition() / 1000),
			Util.formatDate(bookmark.getCreated()), Util.formatDate(bookmark.getChanged()), comment);
		
		Util.info(context, R.string.bookmark_details_title, msg, false);
	}
}
