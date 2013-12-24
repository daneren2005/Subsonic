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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.view.GenreAdapter;

import java.util.List;

public class SelectGenreFragment extends SelectListFragment<Genre> {
	private static final String TAG = SelectGenreFragment.class.getSimpleName();

	@Override
	public int getOptionsMenu() {
		return R.menu.empty;
	}

	@Override
	public ArrayAdapter getAdapter(List<Genre> objs) {
		return new GenreAdapter(context, objs);
	}

	@Override
	public List<Genre> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return musicService.getGenres(refresh, context, listener);
	}

	@Override
	public int getTitleResource() {
		return R.string.main_albums_genres;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Genre genre = (Genre) parent.getItemAtPosition(position);
		
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "genres");
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_EXTRA, genre.getName());
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.fragment_list_layout);
	}
}
