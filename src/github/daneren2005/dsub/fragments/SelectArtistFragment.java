package github.daneren2005.dsub.fragments;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.ArtistAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectArtistFragment extends SelectListFragment<Artist> {
	private static final String TAG = SelectArtistFragment.class.getSimpleName();
	private static final int MENU_GROUP_MUSIC_FOLDER = 10;

	private View folderButtonParent;
	private View folderButton;
	private TextView folderName;
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

		folderButton = null;
		super.onCreateView(inflater, container, bundle);
		
		if("4.4.2".equals(Build.VERSION.RELEASE)) {
			listView.setFastScrollAlwaysVisible(true);
		}

		if(objects != null && currentTask == null) {
			if (Util.isOffline(context) || Util.isTagBrowsing(context) || groupId != null) {
				folderButton.setVisibility(View.GONE);
			}
			setMusicFolders();
		}

		return rootView;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Object entry = listView.getItemAtPosition(info.position);

		if (entry instanceof Artist) {
			onCreateContextMenu(menu, view, menuInfo, entry);
		} else if (info.position == 0) {
			String musicFolderId = Util.getSelectedMusicFolderId(context);
			MenuItem menuItem = menu.add(MENU_GROUP_MUSIC_FOLDER, -1, 0, R.string.select_artist_all_folders);
			if (musicFolderId == null) {
				menuItem.setChecked(true);
			}
			if (musicFolders != null) {
				for (int i = 0; i < musicFolders.size(); i++) {
					MusicFolder musicFolder = musicFolders.get(i);
					menuItem = menu.add(MENU_GROUP_MUSIC_FOLDER, i, i + 1, musicFolder.getName());
					if (musicFolder.getId().equals(musicFolderId)) {
						menuItem.setChecked(true);
					}
				}
			}
			menu.setGroupCheckable(MENU_GROUP_MUSIC_FOLDER, true, true);
		}

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Artist artist = (Artist) listView.getItemAtPosition(info.position);

		if (artist != null) {
			return onContextItemSelected(menuItem, artist);
		} else if (info.position == 0) {
			MusicFolder selectedFolder = menuItem.getItemId() == -1 ? null : musicFolders.get(menuItem.getItemId());
			String musicFolderId = selectedFolder == null ? null : selectedFolder.getId();
			String musicFolderName = selectedFolder == null ? context.getString(R.string.select_artist_all_folders)
															: selectedFolder.getName();
			Util.setSelectedMusicFolderId(context, musicFolderId);
			folderName.setText(musicFolderName);
			context.invalidate();
		}

		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (view == folderButtonParent) {
			selectFolder();
		} else {
			Artist artist = (Artist) parent.getItemAtPosition(position);

			SubsonicFragment fragment;
			if(Util.isFirstLevelArtist(context) || "root".equals(artist.getId()) || groupId != null) {
				fragment = new SelectDirectoryFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
				if ("root".equals(artist.getId())) {
					args.putSerializable(Constants.FRAGMENT_LIST, (Serializable) entries);
				}
				args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
				fragment.setArguments(args);
			} else {
				fragment = new SelectArtistFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
				fragment.setArguments(args);
			}

			replaceFragment(fragment);
		}
	}

	@Override
	public void onFinishRefresh() {
		setMusicFolders();
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
	public ArrayAdapter getAdapter(List<Artist> objects) {
		createMusicFolderButton();
		return new ArtistAdapter(context, objects);
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
			}
			String musicFolderId = Util.getSelectedMusicFolderId(context);

			Indexes indexes = musicService.getIndexes(musicFolderId, refresh, context, listener);
			artists = new ArrayList<Artist>(indexes.getShortcuts().size() + indexes.getArtists().size());
			artists.addAll(indexes.getShortcuts());
			artists.addAll(indexes.getArtists());
			entries = indexes.getEntries();
		} else {
			artists = new ArrayList<Artist>();
			MusicDirectory dir = musicService.getMusicDirectory(groupId, groupName, refresh, context, listener);
			for(MusicDirectory.Entry entry: dir.getChildren(true, false)) {
				Artist artist = new Artist();
				artist.setId(entry.getId());
				artist.setName(entry.getTitle());
				artist.setStarred(entry.isStarred());
				artists.add(artist);
			}

			entries = new ArrayList<MusicDirectory.Entry>();
			entries.addAll(dir.getChildren(false, true));
			if(!entries.isEmpty()) {
				Artist root = new Artist();
				root.setId("root");
				root.setName("Root");
				root.setIndex("#");
				artists.add(root);
			}
		}
		
		return artists;
	}

	@Override
	public int getTitleResource() {
		return groupId == null ? R.string.button_bar_browse : 0;
	}

	private void createMusicFolderButton() {
		if(folderButton == null) {
			folderButtonParent = context.getLayoutInflater().inflate(R.layout.select_artist_header, listView, false);
			folderName = (TextView) folderButtonParent.findViewById(R.id.select_artist_folder_2);
			listView.addHeaderView(folderButtonParent);
			folderButton = folderButtonParent.findViewById(R.id.select_artist_folder);
		}

		if (Util.isOffline(context) || Util.isTagBrowsing(context) || musicFolders == null) {
			folderButton.setVisibility(View.GONE);
		} else {
			folderButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void setEmpty(boolean empty) {
		super.setEmpty(empty);

		if(empty && !Util.isOffline(context)) {
			createMusicFolderButton();
			setMusicFolders();

			objects.clear();
			listView.setAdapter(new ArtistAdapter(context, objects));
			listView.setVisibility(View.VISIBLE);

			View view = rootView.findViewById(R.id.tab_progress);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
			params.height = 0;
			params.weight = 5;
			view.setLayoutParams(params);
		}
	}

	private void setMusicFolders() {
		// Display selected music folder
		if (musicFolders != null) {
			String musicFolderId = Util.getSelectedMusicFolderId(context);
			if (musicFolderId == null) {
				folderName.setText(R.string.select_artist_all_folders);
			} else {
				for (MusicFolder musicFolder : musicFolders) {
					if (musicFolder.getId().equals(musicFolderId)) {
						folderName.setText(musicFolder.getName());
						break;
					}
				}
			}
		}
	}

	private void selectFolder() {
		folderButton.showContextMenu();
	}

	private void toggleFirstLevelArtist() {
		Util.toggleFirstLevelArtist(context);
		context.invalidateOptionsMenu();
	}
}
