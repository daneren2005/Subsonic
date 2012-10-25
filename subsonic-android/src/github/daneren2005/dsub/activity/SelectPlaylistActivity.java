/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */

package github.daneren2005.dsub.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.*;

import java.util.List;

public class SelectPlaylistActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {
    private ListView list;
    private View emptyTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_playlist);

        list = (ListView) findViewById(R.id.select_playlist_list);
        emptyTextView = findViewById(R.id.select_playlist_empty);
        list.setOnItemClickListener(this);
        registerForContextMenu(list);

        // Title: Playlists
        setTitle(R.string.playlist_label);

        load();
    }
	
	@Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.select_playlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		Intent intent;
        switch (item.getItemId()) {
			case R.id.menu_refresh:
				refresh();
				return true;
            case R.id.menu_exit:
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
                Util.startActivityWithoutTransition(this, intent);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
        }

        return false;
    }

	private void refresh() {
		finish();
		Intent intent = new Intent(this, SelectPlaylistActivity.class);
		intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
		Util.startActivityWithoutTransition(this, intent);
	}

    private void load() {
        BackgroundTask<List<Playlist>> task = new TabActivityBackgroundTask<List<Playlist>>(this) {
            @Override
            protected List<Playlist> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SelectPlaylistActivity.this);
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                return musicService.getPlaylists(refresh, SelectPlaylistActivity.this, this);
            }

            @Override
            protected void done(List<Playlist> result) {
                list.setAdapter(new PlaylistAdapter(SelectPlaylistActivity.this, PlaylistAdapter.PlaylistComparator.sort(result)));
                emptyTextView.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }
        };
        task.execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
		
		MenuInflater inflater = getMenuInflater();		
		if (Util.isOffline(this))
			inflater.inflate(R.menu.select_playlist_context_offline, menu);
		else
			inflater.inflate(R.menu.select_playlist_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Playlist playlist = (Playlist) list.getItemAtPosition(info.position);

		Intent intent;
        switch (menuItem.getItemId()) {
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
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Playlist playlist = (Playlist) parent.getItemAtPosition(position);

        Intent intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
        Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
    }

	private void deletePlaylist(final Playlist playlist) {
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle("Confirm")
        .setMessage("Do you want to delete " + playlist.getName())
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(SelectPlaylistActivity.this, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(SelectPlaylistActivity.this);
						musicService.deletePlaylist(playlist.getId(), SelectPlaylistActivity.this, null);
						return null;
					}

					@Override
					protected void done(Void result) {
						refresh();
						Util.toast(SelectPlaylistActivity.this, getResources().getString(R.string.menu_deleted_playlist, playlist.getName()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = getResources().getString(R.string.menu_deleted_playlist_error, playlist.getName()) + " " + getErrorMessage(error);
						}

						Util.toast(SelectPlaylistActivity.this, msg, false);
					}
				}.execute();
			}

        })
        .setNegativeButton("Cancel", null)
        .show();
	}
	
	private void displayPlaylistInfo(final Playlist playlist) {
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(playlist.getName())
			.setMessage("Owner: " + playlist.getOwner() + "\nComments: " +
				((playlist.getComment() == null) ? "" : playlist.getComment()) +
				"\nSong Count: " + playlist.getSongCount() + "\nCreation Date: " + playlist.getCreated().replace('T', ' '))
			.show();
	}
	
	private void updatePlaylistInfo(final Playlist playlist) {
		View dialogView = getLayoutInflater().inflate(R.layout.update_playlist, null);
		final EditText nameBox = (EditText)dialogView.findViewById(R.id.get_playlist_name);
		final EditText commentBox = (EditText)dialogView.findViewById(R.id.get_playlist_comment);
		
		nameBox.setText(playlist.getName());
		commentBox.setText(playlist.getComment());
		
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.playlist_update_info)
			.setView(dialogView)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {					
					new LoadingTask<Void>(SelectPlaylistActivity.this, false) {
						@Override
						protected Void doInBackground() throws Throwable {
							MusicService musicService = MusicServiceFactory.getMusicService(SelectPlaylistActivity.this);
							musicService.updatePlaylist(playlist.getId(), nameBox.getText().toString(), commentBox.getText().toString(), SelectPlaylistActivity.this, null);
							return null;
						}

						@Override
						protected void done(Void result) {
							refresh();
							Util.toast(SelectPlaylistActivity.this, getResources().getString(R.string.playlist_updated_info, playlist.getName()));
						}

						@Override
						protected void error(Throwable error) {
							String msg;
							if (error instanceof OfflineException || error instanceof ServerTooOldException) {
								msg = getErrorMessage(error);
							} else {
								msg = getResources().getString(R.string.playlist_updated_info_error, playlist.getName()) + " " + getErrorMessage(error);
							}

							Util.toast(SelectPlaylistActivity.this, msg, false);
						}
					}.execute();
				}

			})
			.setNegativeButton("Cancel", null)
			.show();
	}
}