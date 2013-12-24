/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.commonsware.cwac.endless.EndlessAdapter;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Util;

import java.util.List;

public class AlbumListAdapter extends EndlessAdapter {
	Context context;
	ArrayAdapter<MusicDirectory.Entry> adapter;
	String type;
	String extra;
	int size;
	int offset;
	List<MusicDirectory.Entry> entries;
	
	public AlbumListAdapter(Context context, ArrayAdapter<MusicDirectory.Entry> adapter, String type, String extra, int size) {
		super(adapter);
		this.context = context;
		this.adapter = adapter;
		this.type = type;
		this.extra = extra;
		this.size = size;
		this.offset = size;
	}
	
	@Override
	protected boolean cacheInBackground() throws Exception {
		MusicService service = MusicServiceFactory.getMusicService(context);
		MusicDirectory result;
		if(("genres".equals(type) && Util.checkServerVersion(context, "1.10.0")) || "years".equals(type)) {
			result = service.getAlbumList(type, extra, size, offset, context, null);
		} else if("genres".equals(type)) {
			result = service.getSongsByGenre(extra, size, offset, context, null);
		} else {
			result = service.getAlbumList(type, size, offset, context, null);
		}
		entries = result.getChildren();
		if(entries.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void appendCachedData() {
		for(MusicDirectory.Entry entry: entries) {
			adapter.add(entry);
		}
		offset += entries.size();
	}
	
	@Override
	protected View getPendingView(ViewGroup parent) {
		View progress = LayoutInflater.from(context).inflate(R.layout.tab_progress, null);
		progress.setVisibility(View.VISIBLE);
		return progress;
	}
}
