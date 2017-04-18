package github.daneren2005.dsub.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.SyncUtil;
import github.daneren2005.dsub.util.CacheCleaner;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.PlaylistAdapter;
import github.daneren2005.dsub.view.UpdateView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectPlaylistFragment extends SelectRecyclerFragment<Playlist> {
	private static final String TAG = SelectPlaylistFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		if (Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_LARGE_ALBUM_ART, true)) {
			largeAlbums = true;
		}
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Playlist> updateView, Playlist playlist) {
		if (Util.isOffline(context)) {
			menuInflater.inflate(R.menu.select_playlist_context_offline, menu);
		}
		else {
			menuInflater.inflate(R.menu.select_playlist_context, menu);

			if(SyncUtil.isSyncedPlaylist(context, playlist.getId())) {
				menu.removeItem(R.id.playlist_menu_sync);
			} else {
				menu.removeItem(R.id.playlist_menu_stop_sync);
			}

			if(!ServerInfo.checkServerVersion(context, "1.8")) {
				menu.removeItem(R.id.playlist_update_info);
			} else if(playlist.getPublic() != null && playlist.getPublic() == true && playlist.getId().indexOf(".m3u") == -1 && !UserUtil.getCurrentUsername(context).equals(playlist.getOwner())) {
				menu.removeItem(R.id.playlist_update_info);
				menu.removeItem(R.id.playlist_menu_delete);
			}
		}

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Playlist> updateView, Playlist playlist) {
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

				replaceFragment(fragment);
				break;
			case R.id.playlist_menu_play_shuffled:
				fragment = new SelectDirectoryFragment();
				args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
				args.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
				args.putBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				fragment.setArguments(args);

				replaceFragment(fragment);
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
		}

		return false;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public SectionAdapter<Playlist> getAdapter(List<Playlist> playlists) {
		List<Playlist> mine = new ArrayList<>();
		List<Playlist> shared = new ArrayList<>();

		String currentUsername = UserUtil.getCurrentUsername(context);
		for(Playlist playlist: playlists) {
			if(playlist.getOwner() == null || playlist.getOwner().equals(currentUsername)) {
				mine.add(playlist);
			} else {
				shared.add(playlist);
			}
		}

		if(shared.isEmpty()) {
			return new PlaylistAdapter(context, playlists, getImageLoader(), largeAlbums, this);
		} else {
			Resources res = context.getResources();
			List<String> headers = Arrays.asList(res.getString(R.string.playlist_mine), res.getString(R.string.playlist_shared));

			List<List<Playlist>> sections = new ArrayList<>();
			sections.add(mine);
			sections.add(shared);

			return new PlaylistAdapter(context, headers, sections, getImageLoader(), largeAlbums, this);
		}
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
	public void onItemClicked(UpdateView<Playlist> updateView, Playlist playlist) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
		if(ServerInfo.checkServerVersion(context, "1.8") && (playlist.getOwner() != null && playlist.getOwner().equals(UserUtil.getCurrentUsername(context)) || playlist.getId().indexOf(".m3u") != -1)) {
			args.putBoolean(Constants.INTENT_EXTRA_NAME_PLAYLIST_OWNER, true);
		}
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	@Override
	public void onFinishRefresh() {
		Bundle args = getArguments();
		if(args != null) {
			String playlistId = args.getString(Constants.INTENT_EXTRA_NAME_ID, null);
			if (playlistId != null && objects != null) {
				for (Playlist playlist : objects) {
					if (playlistId.equals(playlist.getId())) {
						onItemClicked(null, playlist);
						break;
					}
				}
			}
		}
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
						adapter.removeItem(playlist);
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
		List<Integer> headers = new ArrayList<>();
		List<String> details = new ArrayList<>();

		headers.add(R.string.details_title);
		details.add(playlist.getName());

		if(playlist.getOwner() != null) {
			headers.add(R.string.details_owner);
			details.add(playlist.getOwner());
		}

		if(playlist.getComment() != null) {
			headers.add(R.string.details_comments);
			details.add(playlist.getComment());
		}

		headers.add(R.string.details_song_count);
		details.add(playlist.getSongCount());

		if(playlist.getDuration() != null) {
			headers.add(R.string.details_length);
			details.add(Util.formatDuration(playlist.getDuration()));
		}

		if(playlist.getPublic() != null) {
			headers.add(R.string.details_public);
			details.add(Util.formatBoolean(context, playlist.getPublic()));
		}

		if(playlist.getCreated() != null) {
			headers.add(R.string.details_created);
			details.add(Util.formatDate(playlist.getCreated()));
		}
		if(playlist.getChanged() != null) {
			headers.add(R.string.details_updated);
			details.add(Util.formatDate(playlist.getChanged()));
		}

		Util.showDetailsDialog(context, R.string.details_title_playlist, headers, details);
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
							String name = nameBox.getText().toString();
							String comment = commentBox.getText().toString();
							boolean isPublic = publicBox.isChecked();

							MusicService musicService = MusicServiceFactory.getMusicService(context);
							musicService.updatePlaylist(playlist.getId(), name, comment, isPublic, context, null);

							playlist.setName(name);
							playlist.setComment(comment);
							playlist.setPublic(isPublic);

							return null;
						}

						@Override
						protected void done(Void result) {
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

		boolean syncImmediately;
		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_SYNC_WIFI, true)) {
			ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = manager.getActiveNetworkInfo();

			if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				syncImmediately = true;
			} else {
				syncImmediately = false;
			}
		} else {
			syncImmediately = true;
		}

		if(syncImmediately) {
			downloadPlaylist(playlist.getId(), playlist.getName(), true, true, false, false, true);
		}
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
