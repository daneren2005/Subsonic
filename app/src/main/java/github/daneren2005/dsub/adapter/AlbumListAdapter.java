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
package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import com.commonsware.cwac.endless.EndlessAdapter;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AlbumListAdapter extends EndlessAdapter implements SectionIndexer {
	private static final String TAG = AlbumListAdapter.class.getSimpleName();
	Context context;
	ArrayAdapter<MusicDirectory.Entry> adapter;
	String type;
	String extra;
	int size;
	int offset;
	List<MusicDirectory.Entry> entries;

	private boolean shouldIndex = false;
	private Object[] sections;
	private Integer[] positions;
	
	public AlbumListAdapter(Context context, ArrayAdapter<MusicDirectory.Entry> adapter, String type, String extra, int size) {
		super(adapter);
		this.context = context;
		this.adapter = adapter;
		this.type = type;
		this.extra = extra;
		this.size = size;
		this.offset = size;

		if("alphabeticalByName".equals(this.type)) {
			shouldIndex = true;
			recreateIndexes();
		}
	}
	
	@Override
	protected boolean cacheInBackground() throws Exception {
		MusicService service = MusicServiceFactory.getMusicService(context);
		MusicDirectory result;
		if(("genres".equals(type) && ServerInfo.checkServerVersion(context, "1.10.0")) || "years".equals(type)) {
			result = service.getAlbumList(type, extra, size, offset, false, context, null);
		} else if("genres".equals(type) || "genres-songs".equals(type)) {
			result = service.getSongsByGenre(extra, size, offset, context, null);
		} else {
			result = service.getAlbumList(type, size, offset, shouldIndex, context, null);
		}
		entries = result.getChildren();
		return entries.size() > 0;
	}

	@Override
	protected void appendCachedData() {
		for(MusicDirectory.Entry entry: entries) {
			adapter.add(entry);
		}
		offset += entries.size();
		recreateIndexes();
	}
	
	@Override
	protected View getPendingView(ViewGroup parent) {
		View progress = LayoutInflater.from(context).inflate(R.layout.tab_progress, null);
		progress.setVisibility(View.VISIBLE);
		return progress;
	}

	private void recreateIndexes() {
		try {
			if (!shouldIndex) {
				return;
			}

			Set<String> sectionSet = new LinkedHashSet<String>(30);
			List<Integer> positionList = new ArrayList<Integer>(30);
			for (int i = 0; i < adapter.getCount(); i++) {
				MusicDirectory.Entry entry = adapter.getItem(i);
				String index;
				if (entry.getAlbum() != null) {
					index = entry.getAlbum().substring(0, 1);
					if (!Character.isLetter(index.charAt(0))) {
						index = "#";
					}
				} else {
					index = "*";
				}

				if (!sectionSet.contains(index)) {
					sectionSet.add(index);
					positionList.add(i);
				}
			}
			sections = sectionSet.toArray(new Object[sectionSet.size()]);
			positions = positionList.toArray(new Integer[positionList.size()]);
		} catch(Exception e) {
			Log.e(TAG, "Error while recreating indexes");
		}
	}

	@Override
	public Object[] getSections() {
		if(sections != null) {
			return sections;
		} else {
			return new Object[0];
		}
	}

	@Override
	public int getPositionForSection(int section) {
		if(sections != null) {
			section = Math.min(section, positions.length - 1);
			return positions[section];
		} else {
			return 0;
		}
	}

	@Override
	public int getSectionForPosition(int pos) {
		if(sections != null) {
			for (int i = 0; i < sections.length - 1; i++) {
				if (pos < positions[i + 1]) {
					return i;
				}
			}
			return sections.length - 1;
		} else {
			return 0;
		}
	}
}
