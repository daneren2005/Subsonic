package github.daneren2005.dsub.fragments;

import android.annotation.TargetApi;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.AlphabeticalAlbumAdapter;
import github.daneren2005.dsub.adapter.EntryInfiniteGridAdapter;
import github.daneren2005.dsub.adapter.EntryGridAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.adapter.TopRatedAlbumAdapter;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.service.CachedMusicService;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.util.ImageLoader;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.UpdateHelper;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.FastScroller;
import github.daneren2005.dsub.view.GridSpacingDecoration;
import github.daneren2005.dsub.view.MyLeadingMarginSpan2;
import github.daneren2005.dsub.view.RecyclingImageView;
import github.daneren2005.dsub.view.UpdateView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static github.daneren2005.dsub.domain.MusicDirectory.Entry;

public class SelectDirectoryFragment extends SubsonicFragment implements SectionAdapter.OnItemClickedListener<Entry> {
	private static final String TAG = SelectDirectoryFragment.class.getSimpleName();

	private RecyclerView recyclerView;
	private FastScroller fastScroller;
	private EntryGridAdapter entryGridAdapter;
	private Boolean licenseValid;
	private List<Entry> albums;
	private List<Entry> entries;
	private LoadTask currentTask;
	private ArtistInfo artistInfo;
	private String artistInfoDelayed;

	private SilentBackgroundTask updateCoverArtTask;
	private ImageView coverArtView;
	private Entry coverArtRep;
	private String coverArtId;

	String id;
	String name;
	Entry directory;
	String playlistId;
	String playlistName;
	boolean playlistOwner;
	String podcastId;
	String podcastName;
	String podcastDescription;
	String albumListType;
	String albumListExtra;
	int albumListSize;
	boolean refreshListing = false;
	boolean showAll = false;
	boolean restoredInstance = false;
	boolean lookupParent = false;
	boolean largeAlbums = false;
	boolean topTracks = false;
	String lookupEntry;

	public SelectDirectoryFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		if(bundle != null) {
			entries = (List<Entry>) bundle.getSerializable(Constants.FRAGMENT_LIST);
			albums = (List<Entry>) bundle.getSerializable(Constants.FRAGMENT_LIST2);
			if(albums == null) {
				albums = new ArrayList<>();
			}
			artistInfo = (ArtistInfo) bundle.getSerializable(Constants.FRAGMENT_EXTRA);
			restoredInstance = true;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) entries);
		outState.putSerializable(Constants.FRAGMENT_LIST2, (Serializable) albums);
		outState.putSerializable(Constants.FRAGMENT_EXTRA, (Serializable) artistInfo);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		Bundle args = getArguments();
		if(args != null) {
			id = args.getString(Constants.INTENT_EXTRA_NAME_ID);
			name = args.getString(Constants.INTENT_EXTRA_NAME_NAME);
			directory = (Entry) args.getSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY);
			playlistId = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
			playlistName = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
			playlistOwner = args.getBoolean(Constants.INTENT_EXTRA_NAME_PLAYLIST_OWNER, false);
			podcastId = args.getString(Constants.INTENT_EXTRA_NAME_PODCAST_ID);
			podcastName = args.getString(Constants.INTENT_EXTRA_NAME_PODCAST_NAME);
			podcastDescription = args.getString(Constants.INTENT_EXTRA_NAME_PODCAST_DESCRIPTION);
			Object shareObj = args.getSerializable(Constants.INTENT_EXTRA_NAME_SHARE);
			share = (shareObj != null) ? (Share) shareObj : null;
			albumListType = args.getString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
			albumListExtra = args.getString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_EXTRA);
			albumListSize = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
			refreshListing = args.getBoolean(Constants.INTENT_EXTRA_REFRESH_LISTINGS);
			artist = args.getBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, false);
			lookupEntry = args.getString(Constants.INTENT_EXTRA_SEARCH_SONG);
			topTracks = args.getBoolean(Constants.INTENT_EXTRA_TOP_TRACKS);
			showAll = args.getBoolean(Constants.INTENT_EXTRA_SHOW_ALL);

			String childId = args.getString(Constants.INTENT_EXTRA_NAME_CHILD_ID);
			if(childId != null) {
				id = childId;
				lookupParent = true;
			}
			if(entries == null) {
				entries = (List<Entry>) args.getSerializable(Constants.FRAGMENT_LIST);
				albums = (List<Entry>) args.getSerializable(Constants.FRAGMENT_LIST2);

				if(albums == null) {
					albums = new ArrayList<Entry>();
				}
			}
		}

		rootView = inflater.inflate(R.layout.abstract_recycler_fragment, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(this);

		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_LARGE_ALBUM_ART, true)) {
			largeAlbums = true;
		}

		recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_recycler);
		recyclerView.setHasFixedSize(true);
		fastScroller = (FastScroller) rootView.findViewById(R.id.fragment_fast_scroller);
		setupScrollList(recyclerView);
		setupLayoutManager(recyclerView, largeAlbums);

		if(entries == null) {
			if(primaryFragment || secondaryFragment) {
				load(false);
			} else {
				invalidated = true;
			}
		} else {
            licenseValid = true;
            finishLoading();
		}

		if(name != null) {
			setTitle(name);
		}

		return rootView;
	}

	@Override
	public void setIsOnlyVisible(boolean isOnlyVisible) {
		boolean update = this.isOnlyVisible != isOnlyVisible;
		super.setIsOnlyVisible(isOnlyVisible);
		if(update && entryGridAdapter != null) {
			RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
			if(layoutManager instanceof GridLayoutManager) {
				((GridLayoutManager) layoutManager).setSpanCount(getRecyclerColumnCount());
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(licenseValid == null) {
			menuInflater.inflate(R.menu.empty, menu);
		} else if(albumListType != null && !"starred".equals(albumListType)) {
			menuInflater.inflate(R.menu.select_album_list, menu);
		} else if(artist && !showAll) {
			menuInflater.inflate(R.menu.select_album, menu);

			if(!ServerInfo.hasTopSongs(context)) {
				menu.removeItem(R.id.menu_top_tracks);
			}
			if(!ServerInfo.checkServerVersion(context, "1.11")) {
				menu.removeItem(R.id.menu_radio);
				menu.removeItem(R.id.menu_similar_artists);
			} else if(!ServerInfo.hasSimilarArtists(context)) {
				menu.removeItem(R.id.menu_similar_artists);
			}
		} else {
			if(podcastId == null) {
				if(Util.isOffline(context)) {
					menuInflater.inflate(R.menu.select_song_offline, menu);
				}
				else {
					menuInflater.inflate(R.menu.select_song, menu);

					if(playlistId == null || !playlistOwner) {
						menu.removeItem(R.id.menu_remove_playlist);
					}
				}

				SharedPreferences prefs = Util.getPreferences(context);
				if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_NEXT, true)) {
					menu.setGroupVisible(R.id.hide_play_next, false);
				}
				if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_LAST, true)) {
					menu.setGroupVisible(R.id.hide_play_last, false);
				}
			} else {
				if(Util.isOffline(context)) {
					menuInflater.inflate(R.menu.select_podcast_episode_offline, menu);
				}
				else {
					menuInflater.inflate(R.menu.select_podcast_episode, menu);

					if(!UserUtil.canPodcast()) {
						menu.removeItem(R.id.menu_download_all);
					}
				}
			}
		}

		if("starred".equals(albumListType)) {
			menuInflater.inflate(R.menu.unstar, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_remove_playlist:
				removeFromPlaylist(playlistId, playlistName, getSelectedIndexes());
				return true;
			case R.id.menu_download_all:
				downloadAllPodcastEpisodes();
				return true;
			case R.id.menu_show_all:
				setShowAll();
				return true;
			case R.id.menu_top_tracks:
				showTopTracks();
				return true;
			case R.id.menu_similar_artists:
				showSimilarArtists(id);
				return true;
			case R.id.menu_radio:
				startArtistRadio(id);
				return true;
		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView updateView, Entry entry) {
		onCreateContextMenuSupport(menu, menuInflater, updateView, entry);
		if(!entry.isVideo() && !Util.isOffline(context) && (playlistId == null || !playlistOwner) && (podcastId == null  || Util.isOffline(context) && podcastId != null)) {
			menu.removeItem(R.id.song_menu_remove_playlist);
		}

		recreateContextMenu(menu);
	}
	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Entry> updateView, Entry entry) {
		if(onContextItemSelected(menuItem, entry)) {
			return true;
		}

		switch (menuItem.getItemId()) {
			case R.id.song_menu_remove_playlist:
				removeFromPlaylist(playlistId, playlistName, Arrays.<Integer>asList(entries.indexOf(entry)));
				break;
		}

		return true;
	}

	@Override
	public void onItemClicked(UpdateView<Entry> updateView, Entry entry) {
		if (entry.isDirectory()) {
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
			args.putSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY, entry);
			if ("newest".equals(albumListType)) {
				args.putBoolean(Constants.INTENT_EXTRA_REFRESH_LISTINGS, true);
			}
			if(!entry.isAlbum()) {
				args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
			}
			fragment.setArguments(args);

			replaceFragment(fragment, true);
		} else if (entry.isVideo()) {
			playVideo(entry);
		} else if(entry instanceof PodcastEpisode) {
			String status = ((PodcastEpisode)entry).getStatus();
			if("error".equals(status)) {
				Util.toast(context, R.string.select_podcasts_error);
				return;
			} else if(!"completed".equals(status)) {
				Util.toast(context, R.string.select_podcasts_skipped);
				return;
			}

			onSongPress(Arrays.asList(entry), entry, false);
		} else {
			onSongPress(entries, entry, albumListType == null || "starred".equals(albumListType));
		}
	}

	@Override
	protected void refresh(boolean refresh) {
		load(refresh);
	}

	@Override
	protected boolean isShowArtistEnabled() {
		return albumListType != null;
	}

	private void load(boolean refresh) {
		if(refreshListing) {
			refresh = true;
		}

		if(currentTask != null) {
			currentTask.cancel();
		}

		recyclerView.setVisibility(View.INVISIBLE);
		if (playlistId != null) {
			getPlaylist(playlistId, playlistName, refresh);
		} else if(podcastId != null) {
			getPodcast(podcastId, podcastName, refresh);
		} else if (share != null) {
			if(showAll) {
				getRecursiveMusicDirectory(share.getId(), share.getName(), refresh);
			} else {
				getShare(share, refresh);
			}
		} else if (albumListType != null) {
			getAlbumList(albumListType, albumListSize, refresh);
		} else {
			if(showAll) {
				getRecursiveMusicDirectory(id, name, refresh);
			} else if(topTracks) {
				getTopTracks(id, name, refresh);
			} else {
				getMusicDirectory(id, name, refresh);
			}
		}
	}

	private void getMusicDirectory(final String id, final String name, final boolean refresh) {
		setTitle(name);

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				MusicDirectory dir = getMusicDirectory(id, name, refresh, service, this);

				if(lookupParent && dir.getParent() != null) {
					dir = getMusicDirectory(dir.getParent(), name, refresh, service, this);

					// Update the fragment pointers so other stuff works correctly
					SelectDirectoryFragment.this.id = dir.getId();
					SelectDirectoryFragment.this.name = dir.getName();
				} else if(id != null && directory == null && dir.getParent() != null && !artist) {
					// View Album, try to lookup parent to get a complete entry to use for starring
					MusicDirectory parentDir = getMusicDirectory(dir.getParent(), name, refresh, true, service, this);
					for(Entry child: parentDir.getChildren()) {
						if(id.equals(child.getId())) {
							directory = child;
							break;
						}
					}
				}

				return dir;
			}

			@Override
			protected void done(Pair<MusicDirectory, Boolean> result) {
				SelectDirectoryFragment.this.name = result.getFirst().getName();
				setTitle(SelectDirectoryFragment.this.name);
				super.done(result);
			}
		}.execute();
	}

	private void getRecursiveMusicDirectory(final String id, final String name, final boolean refresh) {
		setTitle(name);

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				MusicDirectory root;
				if(share == null) {
					root = getMusicDirectory(id, name, refresh, service, this);
				} else {
					root = share.getMusicDirectory();
				}
				List<Entry> songs = new ArrayList<Entry>();
				getSongsRecursively(root, songs);

				// CachedMusicService is refreshing this data in the background, so will wipe out the songs list from root
				MusicDirectory clonedRoot = new MusicDirectory(songs);
				clonedRoot.setId(root.getId());
				clonedRoot.setName(root.getName());
				return clonedRoot;
			}

			private void getSongsRecursively(MusicDirectory parent, List<Entry> songs) throws Exception {
				songs.addAll(parent.getChildren(false, true));
				for (Entry dir : parent.getChildren(true, false)) {
					MusicService musicService = MusicServiceFactory.getMusicService(context);

					MusicDirectory musicDirectory;
					if(Util.isTagBrowsing(context) && !Util.isOffline(context)) {
						musicDirectory = musicService.getAlbum(dir.getId(), dir.getTitle(), false, context, this);
					} else {
						musicDirectory = musicService.getMusicDirectory(dir.getId(), dir.getTitle(), false, context, this);
					}
					getSongsRecursively(musicDirectory, songs);
				}
			}

			@Override
			protected void done(Pair<MusicDirectory, Boolean> result) {
				SelectDirectoryFragment.this.name = result.getFirst().getName();
				setTitle(SelectDirectoryFragment.this.name);
				super.done(result);
			}
		}.execute();
	}

	private void getPlaylist(final String playlistId, final String playlistName, final boolean refresh) {
		setTitle(playlistName);

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getPlaylist(refresh, playlistId, playlistName, context, this);
			}
		}.execute();
	}

	private void getPodcast(final String podcastId, final String podcastName, final boolean refresh) {
		setTitle(podcastName);

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getPodcastEpisodes(refresh, podcastId, context, this);
			}
		}.execute();
	}

	private void getShare(final Share share, final boolean refresh) {
		setTitle(share.getName());

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return share.getMusicDirectory();
			}
		}.execute();
	}

	private void getTopTracks(final String id, final String name, final boolean refresh) {
		setTitle(name);

		new LoadTask(refresh) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getTopTrackSongs(name, 50, context, this);
			}
		}.execute();
	}

	private void getAlbumList(final String albumListType, final int size, final boolean refresh) {
		if ("newest".equals(albumListType)) {
			setTitle(R.string.main_albums_newest);
		} else if ("random".equals(albumListType)) {
			setTitle(R.string.main_albums_random);
		} else if ("highest".equals(albumListType)) {
			setTitle(R.string.main_albums_highest);
		} else if ("recent".equals(albumListType)) {
			setTitle(R.string.main_albums_recent);
		} else if ("frequent".equals(albumListType)) {
			setTitle(R.string.main_albums_frequent);
		} else if ("starred".equals(albumListType)) {
			setTitle(R.string.main_albums_starred);
		} else if("genres".equals(albumListType) || "years".equals(albumListType)) {
			setTitle(albumListExtra);
		} else if("alphabeticalByName".equals(albumListType)) {
			setTitle(R.string.main_albums_alphabetical);
		} if (MainFragment.SONGS_NEWEST.equals(albumListType)) {
			setTitle(R.string.main_songs_newest);
		} else if (MainFragment.SONGS_TOP_PLAYED.equals(albumListType)) {
			setTitle(R.string.main_songs_top_played);
		} else if (MainFragment.SONGS_RECENT.equals(albumListType)) {
			setTitle(R.string.main_songs_recent);
		} else if (MainFragment.SONGS_FREQUENT.equals(albumListType)) {
			setTitle(R.string.main_songs_frequent);
		}

		new LoadTask(true) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				MusicDirectory result;
				if ("starred".equals(albumListType)) {
					result = service.getStarredList(context, this);
				} else if(("genres".equals(albumListType) && ServerInfo.checkServerVersion(context, "1.10.0")) || "years".equals(albumListType)) {
					result = service.getAlbumList(albumListType, albumListExtra, size, 0, refresh, context, this);
					if(result.getChildrenSize() == 0 && "genres".equals(albumListType)) {
						SelectDirectoryFragment.this.albumListType = "genres-songs";
						result = service.getSongsByGenre(albumListExtra, size, 0, context, this);
					}
				} else if("genres".equals(albumListType) || "genres-songs".equals(albumListType)) {
					result = service.getSongsByGenre(albumListExtra, size, 0, context, this);
				} else if(albumListType.indexOf(MainFragment.SONGS_LIST_PREFIX) != -1) {
					result = service.getSongList(albumListType, size, 0, context, this);
				} else {
					result = service.getAlbumList(albumListType, size, 0, refresh, context, this);
				}
				return result;
			}
		}.execute();
	}

	private abstract class LoadTask extends TabBackgroundTask<Pair<MusicDirectory, Boolean>> {
		private boolean refresh;

		public LoadTask(boolean refresh) {
			super(SelectDirectoryFragment.this);
			this.refresh = refresh;

			currentTask = this;
		}

		protected abstract MusicDirectory load(MusicService service) throws Exception;

		@Override
		protected Pair<MusicDirectory, Boolean> doInBackground() throws Throwable {
		  MusicService musicService = MusicServiceFactory.getMusicService(context);
			MusicDirectory dir = load(musicService);
			licenseValid = musicService.isLicenseValid(context, this);

			albums = dir.getChildren(true, false);
			entries = dir.getChildren();

			// This isn't really an artist if no albums on it!
			if(albums.size() == 0) {
				artist = false;
			}

			// If artist, we want to load the artist info to use later
			if(artist && ServerInfo.hasArtistInfo(context)  && !Util.isOffline(context)) {
				try {
					String artistId;
					if(id.indexOf(';') == -1) {
						artistId = id;
					} else {
						artistId = id.substring(0, id.indexOf(';'));
					}

					artistInfo = musicService.getArtistInfo(artistId, refresh, false, context, this);

					if(artistInfo == null) {
						artistInfoDelayed = artistId;
					}
				} catch(Exception e) {
					Log.w(TAG, "Failed to get Artist Info even though it should be supported");
				}
			}

			return new Pair<>(dir, licenseValid);
		}

		@Override
		protected void done(Pair<MusicDirectory, Boolean> result) {
			finishLoading();
			currentTask = null;
		}

		@Override
		public void updateCache(int changeCode) {
			if(entryGridAdapter != null && changeCode == CachedMusicService.CACHE_UPDATE_LIST) {
				entryGridAdapter.notifyDataSetChanged();
			} else if(changeCode == CachedMusicService.CACHE_UPDATE_METADATA) {
				if(coverArtView != null && coverArtRep != null && !Util.equals(coverArtRep.getCoverArt(), coverArtId)) {
					synchronized (coverArtRep) {
						if (updateCoverArtTask != null && updateCoverArtTask.isRunning()) {
							updateCoverArtTask.cancel();
						}
						updateCoverArtTask = getImageLoader().loadImage(coverArtView, coverArtRep, false, true);
						coverArtId = coverArtRep.getCoverArt();
					}
				}
			}
		}
	}

	@Override
	public SectionAdapter<Entry> getCurrentAdapter() {
		return entryGridAdapter;
	}

	@Override
	public GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final GridLayoutManager gridLayoutManager) {
		return new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				int viewType = entryGridAdapter.getItemViewType(position);
				if(viewType == EntryGridAdapter.VIEW_TYPE_SONG || viewType == EntryGridAdapter.VIEW_TYPE_HEADER || viewType == EntryInfiniteGridAdapter.VIEW_TYPE_LOADING) {
					return gridLayoutManager.getSpanCount();
				} else {
					return 1;
				}
			}
		};
	}

    private void finishLoading() {
		boolean validData = !entries.isEmpty() || !albums.isEmpty();
		if(!validData) {
			setEmpty(true);
		}

		if(validData) {
			recyclerView.setVisibility(View.VISIBLE);
		}

		if(albumListType == null || "starred".equals(albumListType)) {
			entryGridAdapter = new EntryGridAdapter(context, entries, getImageLoader(), largeAlbums);
			entryGridAdapter.setRemoveFromPlaylist(playlistId != null);
		} else {
			if("alphabeticalByName".equals(albumListType)) {
				entryGridAdapter = new AlphabeticalAlbumAdapter(context, entries, getImageLoader(), largeAlbums);
			} else if("highest".equals(albumListType)) {
				entryGridAdapter = new TopRatedAlbumAdapter(context, entries, getImageLoader(), largeAlbums);
			} else {
				entryGridAdapter = new EntryInfiniteGridAdapter(context, entries, getImageLoader(), largeAlbums);
			}

			// Setup infinite loading based on scrolling
			final EntryInfiniteGridAdapter infiniteGridAdapter = (EntryInfiniteGridAdapter) entryGridAdapter;
			infiniteGridAdapter.setData(albumListType, albumListExtra, albumListSize);

			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
					super.onScrollStateChanged(recyclerView, newState);
				}

				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);

					RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
					int totalItemCount = layoutManager.getItemCount();
					int lastVisibleItem;
					if(layoutManager instanceof GridLayoutManager) {
						lastVisibleItem = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
					} else if(layoutManager instanceof LinearLayoutManager) {
						lastVisibleItem = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
					} else {
						return;
					}

					if(totalItemCount > 0 && lastVisibleItem >= totalItemCount - 2) {
						infiniteGridAdapter.loadMore();
					}
				}
			});
		}
		entryGridAdapter.setOnItemClickedListener(this);
		// Always show artist if this is not a artist we are viewing
		if(!artist) {
			entryGridAdapter.setShowArtist(true);
		}
		if(topTracks || showAll) {
			entryGridAdapter.setShowAlbum(true);
		}

		// Show header if not album list type and not root and not artist
		// For Subsonic 5.1+ display a header for artists with getArtistInfo data if it exists
		boolean addedHeader = false;
		if(albumListType == null && (!artist || artistInfo != null || artistInfoDelayed != null) && (share == null || entries.size() != albums.size())) {
			View header = createHeader();

			if(header != null) {
				if (artistInfoDelayed != null) {
					final View finalHeader = header.findViewById(R.id.select_album_header);
					final View headerProgress = header.findViewById(R.id.header_progress);

					finalHeader.setVisibility(View.INVISIBLE);
					headerProgress.setVisibility(View.VISIBLE);

					new SilentBackgroundTask<Void>(context) {
						@Override
						protected Void doInBackground() throws Throwable {
							MusicService musicService = MusicServiceFactory.getMusicService(context);
							artistInfo = musicService.getArtistInfo(artistInfoDelayed, false, true, context, this);

							return null;
						}

						@Override
						protected void done(Void result) {
							setupCoverArt(finalHeader);
							setupTextDisplay(finalHeader);
							setupButtonEvents(finalHeader);

							finalHeader.setVisibility(View.VISIBLE);
							headerProgress.setVisibility(View.GONE);
						}
					}.execute();
				}

				entryGridAdapter.setHeader(header);
				addedHeader = true;
			}
		}

		int scrollToPosition = -1;
		if(lookupEntry != null) {
			for(int i = 0; i < entries.size(); i++) {
				if(lookupEntry.equals(entries.get(i).getTitle())) {
					scrollToPosition = i;
					entryGridAdapter.addSelected(entries.get(i));
					lookupEntry = null;
					break;
				}
			}
		}

		recyclerView.setAdapter(entryGridAdapter);
		fastScroller.attachRecyclerView(recyclerView);
		context.supportInvalidateOptionsMenu();

		if(scrollToPosition != -1) {
			recyclerView.scrollToPosition(scrollToPosition + (addedHeader ? 1 : 0));
		}

		Bundle args = getArguments();
		boolean playAll = args.getBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
		if (playAll && !restoredInstance) {
			playAll(args.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, false), false, false);
		}
	}

	@Override
	protected void playNow(final boolean shuffle, final boolean append, final boolean playNext) {
		List<Entry> songs = getSelectedEntries();
		if(!songs.isEmpty()) {
			download(songs, append, false, !append, playNext, shuffle);
			entryGridAdapter.clearSelected();
		} else {
			playAll(shuffle, append, playNext);
		}
	}
	private void playAll(final boolean shuffle, final boolean append, final boolean playNext) {
		boolean hasSubFolders = albums != null && !albums.isEmpty();

		if (hasSubFolders && (id != null || share != null || "starred".equals(albumListType))) {
			downloadRecursively(id, false, append, !append, shuffle, false, playNext);
		} else if(hasSubFolders && albumListType != null) {
			downloadRecursively(albums, shuffle, append, playNext);
		} else {
			download(entries, append, false, !append, playNext, shuffle);
		}
	}

	private List<Integer> getSelectedIndexes() {
		List<Entry> selected = entryGridAdapter.getSelected();
		List<Integer> indexes = new ArrayList<Integer>();

		for(Entry entry: selected) {
			indexes.add(entries.indexOf(entry));
		}

		return indexes;
	}

	@Override
	protected void executeOnValid(RecursiveLoader onValid) {
		checkLicenseAndTrialPeriod(onValid);
	}

	@Override
	protected void downloadBackground(final boolean save) {
		List<Entry> songs = getSelectedEntries();
		if(playlistId != null) {
			songs = entries;
		}

		if(songs.isEmpty()) {
			// Get both songs and albums
			downloadRecursively(id, save, false, false, false, true);
		} else {
			downloadBackground(save, songs);
		}
	}
	@Override
	protected void downloadBackground(final boolean save, final List<Entry> entries) {
		if (getDownloadService() == null) {
			return;
		}

		warnIfStorageUnavailable();
		RecursiveLoader onValid = new RecursiveLoader(context) {
			@Override
			protected Boolean doInBackground() throws Throwable {
				getSongsRecursively(entries, true);
				getDownloadService().downloadBackground(songs, save);
				return null;
			}

			@Override
			protected void done(Boolean result) {
				Util.toast(context, context.getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
			}
		};

		checkLicenseAndTrialPeriod(onValid);
	}

	@Override
	protected void download(List<Entry> entries, boolean append, boolean save, boolean autoplay, boolean playNext, boolean shuffle) {
		download(entries, append, save, autoplay, playNext, shuffle, playlistName, playlistId);
	}

	@Override
	protected void delete() {
		List<Entry> songs = getSelectedEntries();
		if(songs.isEmpty()) {
			for(Entry entry: entries) {
				if(entry.isDirectory()) {
					deleteRecursively(entry);
				} else {
					songs.add(entry);
				}
			}
		}
		if (getDownloadService() != null) {
			getDownloadService().delete(songs);
		}
	}

	public void removeFromPlaylist(final String id, final String name, final List<Integer> indexes) {
		new LoadingTask<Void>(context, true) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.removeFromPlaylist(id, indexes, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				for(Integer index: indexes) {
					entryGridAdapter.removeAt(index);
				}
				Util.toast(context, context.getResources().getString(R.string.removed_playlist, String.valueOf(indexes.size()), name));
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.updated_playlist_error, name) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}
	
	public void downloadAllPodcastEpisodes() {
		new LoadingTask<Void>(context, true) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(context);

				for(int i = 0; i < entries.size(); i++) {
					PodcastEpisode episode = (PodcastEpisode) entries.get(i);
					if("skipped".equals(episode.getStatus())) {
						musicService.downloadPodcastEpisode(episode.getEpisodeId(), context, null);
					}
				}
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getString(R.string.select_podcasts_downloading, podcastName));
			}

			@Override
			protected void error(Throwable error) {
				Util.toast(context, getErrorMessage(error), false);
			}
		}.execute();
	}

	@Override
	protected void toggleSelectedStarred() {
		UpdateHelper.OnStarChange onStarChange = null;
		if(albumListType != null && "starred".equals(albumListType)) {
			onStarChange = new UpdateHelper.OnStarChange() {
				@Override
				public void starChange(boolean starred) {

				}

				@Override
				public void starCommited(boolean starred) {
					if(!starred) {
						for (Entry entry : entries) {
							entryGridAdapter.removeItem(entry);
						}
					}
				}
			};
		}

		UpdateHelper.toggleStarred(context, getSelectedEntries(), onStarChange);
	}

	private void checkLicenseAndTrialPeriod(LoadingTask onValid) {
		if (licenseValid) {
			onValid.execute();
			return;
		}

		int trialDaysLeft = Util.getRemainingTrialDays(context);
		Log.i(TAG, trialDaysLeft + " trial days left.");

		if (trialDaysLeft == 0) {
			showDonationDialog(trialDaysLeft, null);
		} else if (trialDaysLeft < Constants.FREE_TRIAL_DAYS / 2) {
			showDonationDialog(trialDaysLeft, onValid);
		} else {
			Util.toast(context, context.getResources().getString(R.string.select_album_not_licensed, trialDaysLeft));
			onValid.execute();
		}
	}

	private void showDonationDialog(int trialDaysLeft, final LoadingTask onValid) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(android.R.drawable.ic_dialog_info);

		if (trialDaysLeft == 0) {
			builder.setTitle(R.string.select_album_donate_dialog_0_trial_days_left);
		} else {
			builder.setTitle(context.getResources().getQuantityString(R.plurals.select_album_donate_dialog_n_trial_days_left,
															  trialDaysLeft, trialDaysLeft));
		}

		builder.setMessage(R.string.select_album_donate_dialog_message);

		builder.setPositiveButton(R.string.select_album_donate_dialog_now,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL)));
				}
			});

		builder.setNegativeButton(R.string.select_album_donate_dialog_later,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
					if (onValid != null) {
						onValid.execute();
					}
				}
			});

		builder.create().show();
	}

	private void showTopTracks() {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle(getArguments());
		args.putBoolean(Constants.INTENT_EXTRA_TOP_TRACKS, true);
		fragment.setArguments(args);

		replaceFragment(fragment, true);
	}

	private void setShowAll() {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle(getArguments());
		args.putBoolean(Constants.INTENT_EXTRA_SHOW_ALL, true);
		fragment.setArguments(args);

		replaceFragment(fragment, true);
	}

	private void showSimilarArtists(String artistId) {
		SubsonicFragment fragment = new SimilarArtistFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ARTIST, artistId);
		fragment.setArguments(args);

		replaceFragment(fragment, true);
	}

	private void startArtistRadio(final String artistId) {
		new LoadingTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				DownloadService downloadService = getDownloadService();
				downloadService.clear();
				downloadService.setArtistRadio(artistId);
				return null;
			}

			@Override
			protected void done(Void result) {
				context.openNowPlaying();
			}
		}.execute();
	}

	private View createHeader() {
		View header = LayoutInflater.from(context).inflate(R.layout.select_album_header, null, false);

		setupCoverArt(header);
		setupTextDisplay(header);
		setupButtonEvents(header);

		return header;
	}

	private void setupCoverArt(View header) {
		setupCoverArtImpl((RecyclingImageView) header.findViewById(R.id.select_album_art));
	}
	private void setupCoverArtImpl(RecyclingImageView coverArtView) {
		final ImageLoader imageLoader = getImageLoader();

		// Try a few times to get a random cover art
		if(artistInfo != null) {
			final String url = artistInfo.getImageUrl();
			coverArtView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (url == null) {
						return;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					ImageView fullScreenView = new ImageView(context);
					imageLoader.loadImage(fullScreenView, url, true);
					builder.setCancelable(true);

					AlertDialog imageDialog = builder.create();
					// Set view here with unecessary 0's to remove top/bottom border
					imageDialog.setView(fullScreenView, 0, 0, 0, 0);
					imageDialog.show();
				}
			});
			imageLoader.loadImage(coverArtView, url, false);
		} else if(entries.size() > 0) {
			coverArtRep = null;
			this.coverArtView = coverArtView;
			for (int i = 0; (i < 3) && (coverArtRep == null || coverArtRep.getCoverArt() == null); i++) {
				coverArtRep = entries.get(random.nextInt(entries.size()));
			}

			coverArtView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (coverArtRep == null || coverArtRep.getCoverArt() == null) {
						return;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					ImageView fullScreenView = new ImageView(context);
					imageLoader.loadImage(fullScreenView, coverArtRep, true, true);
					builder.setCancelable(true);

					AlertDialog imageDialog = builder.create();
					// Set view here with unecessary 0's to remove top/bottom border
					imageDialog.setView(fullScreenView, 0, 0, 0, 0);
					imageDialog.show();
				}
			});
			synchronized (coverArtRep) {
				coverArtId = coverArtRep.getCoverArt();
				updateCoverArtTask = imageLoader.loadImage(coverArtView, coverArtRep, false, true);
			}
		}

		coverArtView.setOnInvalidated(new RecyclingImageView.OnInvalidated() {
			@Override
			public void onInvalidated(RecyclingImageView imageView) {
				setupCoverArtImpl(imageView);
			}
		});
	}
	private void setupTextDisplay(final View header) {
		final TextView titleView = (TextView) header.findViewById(R.id.select_album_title);
		if(playlistName != null) {
			titleView.setText(playlistName);
		} else if(podcastName != null) {
			titleView.setText(podcastName);
			titleView.setPadding(0, 6, 4, 8);
		} else if(name != null) {
			titleView.setText(name);

			if(artistInfo != null) {
				titleView.setPadding(0, 6, 4, 8);
			}
		} else if(share != null) {
			titleView.setVisibility(View.GONE);
		}

		int songCount = 0;

		Set<String> artists = new HashSet<String>();
		Set<Integer> years = new HashSet<Integer>();
		Integer totalDuration = 0;
		for (Entry entry : entries) {
			if (!entry.isDirectory()) {
				songCount++;
				if (entry.getArtist() != null) {
					artists.add(entry.getArtist());
				}
				if(entry.getYear() != null) {
					years.add(entry.getYear());
				}
				Integer duration = entry.getDuration();
				if(duration != null) {
					totalDuration += duration;
				}
			}
		}

		final TextView artistView = (TextView) header.findViewById(R.id.select_album_artist);
		if(podcastDescription != null || artistInfo != null) {
			artistView.setVisibility(View.VISIBLE);
			String text = podcastDescription != null ? podcastDescription : artistInfo.getBiography();
			Spanned spanned = null;
			if(text != null) {
				spanned = Html.fromHtml(text);
			}
			artistView.setText(spanned);
			artistView.setSingleLine(false);
			final int minLines = context.getResources().getInteger(R.integer.TextDescriptionLength);
			artistView.setLines(minLines);
			artistView.setTextAppearance(context, android.R.style.TextAppearance_Small);

			final Spanned spannedText = spanned;
			artistView.setOnClickListener(new View.OnClickListener() {
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void onClick(View v) {
					if(artistView.getMaxLines() == minLines) {
						// Use LeadingMarginSpan2 to try to make text flow around image
						Display display = context.getWindowManager().getDefaultDisplay();
						ImageView coverArtView = (ImageView) header.findViewById(R.id.select_album_art);
						coverArtView.measure(display.getWidth(), display.getHeight());

						int height, width;
						ViewGroup.MarginLayoutParams vlp = (ViewGroup.MarginLayoutParams) coverArtView.getLayoutParams();
						if(coverArtView.getDrawable() != null) {
							height = coverArtView.getMeasuredHeight() + coverArtView.getPaddingBottom();
							width = coverArtView.getWidth() + coverArtView.getPaddingRight();
						} else {
							height = coverArtView.getHeight();
							width = coverArtView.getWidth() + coverArtView.getPaddingRight();
						}
						float textLineHeight = artistView.getPaint().getTextSize();
						int lines = (int) Math.ceil(height / textLineHeight);

						SpannableString ss = new SpannableString(spannedText);
						ss.setSpan(new MyLeadingMarginSpan2(lines, width), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

						View linearLayout = header.findViewById(R.id.select_album_text_layout);
						RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) linearLayout.getLayoutParams();
						int[]rules = params.getRules();
						rules[RelativeLayout.RIGHT_OF] = 0;
						params.leftMargin = vlp.rightMargin;

						artistView.setText(ss);
						artistView.setMaxLines(100);

						vlp = (ViewGroup.MarginLayoutParams) titleView.getLayoutParams();
						vlp.leftMargin = width;
					} else {
						artistView.setMaxLines(minLines);
					}
				}
			});
			artistView.setMovementMethod(LinkMovementMethod.getInstance());
		} else if(topTracks) {
			artistView.setText(R.string.menu_top_tracks);
			artistView.setVisibility(View.VISIBLE);
		} else if(showAll) {
			artistView.setText(R.string.menu_show_all);
			artistView.setVisibility(View.VISIBLE);
		} else if (artists.size() == 1) {
			String artistText = artists.iterator().next();
			if(years.size() == 1) {
				artistText += " - " + years.iterator().next();
			}
			artistView.setText(artistText);
			artistView.setVisibility(View.VISIBLE);
		} else {
			artistView.setVisibility(View.GONE);
		}

		TextView songCountView = (TextView) header.findViewById(R.id.select_album_song_count);
		TextView songLengthView = (TextView) header.findViewById(R.id.select_album_song_length);
		if(podcastDescription != null || artistInfo != null) {
			songCountView.setVisibility(View.GONE);
			songLengthView.setVisibility(View.GONE);
		} else {
			String s = context.getResources().getQuantityString(R.plurals.select_album_n_songs, songCount, songCount);
			songCountView.setText(s.toUpperCase());
			songLengthView.setText(Util.formatDuration(totalDuration));
		}
	}
	private void setupButtonEvents(View header) {
		ImageView shareButton = (ImageView) header.findViewById(R.id.select_album_share);
		if(share != null || podcastId != null || !Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_MENU_SHARED, true) || Util.isOffline(context) || !UserUtil.canShare() || artistInfo != null) {
			shareButton.setVisibility(View.GONE);
		} else {
			shareButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					createShare(SelectDirectoryFragment.this.entries);
				}
			});
		}

		final ImageButton starButton = (ImageButton) header.findViewById(R.id.select_album_star);
		if(directory != null && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_MENU_STAR, true) && artistInfo == null) {
			if(directory.isStarred()) {
				starButton.setImageDrawable(DrawableTint.getTintedDrawable(context, R.drawable.ic_toggle_star));
			} else {
				starButton.setImageResource(DrawableTint.getDrawableRes(context, R.attr.star_outline));
			}
			starButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					UpdateHelper.toggleStarred(context, directory, new UpdateHelper.OnStarChange() {
						@Override
						public void starChange(boolean starred) {
							if (directory.isStarred()) {
								starButton.setImageResource(DrawableTint.getDrawableRes(context, R.attr.star_outline));
								starButton.setImageDrawable(DrawableTint.getTintedDrawable(context, R.drawable.ic_toggle_star));
							} else {
								starButton.setImageResource(DrawableTint.getDrawableRes(context, R.attr.star_outline));
							}
						}

						@Override
						public void starCommited(boolean starred) {

						}
					});
				}
			});
		} else {
			starButton.setVisibility(View.GONE);
		}

		View ratingBarWrapper = header.findViewById(R.id.select_album_rate_wrapper);
		final RatingBar ratingBar = (RatingBar) header.findViewById(R.id.select_album_rate);
		if(directory != null && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_MENU_RATING, true) && !Util.isOffline(context)  && artistInfo == null) {
			ratingBar.setRating(directory.getRating());
			ratingBarWrapper.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					UpdateHelper.setRating(context, directory, new UpdateHelper.OnRatingChange() {
						@Override
						public void ratingChange(int rating) {
							ratingBar.setRating(directory.getRating());
						}
					});
				}
			});
		} else {
			ratingBar.setVisibility(View.GONE);
		}
	}
}
