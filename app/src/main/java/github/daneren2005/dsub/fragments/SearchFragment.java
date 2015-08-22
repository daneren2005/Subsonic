package github.daneren2005.dsub.fragments;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.net.Uri;
import android.view.ViewGroup;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.ArtistAdapter;
import github.daneren2005.dsub.adapter.EntryGridAdapter;
import github.daneren2005.dsub.adapter.SearchAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;

public class SearchFragment extends SubsonicFragment implements SectionAdapter.OnItemClickedListener<Serializable> {
	private static final String TAG = SearchFragment.class.getSimpleName();

	private static final int MAX_ARTISTS = 10;
	private static final int MAX_ALBUMS = 10;
	private static final int MAX_SONGS = 25;

	protected RecyclerView recyclerView;
	protected SearchAdapter adapter;
	protected boolean largeAlbums = false;

	private SearchResult searchResult;
	private boolean skipSearch = false;
	private String currentQuery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			searchResult = (SearchResult) savedInstanceState.getSerializable(Constants.FRAGMENT_LIST);
		}
		largeAlbums = Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_LARGE_ALBUM_ART, true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, searchResult);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_recycler_fragment, container, false);
		setTitle(R.string.search_title);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setEnabled(false);

		recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_recycler);
		setupLayoutManager(recyclerView, largeAlbums);

		registerForContextMenu(recyclerView);
		context.onNewIntent(context.getIntent());

		if(searchResult != null) {
			skipSearch = true;
			recyclerView.setAdapter(adapter = new SearchAdapter(context, searchResult, getImageLoader(), largeAlbums, this));
		}

		return rootView;
	}

	@Override
	public GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final int columns) {
		return new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				int viewType = adapter.getItemViewType(position);
				if(viewType == EntryGridAdapter.VIEW_TYPE_SONG || viewType == EntryGridAdapter.VIEW_TYPE_HEADER || viewType == ArtistAdapter.VIEW_TYPE_ARTIST) {
					return columns;
				} else {
					return 1;
				}
			}
		};
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.search, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_search:
				context.startSearch(currentQuery, false, null, false);
				return true;
		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Serializable> updateView, Serializable item) {
		onCreateContextMenuSupport(menu, menuInflater, updateView, item);
		if(item instanceof MusicDirectory.Entry && !((MusicDirectory.Entry) item).isVideo() && !Util.isOffline(context)) {
			menu.removeItem(R.id.song_menu_remove_playlist);
		}
		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Serializable> updateView, Serializable item) {
		return onContextItemSelected(menuItem, item);
	}

	@Override
	public void refresh(boolean refresh) {
		context.onNewIntent(context.getIntent());
	}

	@Override
	public void onItemClicked(Serializable item) {
		Log.d(TAG, item.getClass().getSimpleName());
		if (item instanceof Artist) {
			onArtistSelected((Artist) item, false);
		} else if (item instanceof MusicDirectory.Entry) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) item;
			if (entry.isDirectory()) {
				onAlbumSelected(entry, false);
			} else if (entry.isVideo()) {
				onVideoSelected(entry);
			} else {
				onSongSelected(entry, false, true, true, false);
			}
		}
	}

	@Override
	protected List<MusicDirectory.Entry> getSelectedEntries() {
		List<Serializable> selected = adapter.getSelected();
		List<MusicDirectory.Entry> selectedMedia = new ArrayList<>();
		for(Serializable ser: selected) {
			if(ser instanceof MusicDirectory.Entry) {
				selectedMedia.add((MusicDirectory.Entry) ser);
			}
		}

		return selectedMedia;
	}

	public void search(final String query, final boolean autoplay) {
		if(skipSearch) {
			skipSearch = false;
			return;
		}
		currentQuery = query;

		BackgroundTask<SearchResult> task = new TabBackgroundTask<SearchResult>(this) {
			@Override
			protected SearchResult doInBackground() throws Throwable {
				SearchCritera criteria = new SearchCritera(query, MAX_ARTISTS, MAX_ALBUMS, MAX_SONGS);
				MusicService service = MusicServiceFactory.getMusicService(context);
				return service.search(criteria, context, this);
			}

			@Override
			protected void done(SearchResult result) {
				searchResult = result;
				recyclerView.setAdapter(adapter = new SearchAdapter(context, searchResult, getImageLoader(), largeAlbums, SearchFragment.this));
				if (autoplay) {
					autoplay(query);
				}

			}
		};
		task.execute();
	}

	private void onArtistSelected(Artist artist, boolean autoplay) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
		if(autoplay) {
			args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
		}
		args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	private void onAlbumSelected(MusicDirectory.Entry album, boolean autoplay) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ID, album.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_NAME, album.getTitle());
		if(autoplay) {
			args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
		}
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	private void onSongSelected(MusicDirectory.Entry song, boolean save, boolean append, boolean autoplay, boolean playNext) {
		DownloadService downloadService = getDownloadService();
		if (downloadService != null) {
			if (!append) {
				downloadService.clear();
			}
			downloadService.download(Arrays.asList(song), save, false, playNext, false);
			if (autoplay) {
				downloadService.play(downloadService.size() - 1);
			}

			Util.toast(context, getResources().getQuantityString(R.plurals.select_album_n_songs_added, 1, 1));
		}
	}

	private void onVideoSelected(MusicDirectory.Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(context);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoUrl(maxBitrate, context, entry.getId())));
		startActivity(intent);
	}

	private void autoplay(String query) {
		Artist artist = searchResult.getArtists().isEmpty() ? null : searchResult.getArtists().get(0);
		MusicDirectory.Entry album = searchResult.getAlbums().isEmpty() ? null : searchResult.getAlbums().get(0); 
		MusicDirectory.Entry song = searchResult.getSongs().isEmpty() ? null : searchResult.getSongs().get(0);
		
		if(artist != null && query.equals(artist.getName())) {
			onArtistSelected(artist, true);
		} else if(album != null && query.equals(album.getTitle())) {
			onAlbumSelected(album, true);
		} else if(song != null) {
			onSongSelected(song, false, false, true, false);
		} else if(album != null) {
			onAlbumSelected(album, true);
		}
	}
}
