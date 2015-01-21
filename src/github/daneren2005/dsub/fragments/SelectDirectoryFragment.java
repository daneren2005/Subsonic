package github.daneren2005.dsub.fragments;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.AlbumGridAdapter;
import github.daneren2005.dsub.view.EntryAdapter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.AlbumListAdapter;
import github.daneren2005.dsub.view.HeaderGridView;
import github.daneren2005.dsub.view.MyLeadingMarginSpan2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static github.daneren2005.dsub.domain.MusicDirectory.Entry;

public class SelectDirectoryFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectDirectoryFragment.class.getSimpleName();

	private GridView albumList;
	private ListView entryList;
	private Boolean licenseValid;
	private EntryAdapter entryAdapter;
	private List<Entry> albums;
	private List<Entry> entries;
	private boolean albumContext = false;
	private boolean addAlbumHeader = false;
	private LoadTask currentTask;
	ArtistInfo artistInfo;

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
			restoredInstance = true;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) entries);
		outState.putSerializable(Constants.FRAGMENT_LIST2, (Serializable) albums);
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

		rootView = inflater.inflate(R.layout.select_album, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(this);

		entryList = (ListView) rootView.findViewById(R.id.select_album_entries);
		entryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		entryList.setOnItemClickListener(this);
		setupScrollList(entryList);

		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_LARGE_ALBUM_ART, true)) {
			largeAlbums = true;
		}

		if(albumListType == null || "starred".equals(albumListType) || !largeAlbums) {
			albumList = (GridView) inflater.inflate(R.layout.unscrollable_grid_view, entryList, false);
			addAlbumHeader = true;
		} else {
			ViewGroup rootGroup = (ViewGroup) rootView.findViewById(R.id.select_album_layout);
			albumList = (GridView) inflater.inflate(R.layout.grid_view, rootGroup, false);
			rootGroup.removeView(entryList);
			rootGroup.addView(albumList);

			setupScrollList(albumList);
		}
		registerForContextMenu(entryList);
		setupAlbumList();

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(licenseValid == null) {
			menuInflater.inflate(R.menu.empty, menu);
		} else if(albumListType != null && !"starred".equals(albumListType)) {
			menuInflater.inflate(R.menu.select_album_list, menu);
		} else if(artist && !showAll) {
			menuInflater.inflate(R.menu.select_album, menu);

			if(!ServerInfo.isMadsonic(context)) {
				menu.removeItem(R.id.menu_top_tracks);
			}
			if(!ServerInfo.checkServerVersion(context, "1.11")) {
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
			case R.id.menu_play_now:
				playNow(false, false);
				return true;
			case R.id.menu_play_last:
				playNow(false, true);
				return true;
			case R.id.menu_play_next:
				playNow(false, true, true);
				return true;
			case R.id.menu_shuffle:
				playNow(true, false);
				return true;
			case R.id.menu_download:
				downloadBackground(false);
				selectAll(false, false);
				return true;
			case R.id.menu_cache:
				downloadBackground(true);
				selectAll(false, false);
				return true;
			case R.id.menu_delete:
				delete();
				selectAll(false, false);
				return true;
			case R.id.menu_add_playlist:
				if(getSelectedSongs().isEmpty()) {
					selectAll(true, false);
				}
				addToPlaylist(getSelectedSongs());
				return true;
			case R.id.menu_remove_playlist:
				removeFromPlaylist(playlistId, playlistName, getSelectedIndexes());
				return true;
			case R.id.menu_download_all:
				downloadAllPodcastEpisodes();
				return true;
			case R.id.menu_show_all:
				setShowAll();
				return true;
			case R.id.menu_unstar:
				unstarSelected();
				return true;
			case R.id.menu_top_tracks:
				showTopTracks();
				return true;
			case R.id.menu_similar_artists:
				showSimilarArtists(id);
				return true;
		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		Entry entry;
		if(view.getId() == R.id.select_album_entries) {
			if(info.position == 0) {
				return;
			}
			entry = (Entry) entryList.getItemAtPosition(info.position);
			// When List has Grid embedded in header, this is called against the header as well
			if(entry != null) {
				albumContext = false;
			}
		} else {
			entry = (Entry) albumList.getItemAtPosition(info.position);
			albumContext = true;
		}

		// Don't try to display a context menu if error here
		if(entry == null) {
			return;
		}

		onCreateContextMenu(menu, view, menuInfo, entry);
		if(!entry.isVideo() && !Util.isOffline(context) && (playlistId == null || !playlistOwner) && (podcastId == null  || Util.isOffline(context) && podcastId != null)) {
			menu.removeItem(R.id.song_menu_remove_playlist);
		}
		// Remove show artists if parent is not set and if not on a album list
		if((albumListType == null || (entry.getParent() == null && entry.getArtistId() == null)) && !Util.isOffline(context)) {
			menu.removeItem(R.id.album_menu_show_artist);
		}
		if(podcastId != null && !Util.isOffline(context)) {
			if(UserUtil.canPodcast()) {
				String status = ((PodcastEpisode)entry).getStatus();
				if("completed".equals(status)) {
					menu.removeItem(R.id.song_menu_server_download);
				}
			} else {
				menu.removeItem(R.id.song_menu_server_download);
				menu.removeItem(R.id.song_menu_server_delete);
			}
		}

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Object selectedItem;
		int headers = entryList.getHeaderViewsCount();
		if(albumContext) {
			selectedItem = albumList.getItemAtPosition(info.position);
		} else {
			if(info.position == 0) {
				return false;
			}
			selectedItem = entries.get(info.position - headers);
		}

		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_PLAY_NOW_AFTER, false) && menuItem.getItemId() == R.id.song_menu_play_now) {
			List<Entry> songs = new ArrayList<Entry>();
			Iterator it = entries.listIterator(info.position - headers);
			while(it.hasNext()) {
				songs.add((Entry) it.next());
			}

			playNow(songs);
			return true;
		}
		
		if(onContextItemSelected(menuItem, selectedItem)) {
			return true;
		}

		switch (menuItem.getItemId()) {
			case R.id.song_menu_remove_playlist:
				removeFromPlaylist(playlistId, playlistName, Arrays.<Integer>asList(info.position - headers));
				break;
			case R.id.song_menu_server_download:
				downloadPodcastEpisode((PodcastEpisode)selectedItem);
				break;
			case R.id.song_menu_server_delete:
				deletePodcastEpisode((PodcastEpisode)selectedItem);
				break;
		}
		
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position >= 0) {
			Entry entry = (Entry) parent.getItemAtPosition(position);
			if (entry.isDirectory()) {
				SubsonicFragment fragment = new SelectDirectoryFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
				args.putSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY, entry);
				if ("newest".equals(albumListType)) {
					args.putBoolean(Constants.INTENT_EXTRA_REFRESH_LISTINGS, true);
				}
				if(entry.getArtist() == null && entry.getParent() == null) {
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
				
				playNow(Arrays.asList(entry));
			}
		}
	}

	@Override
	protected void refresh(boolean refresh) {
		if(!"root".equals(id)) {
			load(refresh);
		}
	}

	private void load(boolean refresh) {
		if(refreshListing) {
			refresh = true;
		}
		
		if(currentTask != null) {
			currentTask.cancel();
		}
		
		entryList.setVisibility(View.INVISIBLE);
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
			getAlbumList(albumListType, albumListSize);
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
				} else if(id != null && directory == null && dir.getParent() != null) {
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
				root.replaceChildren(songs);
				return root;
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
				return service.getTopTrackSongs(name, 20, context, this);
			}
		}.execute();
	}

	private void getAlbumList(final String albumListType, final int size) {
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
		}

		new LoadTask(true) {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				MusicDirectory result;
				if ("starred".equals(albumListType)) {
					result = service.getStarredList(context, this);
				} else if(("genres".equals(albumListType) && ServerInfo.checkServerVersion(context, "1.10.0")) || "years".equals(albumListType)) {
					result = service.getAlbumList(albumListType, albumListExtra, size, 0, context, this);
					if(result.getChildrenSize() == 0 && "genres".equals(albumListType)) {
						SelectDirectoryFragment.this.albumListType = "genres-songs";
						result = service.getSongsByGenre(albumListExtra, size, 0, context, this);
					}
				} else if("genres".equals(albumListType) || "genres-songs".equals(albumListType)) {
					result = service.getSongsByGenre(albumListExtra, size, 0, context, this);
				} else {
					result = service.getAlbumList(albumListType, size, 0, context, this);
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
			if(largeAlbums) {
				entries = dir.getChildren(false, true);
			} else {
				entries = dir.getChildren();
			}

			// This isn't really an artist if no albums on it!
			if(albums.size() == 0) {
				artist = false;
			}

			// If artist, we want to load the artist info to use later
			if(artist && ServerInfo.checkServerVersion(context, "1.11")  && !Util.isOffline(context)) {
				artistInfo = musicService.getArtistInfo(id, refresh, context, this);
			}
			
			return new Pair<MusicDirectory, Boolean>(dir, licenseValid);
		}

		@Override
		protected void done(Pair<MusicDirectory, Boolean> result) {
			finishLoading();
			currentTask = null;
		}
	}

    private void finishLoading() {
		// Show header if not album list type and not root and not artist
		// For Subsonic 5.1+ display a header for artists with getArtistInfo data if it exists
		View header = null;
		if(albumListType == null && !"root".equals(id) && (!artist || artistInfo != null)) {
			header = createHeader();
			// Only add header to entry list if we aren't going recreate album grid as root anyways
			if(header != null && entryList != null && (!addAlbumHeader || entries.size() > 0)) {
				entryList.addHeaderView(header, null, false);
				header = null;
			}
		}

		// Needs to be added here, GB crashes if you to try to remove the header view before adapter is set
		if(addAlbumHeader) {
			if(entries.size() > 0) {
				entryList.addHeaderView(albumList);
			} else {
				ViewGroup rootGroup = (ViewGroup) rootView.findViewById(R.id.select_album_layout);
				albumList = (GridView) context.getLayoutInflater().inflate(R.layout.grid_view, rootGroup, false);
				rootGroup.removeView(entryList);
				rootGroup.addView(albumList);
				
				setupScrollList(albumList);
				setupAlbumList();

				// This should only not be null for a artist with only albums
				if(header != null) {
					HeaderGridView headerGridView = (HeaderGridView) albumList;
					headerGridView.addHeaderView(header);
				}
			}
			addAlbumHeader = false;
		}

		boolean validData = !entries.isEmpty() || !albums.isEmpty();
		if(!validData) {
			setEmpty(true);
		}
		// Always going to have entries in entryAdapter
		entryAdapter = new EntryAdapter(context, getImageLoader(), entries, (podcastId == null));
		ListAdapter listAdapter = entryAdapter;
		// Song-only genre needs to always be entry list + infinite adapter
		if("genres-songs".equals(albumListType)) {
			ViewGroup rootGroup = (ViewGroup) rootView.findViewById(R.id.select_album_layout);
			if(rootGroup.findViewById(R.id.gridview) != null && largeAlbums) {
				rootGroup.removeView(albumList);
				rootGroup.addView(entryList);
			}

			listAdapter = new AlbumListAdapter(context, entryAdapter, albumListType, albumListExtra, albumListSize);
		} else if(albumListType == null || "starred".equals(albumListType)) {
			// Only set standard album adapter if not album list and largeAlbums is true
			if(largeAlbums) {
				albumList.setAdapter(new AlbumGridAdapter(context, getImageLoader(), albums, !artist));
			}
		} else {
			// If album list, use infinite adapters for either depending on whether or not largeAlbums is true
			if(largeAlbums) {
				albumList.setAdapter(new AlbumListAdapter(context, new AlbumGridAdapter(context, getImageLoader(), albums, true), albumListType, albumListExtra, albumListSize));
			} else {
				listAdapter = new AlbumListAdapter(context, entryAdapter, albumListType, albumListExtra, albumListSize);
			}
		}
		entryList.setAdapter(listAdapter);
		if(validData) {
			entryList.setVisibility(View.VISIBLE);
		}
		context.supportInvalidateOptionsMenu();

		if(lookupEntry != null) {
			for(int i = 0; i < entries.size(); i++) {
				if(lookupEntry.equals(entries.get(i).getTitle())) {
					entryList.setSelection(i + entryList.getHeaderViewsCount());
					lookupEntry = null;
					break;
				}
			}
		}

		Bundle args = getArguments();
		boolean playAll = args.getBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
		if (playAll && !restoredInstance) {
			playAll(args.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, false), false);
		}
	}

	private void setupAlbumList() {
		albumList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Entry entry = (Entry) parent.getItemAtPosition(position);
				SubsonicFragment fragment = new SelectDirectoryFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
				args.putSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY, entry);
				if ("newest".equals(albumListType)) {
					args.putBoolean(Constants.INTENT_EXTRA_REFRESH_LISTINGS, true);
				}
				if(entry.getArtist() == null && entry.getParent() == null) {
					args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
				}
				fragment.setArguments(args);

				replaceFragment(fragment, true);
			}
		});

		registerForContextMenu(entryList);
		registerForContextMenu(albumList);
	}

	private void playNow(final boolean shuffle, final boolean append) {
		playNow(shuffle, append, false);
	}
	private void playNow(final boolean shuffle, final boolean append, final boolean playNext) {
		if(getSelectedSongs().size() > 0) {
			download(append, false, !append, playNext, shuffle);
			selectAll(false, false);
		}
		else {
			playAll(shuffle, append);
		}
	}
	private void playAll(final boolean shuffle, final boolean append) {
		boolean hasSubFolders = false;
		for (int i = 0; i < entryList.getCount(); i++) {
			Entry entry = (Entry) entryList.getItemAtPosition(i);
			if (entry != null && entry.isDirectory()) {
				hasSubFolders = true;
				break;
			}
		}
		if(albums.size() > 0) {
			hasSubFolders = true;
		}

		if (hasSubFolders && (id != null || share != null || "starred".equals(albumListType))) {
			downloadRecursively(id, false, append, !append, shuffle, false);
		} else if(hasSubFolders && albumListType != null) {
			downloadRecursively(albums, shuffle, append);
		} else {
			selectAll(true, false);
			download(append, false, !append, false, shuffle);
			selectAll(false, false);
		}
	}

	private void selectAll(boolean selected, boolean toast) {
		int count = entryList.getCount();
		int selectedCount = 0;
		for (int i = 0; i < count; i++) {
			Entry entry = (Entry) entryList.getItemAtPosition(i);
			if (entry != null && !entry.isDirectory() && !entry.isVideo()) {
				entryList.setItemChecked(i, selected);
				selectedCount++;
			}
		}

		// Display toast: N tracks selected / N tracks unselected
		if (toast) {
			int toastResId = selected ? R.string.select_album_n_selected
									  : R.string.select_album_n_unselected;
			Util.toast(context, context.getString(toastResId, selectedCount));
		}
	}

	private List<Entry> getSelectedSongs() {
		List<Entry> songs = new ArrayList<Entry>(10);
		int count = entryList.getCount();
		for (int i = 0; i < count; i++) {
			if (entryList.isItemChecked(i)) {
				Entry entry = (Entry) entryList.getItemAtPosition(i);
				// Don't try to add directories or 1-starred songs
				if(!entry.isDirectory() && entry.getRating() != 1) {
					songs.add(entry);
				}
			}
		}
		return songs;
	}

	private List<Integer> getSelectedIndexes() {
		List<Integer> indexes = new ArrayList<Integer>();

		int count = entryList.getCount();
		int headers = entryList.getHeaderViewsCount();
		for (int i = 0; i < count; i++) {
			if (entryList.isItemChecked(i)) {
				indexes.add(i - headers);
			}
		}

		return indexes;
	}

	private void download(final boolean append, final boolean save, final boolean autoplay, final boolean playNext, final boolean shuffle) {
		if (getDownloadService() == null) {
			return;
		}

		final List<Entry> songs = getSelectedSongs();
		warnIfStorageUnavailable();
		
		// Conditions for using play now button
		if(!append && !save && autoplay && !playNext && !shuffle) {
			// Call playNow which goes through and tries to use bookmark information
			playNow(songs, playlistName, playlistId);
			return;
		}
		
		LoadingTask<Void> onValid = new LoadingTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				if (!append) {
					getDownloadService().clear();
				}

				getDownloadService().download(songs, save, autoplay, playNext, shuffle);
				if (playlistName != null) {
					getDownloadService().setSuggestedPlaylistName(playlistName, playlistId);
				} else {
					getDownloadService().setSuggestedPlaylistName(null, null);
				}
				return null;
			}

			@Override
			protected void done(Void result) {
				if (autoplay) {
					Util.startActivityWithoutTransition(context, DownloadActivity.class);
				} else if (save) {
					Util.toast(context,
							context.getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
				} else if (append) {
					Util.toast(context,
							context.getResources().getQuantityString(R.plurals.select_album_n_songs_added, songs.size(), songs.size()));
				}
			}
		};

		checkLicenseAndTrialPeriod(onValid);
	}
	private void downloadBackground(final boolean save) {
		if(playlistId != null) {
			selectAll(true, false);
		}

		List<Entry> songs = getSelectedSongs();
		if(songs.isEmpty()) {
			// Get both songs and albums
			downloadRecursively(id, save, false, false, false, true);
		} else {
			downloadBackground(save, songs);
		}
	}
	private void downloadBackground(final boolean save, final List<Entry> songs) {
		if (getDownloadService() == null) {
			return;
		}

		warnIfStorageUnavailable();
		LoadingTask<Void> onValid = new LoadingTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				getDownloadService().downloadBackground(songs, save);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
			}
		};

		checkLicenseAndTrialPeriod(onValid);
	}

	private void delete() {
		List<Entry> songs = getSelectedSongs();
		if(songs.isEmpty()) {
			selectAll(true, false);
			songs = getSelectedSongs();

			// Also delete all directories
			for(Entry album: albums) {
				deleteRecursively(album);
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
				for(int i = indexes.size() - 1; i >= 0; i--) {
					entryList.setItemChecked(indexes.get(i) + 1, false);
					entryAdapter.removeAt(indexes.get(i));
				}
				entryAdapter.notifyDataSetChanged();
				Util.toast(context, context.getResources().getString(R.string.removed_playlist, indexes.size(), name));
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
	
	public void downloadPodcastEpisode(final PodcastEpisode episode) {
		new LoadingTask<Void>(context, true) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.downloadPodcastEpisode(episode.getEpisodeId(), context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getString(R.string.select_podcasts_downloading, episode.getTitle()));
			}

			@Override
			protected void error(Throwable error) {
				Util.toast(context, getErrorMessage(error), false);
			}
		}.execute();
	}
	
	public void deletePodcastEpisode(final PodcastEpisode episode) {
		Util.confirmDialog(context, R.string.common_delete, episode.getTitle(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(context, true) {
					@Override
					protected Void doInBackground() throws Throwable {				
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deletePodcastEpisode(episode.getEpisodeId(), episode.getParent(), null, context);
						if (getDownloadService() != null) {
							List<Entry> episodeList = new ArrayList<Entry>(1);
							episodeList.add(episode);
							getDownloadService().delete(episodeList);
						}
						return null;
					}

					@Override
					protected void done(Void result) {
						entries.remove(episode);
						entryAdapter.notifyDataSetChanged();
					}

					@Override
					protected void error(Throwable error) {
						Log.w(TAG, "Failed to delete podcast episode", error);
						Util.toast(context, getErrorMessage(error), false);
					}
				}.execute();
			}
		});
	}

	public void unstarSelected() {
		List<Entry> selected = getSelectedSongs();
		if(selected.size() == 0) {
			selected = entries;
		}
		if(selected.size() == 0) {
			return;
		}
		final List<Entry> unstar = new ArrayList<Entry>();
		unstar.addAll(selected);

		new LoadingTask<Void>(context, true) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				List<Entry> entries = new ArrayList<Entry>();
				List<Entry> artists = new ArrayList<Entry>();
				List<Entry> albums = new ArrayList<Entry>();
				for(Entry entry: unstar) {
					if(entry.isDirectory()) {
						if(entry.isAlbum()) {
							albums.add(entry);
						} else {
							artists.add(entry);
						}
					} else {
						entries.add(entry);
					}
				}
				musicService.setStarred(entries, artists, albums, false, this, context);

				for(Entry entry: unstar) {
					new EntryInstanceUpdater(entry) {
						@Override
						public void update(Entry found) {
							found.setStarred(false);
						}
					}.execute();
				}

				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getString(R.string.starring_content_unstarred, Integer.toString(unstar.size())));

				for(Entry entry: unstar) {
					entries.remove(entry);
				}
				entryAdapter.notifyDataSetChanged();
				selectAll(false, false);
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.starring_content_error, Integer.toString(unstar.size())) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
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

	private View createHeader() {
		View header = entryList.findViewById(R.id.select_album_header);
		boolean add = false;
		if(header == null) {
			header = LayoutInflater.from(context).inflate(R.layout.select_album_header, entryList, false);
			add = true;
		}

		setupCoverArt(header);
		setupTextDisplay(header);

		if(add) {
			setupButtonEvents(header);
		}

		if(add) {
			return header;
		} else {
			return null;
		}
	}

	private void setupCoverArt(View header) {
		final ImageLoader imageLoader = getImageLoader();
		View coverArtView = header.findViewById(R.id.select_album_art);

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
			Entry coverArt = null;
			for (int i = 0; (i < 3) && (coverArt == null || coverArt.getCoverArt() == null); i++) {
				coverArt = entries.get(random.nextInt(entries.size()));
			}

			final Entry albumRep = coverArt;
			coverArtView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (albumRep.getCoverArt() == null) {
						return;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					ImageView fullScreenView = new ImageView(context);
					imageLoader.loadImage(fullScreenView, albumRep, true, true);
					builder.setCancelable(true);

					AlertDialog imageDialog = builder.create();
					// Set view here with unecessary 0's to remove top/bottom border
					imageDialog.setView(fullScreenView, 0, 0, 0, 0);
					imageDialog.show();
				}
			});
			imageLoader.loadImage(coverArtView, albumRep, false, true);
		}
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
						View coverArtView = header.findViewById(R.id.select_album_art);
						coverArtView.measure(display.getWidth(), display.getHeight());
						ViewGroup.MarginLayoutParams vlp = (ViewGroup.MarginLayoutParams) coverArtView.getLayoutParams();
						int height = coverArtView.getMeasuredHeight() + coverArtView.getPaddingBottom();
						int width = coverArtView.getWidth() + coverArtView.getPaddingRight();
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

						if(albumList instanceof HeaderGridView) {
							HeaderGridView headerGridView = (HeaderGridView) albumList;
							((BaseAdapter) headerGridView.getAdapter()).notifyDataSetChanged();
						}

						vlp = (ViewGroup.MarginLayoutParams) titleView.getLayoutParams();
						vlp.leftMargin = width;
					} else {
						artistView.setMaxLines(minLines);

						if(albumList instanceof HeaderGridView) {
							HeaderGridView headerGridView = (HeaderGridView) albumList;
							((BaseAdapter) headerGridView.getAdapter()).notifyDataSetChanged();
						}
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
		if(directory != null && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_MENU_STAR, true)) {
			starButton.setImageResource(directory.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
			starButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleStarred(directory, new OnStarChange() {
						@Override
						void starChange(boolean starred) {
							starButton.setImageResource(directory.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
						}
					});
				}
			});
		} else {
			starButton.setVisibility(View.GONE);
		}

		View ratingBarWrapper = header.findViewById(R.id.select_album_rate_wrapper);
		final RatingBar ratingBar = (RatingBar) header.findViewById(R.id.select_album_rate);
		if(directory != null && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_MENU_RATING, true) && !Util.isOffline(context)) {
			ratingBar.setRating(directory.getRating());
			ratingBarWrapper.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					setRating(directory, new OnRatingChange() {
						@Override
						void ratingChange(int rating) {
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
