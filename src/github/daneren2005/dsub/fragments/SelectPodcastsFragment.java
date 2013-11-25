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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.PodcastChannelAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott
 */
public class SelectPodcastsFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectPodcastsFragment.class.getSimpleName();
	private ListView podcastListView;
	private PodcastChannelAdapter podcastAdapter;
	private View emptyView;
	private List<PodcastChannel> channels;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			channels = (List<PodcastChannel>) bundle.getSerializable(Constants.FRAGMENT_LIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) channels);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		podcastListView = (ListView)rootView.findViewById(R.id.fragment_list);
		podcastListView.setOnItemClickListener(this);
		registerForContextMenu(podcastListView);
		emptyView = rootView.findViewById(R.id.fragment_list_empty);

		if(channels == null) {
			if(!primaryFragment) {
				invalidated = true;
			} else {
				refresh(false);
			}
		} else {
			podcastListView.setAdapter(podcastAdapter = new PodcastChannelAdapter(context, channels));
		}

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
		
		switch (item.getItemId()) {
			case R.id.menu_check:
				refreshPodcasts();
				break;
			case R.id.menu_add_podcast:
				addNewPodcast();
				break;
		}

		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		if(!primaryFragment) {
			return;
		}

		if(!Util.isOffline(context)) {
			android.view.MenuInflater inflater = context.getMenuInflater();
			inflater.inflate(R.menu.select_podcasts_context, menu);

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			PodcastChannel podcast = (PodcastChannel) podcastListView.getItemAtPosition(info.position);
			if(SyncUtil.isSyncedPodcast(context, podcast.getId())) {
				menu.removeItem(R.id.podcast_menu_sync);
			} else {
				menu.removeItem(R.id.podcast_menu_stop_sync);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(!primaryFragment) {
			return false;
		}
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		PodcastChannel channel = (PodcastChannel) podcastListView.getItemAtPosition(info.position);

		switch (menuItem.getItemId()) {
			case R.id.podcast_menu_sync:
				syncPodcast(channel);
				break;
			case R.id.podcast_menu_stop_sync:
				stopSyncPodcast(channel);
				break;
			case R.id.podcast_channel_info:
				displayPodcastInfo(channel);
				break;
			case R.id.podcast_channel_delete:
				deletePodcast(channel);
				break;
		}
		
		return true;
	}
	
	@Override
	protected void refresh(final boolean refresh) {
		setTitle(R.string.button_bar_podcasts);
		podcastListView.setVisibility(View.INVISIBLE);
		emptyView.setVisibility(View.GONE);
		
		BackgroundTask<List<PodcastChannel>> task = new TabBackgroundTask<List<PodcastChannel>>(this) {
			@Override
			protected List<PodcastChannel> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);

				channels = new ArrayList<PodcastChannel>();

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
					podcastListView.setAdapter(podcastAdapter = new PodcastChannelAdapter(context, result));
					podcastListView.setVisibility(View.VISIBLE);
				}
			}
		};
		task.execute();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		PodcastChannel channel = (PodcastChannel) parent.getItemAtPosition(position);
		
		if("error".equals(channel.getStatus())) {
			Util.toast(context, context.getResources().getString(R.string.select_podcasts_invalid_podcast_channel, channel.getErrorMessage() == null ? "error" : channel.getErrorMessage()));
		} else if("downloading".equals(channel.getStatus())) {
			Util.toast(context, R.string.select_podcasts_initializing);
		} else {
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_ID, channel.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_NAME, channel.getName());
			args.putString(Constants.INTENT_EXTRA_NAME_PODCAST_DESCRIPTION, channel.getDescription());
			fragment.setArguments(args);

			replaceFragment(fragment, R.id.fragment_list_layout);
		}
	}
	
	public void refreshPodcasts() {
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.refreshPodcasts(context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, R.string.select_podcasts_refreshing);
			}

			@Override
			protected void error(Throwable error) {
				Util.toast(context, getErrorMessage(error), false);
			}
		}.execute();
	}
	
	private void addNewPodcast() {
		View dialogView = context.getLayoutInflater().inflate(R.layout.create_podcast, null);
		final TextView urlBox = (TextView) dialogView.findViewById(R.id.create_podcast_url);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.menu_add_podcast)
			.setView(dialogView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					addNewPodcast(urlBox.getText().toString());
				}
			})
			.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.setCancelable(true);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	private void addNewPodcast(final String url) {
		new LoadingTask<Void>(context, false) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.createPodcastChannel(url, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				refresh();
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.select_podcasts_created_error) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}
	
	private void displayPodcastInfo(final PodcastChannel channel) {
		String message = ((channel.getName()) == null ? "" : "Title: " + channel.getName()) +
			"\nURL: " + channel.getUrl() +
			"\nStatus: " + channel.getStatus() +
			((channel.getErrorMessage()) == null ? "" : "\nError Message: " + channel.getErrorMessage()) +
			((channel.getDescription()) == null ? "" : "\nDescription: " + channel.getDescription());
		
		Util.info(context, channel.getName(), message);
	}
	
	private void deletePodcast(final PodcastChannel channel) {
		Util.confirmDialog(context, R.string.common_delete, channel.getName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(context, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deletePodcastChannel(channel.getId(), context, null);
						stopSyncPodcast(channel);
						return null;
					}

					@Override
					protected void done(Void result) {
						podcastAdapter.remove(channel);
						podcastAdapter.notifyDataSetChanged();
						Util.toast(context, context.getResources().getString(R.string.select_podcasts_deleted, channel.getName()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.select_podcasts_deleted_error, channel.getName()) + " " + getErrorMessage(error);
						}

						Util.toast(context, msg, false);
					}
				}.execute();
			}
		});
	}

	private void syncPodcast(final PodcastChannel podcast) {
		new LoadingTask<MusicDirectory>(context, false) {
			@Override
			protected MusicDirectory doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				return musicService.getPodcastEpisodes(true, podcast.getId(), context, this);
			}

			@Override
			protected void done(MusicDirectory result) {
				List<String> existingEpisodes = new ArrayList<String>();
				for(MusicDirectory.Entry entry: result.getChildren()) {
					String id = entry.getId();
					if(id != null) {
						existingEpisodes.add(entry.getId());
					}
				}

				SyncUtil.addSyncedPodcast(context, podcast.getId(), existingEpisodes);
			}
		}.execute();
	}

	private void stopSyncPodcast(PodcastChannel podcast) {
		SyncUtil.removeSyncedPodcast(context, podcast.getId());
	}
}
