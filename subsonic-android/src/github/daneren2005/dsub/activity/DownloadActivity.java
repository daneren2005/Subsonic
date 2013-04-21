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

import github.daneren2005.dsub.R;
import android.os.Bundle;
import android.view.MotionEvent;
import github.daneren2005.dsub.fragments.DownloadFragment;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.widget.EditText;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import com.actionbarsherlock.view.MenuItem;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import java.util.LinkedList;
import java.util.List;

public class DownloadActivity extends SubsonicActivity {
	private static final String TAG = DownloadActivity.class.getSimpleName();
	private DownloadFragment fragment;
	private EditText playlistNameView;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);

		if (findViewById(R.id.download_container) != null && savedInstanceState == null) {
			fragment = new DownloadFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.download_container, fragment).commit();
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			Intent i = new Intent();
			i.setClass(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if(fragment != null) {
			return fragment.getGestureDetector().onTouchEvent(me);
		} else {
			return false;
		}
	}

	@Override
	public Dialog onCreateDialog(int id) {
		if (id == DownloadFragment.DIALOG_SAVE_PLAYLIST) {
			AlertDialog.Builder builder;

			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.save_playlist, null);
			playlistNameView = (EditText) layout.findViewById(R.id.save_playlist_name);

			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.download_playlist_title);
			builder.setMessage(R.string.download_playlist_name);
			builder.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					savePlaylistInBackground(String.valueOf(playlistNameView.getText()));
				}
			});
			builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			builder.setView(layout);
			builder.setCancelable(true);

			return builder.create();
		} else {
			return super.onCreateDialog(id);
		}
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		if (id == DownloadFragment.DIALOG_SAVE_PLAYLIST) {
			String playlistName = (getDownloadService() != null) ? getDownloadService().getSuggestedPlaylistName() : null;
			if (playlistName != null) {
				playlistNameView.setText(playlistName);
			} else {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				playlistNameView.setText(dateFormat.format(new Date()));
			}
		}
	}

	private void savePlaylistInBackground(final String playlistName) {
		Util.toast(this, getResources().getString(R.string.download_playlist_saving, playlistName));
		getDownloadService().setSuggestedPlaylistName(playlistName);
		new SilentBackgroundTask<Void>(DownloadActivity.this) {
			@Override
			protected Void doInBackground() throws Throwable {
				List<MusicDirectory.Entry> entries = new LinkedList<MusicDirectory.Entry>();
				for (DownloadFile downloadFile : getDownloadService().getSongs()) {
					entries.add(downloadFile.getSong());
				}
				MusicService musicService = MusicServiceFactory.getMusicService(DownloadActivity.this);
				musicService.createPlaylist(null, playlistName, entries, DownloadActivity.this, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(DownloadActivity.this, R.string.download_playlist_done);
			}

			@Override
			protected void error(Throwable error) {
				String msg = getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
				Util.toast(DownloadActivity.this, msg);
			}
		}.execute();
	}
}
