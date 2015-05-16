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

import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.SilentBackgroundTask;

public class EntryInfiniteGridAdapter extends EntryGridAdapter {
	private String type;
	private String extra;
	private int size;

	private boolean loading = false;
	private boolean allLoaded = false;

	public EntryInfiniteGridAdapter(Context context, List<Entry> entries, ImageLoader imageLoader, boolean largeCell) {
		super(context, entries, imageLoader, largeCell);
	}

	public void setData(String type, String extra, int size) {
		this.type = type;
		this.extra = extra;
		this.size = size;
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
				if(newData.isEmpty()) {
					allLoaded = true;
				}
				return null;
			}

			@Override
			protected void done(Void result) {
				appendCachedData(newData);
				loading = false;
			}
		}.execute();
	}

	protected List<Entry> cacheInBackground() throws Exception {
		MusicService service = MusicServiceFactory.getMusicService(context);
		MusicDirectory result;
		int offset = entries.size();
		if(("genres".equals(type) && ServerInfo.checkServerVersion(context, "1.10.0")) || "years".equals(type)) {
			result = service.getAlbumList(type, extra, size, offset, context, null);
		} else if("genres".equals(type) || "genres-songs".equals(type)) {
			result = service.getSongsByGenre(extra, size, offset, context, null);
		} else {
			result = service.getAlbumList(type, size, offset, context, null);
		}
		return result.getChildren();
	}

	protected void appendCachedData(List<Entry> newData) {
		if(newData.size() > 0) {
			int start = entries.size();
			entries.addAll(newData);
			this.notifyItemRangeInserted(start, newData.size());
		}
	}
}
