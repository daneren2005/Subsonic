package github.daneren2005.dsub.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.MergeAdapter;
import github.daneren2005.dsub.util.Util;
import java.util.Arrays;

public class MainFragment extends SubsonicTabFragment {
	private LayoutInflater inflater;

	private static final int MENU_GROUP_SERVER = 10;
	private static final int MENU_ITEM_SERVER_1 = 101;
	private static final int MENU_ITEM_SERVER_2 = 102;
	private static final int MENU_ITEM_SERVER_3 = 103;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		this.inflater = inflater;
		rootView = inflater.inflate(R.layout.home, container, false);

		loadSettings();
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
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		android.view.MenuItem menuItem1 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_1, MENU_ITEM_SERVER_1, Util.getServerName(context, 1));
		android.view.MenuItem menuItem2 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_2, MENU_ITEM_SERVER_2, Util.getServerName(context, 2));
		android.view.MenuItem menuItem3 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_3, MENU_ITEM_SERVER_3, Util.getServerName(context, 3));
		menu.setGroupCheckable(MENU_GROUP_SERVER, true, true);
		menu.setHeaderTitle(R.string.main_select_server);

		switch (Util.getActiveServer(context)) {
			case 1:
				menuItem1.setChecked(true);
				break;
			case 2:
				menuItem2.setChecked(true);
				break;
			case 3:
				menuItem3.setChecked(true);
				break;
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case MENU_ITEM_SERVER_1:
				setActiveServer(1);
				break;
			case MENU_ITEM_SERVER_2:
				setActiveServer(2);
				break;
			case MENU_ITEM_SERVER_3:
				setActiveServer(3);
				break;
			default:
				return super.onContextItemSelected(menuItem);
		}

		return true;
	}

	@Override
	protected void refresh() {
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
			adapter.addViews(Arrays.asList(albumsNewestButton, albumsRandomButton, albumsHighestButton, albumsStarredButton, albumsRecentButton, albumsFrequentButton), true);
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
				}
			}
		});
	}

	private void loadSettings() {
		PreferenceManager.setDefaultValues(context, R.xml.settings, false);
		SharedPreferences prefs = Util.getPreferences(context);
		if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, FileUtil.getDefaultMusicDirectory().getPath());
			editor.commit();
		}

		if (!prefs.contains(Constants.PREFERENCES_KEY_OFFLINE)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCES_KEY_OFFLINE, false);
			editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
			editor.commit();
		} 
	}

	private void setActiveServer(int instance) {
		if (Util.getActiveServer(context) != instance) {
			DownloadService service = getDownloadService();
			if (service != null) {
				service.clearIncomplete();
			}
			Util.setActiveServer(context, instance);
		}
	}

	private void toggleOffline() {
		Util.setOffline(context, !Util.isOffline(context));
		refresh();
	}
	
	private void showAlbumList(String type) {
		SubsonicTabFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
		args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		fragment.setArguments(args);

		final FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.home_layout, fragment);
		trans.addToBackStack(null);
		trans.commit();
	}
}
