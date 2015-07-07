package github.daneren2005.dsub.fragments;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
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
import android.view.View;
import android.widget.CheckBox;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.MainAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.SilentBackgroundTask;
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

		if (!Util.isOffline(context)) {
			List<Integer> offline = Arrays.asList(R.string.main_offline);
			sections.add(offline);
			headers.add(null);

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
			albums.add(R.string.main_albums_highest);

			sections.add(albums);
			headers.add("albums");

			if(ServerInfo.checkServerVersion(context, "1.8") && !Util.isTagBrowsing(context)) {
				List<Integer> videos = Arrays.asList(R.string.main_videos);
				sections.add(videos);
				headers.add("videos");
			}
		} else {
			List<Integer> online = Arrays.asList(R.string.main_online);
			sections.add(online);
			headers.add(null);
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

	private void toggleOffline() {
		boolean isOffline = Util.isOffline(context);
		Util.setOffline(context, !isOffline);
		context.invalidate();
		DownloadService service = getDownloadService();
		if (service != null) {
			service.setOnline(isOffline);
		}

		// Coming back online
		if(isOffline) {
			int scrobblesCount = Util.offlineScrobblesCount(context);
			int starsCount = Util.offlineStarsCount(context);
			if(scrobblesCount > 0 || starsCount > 0){
				showOfflineSyncDialog(scrobblesCount, starsCount);
			}
		}
		
		UserUtil.seedCurrentUser(context);
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
	
	private void showOfflineSyncDialog(final int scrobbleCount, final int starsCount) {
		String syncDefault = Util.getSyncDefault(context);
		if(syncDefault != null) {
			if("sync".equals(syncDefault)) {
				syncOffline(scrobbleCount, starsCount);
				return;
			} else if("delete".equals(syncDefault)) {
				deleteOffline();
				return;
			}
		}
		
		View checkBoxView = context.getLayoutInflater().inflate(R.layout.sync_dialog, null);
		final CheckBox checkBox = (CheckBox)checkBoxView.findViewById(R.id.sync_default);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.offline_sync_dialog_title)
			.setMessage(context.getResources().getString(R.string.offline_sync_dialog_message, scrobbleCount, starsCount))
			.setView(checkBoxView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if(checkBox.isChecked()) {
						Util.setSyncDefault(context, "sync");
					}
					syncOffline(scrobbleCount, starsCount);
				}
			}).setNeutralButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
				}
			}).setNegativeButton(R.string.common_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if(checkBox.isChecked()) {
						Util.setSyncDefault(context, "delete");
					}
					deleteOffline();
				}
			});

		builder.create().show();
	}
	
	private void syncOffline(final int scrobbleCount, final int starsCount) {
		new SilentBackgroundTask<Integer>(context) {
			@Override
			protected Integer doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				return musicService.processOfflineSyncs(context, null);
			}

			@Override
			protected void done(Integer result) {
				if(result == scrobbleCount) {
					Util.toast(context, context.getResources().getString(R.string.offline_sync_success, result));
				} else {
					Util.toast(context, context.getResources().getString(R.string.offline_sync_partial, result, scrobbleCount + starsCount));
				}
			}

			@Override
			protected void error(Throwable error) {
				Log.w(TAG, "Failed to sync offline stats", error);
				String msg = context.getResources().getString(R.string.offline_sync_error) + " " + getErrorMessage(error);
				Util.toast(context, msg);
			}
		}.execute();
	}
	private void deleteOffline() {
		SharedPreferences.Editor offline = Util.getOfflineSync(context).edit();
		offline.putInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		offline.putInt(Constants.OFFLINE_STAR_COUNT, 0);
		offline.commit();
	}

	private void showAboutDialog() {
		new LoadingTask<String>(context) {
			@Override
			protected String doInBackground() throws Throwable {
				File rootFolder = FileUtil.getMusicDirectory(context);
				StatFs stat = new StatFs(rootFolder.getPath());
				long bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
				long bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

				Pair<Long, Long> used = FileUtil.getUsedSize(context, rootFolder);

				return getResources().getString(R.string.main_about_text,
					context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName,
					used.getFirst(),
					Util.formatLocalizedBytes(used.getSecond(), context),
					Util.formatLocalizedBytes(Util.getCacheSizeMB(context) * 1024L * 1024L, context),
					Util.formatLocalizedBytes(bytesAvailableFs, context),
					Util.formatLocalizedBytes(bytesTotalFs, context));
			}

			@Override
			protected void done(String msg) {
				try {
					Util.info(context, R.string.main_about_title, msg);
				} catch(Exception e) {
					Util.toast(context, "Failed to open dialog");
				}
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
					File logcat = new File(FileUtil.getSubsonicDirectory(context), "logcat.txt");
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
	public void onItemClicked(Integer item) {
		if(item == R.string.main_offline || item == R.string.main_online) {
			toggleOffline();
		} else if (item == R.string.main_albums_newest) {
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
