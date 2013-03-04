package github.daneren2005.dsub.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.MergeAdapter;
import github.daneren2005.dsub.util.Util;
import java.util.Arrays;

public class MainFragment extends SubsonicTabFragment {	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		View view = inflater.inflate(R.layout.main, container, false);

		loadSettings();

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

		final View dummyView = view.findViewById(R.id.main_dummy);

		int instance = Util.getActiveServer(context);
		String name = Util.getServerName(context, instance);
		serverTextView.setText(name);

		ListView list = (ListView) view.findViewById(R.id.main_list);

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
                    // showAlbumList("newest");
                } else if (view == albumsRandomButton) {
                    // showAlbumList("random");
                } else if (view == albumsHighestButton) {
                    // showAlbumList("highest");
                } else if (view == albumsRecentButton) {
                    // showAlbumList("recent");
                } else if (view == albumsFrequentButton) {
                    // showAlbumList("frequent");
                } else if (view == albumsStarredButton) {
					// showAlbumList("starred");
				}
			}
		});
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
	
	private void toggleOffline() {
		Util.setOffline(this, !Util.isOffline(context));
		restart();
	}
}
