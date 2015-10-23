package github.daneren2005.dsub.fragments;

import android.content.res.Resources;
import android.os.Environment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.MainAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.view.ChangeLog;
import github.daneren2005.dsub.view.UpdateView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainFragment extends SelectRecyclerFragment<Integer> {
	private static final String TAG = MainFragment.class.getSimpleName();

	public MainFragment() {
		super();
		pullToRefresh = false;
		serialize = false;
		backgroundUpdate = false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.main, menu);

		try {
			if (!ServerInfo.isMadsonic(context) || !UserUtil.isCurrentAdmin()) {
				menu.setGroupVisible(R.id.madsonic, false);
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

		if(ServerInfo.checkServerVersion(context, "1.8")) {
			List<Integer> videos = Arrays.asList(R.string.main_videos);
			sections.add(videos);
			headers.add("videos");
		}

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
	private void showVideos() {
		SubsonicFragment fragment = new SelectVideoFragment();
		replaceFragment(fragment);
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
				details.add("Scott Jackson");

				headers.add(R.string.details_email);
				details.add("dsub.android@gmail.com");

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
			final String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			new LoadingTask<File>(context) {
				@Override
				protected File doInBackground() throws Throwable {
					updateProgress("Gathering Logs");
					File logcat = new File(Environment.getExternalStorageDirectory(), "dsub-logcat.txt");
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
					} catch(Exception e) {
						Util.toast(context, "Failed to gather logs");
					} finally {
						if(logcatProc != null) {
							logcatProc.destroy();
						}
					}

					return logcat;
				}

				@Override
				protected void done(File logcat) {
					String footer = "Android SDK: " + Build.VERSION.SDK;
					footer += "\nDevice Model: " + Build.MODEL;
					footer += "\nDevice Name: " + Build.MANUFACTURER + " "  + Build.PRODUCT;
					footer += "\nROM: " + Build.DISPLAY;

					Intent email = new Intent(Intent.ACTION_SENDTO,
						Uri.fromParts("mailto", "dsub.android@gmail.com", null));
					email.putExtra(Intent.EXTRA_SUBJECT, "DSub " + version + " Error Logs");
					email.putExtra(Intent.EXTRA_TEXT, "Describe the problem here\n\n\n" + footer);
					Uri attachment = Uri.fromFile(logcat);
					email.putExtra(Intent.EXTRA_STREAM, attachment);
					startActivity(email);
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
		} else if (item == R.string.main_albums_highest) {
			showAlbumList("highest");
		} else if (item == R.string.main_albums_recent) {
			showAlbumList("recent");
		} else if (item == R.string.main_albums_frequent) {
			showAlbumList("frequent");
		} else if (item == R.string.main_albums_starred) {
			showAlbumList("starred");
		} else if(item == R.string.main_albums_genres) {
			showAlbumList("genres");
		} else if(item == R.string.main_albums_year) {
			showAlbumList("years");
		} else if(item == R.string.main_albums_alphabetical) {
			showAlbumList("alphabeticalByName");
		} else if(item == R.string.main_videos) {
			showVideos();
		}
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Integer> updateView, Integer item) {}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Integer> updateView, Integer item) {
		return false;
	}
}
