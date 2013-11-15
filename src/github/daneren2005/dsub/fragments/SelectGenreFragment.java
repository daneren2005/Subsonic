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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.view.GenreAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectGenreFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectGenreFragment.class.getSimpleName();
	private ListView genreListView;
	private View emptyView;
	private List<Genre> genres;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			genres = (List<Genre>) bundle.getSerializable(Constants.FRAGMENT_LIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) genres);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		genreListView = (ListView)rootView.findViewById(R.id.fragment_list);
		genreListView.setOnItemClickListener(this);
		emptyView = rootView.findViewById(R.id.fragment_list_empty);

		if(genres == null) {
			refresh();
		} else {
			genreListView.setAdapter(new GenreAdapter(context, genres));
		}

		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(!primaryFragment) {
			return;
		}

		menuInflater.inflate(R.menu.select_genres, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}

	@Override
	protected void refresh(boolean refresh) {
		load(refresh);
	}

	private void load(final boolean refresh) {
		setTitle(R.string.main_albums_genres);
		genreListView.setVisibility(View.INVISIBLE);
		
		BackgroundTask<List<Genre>> task = new TabBackgroundTask<List<Genre>>(this) {
			@Override
			protected List<Genre> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);

				genres = new ArrayList<Genre>();

				try {
					genres = musicService.getGenres(refresh, context, this);
				} catch (Exception x) {
					Log.e(TAG, "Failed to load genres", x);
				}

				return genres;
			}

			@Override
			protected void done(List<Genre> result) {
				emptyView.setVisibility(result == null || result.isEmpty() ? View.VISIBLE : View.GONE);

				if (result != null) {
					genreListView.setAdapter(new GenreAdapter(context, result));
					genreListView.setVisibility(View.VISIBLE);
				}
			}
		};
		task.execute();
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
