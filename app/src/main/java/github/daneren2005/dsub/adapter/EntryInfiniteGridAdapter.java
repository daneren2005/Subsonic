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

package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.view.UpdateView;

public class EntryInfiniteGridAdapter extends EntryGridAdapter {
	public static int VIEW_TYPE_LOADING = 4;

	private String type;
	private String extra;
	private int size;

	private boolean loading = false;
	private boolean allLoaded = false;

	public EntryInfiniteGridAdapter(Context context, List<Entry> entries, ImageLoader imageLoader, boolean largeCell) {
		super(context, entries, imageLoader, largeCell);
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if(viewType == VIEW_TYPE_LOADING) {
			View progress = LayoutInflater.from(context).inflate(R.layout.tab_progress, null);
			progress.setVisibility(View.VISIBLE);
			return new UpdateView.UpdateViewHolder(progress, false);
		}

		return super.onCreateViewHolder(parent, viewType);
	}

	@Override
	public int getItemViewType(int position) {
		if(isLoadingView(position)) {
			return VIEW_TYPE_LOADING;
		}

		return super.getItemViewType(position);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, int position) {
		if(!isLoadingView(position)) {
			super.onBindViewHolder(holder, position);
		}
	}

	@Override
	public int getItemCount() {
		int size = super.getItemCount();

		if(!allLoaded) {
			size++;
		}

		return size;
	}

	public void setData(String type, String extra, int size) {
		this.type = type;
		this.extra = extra;
		this.size = size;

		if(super.getItemCount() < size) {
			allLoaded = true;
		}
	}

	public void loadMore() {
		if(loading || allLoaded) {
			return;
		}
		loading = true;

		new SilentBackgroundTask<Void>(context) {
			private List<Entry> newData;

			@Override
			protected Void doInBackground() throws Throwable {
				newData = cacheInBackground();
				return null;
			}

			@Override
			protected void done(Void result) {
				appendCachedData(newData);
				loading = false;

				if(newData.size() < size) {
					allLoaded = true;
					notifyDataSetChanged();
				}
			}
		}.execute();
	}

	protected List<Entry> cacheInBackground() throws Exception {
		MusicService service = MusicServiceFactory.getMusicService(context);
		MusicDirectory result;
		int offset = sections.get(0).size();
		if(("genres".equals(type) && ServerInfo.checkServerVersion(context, "1.10.0")) || "years".equals(type)) {
			result = service.getAlbumList(type, extra, size, offset, false, context, null);
		} else if("genres".equals(type) || "genres-songs".equals(type)) {
			result = service.getSongsByGenre(extra, size, offset, context, null);
		}else if(type.indexOf(MainFragment.SONGS_LIST_PREFIX) != -1) {
			result = service.getSongList(type, size, offset, context, null);
		} else {
			result = service.getAlbumList(type, size, offset, false, context, null);
		}
		return result.getChildren();
	}

	protected void appendCachedData(List<Entry> newData) {
		if(newData.size() > 0) {
			int start = sections.get(0).size();
			sections.get(0).addAll(newData);
			this.notifyItemRangeInserted(start, newData.size());
		}
	}

	protected boolean isLoadingView(int position) {
		return !allLoaded && position >= sections.get(0).size();
	}
}
