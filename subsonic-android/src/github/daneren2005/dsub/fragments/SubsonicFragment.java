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
package github.daneren2005.dsub.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.activity.HelpActivity;
import github.daneren2005.dsub.activity.SettingsActivity;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.ModalBackgroundTask;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SubsonicFragment extends SherlockFragment {
	private static final String TAG = SubsonicFragment.class.getSimpleName();
	protected SubsonicActivity context;
	protected CharSequence title = "DSub";
	protected View rootView;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = (SubsonicActivity)activity;
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				refresh();
				return true;
			case R.id.menu_shuffle:
				onShuffleRequested();
				return true;
			case R.id.menu_search:
				context.onSearchRequested();
				return true;
			case R.id.menu_exit:
				exit();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(context, SettingsActivity.class));
				return true;
			case R.id.menu_help:
				startActivity(new Intent(context, HelpActivity.class));
				return true;
		}

		return false;
	}

	public DownloadService getDownloadService() {
		return context != null ? context.getDownloadService() : null;
	}

	protected void refresh() {

	}

	protected void exit() {
		context.stopService(new Intent(context, DownloadServiceImpl.class));
		context.finish();
	}

	public void setProgressVisible(boolean visible) {
		View view = rootView.findViewById(R.id.tab_progress);
		if (view != null) {
			view.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void updateProgress(String message) {
		TextView view = (TextView) rootView.findViewById(R.id.tab_progress_message);
		if (view != null) {
			view.setText(message);
		}
	}

	protected synchronized ImageLoader getImageLoader() {
		return context.getImageLoader();
	}
	public synchronized static ImageLoader getStaticImageLoader(Context context) {
		return SubsonicActivity.getStaticImageLoader(context);
	}

	protected void setTitle(CharSequence title) {
		this.title = title;
		context.setTitle(title);
	}
	protected void setTitle(int title) {
		this.title = context.getResources().getString(title);
		context.setTitle(this.title);
	}

	protected void warnIfNetworkOrStorageUnavailable() {
		if (!Util.isExternalStoragePresent()) {
			Util.toast(context, R.string.select_album_no_sdcard);
		} else if (!Util.isOffline(context) && !Util.isNetworkConnected(context)) {
			Util.toast(context, R.string.select_album_no_network);
		}
	}

	protected void onShuffleRequested() {
		if(Util.isOffline(context)) {
			Intent intent = new Intent(context, DownloadActivity.class);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
			Util.startActivityWithoutTransition(context, intent);
			return;
		}

		View dialogView = context.getLayoutInflater().inflate(R.layout.shuffle_dialog, null);
		final EditText startYearBox = (EditText)dialogView.findViewById(R.id.start_year);
		final EditText endYearBox = (EditText)dialogView.findViewById(R.id.end_year);
		final EditText genreBox = (EditText)dialogView.findViewById(R.id.genre);

		final SharedPreferences prefs = Util.getPreferences(context);
		final String oldStartYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, "");
		final String oldEndYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, "");
		final String oldGenre = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, "");

		startYearBox.setText(oldStartYear);
		endYearBox.setText(oldEndYear);
		genreBox.setText(oldGenre);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Shuffle By")
			.setView(dialogView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent(context, DownloadActivity.class);
					intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
					String genre = genreBox.getText().toString();
					String startYear = startYearBox.getText().toString();
					String endYear = endYearBox.getText().toString();

					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, startYear);
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, endYear);
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, genre);
					editor.commit();

					Util.startActivityWithoutTransition(context, intent);
				}
			})
			.setNegativeButton(R.string.common_cancel, null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public void toggleStarred(final MusicDirectory.Entry entry) {
		final boolean starred = !entry.isStarred();
		entry.setStarred(starred);

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.setStarred(entry.getId(), starred, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				// UpdateView
				Util.toast(context, context.getResources().getString(starred ? R.string.starring_content_starred : R.string.starring_content_unstarred, entry.getTitle()));
			}

			@Override
			protected void error(Throwable error) {
				entry.setStarred(!starred);

				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.starring_content_error, entry.getTitle()) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}
	public void toggleStarred(final Artist entry) {
		final boolean starred = !entry.isStarred();
		entry.setStarred(starred);

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.setStarred(entry.getId(), starred, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				// UpdateView
				Util.toast(context, context.getResources().getString(starred ? R.string.starring_content_starred : R.string.starring_content_unstarred, entry.getName()));
			}

			@Override
			protected void error(Throwable error) {
				entry.setStarred(!starred);

				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.starring_content_error, entry.getName()) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	protected void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background) {
		downloadRecursively(id, "", true, save, append, autoplay, shuffle, background);
	}
	protected void downloadPlaylist(final String id, final String name, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background) {
		downloadRecursively(id, name, false, save, append, autoplay, shuffle, background);
	}
	protected void downloadRecursively(final String id, final String name, final boolean isDirectory, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background) {
		ModalBackgroundTask<List<MusicDirectory.Entry>> task = new ModalBackgroundTask<List<MusicDirectory.Entry>>(context, false) {
			private static final int MAX_SONGS = 500;

			@Override
			protected List<MusicDirectory.Entry> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				MusicDirectory root;
				if(isDirectory)
					root = musicService.getMusicDirectory(id, name, false, context, this);
				else
					root = musicService.getPlaylist(id, name, context, this);
				List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
				getSongsRecursively(root, songs);
				return songs;
			}

			private void getSongsRecursively(MusicDirectory parent, List<MusicDirectory.Entry> songs) throws Exception {
				if (songs.size() > MAX_SONGS) {
					return;
				}

				for (MusicDirectory.Entry song : parent.getChildren(false, true)) {
					if (!song.isVideo()) {
						songs.add(song);
					}
				}
				for (MusicDirectory.Entry dir : parent.getChildren(true, false)) {
					MusicService musicService = MusicServiceFactory.getMusicService(context);
					getSongsRecursively(musicService.getMusicDirectory(dir.getId(), dir.getTitle(), false, context, this), songs);
				}
			}

			@Override
			protected void done(List<MusicDirectory.Entry> songs) {
				DownloadService downloadService = getDownloadService();
				if (!songs.isEmpty() && downloadService != null) {
					if (!append) {
						downloadService.clear();
					}
					warnIfNetworkOrStorageUnavailable();
					if(!background) {
						downloadService.download(songs, save, autoplay, false, shuffle);
						if(!append) {
							Util.startActivityWithoutTransition(context, DownloadActivity.class);
						}
					}
					else {
						downloadService.downloadBackground(songs, save);
					}
				}
			}
		};

		task.execute();
	}

	protected void addToPlaylist(final List<MusicDirectory.Entry> songs) {
		if(songs.isEmpty()) {
			Util.toast(context, "No songs selected");
			return;
		}

		new LoadingTask<List<Playlist>>(context, true) {
			@Override
			protected List<Playlist> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				return musicService.getPlaylists(false, context, this);
			}

			@Override
			protected void done(final List<Playlist> playlists) {
				List<String> names = new ArrayList<String>();
				for(Playlist playlist: playlists) {
					names.add(playlist.getName());
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Add to Playlist")
					.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						addToPlaylist(playlists.get(which), songs);
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}

			@Override
			protected void error(Throwable error) {            	
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.playlist_error) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	private void addToPlaylist(final Playlist playlist, final List<MusicDirectory.Entry> songs) {		
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.addToPlaylist(playlist.getId(), songs, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getString(R.string.updated_playlist, songs.size(), playlist.getName()));
			}

			@Override
			protected void error(Throwable error) {            	
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.updated_playlist_error, playlist.getName()) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	public void displaySongInfo(final MusicDirectory.Entry song) {
		Integer bitrate = null;
		String format = null;
		long size = 0;
		try {
			DownloadFile downloadFile = new DownloadFile(context, song, false);
			File file = downloadFile.getCompleteFile();
			if(file.exists()) {
				MediaMetadataRetriever metadata = new MediaMetadataRetriever();
				metadata.setDataSource(file.getAbsolutePath());
				String tmp = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
				bitrate = Integer.parseInt((tmp != null) ? tmp : "0") / 1000;
				format = FileUtil.getExtension(file.getName());
				size = file.length();

				if(Util.isOffline(context)) {
					song.setGenre(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
					String year = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
					song.setYear(Integer.parseInt((year != null) ? year : "0"));
				}
			}
		} catch(Exception e) {
			Log.i(TAG, "Device doesn't properly support MediaMetadataRetreiver");
		}

		String msg = "";
		if(!song.isVideo()) {
			msg += "Artist: " + song.getArtist() + "\nAlbum: " + song.getAlbum();
		}
		if(song.getTrack() != null && song.getTrack() != 0) {
			msg += "\nTrack: " + song.getTrack();
		}
		if(song.getGenre() != null && !"".equals(song.getGenre())) {
			msg += "\nGenre: " + song.getGenre();
		}
		if(song.getYear() != null && song.getYear() != 0) {
			msg += "\nYear: " + song.getYear();
		}
		if(!Util.isOffline(context)) {
			msg += "\nServer Format: " + song.getSuffix();
			if(song.getBitRate() != null && song.getBitRate() != 0) {
				msg += "\nServer Bitrate: " + song.getBitRate() + " kpbs";
			}
		}
		if(format != null && !"".equals(format)) {
			msg += "\nCached Format: " + format;
		}
		if(bitrate != null && bitrate != 0) {
			msg += "\nCached Bitrate: " + bitrate + " kpbs";
		}
		if(size != 0) {
			msg += "\nSize: " + Util.formatBytes(size);
		}
		if(song.getDuration() != null && song.getDuration() != 0) {
			msg += "\nLength: " + Util.formatDuration(song.getDuration());
		}

		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(song.getTitle())
			.setMessage(msg)
			.show();
	}
}
