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

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.MenuUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.BookmarkAdapter;
import github.daneren2005.dsub.view.UpdateView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectBookmarkFragment extends SelectRecyclerFragment<MusicDirectory.Entry> {
	private static final String TAG = SelectBookmarkFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry item) {
		menuInflater.inflate(R.menu.select_bookmark_context, menu);
		MenuUtil.hideMenuItems(context, menu, updateView);
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
	public void onItemClicked(UpdateView<MusicDirectory.Entry> updateView, final MusicDirectory.Entry bookmark) {
		final DownloadService downloadService = getDownloadService();
		if(downloadService == null) {
			return;
		}

		boolean allowPlayAll = ((!Util.isTagBrowsing(context) && bookmark.getParent() != null) || (Util.isTagBrowsing(context) && bookmark.getAlbumId() != null)) && !bookmark.isPodcast();
		if(allowPlayAll && "all".equals(Util.getSongPressAction(context))) {
			new RecursiveLoader(context) {
				@Override
				protected Boolean doInBackground() throws Throwable {
					getSiblingsRecursively(bookmark);

					if(songs.isEmpty() || !songs.contains(bookmark)) {
						playNowInTask(Arrays.asList(bookmark), bookmark, bookmark.getBookmark().getPosition());
					} else {
						playNowInTask(songs, bookmark, bookmark.getBookmark().getPosition());
					}
					return null;
				}

				@Override
				protected void done(Boolean result) {
					context.openNowPlaying();
				}
			}.execute();
		} else {
			onSongPress(Arrays.asList(bookmark), bookmark, bookmark.getBookmark().getPosition(), false);
		}
	}

	private void displayBookmarkInfo(final MusicDirectory.Entry entry) {
		Bookmark bookmark = entry.getBookmark();
		List<Integer> headers = new ArrayList<>();
		List<String> details = new ArrayList<>();

		headers.add(R.string.details_song);
		details.add(entry.getTitle());

		if(entry.getArtist() != null) {
			headers.add(R.string.details_artist);
			details.add(entry.getArtist());
		}
		if(entry.getAlbum() != null) {
			headers.add(R.string.details_album);
			details.add(entry.getAlbum());
		}

		headers.add(R.string.details_position);
		details.add(Util.formatDuration(bookmark.getPosition() / 1000));

		headers.add(R.string.details_created);
		details.add(Util.formatDate(bookmark.getCreated()));

		headers.add(R.string.details_updated);
		details.add(Util.formatDate(bookmark.getChanged()));

		if(bookmark.getComment() != null) {
			headers.add(R.string.details_comments);
			details.add(bookmark.getComment());
		}

		Util.showDetailsDialog(context, R.string.bookmark_details_title, headers, details);
	}
}
