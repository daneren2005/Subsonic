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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.view.PodcastChannelAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott
 */
public class SelectPodcastsFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectPodcastsFragment.class.getSimpleName();
	private ListView podcastListView;
	private View emptyView;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.select_podcasts, container, false);

		podcastListView = (ListView)rootView.findViewById(R.id.select_podcasts_list);
		podcastListView.setOnItemClickListener(this);
		emptyView = rootView.findViewById(R.id.select_podcasts_empty);
		refresh();

		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.select_podcasts, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}
	
	@Override
	public void setPrimaryFragment(boolean primary) {
		super.setPrimaryFragment(primary);
		if(rootView != null) {
			if(primary) {
				((ViewGroup)rootView).getChildAt(0).setVisibility(View.VISIBLE);
			} else {
				((ViewGroup)rootView).getChildAt(0).setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	protected void refresh(final boolean refresh) {
		setTitle(R.string.button_bar_podcasts);
		
		BackgroundTask<List<PodcastChannel>> task = new TabBackgroundTask<List<PodcastChannel>>(this) {
			@Override
			protected List<PodcastChannel> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);

				List<PodcastChannel> channels = new ArrayList<PodcastChannel>(); 

				try {
					channels = musicService.getPodcastChannels(refresh, context, this);
				} catch (Exception x) {
					Log.e(TAG, "Failed to load podcasts", x);
				}

				return channels;
			}

			@Override
			protected void done(List<PodcastChannel> result) {
				emptyView.setVisibility(result == null || result.isEmpty() ? View.VISIBLE : View.GONE);

				if (result != null) {
					podcastListView.setAdapter(new PodcastChannelAdapter(context, result));
				}

			}
		};
		task.execute();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		PodcastChannel channel = (PodcastChannel) parent.getItemAtPosition(position);
		
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_ID, channel.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_NAME, channel.getName());
		args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_DESCRIPTION, channel.getDescription());
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.select_podcasts_layout);
	}
}
