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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.view.BookmarkAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectBookmarkFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectBookmarkFragment.class.getSimpleName();
	private ListView bookmarkListView;
	private View emptyView;
	private List<Bookmark> bookmarks;
	
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
					bookmarkListView.setAdapter(new BookmarkAdapter(context, result));
					bookmarkListView.setVisibility(View.VISIBLE);
				}
			}
		};
		task.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}
}
