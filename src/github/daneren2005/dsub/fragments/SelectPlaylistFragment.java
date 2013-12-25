package github.daneren2005.dsub.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.CacheCleaner;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.PlaylistAdapter;

import java.io.Serializable;
import java.util.List;

public class SelectPlaylistFragment extends SelectListFragment<Playlist> {
	private static final String TAG = SelectPlaylistFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = context.getMenuInflater();		
		if (Util.isOffline(context)) {
			inflater.inflate(R.menu.select_playlist_context_offline, menu);
		}
		else {
			inflater.inflate(R.menu.select_playlist_context, menu);

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Playlist playlist = (Playlist) listView.getItemAtPosition(info.position);
			if(SyncUtil.isSyncedPlaylist(context, playlist.getId())) {
				menu.removeItem(R.id.playlist_menu_sync);
			} else {
				menu.removeItem(R.id.playlist_menu_stop_sync);
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
		Playlist playlist = (Playlist) listView.getItemAtPosition(info.position);

		SubsonicFragment fragment;
		Bundle args;
		FragmentTransaction trans;
		switch (menuItem.getItemId()) {
			case R.id.playlist_menu_download:
				downloadPlaylist(playlist.getId(), playlist.getName(), false, true, false, false, true);
				break;
			case R.id.playlist_menu_sync:
				syncPlaylist(playlist);
				break;
			case R.id.playlist_menu_stop_sync:
				stopSyncPlaylist(playlist);
				break;
			case R.id.playlist_menu_play_now:
				fragment = new SelectDirectoryFragment();
				args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
				args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				fragment.setArguments(args);

				replaceFragment(fragment, R.id.fragment_list_layout);
				break;
			case R.id.playlist_menu_play_shuffled:
				fragment = new SelectDirectoryFragment();
				args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
				args.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
				args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				fragment.setArguments(args);

				replaceFragment(fragment, R.id.fragment_list_layout);
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
				return false;
		}
		return true;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public ArrayAdapter getAdapter(List<Playlist> playlists) {
		return new PlaylistAdapter(context, playlists);
	}

	@Override
	public List<Playlist> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		List<Playlist> playlists = musicService.getPlaylists(refresh, context, listener);
		if(!Util.isOffline(context) && refresh) {
			new CacheCleaner(context, getDownloadService()).cleanPlaylists(playlists);
		}
		return playlists;
	}

	@Override
	public int getTitleResource() {
		return R.string.playlist_label;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Playlist playlist = (Playlist) parent.getItemAtPosition(position);

		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.fragment_list_layout);
	}

	private void deletePlaylist(final Playlist playlist) {
		Util.confirmDialog(context, R.string.common_delete, playlist.getName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(context, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deletePlaylist(playlist.getId(), context, null);
						SyncUtil.removeSyncedPlaylist(context, playlist.getId());
						return null;
					}

					@Override
					protected void done(Void result) {
						adapter.remove(playlist);
						adapter.notifyDataSetChanged();
						Util.toast(context, context.getResources().getString(R.string.menu_deleted_playlist, playlist.getName()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.menu_deleted_playlist_error, playlist.getName()) + " " + getErrorMessage(error);
						}

						Util.toast(context, msg, false);
					}
				}.execute();
			}
		});
	}

	private void displayPlaylistInfo(final Playlist playlist) {
		String message = "Owner: " + playlist.getOwner() + "\nComments: " +
			((playlist.getComment() == null) ? "" : playlist.getComment()) +
			"\nSong Count: " + playlist.getSongCount() +
			((playlist.getPublic() == null) ? "" : ("\nPublic: " + playlist.getPublic())) +
			"\nCreation Date: " + playlist.getCreated().replace('T', ' ');
		Util.info(context, playlist.getName(), message);
	}

	private void updatePlaylistInfo(final Playlist playlist) {
		View dialogView = context.getLayoutInflater().inflate(R.layout.update_playlist, null);
		final EditText nameBox = (EditText)dialogView.findViewById(R.id.get_playlist_name);
		final EditText commentBox = (EditText)dialogView.findViewById(R.id.get_playlist_comment);
		final CheckBox publicBox = (CheckBox)dialogView.findViewById(R.id.get_playlist_public);

		nameBox.setText(playlist.getName());
		commentBox.setText(playlist.getComment());
		Boolean pub = playlist.getPublic();
		if(pub == null) {
			publicBox.setEnabled(false);
		} else {
			publicBox.setChecked(pub);
		}

		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.playlist_update_info)
			.setView(dialogView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {					
					new LoadingTask<Void>(context, false) {
						@Override
						protected Void doInBackground() throws Throwable {
							MusicService musicService = MusicServiceFactory.getMusicService(context);
							musicService.updatePlaylist(playlist.getId(), nameBox.getText().toString(), commentBox.getText().toString(), publicBox.isChecked(), context, null);
							return null;
						}

						@Override
						protected void done(Void result) {
							refresh();
							Util.toast(context, context.getResources().getString(R.string.playlist_updated_info, playlist.getName()));
						}

						@Override
						protected void error(Throwable error) {
							String msg;
							if (error instanceof OfflineException || error instanceof ServerTooOldException) {
								msg = getErrorMessage(error);
							} else {
								msg = context.getResources().getString(R.string.playlist_updated_info_error, playlist.getName()) + " " + getErrorMessage(error);
							}

							Util.toast(context, msg, false);
						}
					}.execute();
				}

			})
			.setNegativeButton(R.string.common_cancel, null)
			.show();
	}

	private void syncPlaylist(Playlist playlist) {
		SyncUtil.addSyncedPlaylist(context, playlist.getId());
		downloadPlaylist(playlist.getId(), playlist.getName(), true, true, false, false, true);
	}

	private void stopSyncPlaylist(final Playlist playlist) {
		SyncUtil.removeSyncedPlaylist(context, playlist.getId());

		new LoadingTask<Void>(context, false) {
			@Override
			protected Void doInBackground() throws Throwable {
				// Unpin all of the songs in playlist
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				MusicDirectory root = musicService.getPlaylist(true, playlist.getId(), playlist.getName(), context, this);
				for(MusicDirectory.Entry entry: root.getChildren()) {
					DownloadFile file = new DownloadFile(context, entry, false);
					file.unpin();
				}

				return null;
			}

			@Override
			protected void done(Void result) {

			}
		}.execute();
	}
}
