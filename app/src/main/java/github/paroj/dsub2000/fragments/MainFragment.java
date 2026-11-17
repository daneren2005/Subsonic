package github.paroj.dsub2000.fragments;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.adapter.MainAdapter;
import github.paroj.dsub2000.adapter.SectionAdapter;
import github.paroj.dsub2000.domain.ServerInfo;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.FileUtil;
import github.paroj.dsub2000.util.LoadingTask;
import github.paroj.dsub2000.util.ProgressListener;
import github.paroj.dsub2000.util.SilentBackgroundTask;
import github.paroj.dsub2000.util.UserUtil;
import github.paroj.dsub2000.util.Util;
import github.paroj.dsub2000.service.MusicService;
import github.paroj.dsub2000.service.MusicServiceFactory;
import github.paroj.dsub2000.view.ChangeLog;
import github.paroj.dsub2000.view.UpdateView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.core.content.FileProvider;

public class MainFragment extends SelectRecyclerFragment<Integer> {
	private static final String TAG = MainFragment.class.getSimpleName();
	public static final String SONGS_LIST_PREFIX = "songs-";
	public static final String SONGS_NEWEST = SONGS_LIST_PREFIX + "newest";
	public static final String SONGS_TOP_PLAYED = SONGS_LIST_PREFIX + "topPlayed";
	public static final String SONGS_RECENT = SONGS_LIST_PREFIX + "recent";
	public static final String SONGS_FREQUENT = SONGS_LIST_PREFIX + "frequent";
	public static final String SONGS_HIGHEST = SONGS_LIST_PREFIX + "highest";
	public static final String SONGS_ALPHABETICAL = SONGS_LIST_PREFIX + "alphabetical";
	public static final String SONGS_YEARS = SONGS_LIST_PREFIX + "years";

	public MainFragment() {
		super();
		pullToRefresh = false;
		serialize = false;
		backgroundUpdate = false;
		alwaysFullscreen = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		View view = super.onCreateView(inflater, container, bundle);
		detectOpenSubsonicExtensions();
		return view;
	}

	/**
	 * The supported OpenSubsonic extensions are needed to decide which song
	 * lists to show. Query them once per server and rebuild the adapter when
	 * the result becomes available.
	 */
	private void detectOpenSubsonicExtensions() {
		if (Util.isOffline(context)
				|| ServerInfo.isMadsonic(context)
				|| ServerInfo.hasOpenSubsonicExtensionsInfo(context)) {
			return;
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicServiceFactory.getMusicService(context).getOpenSubsonicExtensions(context, this);
				return null;
			}

			@Override
			protected void done(Void result) {
				if (recyclerView != null && objects != null) {
					recyclerView.setAdapter(adapter = getAdapter(objects));
				}
			}

			@Override
			protected void error(Throwable error) {
				Log.d(TAG, "Failed to query OpenSubsonic extensions", error);
			}
		}.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.main, menu);
		onFinishSetupOptionsMenu(menu);

		try {
			if (!ServerInfo.canRescanServer(context) || !UserUtil.isCurrentAdmin()) {
				menu.setGroupVisible(R.id.rescan_server, false);
			}
		} catch(Exception e) {
			Log.w(TAG, "Error on setting madsonic invisible", e);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_log:
				getLogs();
				return true;
			case R.id.menu_about:
				showAboutDialog();
				return true;
			case R.id.menu_changelog:
				ChangeLog changeLog = new ChangeLog(context, Util.getPreferences(context));
				changeLog.getFullLogDialog().show();
				return true;
			case R.id.menu_faq:
				showFAQDialog();
				return true;
			case R.id.menu_rescan:
				rescanServer();
				return true;
		}

		return false;
	}

	@Override
	public int getOptionsMenu() {
		return 0;
	}

	@Override
	public SectionAdapter getAdapter(List objs) {
		List<List<Integer>> sections = new ArrayList<>();
		List<String> headers = new ArrayList<>();

		List<Integer> albums = new ArrayList<>();
		albums.add(R.string.main_albums_newest);
		albums.add(R.string.main_albums_random);
		if(ServerInfo.checkServerVersion(context, "1.8")) {
			albums.add(R.string.main_albums_alphabetical);
		}
		if(!Util.isTagBrowsing(context)) {
			albums.add(R.string.main_albums_highest);
		}
		albums.add(R.string.main_albums_starred);
		albums.add(R.string.main_albums_genres);
		albums.add(R.string.main_albums_year);
		albums.add(R.string.main_albums_recent);
		albums.add(R.string.main_albums_frequent);

		sections.add(albums);
		headers.add("albums");

		List<Integer> songs = new ArrayList<>();

		if(ServerInfo.isMadsonic6(context)) {

			songs.add(R.string.main_songs_newest);
			if(ServerInfo.checkServerVersion(context, "2.0.1")) {
				songs.add(R.string.main_songs_top_played);
			}
			songs.add(R.string.main_songs_recent);
			if(ServerInfo.checkServerVersion(context, "2.0.1")) {
				songs.add(R.string.main_songs_frequent);
			}

		} else if(ServerInfo.hasOpenSubsonicExtension(context, Constants.OPENSUBSONIC_EXTENSION_SONG_LISTS)) {

			songs.add(R.string.main_songs_newest);
			songs.add(R.string.main_songs_recent);
			songs.add(R.string.main_songs_frequent);

		}

		if(ServerInfo.checkServerVersion(context, "1.2.0")) {
			songs.add(R.string.main_songs_random);
		}
        songs.add(R.string.main_songs_starred);

		sections.add(songs);
		headers.add("songs");

		return new MainAdapter(context, headers, sections, this);
	}

	@Override
	public List<Integer> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return Arrays.asList(0);
	}

	@Override
	public int getTitleResource() {
		return R.string.common_appname;
	}

	private void showAlbumList(String type) {
		if("genres".equals(type)) {
			SubsonicFragment fragment = new SelectGenreFragment();
			replaceFragment(fragment);
		} else if("years".equals(type)) {
			SubsonicFragment fragment = new SelectYearFragment();
			replaceFragment(fragment);
		} else {
			// Clear out recently added count when viewing
			if("newest".equals(type)) {
				SharedPreferences.Editor editor = Util.getPreferences(context).edit();
				editor.putInt(Constants.PREFERENCES_KEY_RECENT_COUNT + Util.getActiveServer(context), 0);
				editor.commit();
			}
			
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
			args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
			args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
			fragment.setArguments(args);

			replaceFragment(fragment);
		}
	}

	private void showAboutDialog() {
		new LoadingTask<Void>(context) {
			Long[] used;
			long bytesTotalFs;
			long bytesAvailableFs;

			@Override
			protected Void doInBackground() throws Throwable {
				File rootFolder = FileUtil.getMusicDirectory(context);
				StatFs stat = new StatFs(rootFolder.getPath());
				bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
				bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

				used = FileUtil.getUsedSize(context, rootFolder);
				return null;
			}

			@Override
			protected void done(Void result) {
				List<Integer> headers = new ArrayList<>();
				List<String> details = new ArrayList<>();

				headers.add(R.string.details_author);
				details.add("Pavel Rojtberg / Contributors");

				headers.add(R.string.details_email);
				details.add("play.dsub2000@gmail.com");

				try {
					headers.add(R.string.details_version);
					details.add(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
				} catch(Exception e) {
					details.add("");
				}

				Resources res = context.getResources();
				headers.add(R.string.details_files_cached);
				details.add(Long.toString(used[0]));

				headers.add(R.string.details_files_permanent);
				details.add(Long.toString(used[1]));

				headers.add(R.string.details_used_space);
				details.add(res.getString(R.string.details_of, Util.formatLocalizedBytes(used[2], context), Util.formatLocalizedBytes(Util.getCacheSizeMB(context) * 1024L * 1024L, context)));

				headers.add(R.string.details_available_space);
				details.add(res.getString(R.string.details_of, Util.formatLocalizedBytes(bytesAvailableFs, context), Util.formatLocalizedBytes(bytesTotalFs, context)));

				Util.showDetailsDialog(context, R.string.main_about_title, headers, details);
			}
		}.execute();
	}

	private void showFAQDialog() {
		Util.showHTMLDialog(context, R.string.main_faq_title, R.string.main_faq_text);
	}

	private void rescanServer() {
		new LoadingTask<Void>(context, false) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.startRescan(context, this);
				return null;
			}

			@Override
			protected void done(Void value) {
				Util.toast(context, R.string.main_scan_complete);
			}
		}.execute();
	}

	private void getLogs() {
		try {
			final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			new LoadingTask<ArrayList<Uri>>(context) {
				@Override
				protected ArrayList<Uri> doInBackground() throws Throwable {
					updateProgress("Gathering Logs");
					File logcat = new File(context.getExternalFilesDir(null), "dsub-logcat.txt");
					Util.delete(logcat);
					Process logcatProc = null;

					try {
						List<String> progs = new ArrayList<String>();
						progs.add("logcat");
						progs.add("-v");
						progs.add("time");
						progs.add("-d");
						progs.add("-f");
						progs.add(logcat.getCanonicalPath());
						progs.add("*:I");

						logcatProc = Runtime.getRuntime().exec(progs.toArray(new String[progs.size()]));
						logcatProc.waitFor();
					} finally {
						if(logcatProc != null) {
							logcatProc.destroy();
						}
					}

					String authority = context.getPackageName() + ".fileprovider";
					ArrayList<Uri> attachments = new ArrayList<>();
					if(logcat.exists()) {
						attachments.add(FileProvider.getUriForFile(context, authority, logcat));
					}

					File stacktrace = new File(context.getExternalFilesDir(null), "dsub-stacktrace.txt");
					if(stacktrace.exists() && stacktrace.isFile()) {
						attachments.add(FileProvider.getUriForFile(context, authority, stacktrace));
					}

					return attachments;
				}

				@Override
				protected void error(Throwable error) {
					Log.e(TAG, "Failed to gather logs", error);
					Util.toast(context, "Failed to gather logs");
				}

				@Override
				protected void done(ArrayList<Uri> attachments) {
					String footer = "Android SDK: " + Build.VERSION.SDK;
					footer += "\nDevice Model: " + Build.MODEL;
					footer += "\nDevice Name: " + Build.MANUFACTURER + " "  + Build.PRODUCT;
					footer += "\nROM: " + Build.DISPLAY;
					footer += "\nBuild Number: " + packageInfo.versionCode;

					Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
					selectorIntent.setData(Uri.parse("mailto:"));

					final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
					emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"play.dsub2000@gmail.com"});
					emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DSub " + packageInfo.versionName + " Error Logs");
					emailIntent.putExtra(Intent.EXTRA_TEXT, "Describe the problem here\n\n\n" + footer);
					emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
					emailIntent.setType("text/plain");
					emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					startActivity(Intent.createChooser(emailIntent, "Send log..."));
				}
			}.execute();
		} catch(Exception e) {}
	}

	@Override
	public void onItemClicked(UpdateView<Integer> updateView, Integer item) {
		if (item == R.string.main_albums_newest) {
			showAlbumList("newest");
		} else if (item == R.string.main_albums_random) {
			showAlbumList("random");
		} else if (item == R.string.main_songs_random) {
			showAlbumList("randomsongs");
		} else if (item == R.string.main_albums_highest) {
			showAlbumList("highest");
		} else if (item == R.string.main_albums_recent) {
			showAlbumList("recent");
		} else if (item == R.string.main_albums_frequent) {
			showAlbumList("frequent");
		} else if (item == R.string.main_albums_starred) {
			showAlbumList("starredalbums");
		} else if (item == R.string.main_songs_starred) {
            showAlbumList("starredsongs");
        } else if(item == R.string.main_albums_genres) {
			showAlbumList("genres");
		} else if(item == R.string.main_albums_year) {
			showAlbumList("years");
		} else if(item == R.string.main_albums_alphabetical) {
			showAlbumList("alphabeticalByName");
		} else if (item == R.string.main_songs_newest) {
			showAlbumList(SONGS_NEWEST);
		} else if (item == R.string.main_songs_top_played) {
			showAlbumList(SONGS_TOP_PLAYED);
		} else if (item == R.string.main_songs_recent) {
			showAlbumList(SONGS_RECENT);
		} else if (item == R.string.main_songs_frequent) {
			showAlbumList(SONGS_FREQUENT);
		}
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Integer> updateView, Integer item) {}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Integer> updateView, Integer item) {
		return false;
	}
}
