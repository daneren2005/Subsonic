package github.daneren2005.dsub.fragments;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.net.Uri;
import android.view.ViewGroup;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.view.ArtistAdapter;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.view.EntryAdapter;
import github.daneren2005.dsub.view.MergeAdapter;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;

public class SearchFragment extends SubsonicFragment {
	private static final String TAG = SearchFragment.class.getSimpleName();

	private static final int DEFAULT_ARTISTS = 3;
	private static final int DEFAULT_ALBUMS = 5;
	private static final int DEFAULT_SONGS = 10;

	private static final int MAX_ARTISTS = 10;
	private static final int MAX_ALBUMS = 20;
	private static final int MAX_SONGS = 25;
	private ListView list;

	private View artistsHeading;
	private View albumsHeading;
	private View songsHeading;
	private View moreArtistsButton;
	private View moreAlbumsButton;
	private View moreSongsButton;
	private SearchResult searchResult;
	private MergeAdapter mergeAdapter;
	private ArtistAdapter artistAdapter;
	private ListAdapter moreArtistsAdapter;
	private EntryAdapter albumAdapter;
	private ListAdapter moreAlbumsAdapter;
	private ListAdapter moreSongsAdapter;
	private EntryAdapter songAdapter;
	private boolean skipSearch = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			searchResult = (SearchResult) savedInstanceState.getSerializable(Constants.FRAGMENT_LIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, searchResult);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);
		setTitle(R.string.search_title);

		View buttons = inflater.inflate(R.layout.search_buttons, null);

		artistsHeading = buttons.findViewById(R.id.search_artists);
		albumsHeading = buttons.findViewById(R.id.search_albums);
		songsHeading = buttons.findViewById(R.id.search_songs);

		moreArtistsButton = buttons.findViewById(R.id.search_more_artists);
		moreAlbumsButton = buttons.findViewById(R.id.search_more_albums);
		moreSongsButton = buttons.findViewById(R.id.search_more_songs);

		list = (ListView) rootView.findViewById(R.id.fragment_list);

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (view == moreArtistsButton) {
					expandArtists();
				} else if (view == moreAlbumsButton) {
					expandAlbums();
				} else if (view == moreSongsButton) {
					expandSongs();
				} else {
					Object item = parent.getItemAtPosition(position);
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
			}
		});
		registerForContextMenu(list);
		context.onNewIntent(context.getIntent());

		if(searchResult != null) {
			skipSearch = true;
            populateList();
		}

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.search, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Object selectedItem = list.getItemAtPosition(info.position);
		onCreateContextMenu(menu, view, menuInfo, selectedItem);
		if(selectedItem instanceof MusicDirectory.Entry && !((MusicDirectory.Entry) selectedItem).isVideo() && !Util.isOffline(context)) {
			menu.removeItem(R.id.song_menu_remove_playlist);
		}

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Object selectedItem = list.getItemAtPosition(info.position);
		
		if(onContextItemSelected(menuItem, selectedItem)) {
			return true;
		}

		return true;
	}
	
	@Override
	public void setPrimaryFragment(boolean primary) {
		super.setPrimaryFragment(primary);
	}

	@Override
	public void refresh(boolean refresh) {
		context.onNewIntent(context.getIntent());
	}

	public void search(final String query, final boolean autoplay) {
		if(skipSearch) {
			skipSearch = false;
			return;
		}
		mergeAdapter = new MergeAdapter();
		list.setAdapter(mergeAdapter);
		
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
				populateList();
				if (autoplay) {
					autoplay(query);
				}

			}
		};
		task.execute();
	}

	public void populateList() {
		mergeAdapter = new MergeAdapter();

		if (searchResult != null) {
			List<Artist> artists = searchResult.getArtists();
			if (!artists.isEmpty()) {
				mergeAdapter.addView(artistsHeading);
				List<Artist> displayedArtists = new ArrayList<Artist>(artists.subList(0, Math.min(DEFAULT_ARTISTS, artists.size())));
				artistAdapter = new ArtistAdapter(context, displayedArtists);
				mergeAdapter.addAdapter(artistAdapter);
				if (artists.size() > DEFAULT_ARTISTS) {
					moreArtistsAdapter = mergeAdapter.addView(moreArtistsButton, true);
				}
			}

			List<MusicDirectory.Entry> albums = searchResult.getAlbums();
			if (!albums.isEmpty()) {
				mergeAdapter.addView(albumsHeading);
				List<MusicDirectory.Entry> displayedAlbums = new ArrayList<MusicDirectory.Entry>(albums.subList(0, Math.min(DEFAULT_ALBUMS, albums.size())));
				albumAdapter = new EntryAdapter(context, getImageLoader(), displayedAlbums, false);
				mergeAdapter.addAdapter(albumAdapter);
				if (albums.size() > DEFAULT_ALBUMS) {
					moreAlbumsAdapter = mergeAdapter.addView(moreAlbumsButton, true);
				}
			}

			List<MusicDirectory.Entry> songs = searchResult.getSongs();
			if (!songs.isEmpty()) {
				mergeAdapter.addView(songsHeading);
				List<MusicDirectory.Entry> displayedSongs = new ArrayList<MusicDirectory.Entry>(songs.subList(0, Math.min(DEFAULT_SONGS, songs.size())));
				songAdapter = new EntryAdapter(context, getImageLoader(), displayedSongs, false);
				mergeAdapter.addAdapter(songAdapter);
				if (songs.size() > DEFAULT_SONGS) {
					moreSongsAdapter = mergeAdapter.addView(moreSongsButton, true);
				}
			}

			boolean empty = searchResult.getArtists().isEmpty() && searchResult.getAlbums().isEmpty() && searchResult.getSongs().isEmpty();
		}

		list.setAdapter(mergeAdapter);
	}

	private void expandArtists() {
		artistAdapter.clear();
		for (Artist artist : searchResult.getArtists()) {
			artistAdapter.add(artist);
		}
		artistAdapter.notifyDataSetChanged();
		mergeAdapter.removeAdapter(moreArtistsAdapter);
		mergeAdapter.notifyDataSetChanged();
	}

	private void expandAlbums() {
		albumAdapter.clear();
		for (MusicDirectory.Entry album : searchResult.getAlbums()) {
			albumAdapter.add(album);
		}
		albumAdapter.notifyDataSetChanged();
		mergeAdapter.removeAdapter(moreAlbumsAdapter);
		mergeAdapter.notifyDataSetChanged();
	}

	private void expandSongs() {
		songAdapter.clear();
		for (MusicDirectory.Entry song : searchResult.getSongs()) {
			songAdapter.add(song);
		}
		songAdapter.notifyDataSetChanged();
		mergeAdapter.removeAdapter(moreSongsAdapter);
		mergeAdapter.notifyDataSetChanged();
	}

	private void onArtistSelected(Artist artist, boolean autoplay) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
		if(autoplay) {
			args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
		}
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.fragment_list_layout);
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

		replaceFragment(fragment, R.id.fragment_list_layout);
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
