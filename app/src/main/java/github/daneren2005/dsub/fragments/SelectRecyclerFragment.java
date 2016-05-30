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
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.fragments;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.view.FastScroller;

public abstract class SelectRecyclerFragment<T> extends SubsonicFragment implements SectionAdapter.OnItemClickedListener<T> {
	private static final String TAG = SelectRecyclerFragment.class.getSimpleName();
	protected RecyclerView recyclerView;
	protected FastScroller fastScroller;
	protected SectionAdapter<T> adapter;
	protected UpdateTask currentTask;
	protected List<T> objects;
	protected boolean serialize = true;
	protected boolean largeAlbums = false;
	protected boolean pullToRefresh = true;
	protected boolean backgroundUpdate = true;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null && serialize) {
			objects = (List<T>) bundle.getSerializable(Constants.FRAGMENT_LIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(serialize) {
			outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) objects);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_recycler_fragment, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(this);

		recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_recycler);
		fastScroller = (FastScroller) rootView.findViewById(R.id.fragment_fast_scroller);
		setupLayoutManager();

		if(pullToRefresh) {
			setupScrollList(recyclerView);
		} else {
			refreshLayout.setEnabled(false);
		}

		if(objects == null) {
			refresh(false);
		} else {
			recyclerView.setAdapter(adapter = getAdapter(objects));
		}

		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(!primaryFragment) {
			return;
		}

		menuInflater.inflate(getOptionsMenu(), menu);
		onFinishSetupOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void setIsOnlyVisible(boolean isOnlyVisible) {
		boolean update = this.isOnlyVisible != isOnlyVisible;
		super.setIsOnlyVisible(isOnlyVisible);
		if(update && adapter != null) {
			RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
			if(layoutManager instanceof GridLayoutManager) {
				((GridLayoutManager) layoutManager).setSpanCount(getRecyclerColumnCount());
			}
		}
	}

	@Override
	protected void refresh(final boolean refresh) {
		int titleRes = getTitleResource();
		if(titleRes != 0) {
			setTitle(getTitleResource());
		}
		if(backgroundUpdate) {
			recyclerView.setVisibility(View.GONE);
		}
		
		// Cancel current running task before starting another one
		if(currentTask != null) {
			currentTask.cancel();
		}

		currentTask = new UpdateTask(this, refresh);

		if(backgroundUpdate) {
			currentTask.execute();
		} else {
			objects = new ArrayList<T>();

			try {
				objects = getObjects(null, refresh, null);
			} catch (Exception x) {
				Log.e(TAG, "Failed to load", x);
			}

			currentTask.done(objects);
		}
	}

	public SectionAdapter getCurrentAdapter() {
		return adapter;
	}

	private void setupLayoutManager() {
		setupLayoutManager(recyclerView, largeAlbums);
	}

	public abstract int getOptionsMenu();
	public abstract SectionAdapter<T> getAdapter(List<T> objs);
	public abstract List<T> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception;
	public abstract int getTitleResource();
	
	public void onFinishRefresh() {
		
	}

	private class UpdateTask extends TabBackgroundTask<List<T>> {
		private boolean refresh;

		public UpdateTask(SubsonicFragment fragment, boolean refresh) {
			super(fragment);
			this.refresh = refresh;
		}

		@Override
		public List<T> doInBackground() throws Exception {
			MusicService musicService = MusicServiceFactory.getMusicService(context);

			objects = new ArrayList<T>();

			try {
				objects = getObjects(musicService, refresh, this);
			} catch (Exception x) {
				Log.e(TAG, "Failed to load", x);
			}

			return objects;
		}

		@Override
		public void done(List<T> result) {
			if (result != null && !result.isEmpty()) {
				recyclerView.setAdapter(adapter = getAdapter(result));
				if(!fastScroller.isAttached()) {
					fastScroller.attachRecyclerView(recyclerView);
				}

				onFinishRefresh();
				recyclerView.setVisibility(View.VISIBLE);
			} else {
				setEmpty(true);
			}

			currentTask = null;
		}
	}
}
