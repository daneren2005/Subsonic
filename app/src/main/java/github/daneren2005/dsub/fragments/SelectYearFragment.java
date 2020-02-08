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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.BasicListAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.view.UpdateView;

public class SelectYearFragment extends SelectRecyclerFragment<String> {

	public SelectYearFragment() {
		super();
		pullToRefresh = false;
		serialize = false;
		backgroundUpdate = false;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.empty;
	}

	@Override
	public SectionAdapter getAdapter(List<String> objs) {
		return new BasicListAdapter(context, objs, this);
	}

	@Override
	public List<String> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		List<String> decades = new ArrayList<>();
		for(int i = (int)(Math.ceil(Calendar.getInstance().get(Calendar.YEAR) / 10) * 10); i >= 1800; i -= 10) {
			decades.add(String.valueOf(i));
		}

		return decades;
	}

	@Override
	public int getTitleResource() {
		return R.string.main_albums_year;
	}

	@Override
	public void onItemClicked(UpdateView<String> updateView, String decade) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "years");
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_EXTRA, decade);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<String> updateView, String item) {}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<String> updateView, String item) {
		return false;
	}
}
