/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;

public class AlbumListCountView extends UpdateView2<Integer, Void> {
	private final String TAG = AlbumListCountView.class.getSimpleName();

	private TextView titleView;
	private TextView countView;
	private int startCount;
	private int count = 0;

	public AlbumListCountView(Context context) {
		super(context, false);
		this.context = context;
		LayoutInflater.from(context).inflate(R.layout.basic_count_item, this, true);

		titleView = (TextView) findViewById(R.id.basic_count_name);
		countView = (TextView) findViewById(R.id.basic_count_count);
	}

	protected void setObjectImpl(Integer albumListString, Void dummy) {
		titleView.setText(albumListString);

		SharedPreferences prefs = Util.getPreferences(context);
		startCount = prefs.getInt(Constants.PREFERENCES_KEY_RECENT_COUNT + Util.getActiveServer(context), 0);
		count = startCount;
		update();
	}

	@Override
	protected void updateBackground() {
		try {
			String recentAddedFile = Util.getCacheName(context, "recent_count");
			ArrayList<String> recents = FileUtil.deserialize(context, recentAddedFile, ArrayList.class);
			if (recents == null) {
				recents = new ArrayList<String>();
			}

			MusicService musicService = MusicServiceFactory.getMusicService(context);
			MusicDirectory recentlyAdded = musicService.getAlbumList("newest", 20, 0, false, context, null);

			// If first run, just put everything in it and return 0
			boolean firstRun = recents.isEmpty();

			// Count how many new albums are in the list
			count = 0;
			for (MusicDirectory.Entry album : recentlyAdded.getChildren()) {
				if (!recents.contains(album.getId())) {
					recents.add(album.getId());
					count++;
				}
			}

			// Keep recents list from growing infinitely
			while (recents.size() > 40) {
				recents.remove(0);
			}
			FileUtil.serialize(context, recents, recentAddedFile);

			if (!firstRun) {
				// Add the old count which will get cleared out after viewing recents
				count += startCount;
				SharedPreferences.Editor editor = Util.getPreferences(context).edit();
				editor.putInt(Constants.PREFERENCES_KEY_RECENT_COUNT + Util.getActiveServer(context), count);
				editor.commit();
			}
		} catch(Exception e) {
			Log.w(TAG, "Failed to refresh most recent count", e);
		}
	}

	@Override
	protected void update() {
		// Update count display with appropriate information
		if(count <= 0) {
			countView.setVisibility(View.GONE);
		} else {
			String displayName;
			if(count < 10) {
				displayName = "0" + count;
			} else {
				displayName = "" + count;
			}

			countView.setText(displayName);
			countView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onClick() {
		SharedPreferences.Editor editor = Util.getPreferences(context).edit();
		editor.putInt(Constants.PREFERENCES_KEY_RECENT_COUNT + Util.getActiveServer(context), 0);
		editor.commit();

		count = 0;
		update();
	}
}
