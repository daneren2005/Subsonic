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
package github.daneren2005.dsub.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;

/**
 * Created by Scott on 12/23/13.
 */
public class SelectYearFragment extends SelectListFragment<Integer> {

	@Override
	public int getOptionsMenu() {
		return R.menu.empty;
	}

	@Override
	public ArrayAdapter getAdapter(List<Integer> objs) {
		return new ArrayAdapter<Integer>(context, android.R.layout.simple_list_item_1, objs);
	}

	@Override
	public List<Integer> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		List<Integer> decades = new ArrayList<Integer>();
		for(int i = 2010; i >= 1920; i -= 10) {
			decades.add(i);
		}

		return decades;
	}

	@Override
	public int getTitleResource() {
		return R.string.main_albums_year;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Integer decade = (Integer) parent.getItemAtPosition(position);

		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "years");
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_EXTRA, Integer.toString(decade));
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.fragment_list_layout);
	}
}
