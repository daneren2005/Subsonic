package github.daneren2005.dsub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
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
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ArtistAdapter;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectArtistFragment extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectArtistFragment.class.getSimpleName();
	private static final int MENU_GROUP_MUSIC_FOLDER = 10;

	private ListView artistList;
	private View folderButtonParent;
	private View folderButton;
	private TextView folderName;
	private List<MusicFolder> musicFolders = null;
	private List<MusicDirectory.Entry> entries;
	private List<Artist> artists;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			artists = (List<Artist>) bundle.getSerializable(Constants.FRAGMENT_LIST);
			musicFolders = (List<MusicFolder>) bundle.getSerializable(Constants.FRAGMENT_LIST2);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) artists);
		outState.putSerializable(Constants.FRAGMENT_LIST2, (Serializable) musicFolders);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		artistList = (ListView) rootView.findViewById(R.id.fragment_list);
		artistList.setOnItemClickListener(this);

		folderButtonParent = inflater.inflate(R.layout.select_artist_header, artistList, false);
		folderName = (TextView) folderButtonParent.findViewById(R.id.select_artist_folder_2);
		artistList.addHeaderView(folderButtonParent);
		folderButton = folderButtonParent.findViewById(R.id.select_artist_folder);

		registerForContextMenu(artistList);
		if(artists == null) {
			if(!primaryFragment) {
				invalidated = true;
			} else {
				refresh(false);
			}
		} else {
			artistList.setAdapter(new ArtistAdapter(context, artists));
			setMusicFolders();
		}

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.select_artist, menu);
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
		if(!primaryFragment) {
			return;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Object entry = artistList.getItemAtPosition(info.position);

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
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(!primaryFragment) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Artist artist = (Artist) artistList.getItemAtPosition(info.position);

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
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
			if("root".equals(artist.getId())) {
				Log.d(TAG, "root");
				args.putSerializable(Constants.FRAGMENT_LIST, (Serializable) entries);
			}
			fragment.setArguments(args);

			replaceFragment(fragment, R.id.fragment_list_layout);
		}
	}

	@Override
	protected void refresh(boolean refresh) {
		load(refresh);
	}

	private void load(final boolean refresh) {
		setTitle(R.string.button_bar_browse);
		
		if (Util.isOffline(context)) {
			folderButton.setVisibility(View.GONE);
		} else {
			folderButton.setVisibility(View.VISIBLE);
		}
		artistList.setVisibility(View.INVISIBLE);

		BackgroundTask<Indexes> task = new TabBackgroundTask<Indexes>(this) {
			@Override
			protected Indexes doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				if (!Util.isOffline(context)) {
					musicFolders = musicService.getMusicFolders(refresh, context, this);
				}
				String musicFolderId = Util.getSelectedMusicFolderId(context);
				return musicService.getIndexes(musicFolderId, refresh, context, this);
			}

			@Override
			protected void done(Indexes result) {
				artists = new ArrayList<Artist>(result.getShortcuts().size() + result.getArtists().size());
				artists.addAll(result.getShortcuts());
				artists.addAll(result.getArtists());
				artistList.setFastScrollEnabled(false);
				artistList.setAdapter(new ArtistAdapter(context, artists));
				artistList.setFastScrollEnabled(true);
				entries = result.getEntries();

				setMusicFolders();
				artistList.setVisibility(View.VISIBLE);
			}
		};
		task.execute();
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
}
