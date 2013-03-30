package github.daneren2005.dsub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.CacheCleaner;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.PlaylistAdapter;
import java.util.List;

public class SelectPlaylistFragment extends LibraryFunctionsFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectPlaylistFragment.class.getSimpleName();

	private ListView list;
	private View emptyTextView;
	private PlaylistAdapter playlistAdapter;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.select_playlist, container, false);

		list = (ListView) rootView.findViewById(R.id.select_playlist_list);
		emptyTextView = rootView.findViewById(R.id.select_playlist_empty);
		list.setOnItemClickListener(this);
		registerForContextMenu(list);
		load(false);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.select_playlist, menu);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();		
		if (Util.isOffline(context)) {
			inflater.inflate(R.menu.select_playlist_context_offline, menu);
		}
		else {
			inflater.inflate(R.menu.select_playlist_context, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Playlist playlist = (Playlist) list.getItemAtPosition(info.position);

		Intent intent;
		/*switch (menuItem.getItemId()) {
			case R.id.playlist_menu_download:
				downloadPlaylist(playlist.getId(), playlist.getName(), false, true, false, false, true);
				break;
			case R.id.playlist_menu_pin:
				downloadPlaylist(playlist.getId(), playlist.getName(), true, true, false, false, true);
				break;
			case R.id.playlist_menu_play_now:
				intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
				break;
			case R.id.playlist_menu_play_shuffled:
				intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
				Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
				break;
			case R.id.playlist_menu_delete:
				deletePlaylist(playlist);
				break;
			case R.id.playlist_info:
				displayPlaylistInfo(playlist);
				break;
			case R.id.playlist_update_info:
				updatePlaylistInfo(playlist);
				break;
			default:
				return super.onContextItemSelected(menuItem);
		}*/
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Playlist playlist = (Playlist) parent.getItemAtPosition(position);

		SubsonicTabFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
		fragment.setArguments(args);

		final FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.select_playlist_layout, fragment);
		trans.addToBackStack(null);
		trans.commit();
	}

	@Override
	protected void refresh() {
		load(true);
	}

	private void load(final boolean refresh) {
		BackgroundTask<List<Playlist>> task = new TabBackgroundTask<List<Playlist>>(this) {
			@Override
			protected List<Playlist> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				List<Playlist> playlists = musicService.getPlaylists(refresh, context, this);
				if(!Util.isOffline(context) && refresh) {
					new CacheCleaner(context, getDownloadService()).cleanPlaylists(playlists);
				}
				return playlists;
			}

			@Override
			protected void done(List<Playlist> result) {
				list.setAdapter(playlistAdapter = new PlaylistAdapter(context, result));
				emptyTextView.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
			}
		};
		task.execute();
	}
}
