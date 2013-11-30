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
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.BookmarkAdapter;
import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SelectBookmarkFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectBookmarkFragment.class.getSimpleName();
	private ListView bookmarkListView;
	private View emptyView;
	private List<Bookmark> bookmarks;
	private BookmarkAdapter bookmarkAdapter;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	
		if(bundle != null) {
			bookmarks = (List<Bookmark>) bundle.getSerializable(Constants.FRAGMENT_LIST);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) bookmarks);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);
		
		bookmarkListView = (ListView)rootView.findViewById(R.id.fragment_list);
		bookmarkListView.setOnItemClickListener(this);
		registerForContextMenu(bookmarkListView);
		emptyView = rootView.findViewById(R.id.fragment_list_empty);
		
		if(bookmarks == null) {
			refresh();
		} else {
			bookmarkListView.setAdapter(new BookmarkAdapter(context, bookmarks));
		}
		
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.abstract_top_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}
		
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();
		inflater.inflate(R.menu.select_bookmark_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Bookmark bookmark = bookmarks.get(info.position);
		
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
	protected void refresh(final boolean refresh) {
		setTitle(R.string.button_bar_bookmarks);
		bookmarkListView.setVisibility(View.INVISIBLE);
		
		BackgroundTask<List<Bookmark>> task = new TabBackgroundTask<List<Bookmark>>(this) {
			@Override
			protected List<Bookmark> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				
				bookmarks = new ArrayList<Bookmark>();
				
				try {
					bookmarks = musicService.getBookmarks(refresh, context, this);
				} catch (Exception x) {
					Log.e(TAG, "Failed to load bookmarks", x);
				}
				
				return bookmarks;
			}
		
			@Override
			protected void done(List<Bookmark> result) {
				emptyView.setVisibility(result == null || result.isEmpty() ? View.VISIBLE : View.GONE);
				
				if (result != null) {
					bookmarkListView.setAdapter(bookmarkAdapter = new BookmarkAdapter(context, result));
					bookmarkListView.setVisibility(View.VISIBLE);
				}
			}
		};
		task.execute();
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
						bookmarkAdapter.remove(bookmark);
						bookmarkAdapter.notifyDataSetChanged();
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
