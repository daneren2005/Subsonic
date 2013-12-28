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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.activity.SettingsActivity;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.PodcastEpisode;
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
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SubsonicFragment extends Fragment {
	private static final String TAG = SubsonicFragment.class.getSimpleName();
	private static final AtomicInteger nextGeneratedId = new AtomicInteger(1);
	private static int TAG_INC = 10;
	private int tag;
	
	protected SubsonicActivity context;
	protected CharSequence title = "DSub";
	protected CharSequence subtitle = null;
	protected View rootView;
	protected boolean primaryFragment = false;
	protected boolean secondaryFragment = false;
	protected boolean invalidated = false;
	protected static Random random = new Random();
	protected GestureDetector gestureScanner;
	
	public SubsonicFragment() {
		super();
		tag = TAG_INC++;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			String name = bundle.getString(Constants.FRAGMENT_NAME);
			if(name != null) {
				title = name;
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(Constants.FRAGMENT_NAME, title.toString());
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				refresh(true);
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
		}

		return false;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo, Object selected) {
		MenuInflater inflater = context.getMenuInflater();
		
		if(selected instanceof MusicDirectory.Entry) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) selected;
			if(entry instanceof PodcastEpisode && !entry.isVideo()) {
				if(Util.isOffline(context)) {
					inflater.inflate(R.menu.select_podcast_episode_context_offline, menu);
				}
				else {
					inflater.inflate(R.menu.select_podcast_episode_context, menu);
				}
			}
			else if (entry.isDirectory()) {
				if(Util.isOffline(context)) {
					inflater.inflate(R.menu.select_album_context_offline, menu);
				}
				else {
					inflater.inflate(R.menu.select_album_context, menu);
				}
				menu.findItem(entry.isDirectory() ? R.id.album_menu_star : R.id.song_menu_star).setTitle(entry.isStarred() ? R.string.common_unstar : R.string.common_star);
			} else if(!entry.isVideo()) {
				if(Util.isOffline(context)) {
					inflater.inflate(R.menu.select_song_context_offline, menu);
				}
				else {
					inflater.inflate(R.menu.select_song_context, menu);
				}
				menu.findItem(entry.isDirectory() ? R.id.album_menu_star : R.id.song_menu_star).setTitle(entry.isStarred() ? R.string.common_unstar : R.string.common_star);
			} else {
				if(Util.isOffline(context)) {
					inflater.inflate(R.menu.select_video_context_offline, menu);
				}
				else {
					inflater.inflate(R.menu.select_video_context, menu);
				}
			}
		} else if(selected instanceof Artist) {
			Artist artist = (Artist) selected;
			if(Util.isOffline(context)) {
				inflater.inflate(R.menu.select_artist_context_offline, menu);
			}
			else {
				inflater.inflate(R.menu.select_artist_context, menu);

				menu.findItem(R.id.artist_menu_star).setTitle(artist.isStarred() ? R.string.common_unstar : R.string.common_star);
			}
		}

		if(!Util.checkServerVersion(context, "1.10.1")) {
			menu.setGroupVisible(R.id.server_1_10, false);
		}
		SharedPreferences prefs = Util.getPreferences(context);
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_NEXT, true)) {
			menu.setGroupVisible(R.id.hide_play_next, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_PLAY_LAST, true)) {
			menu.setGroupVisible(R.id.hide_play_last, false);
		}
		if(!prefs.getBoolean(Constants.PREFERENCES_KEY_MENU_STAR, true)) {
			menu.setGroupVisible(R.id.hide_star, false);
		}
	}

	protected void recreateContextMenu(ContextMenu menu) {
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		for(int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if(item.isVisible()) {
				menuItems.add(item);
			}
		}
		menu.clear();
		for(int i = 0; i < menuItems.size(); i++) {
			MenuItem item = menuItems.get(i);
			menu.add(tag, item.getItemId(), Menu.NONE, item.getTitle());
		}
	}

	public boolean onContextItemSelected(MenuItem menuItem, Object selectedItem) {
		Artist artist = selectedItem instanceof Artist ? (Artist) selectedItem : null;
		MusicDirectory.Entry entry = selectedItem instanceof MusicDirectory.Entry ? (MusicDirectory.Entry) selectedItem : null;
		List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
		songs.add(entry);

		switch (menuItem.getItemId()) {
			case R.id.artist_menu_play_now:
				downloadRecursively(artist.getId(), false, false, true, false, false);
				break;
			case R.id.artist_menu_play_shuffled:
				downloadRecursively(artist.getId(), false, false, true, true, false);
				break;
			case R.id.artist_menu_play_next:
				downloadRecursively(artist.getId(), false, true, false, false, false, true);
				break;
			case R.id.artist_menu_play_last:
				downloadRecursively(artist.getId(), false, true, false, false, false);
				break;
			case R.id.artist_menu_download:
				downloadRecursively(artist.getId(), false, true, false, false, true);
				break;
			case R.id.artist_menu_pin:
				downloadRecursively(artist.getId(), true, true, false, false, true);
				break;
			case R.id.artist_menu_delete:
				deleteRecursively(artist);
				break;
			case R.id.artist_menu_star:
				toggleStarred(artist);
				break;
			case R.id.album_menu_play_now:
				downloadRecursively(entry.getId(), false, false, true, false, false);
				break;
			case R.id.album_menu_play_shuffled:
				downloadRecursively(entry.getId(), false, false, true, true, false);
				break;
			case R.id.album_menu_play_next:
				downloadRecursively(entry.getId(), false, true, false, false, false, true);
				break;
			case R.id.album_menu_play_last:
				downloadRecursively(entry.getId(), false, true, false, false, false);
				break;
			case R.id.album_menu_download:
				downloadRecursively(entry.getId(), false, true, false, false, true);
				break;
			case R.id.album_menu_pin:
				downloadRecursively(entry.getId(), true, true, false, false, true);
				break;
			case R.id.album_menu_star:
				toggleStarred(entry);
				break;
			case R.id.album_menu_delete:
				deleteRecursively(entry);
				break;
			case R.id.album_menu_info:
				displaySongInfo(entry);
				break;
			case R.id.album_menu_show_artist:
				showArtist((MusicDirectory.Entry) selectedItem);
				break;
			case R.id.song_menu_play_now:
				getDownloadService().clear();
				getDownloadService().download(songs, false, true, true, false);
				Util.startActivityWithoutTransition(context, DownloadActivity.class);
				break;
			case R.id.song_menu_play_next:
				getDownloadService().download(songs, false, false, true, false);
				break;
			case R.id.song_menu_play_last:
				getDownloadService().download(songs, false, false, false, false);
				break;
			case R.id.song_menu_download:
				getDownloadService().downloadBackground(songs, false);
				break;
			case R.id.song_menu_pin:
				getDownloadService().downloadBackground(songs, true);
				break;
			case R.id.song_menu_delete:
				getDownloadService().delete(songs);
				break;
			case R.id.song_menu_add_playlist:
				addToPlaylist(songs);
				break;
			case R.id.song_menu_star:
				toggleStarred(entry);
				break;
			case R.id.song_menu_play_external:
				playExternalPlayer(entry);
				break;
			case R.id.song_menu_info:
				displaySongInfo(entry);
				break;
			case R.id.song_menu_stream_external:
				streamExternalPlayer(entry);
				break;
			default:
				return false;
		}

		return true;
	}
	
	public void replaceFragment(SubsonicFragment fragment, int id) {
		replaceFragment(fragment, id, true);
	}
	public void replaceFragment(SubsonicFragment fragment, int id, boolean replaceCurrent) {
		context.replaceFragment(fragment, id, fragment.getSupportTag(), secondaryFragment && replaceCurrent);
	}
	
	protected int getNewId() {
		for (;;) {
	        final int result = nextGeneratedId.get();
	        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
	        int newValue = result + 1;
	        if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
	        if (nextGeneratedId.compareAndSet(result, newValue)) {
	            return result;
	        }
	    }
	}
	public int getRootId() {
		return rootView.getId();
	}

	public void setSupportTag(int tag) { this.tag = tag; }
	public void setSupportTag(String tag) { this.tag = Integer.parseInt(tag); }
	public int getSupportTag() {
		return tag;
	}
	
	public void setPrimaryFragment(boolean primary) {
		primaryFragment = primary;
		if(primary) {
			if(context != null) {
				context.setTitle(title);
				context.setSubtitle(subtitle);
			}
			if(invalidated) {
				invalidated = false;
				refresh(false);
			}
		}
	}
	public void setPrimaryFragment(boolean primary, boolean secondary) {
		setPrimaryFragment(primary);
		secondaryFragment = secondary;
	}
	public void setSecondaryFragment(boolean secondary) {
		secondaryFragment = secondary;
	}

	public void invalidate() {
		if(primaryFragment) {
			refresh(true);
		} else {
			invalidated = true;
		}
	}

	public DownloadService getDownloadService() {
		return context != null ? context.getDownloadService() : null;
	}

	protected void refresh() {
		refresh(true);
	}
	protected void refresh(boolean refresh) {

	}

	protected void exit() {
		if(context.getClass() != SubsonicFragmentActivity.class) {
			Intent intent = new Intent(context, SubsonicFragmentActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
			Util.startActivityWithoutTransition(context, intent);
		} else {
			context.stopService(new Intent(context, DownloadServiceImpl.class));
			context.finish();
		}
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

	public void setTitle(CharSequence title) {
		this.title = title;
		context.setTitle(title);
	}
	public void setTitle(int title) {
		this.title = context.getResources().getString(title);
		context.setTitle(this.title);
	}
	public void setSubtitle(CharSequence title) {
		this.subtitle = title;
		context.setSubtitle(title);
	}
	public CharSequence getTitle() {
		return this.title;
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
		final Button genreCombo = (Button)dialogView.findViewById(R.id.genre_combo);

		final SharedPreferences prefs = Util.getPreferences(context);
		final String oldStartYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, "");
		final String oldEndYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, "");
		final String oldGenre = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, "");

		boolean _useCombo = false;
		if(Util.checkServerVersion(context, "1.9.0")) {
			genreBox.setVisibility(View.GONE);
			genreCombo.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					new LoadingTask<List<Genre>>(context, true) {
						@Override
						protected List<Genre> doInBackground() throws Throwable {
							MusicService musicService = MusicServiceFactory.getMusicService(context);
							return musicService.getGenres(false, context, this);
						}

						@Override
						protected void done(final List<Genre> genres) {
							List<String> names = new ArrayList<String>();
							String blank = context.getResources().getString(R.string.select_genre_blank);
							names.add(blank);
							for(Genre genre: genres) {
								names.add(genre.getName());
							}
							final List<String> finalNames = names;

							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							builder.setTitle(R.string.shuffle_pick_genre)
								.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									if(which == 0) {
										genreCombo.setText("");
									} else {
										genreCombo.setText(finalNames.get(which));
									}
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
			});
			_useCombo = true;
		} else {
			genreCombo.setVisibility(View.GONE);
		}
		final boolean useCombo = _useCombo;

		startYearBox.setText(oldStartYear);
		endYearBox.setText(oldEndYear);
		genreBox.setText(oldGenre);
		genreCombo.setText(oldGenre);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.shuffle_title)
			.setView(dialogView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent(context, DownloadActivity.class);
					intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
					String genre;
					if(useCombo) {
						genre = genreCombo.getText().toString();
					} else {
						genre = genreBox.getText().toString();
					}
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
				
				// Make sure to clear parent cache
				String s = Util.getRestUrl(context, null) + entry.getParent();
				String parentCache = "directory-" + s.hashCode() + ".ser";
				File file = new File(context.getCacheDir(), parentCache);
				file.delete();
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
	protected void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background, final boolean playNext) {
		downloadRecursively(id, "", true, save, append, autoplay, shuffle, background, playNext);
	}
	protected void downloadPlaylist(final String id, final String name, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background) {
		downloadRecursively(id, name, false, save, append, autoplay, shuffle, background);
	}
	protected void downloadRecursively(final String id, final String name, final boolean isDirectory, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background) {
		downloadRecursively(id, name, isDirectory, save, append, autoplay, shuffle, background, false);
	}
	protected void downloadRecursively(final String id, final String name, final boolean isDirectory, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle, final boolean background, final boolean playNext) {
		LoadingTask<List<MusicDirectory.Entry>> task = new LoadingTask<List<MusicDirectory.Entry>>(context) {
			private static final int MAX_SONGS = 500;

			@Override
			protected List<MusicDirectory.Entry> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				MusicDirectory root;
				if(isDirectory)
					root = musicService.getMusicDirectory(id, name, false, context, this);
				else
					root = musicService.getPlaylist(true, id, name, context, this);
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
						downloadService.download(songs, save, autoplay, playNext, shuffle);
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
				String createNew = context.getResources().getString(R.string.playlist_create_new);
				names.add(createNew);
				for(Playlist playlist: playlists) {
					names.add(playlist.getName());
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(R.string.playlist_add_to)
					.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
						if(which > 0) {
							addToPlaylist(playlists.get(which - 1), songs);
						} else {
							createNewPlaylist(songs, false);
						}
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
	
	protected void createNewPlaylist(final List<MusicDirectory.Entry> songs, boolean getSuggestion) {
		View layout = context.getLayoutInflater().inflate(R.layout.save_playlist, null);
		final EditText playlistNameView = (EditText) layout.findViewById(R.id.save_playlist_name);
		final CheckBox overwriteCheckBox = (CheckBox) layout.findViewById(R.id.save_playlist_overwrite);
		if(getSuggestion) {
			String playlistName = (getDownloadService() != null) ? getDownloadService().getSuggestedPlaylistName() : null;
			if (playlistName != null) {
				playlistNameView.setText(playlistName);
				try {
					if(Util.checkServerVersion(context, "1.8.0") && Integer.parseInt(getDownloadService().getSuggestedPlaylistId()) != -1) {
						overwriteCheckBox.setChecked(true);
						overwriteCheckBox.setVisibility(View.VISIBLE);
					}
				} catch(Exception e) {
					Log.d(TAG, "Playlist id isn't a integer, probably MusicCabinet");
				}
			} else {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				playlistNameView.setText(dateFormat.format(new Date()));
			}
		} else {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			playlistNameView.setText(dateFormat.format(new Date()));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.download_playlist_title)
			.setMessage(R.string.download_playlist_name)
			.setView(layout)
			.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if(overwriteCheckBox.isChecked()) {
						overwritePlaylist(songs, String.valueOf(playlistNameView.getText()), getDownloadService().getSuggestedPlaylistId());
					} else {
						createNewPlaylist(songs, String.valueOf(playlistNameView.getText()));
					}
				}
			})
			.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.setCancelable(true);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	private void createNewPlaylist(final List<MusicDirectory.Entry> songs, final String name) {
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.createPlaylist(null, name, songs, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, R.string.download_playlist_done);
			}

			@Override
			protected void error(Throwable error) {
				String msg = context.getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
				Util.toast(context, msg);
			}
		}.execute();
	}
	private void overwritePlaylist(final List<MusicDirectory.Entry> songs, final String name, final String id) {
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				MusicDirectory playlist = musicService.getPlaylist(true, id, name, context, null);
				List<MusicDirectory.Entry> toDelete = playlist.getChildren();
				musicService.overwritePlaylist(id, name, toDelete.size(), songs, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, R.string.download_playlist_done);
			}

			@Override
			protected void error(Throwable error) {            	
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	public void displaySongInfo(final MusicDirectory.Entry song) {
		Integer bitrate = null;
		String format = null;
		long size = 0;
		if(!song.isDirectory()) {
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
		}

		String msg = "";
		if(song instanceof PodcastEpisode) {
			msg += "Podcast: " + song.getArtist() + "\nStatus: " + ((PodcastEpisode)song).getStatus();
		} else if(!song.isVideo()) {
			if(song.getArtist() != null && !"".equals(song.getArtist())) {
				msg += "Artist: " + song.getArtist();
			}
			if(song.getAlbum() != null && !"".equals(song.getAlbum())) {
				msg += "\nAlbum: " + song.getAlbum();
			}
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
		if(!Util.isOffline(context) && song.getSuffix() != null) {
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

		Util.info(context, song.getTitle(), msg);
	}
	
	protected void playVideo(MusicDirectory.Entry entry) {
		if(entryExists(entry)) {
			playExternalPlayer(entry);
		} else {
			streamExternalPlayer(entry);
		}
	}

	protected void playWebView(MusicDirectory.Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(context);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoUrl(maxBitrate, context, entry.getId())));

		startActivity(intent);
	}
	protected void playExternalPlayer(MusicDirectory.Entry entry) {
		if(!entryExists(entry)) {
			Util.toast(context, R.string.download_need_download);
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(entry.getPath()), "video/*");

			List<ResolveInfo> intents = context.getPackageManager()
				.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if(intents != null && intents.size() > 0) {
				startActivity(intent);
			}else {
				Util.toast(context, R.string.download_no_streaming_player);
			}
		}
	}
	protected void streamExternalPlayer(MusicDirectory.Entry entry) {
		String videoPlayerType = Util.getVideoPlayerType(context);
		if("flash".equals(videoPlayerType)) {
			playWebView(entry);
		} else if("hls".equals(videoPlayerType)) {
			streamExternalPlayer(entry, "hls");
		} else if("raw".equals(videoPlayerType)) {
			streamExternalPlayer(entry, "raw");
		} else {
			streamExternalPlayer(entry, entry.getTranscodedSuffix());
		}
	}
	protected void streamExternalPlayer(MusicDirectory.Entry entry, String format) {
		try {
			int maxBitrate = Util.getMaxVideoBitrate(context);

			Intent intent = new Intent(Intent.ACTION_VIEW);
			if("hls".equals(format)) {
				intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(context).getHlsUrl(entry.getId(), maxBitrate, context)), "video/*");
			} else {
				intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoStreamUrl(format, maxBitrate, context, entry.getId())), "video/*");
			}
			intent.putExtra("title", entry.getTitle());

			List<ResolveInfo> intents = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if(intents != null && intents.size() > 0) {
				startActivity(intent);
			} else {
				Util.toast(context, R.string.download_no_streaming_player);
			}
		} catch(Exception error) {
			String msg;
			if (error instanceof OfflineException || error instanceof ServerTooOldException) {
				msg = error.getMessage();
			} else {
				msg = context.getResources().getString(R.string.download_no_streaming_player) + " " + error.getMessage();
			}

			Util.toast(context, msg, false);
		}
	}

	protected boolean entryExists(MusicDirectory.Entry entry) {
		DownloadFile check = new DownloadFile(context, entry, false);
		return check.isCompleteFileAvailable();
	}

	public void deleteRecursively(Artist artist) {
		File dir = FileUtil.getArtistDirectory(context, artist);
		if(dir == null) return;

		Util.recursiveDelete(dir);
		if(Util.isOffline(context)) {
			refresh();
		}
	}
	
	public void deleteRecursively(MusicDirectory.Entry album) {
		File dir = FileUtil.getAlbumDirectory(context, album);
		if(dir == null) return;

		Util.recursiveDelete(dir);
		if(Util.isOffline(context)) {
			refresh();
		}
	}

	public void showArtist(MusicDirectory.Entry entry) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ID, entry.getParent());
		args.putString(Constants.INTENT_EXTRA_NAME_NAME, entry.getArtist());
		fragment.setArguments(args);

		replaceFragment(fragment, getRootId(), true);
	}
	
	public GestureDetector getGestureDetector() {
		return gestureScanner;
	}
}
