package github.daneren2005.dsub.fragments;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.MainAdapter;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.EnvironmentVariables;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.view.ChangeLog;
import github.daneren2005.dsub.view.UpdateView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainFragment extends SelectRecyclerFragment<Integer> {
	private static final String TAG = MainFragment.class.getSimpleName();
	public static final String SONGS_LIST_PREFIX = "songs-";
	public static final String SONGS_NEWEST = SONGS_LIST_PREFIX + "newest";
	public static final String SONGS_TOP_PLAYED = SONGS_LIST_PREFIX + "topPlayed";
	public static final String SONGS_RECENT = SONGS_LIST_PREFIX + "recent";
	public static final String SONGS_FREQUENT = SONGS_LIST_PREFIX + "frequent";

	public MainFragment() {
		super();
		pullToRefresh = false;
		serialize = false;
		backgroundUpdate = false;
		alwaysFullscreen = true;
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

		if(ServerInfo.isMadsonic6(context)) {
			List<Integer> songs = new ArrayList<>();

			songs.add(R.string.main_songs_newest);
			if(ServerInfo.checkServerVersion(context, "2.0.1")) {
				songs.add(R.string.main_songs_top_played);
			}
			songs.add(R.string.main_songs_recent);
			if(ServerInfo.checkServerVersion(context, "2.0.1")) {
				songs.add(R.string.main_songs_frequent);
			}

			sections.add(songs);
			headers.add("songs");
		}

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
		if (EnvironmentVariables.PASTEBIN_DEV_KEY == null) {
			Util.toast(context, "No PASTEBIN_DEV_KEY configured - can't upload logs");
			return;
		}
		try {
			final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			new LoadingTask<String>(context) {
				@Override
				protected String doInBackground() throws Throwable {
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
					} finally {
						if(logcatProc != null) {
							logcatProc.destroy();
						}
					}

					URL url = new URL("https://pastebin.com/api/api_post.php");
					HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
					StringBuffer responseBuffer = new StringBuffer();
					try {
						urlConnection.setReadTimeout(10000);
						urlConnection.setConnectTimeout(15000);
						urlConnection.setRequestMethod("POST");
						urlConnection.setDoInput(true);
						urlConnection.setDoOutput(true);

						OutputStream os = urlConnection.getOutputStream();
						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, Constants.UTF_8));
						writer.write("api_dev_key=" + URLEncoder.encode(EnvironmentVariables.PASTEBIN_DEV_KEY, Constants.UTF_8) + "&api_option=paste&api_paste_private=1&api_paste_code=");

						BufferedReader reader = null;
						try {
							reader = new BufferedReader(new InputStreamReader(new FileInputStream(logcat)));
							String line;
							while ((line = reader.readLine()) != null) {
								writer.write(URLEncoder.encode(line + "\n", Constants.UTF_8));
							}
						} finally {
							Util.close(reader);
						}

						File stacktrace = new File(Environment.getExternalStorageDirectory(), "dsub-stacktrace.txt");
						if(stacktrace.exists() && stacktrace.isFile()) {
							writer.write("\n\nMost Recent Stacktrace:\n\n");

							reader = null;
							try {
								reader = new BufferedReader(new InputStreamReader(new FileInputStream(stacktrace)));
								String line;
								while ((line = reader.readLine()) != null) {
									writer.write(URLEncoder.encode(line + "\n", Constants.UTF_8));
								}
							} finally {
								Util.close(reader);
							}
						}

						writer.flush();
						writer.close();
						os.close();

						BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							responseBuffer.append(inputLine);
						}
						in.close();
					} finally {
						urlConnection.disconnect();
					}

					String response = responseBuffer.toString();
					if(response.indexOf("http") == 0) {
						return response.replace("http:", "https:");
					} else {
						throw new Exception("Pastebin Error: " + response);
					}
				}

				@Override
				protected void error(Throwable error) {
					Log.e(TAG, "Failed to gather logs", error);
					Util.toast(context, "Failed to gather logs");
				}

				@Override
				protected void done(String logcat) {
					String footer = "Android SDK: " + Build.VERSION.SDK;
					footer += "\nDevice Model: " + Build.MODEL;
					footer += "\nDevice Name: " + Build.MANUFACTURER + " "  + Build.PRODUCT;
					footer += "\nROM: " + Build.DISPLAY;
					footer += "\nLogs: " + logcat;
					footer += "\nBuild Number: " + packageInfo.versionCode;


					Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
					selectorIntent.setData(Uri.parse("mailto:"));

					final Intent emailIntent = new Intent(Intent.ACTION_SEND);
					emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dsub.android@gmail.com"});
					emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DSub " + packageInfo.versionName + " Error Logs");
					emailIntent.putExtra(Intent.EXTRA_TEXT, "Describe the problem here\n\n\n" + footer);
					emailIntent.setSelector( selectorIntent );

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
