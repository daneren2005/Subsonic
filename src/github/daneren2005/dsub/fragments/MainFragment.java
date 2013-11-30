package github.daneren2005.dsub.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.view.MergeAdapter;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.view.ChangeLog;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainFragment extends SubsonicFragment {
	private static final String TAG = MainFragment.class.getSimpleName();
	private LayoutInflater inflater;

	private static final int MENU_GROUP_SERVER = 10;
	private static final int MENU_ITEM_SERVER_BASE = 100;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		this.inflater = inflater;
		rootView = inflater.inflate(R.layout.home, container, false);

		createLayout();

		return rootView;
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.main, menu);
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
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		
		int serverCount = Util.getServerCount(context);
		int activeServer = Util.getActiveServer(context);
		for(int i = 1; i <= serverCount; i++) {
			android.view.MenuItem menuItem = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_BASE + i, MENU_ITEM_SERVER_BASE + i, Util.getServerName(context, i));
			if(i == activeServer) {
				menuItem.setChecked(true);
			}
		}
		menu.setGroupCheckable(MENU_GROUP_SERVER, true, true);
		menu.setHeaderTitle(R.string.main_select_server);

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}
		
		int activeServer = menuItem.getItemId() - MENU_ITEM_SERVER_BASE;
		setActiveServer(activeServer);
		return true;
	}

	@Override
	protected void refresh(boolean refresh) {
		createLayout();
	}

	private void createLayout() {
		View buttons = inflater.inflate(R.layout.main_buttons, null);

		final View serverButton = buttons.findViewById(R.id.main_select_server);
		final TextView serverTextView = (TextView) serverButton.findViewById(R.id.main_select_server_2);
		final TextView offlineButton = (TextView) buttons.findViewById(R.id.main_offline);
		offlineButton.setText(Util.isOffline(context) ? R.string.main_online : R.string.main_offline);

		final View albumsTitle = buttons.findViewById(R.id.main_albums);
		final View albumsNewestButton = buttons.findViewById(R.id.main_albums_newest);
		final View albumsRandomButton = buttons.findViewById(R.id.main_albums_random);
		final View albumsHighestButton = buttons.findViewById(R.id.main_albums_highest);
		final View albumsRecentButton = buttons.findViewById(R.id.main_albums_recent);
		final View albumsFrequentButton = buttons.findViewById(R.id.main_albums_frequent);
		final View albumsStarredButton = buttons.findViewById(R.id.main_albums_starred);
		final View albumsGenresButton = buttons.findViewById(R.id.main_albums_genres);

		final View dummyView = rootView.findViewById(R.id.main_dummy);

		int instance = Util.getActiveServer(context);
		String name = Util.getServerName(context, instance);
		serverTextView.setText(name);

		ListView list = (ListView) rootView.findViewById(R.id.main_list);

		MergeAdapter adapter = new MergeAdapter();
		if (!Util.isOffline(context)) {
			adapter.addViews(Arrays.asList(serverButton), true);
		}
		adapter.addView(offlineButton, true);
		if (!Util.isOffline(context)) {
			adapter.addView(albumsTitle, false);
			adapter.addViews(Arrays.asList(albumsNewestButton, albumsRandomButton, albumsHighestButton, albumsStarredButton, albumsGenresButton, albumsRecentButton, albumsFrequentButton), true);
		}
		list.setAdapter(adapter);
		registerForContextMenu(dummyView);

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (view == serverButton) {
					dummyView.showContextMenu();
				} else if (view == offlineButton) {
					toggleOffline();
				} else if (view == albumsNewestButton) {
					showAlbumList("newest");
				} else if (view == albumsRandomButton) {
					showAlbumList("random");
				} else if (view == albumsHighestButton) {
					showAlbumList("highest");
				} else if (view == albumsRecentButton) {
					showAlbumList("recent");
				} else if (view == albumsFrequentButton) {
					showAlbumList("frequent");
				} else if (view == albumsStarredButton) {
					showAlbumList("starred");
				} else if(view == albumsGenresButton) {
					showAlbumList("genres");
				}
			}
		});
		setTitle(R.string.common_appname);
	}

	private void setActiveServer(int instance) {
		if (Util.getActiveServer(context) != instance) {
			DownloadService service = getDownloadService();
			if (service != null) {
				service.clearIncomplete();
			}
			Util.setActiveServer(context, instance);
			context.invalidate();
		}
	}

	private void toggleOffline() {
		boolean isOffline = Util.isOffline(context);
		Util.setOffline(context, !isOffline);
		context.invalidate();
		
		if(isOffline) {
			int scrobblesCount = Util.offlineScrobblesCount(context);
			int starsCount = Util.offlineStarsCount(context);
			if(scrobblesCount > 0 || starsCount > 0){
				showOfflineSyncDialog(scrobblesCount, starsCount);
			}
		}
	}

	private void showAlbumList(String type) {
		if("genres".equals(type)) {
			SubsonicFragment fragment = new SelectGenreFragment();
			replaceFragment(fragment, R.id.home_layout);
		} else {
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
			args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
			args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
			fragment.setArguments(args);

			replaceFragment(fragment, R.id.home_layout);
		}
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
				String msg = context.getResources().getString(R.string.offline_sync_error) + " " + getErrorMessage(error);
				Util.toast(context, msg);
			}
		}.execute();
	}
	private void deleteOffline() {
		SharedPreferences.Editor offline = Util.getOfflineSync(context).edit();
		offline.putInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		offline.commit();
	}

	private void showAboutDialog() {
		try {
			File rootFolder = FileUtil.getMusicDirectory(context);
			StatFs stat = new StatFs(rootFolder.getPath());
			long bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
			long bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

			String msg = getResources().getString(R.string.main_about_text,
				context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName,
				Util.formatBytes(FileUtil.getUsedSize(context, rootFolder)),
				Util.formatBytes(Util.getCacheSizeMB(context) * 1024L * 1024L),
				Util.formatBytes(bytesAvailableFs),
				Util.formatBytes(bytesTotalFs));
			Util.info(context, R.string.main_about_title, msg);
		} catch(Exception e) {
			Util.toast(context, "Failed to open dialog");
		}
	}

	private void getLogs() {
		try {
			final String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			new LoadingTask<File>(context) {
				@Override
				protected File doInBackground() throws Throwable {
					updateProgress("Gathering Logs");
					File logcat = new File(FileUtil.getSubsonicDirectory(), "logcat.txt");
					Process logcatProc = null;

					try {
						List<String> progs = new ArrayList<String>();
						progs.add("logcat");
						progs.add("-v");
						progs.add("time");
						progs.add("-d");
						progs.add("-f");
						progs.add(logcat.getPath());
						progs.add("*:I");

						logcatProc = Runtime.getRuntime().exec(progs.toArray(new String[0]));
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
					Intent email = new Intent(android.content.Intent.ACTION_SEND);
					email.setType("text/plain");
					email.putExtra(Intent.EXTRA_EMAIL, new String[] {"dsub.android@gmail.com"});
					email.putExtra(Intent.EXTRA_SUBJECT, "DSub " + version + " Error Logs");
					email.putExtra(Intent.EXTRA_TEXT, "Describe the problem here");
					Uri attachment = Uri.fromFile(logcat);
					email.putExtra(Intent.EXTRA_STREAM, attachment);
					startActivity(email);
				}
			}.execute();
		} catch(Exception e) {}
	}
}
