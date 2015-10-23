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

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.EntryGridAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.view.UpdateView;

public class SelectVideoFragment extends SelectRecyclerFragment<MusicDirectory.Entry> {
	@Override
	public int getOptionsMenu() {
		return R.menu.empty;
	}

	@Override
	public SectionAdapter getAdapter(List<MusicDirectory.Entry> objs) {
		SectionAdapter adapter = new EntryGridAdapter(context, objs, null, false);
		adapter.setOnItemClickedListener(this);
		return adapter;
	}

	@Override
	public List<MusicDirectory.Entry> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		MusicDirectory dir = musicService.getVideos(refresh, context, listener);
		return dir.getChildren();
	}

	@Override
	public int getTitleResource() {
		return R.string.main_videos;
	}

	@Override
	public void onItemClicked(UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry entry) {
		playVideo(entry);
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry item) {
		onCreateContextMenuSupport(menu, menuInflater, updateView, item);
		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<MusicDirectory.Entry> updateView, MusicDirectory.Entry entry) {
		return onContextItemSelected(menuItem, entry);
	}
}
