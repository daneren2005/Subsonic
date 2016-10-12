package github.daneren2005.dsub.fragments;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.net.Uri;
import android.view.ViewGroup;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.ArtistAdapter;
import github.daneren2005.dsub.adapter.EntryGridAdapter;
import github.daneren2005.dsub.adapter.SearchAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
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

	private static final int MAX_ARTISTS = 20;
	private static final int MAX_ALBUMS = 20;
	private static final int MAX_SONGS = 50;
	private static final int MIN_CLOSENESS = 1;

	protected RecyclerView recyclerView;
	protected SearchAdapter adapter;
	protected boolean largeAlbums = false;

	private SearchResult searchResult;
	private boolean skipSearch = false;
	private String currentQuery;

	public SearchFragment() {
		super();
		alwaysStartFullscreen = true;
	}

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
	public void setIsOnlyVisible(boolean isOnlyVisible) {
		boolean update = this.isOnlyVisible != isOnlyVisible;
		super.setIsOnlyVisible(isOnlyVisible);
		if(update && adapter != null) {
			RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
			if(layoutManager instanceof GridLayoutManager) {
				((GridLayoutManager) layoutManager).setSpanCount(getRecyclerColumnCount());
			}
		}
	}

	@Override
	public GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final GridLayoutManager gridLayoutManager) {
		return new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				int viewType = adapter.getItemViewType(position);
				if(viewType == EntryGridAdapter.VIEW_TYPE_SONG || viewType == EntryGridAdapter.VIEW_TYPE_HEADER || viewType == ArtistAdapter.VIEW_TYPE_ARTIST) {
					return gridLayoutManager.getSpanCount();
				} else {
					return 1;
				}
			}
		};
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.search, menu);
		onFinishSetupOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Serializable> updateView, Serializable item) {
		onCreateContextMenuSupport(menu, menuInflater, updateView, item);
		if(item instanceof Entry && !((Entry) item).isVideo() && !Util.isOffline(context)) {
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
	public void onItemClicked(UpdateView<Serializable> updateView, Serializable item) {
		if (item instanceof Artist) {
			onArtistSelected((Artist) item, false);
		} else if (item instanceof Entry) {
			Entry entry = (Entry) item;
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
	protected List<Entry> getSelectedEntries() {
		List<Serializable> selected = adapter.getSelected();
		List<Entry> selectedMedia = new ArrayList<>();
		for(Serializable ser: selected) {
			if(ser instanceof Entry) {
				selectedMedia.add((Entry) ser);
			}
		}

		return selectedMedia;
	}

	@Override
	protected boolean isShowArtistEnabled() {
		return true;
	}

	public void search(final String query, final boolean autoplay, final String artist, final String album, final String title) {
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
					autoplay(query, artist, album, title);
				}

			}
		};
		task.execute();

		if(searchItem != null) {
			MenuItemCompat.collapseActionView(searchItem);
		}
	}

	protected String getCurrentQuery() {
		return currentQuery;
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

	private void onAlbumSelected(Entry album, boolean autoplay) {
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

	private void onSongSelected(Entry song, boolean save, boolean append, boolean autoplay, boolean playNext) {
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

	private void onVideoSelected(Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(context);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoUrl(maxBitrate, context, entry.getId())));
		startActivity(intent);
	}

	private void autoplay(String query, String artistQuery, String albumQuery, String titleQuery) {
		Log.i(TAG, "Query: '" + query + "' ( Artist: '" + artistQuery + "', Album: '" + albumQuery + "', Title: '" + titleQuery + "')");

		if(titleQuery != null && !searchResult.getSongs().isEmpty()) {
			titleQuery = titleQuery.toLowerCase();

			TreeMap<Integer, Entry> tree = new TreeMap<>();
			for(Entry song: searchResult.getSongs()) {
				tree.put(Util.getStringDistance(song.getTitle().toLowerCase(), titleQuery), song);
			}

			Map.Entry<Integer, Entry> entry = tree.firstEntry();
			if(entry.getKey() <= MIN_CLOSENESS) {
				onSongSelected(entry.getValue(), false, false, true, false);
			} else {
				autoplay(query);
			}
		} else if(albumQuery != null && !searchResult.getAlbums().isEmpty()) {
			albumQuery = albumQuery.toLowerCase();

			TreeMap<Integer, Entry> tree = new TreeMap<>();
			for(Entry album: searchResult.getAlbums()) {
				tree.put(Util.getStringDistance(album.getTitle().toLowerCase(), albumQuery), album);
			}

			Map.Entry<Integer, Entry> entry = tree.firstEntry();
			if(entry.getKey() <= MIN_CLOSENESS) {
				onAlbumSelected(entry.getValue(), true);
			} else {
				autoplay(query);
			}
		} else if(artistQuery != null && !searchResult.getArtists().isEmpty()) {
			artistQuery = artistQuery.toLowerCase();

			TreeMap<Integer, Artist> tree = new TreeMap<>();
			for(Artist artist: searchResult.getArtists()) {
				tree.put(Util.getStringDistance(artist.getName().toLowerCase(), artistQuery), artist);
			}
			Map.Entry<Integer, Artist> entry = tree.firstEntry();
			if(entry.getKey() <= MIN_CLOSENESS) {
				onArtistSelected(entry.getValue(), true);
			} else {
				autoplay(query);
			}
		} else {
			autoplay(query);
		}
	}

	private void autoplay(String query) {
		query = query.toLowerCase();

		Artist artist = null;
		if(!searchResult.getArtists().isEmpty()) {
			artist = searchResult.getArtists().get(0);
			artist.setCloseness(Util.getStringDistance(artist.getName().toLowerCase(), query));
		}
		Entry album = null;
		if(!searchResult.getAlbums().isEmpty()) {
			album = searchResult.getAlbums().get(0);
			album.setCloseness(Util.getStringDistance(album.getTitle().toLowerCase(), query));
		}
		Entry song = null;
		if(!searchResult.getSongs().isEmpty()) {
			song = searchResult.getSongs().get(0);
			song.setCloseness(Util.getStringDistance(song.getTitle().toLowerCase(), query));
		}

		if(artist != null && (artist.getCloseness() <= MIN_CLOSENESS ||
				(album == null || artist.getCloseness() <= album.getCloseness()) &&
						(song == null || artist.getCloseness() <= song.getCloseness()))) {
			onArtistSelected(artist, true);
		} else if(album != null && (album.getCloseness() <= MIN_CLOSENESS ||
				song == null || album.getCloseness() <= song.getCloseness())) {
			onAlbumSelected(album, true);
		} else if(song != null) {
			onSongSelected(song, false, false, true, false);
		}
	}
}
