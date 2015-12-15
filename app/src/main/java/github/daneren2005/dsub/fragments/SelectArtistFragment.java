package github.daneren2005.dsub.fragments;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.ArtistAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectArtistFragment extends SelectRecyclerFragment<Artist> implements ArtistAdapter.OnMusicFolderChanged {
	private static final String TAG = SelectArtistFragment.class.getSimpleName();
	private static final int MENU_GROUP_MUSIC_FOLDER = 10;

	private List<MusicFolder> musicFolders = null;
	private List<MusicDirectory.Entry> entries;
	private String groupId;
	private String groupName;

	public SelectArtistFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			musicFolders = (List<MusicFolder>) bundle.getSerializable(Constants.FRAGMENT_LIST2);
		}
		artist = true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST2, (Serializable) musicFolders);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		Bundle args = getArguments();
		if(args != null) {
			groupId = args.getString(Constants.INTENT_EXTRA_NAME_ID);
			groupName = args.getString(Constants.INTENT_EXTRA_NAME_NAME);

			if(groupName != null) {
				setTitle(groupName);
				context.invalidateOptionsMenu();
			}
		}

		super.onCreateView(inflater, container, bundle);

		return rootView;
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Artist> updateView, Artist item) {
		onCreateContextMenuSupport(menu, menuInflater, updateView, item);
		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Artist> updateView, Artist item) {
		return onContextItemSelected(menuItem, item);
	}

	@Override
	public void onItemClicked(UpdateView<Artist> updateView, Artist artist) {
		SubsonicFragment fragment;
		if((Util.isFirstLevelArtist(context) || Util.isOffline(context) || Util.isTagBrowsing(context)) || "root".equals(artist.getId()) || groupId != null) {
			fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());

			if ("root".equals(artist.getId())) {
				args.putSerializable(Constants.FRAGMENT_LIST, (Serializable) entries);
			}
			if(ServerInfo.checkServerVersion(context, "1.13") && !Util.isOffline(context)) {
				args.putSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY, new MusicDirectory.Entry(artist));
			}
			args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);

			fragment.setArguments(args);
		} else {
			fragment = new SelectArtistFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
			if(ServerInfo.checkServerVersion(context, "1.13") && !Util.isOffline(context)) {
				args.putSerializable(Constants.INTENT_EXTRA_NAME_DIRECTORY, new MusicDirectory.Entry(artist));
			}

			fragment.setArguments(args);
		}

		replaceFragment(fragment);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		super.onCreateOptionsMenu(menu, menuInflater);

		if(Util.isOffline(context) || Util.isTagBrowsing(context) || groupId != null) {
			menu.removeItem(R.id.menu_first_level_artist);
		} else {
			if (Util.isFirstLevelArtist(context)) {
				menu.findItem(R.id.menu_first_level_artist).setChecked(true);
			}
		}
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.select_artist;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_first_level_artist:
				toggleFirstLevelArtist();
				break;
		}

		return false;
	}

	@Override
	public SectionAdapter getAdapter(List<Artist> objects) {
		return new ArtistAdapter(context, objects, musicFolders, this, this);
	}

	@Override
	public List<Artist> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		List<Artist> artists;
		if(groupId == null) {
			if (!Util.isOffline(context) && !Util.isTagBrowsing(context)) {
				musicFolders = musicService.getMusicFolders(refresh, context, listener);

				// Hide folders option if there is only one
				if (musicFolders.size() == 1) {
					musicFolders = null;
					Util.setSelectedMusicFolderId(context, null);
				}
			} else {
				musicFolders = null;
			}
			String musicFolderId = Util.getSelectedMusicFolderId(context);

			Indexes indexes = musicService.getIndexes(musicFolderId, refresh, context, listener);
			indexes.sortChildren(context);
			artists = new ArrayList<>(indexes.getShortcuts().size() + indexes.getArtists().size());
			artists.addAll(indexes.getShortcuts());
			artists.addAll(indexes.getArtists());
			entries = indexes.getEntries();
		} else {
			artists = new ArrayList<>();
			MusicDirectory dir = musicService.getMusicDirectory(groupId, groupName, refresh, context, listener);
			for(MusicDirectory.Entry entry: dir.getChildren(true, false)) {
				Artist artist = new Artist();
				artist.setId(entry.getId());
				artist.setName(entry.getTitle());
				artist.setStarred(entry.isStarred());
				artists.add(artist);
			}

			entries = new ArrayList<>();
			entries.addAll(dir.getChildren(false, true));
			if(!entries.isEmpty()) {
				Artist root = new Artist();
				root.setId("root");
				root.setName("Root");
				root.setIndex("#");
				artists.add(root);
			}

			Indexes indexes = new Indexes(0, artists, new ArrayList<Artist>());
			indexes.sortChildren(context);
		}
		
		return artists;
	}

	@Override
	public int getTitleResource() {
		return groupId == null ? R.string.button_bar_browse : 0;
	}

	@Override
	public void setEmpty(boolean empty) {
		super.setEmpty(empty);

		if(empty && !Util.isOffline(context)) {
			objects.clear();
			recyclerView.setAdapter(new ArtistAdapter(context, objects, musicFolders, this, this));
			recyclerView.setVisibility(View.VISIBLE);

			View view = rootView.findViewById(R.id.tab_progress);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
			params.height = 0;
			params.weight = 5;
			view.setLayoutParams(params);
		}
	}

	private void toggleFirstLevelArtist() {
		Util.toggleFirstLevelArtist(context);
		context.invalidateOptionsMenu();
	}

	@Override
	public void onMusicFolderChanged(MusicFolder selectedFolder) {
		String startMusicFolderId = Util.getSelectedMusicFolderId(context);
		String musicFolderId = selectedFolder == null ? null : selectedFolder.getId();

		if(!Util.equals(startMusicFolderId, musicFolderId)) {
			Util.setSelectedMusicFolderId(context, musicFolderId);
			context.invalidate();
		}
	}
}
