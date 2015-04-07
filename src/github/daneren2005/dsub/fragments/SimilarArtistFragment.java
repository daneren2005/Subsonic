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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.fragments;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.ArtistAdapter;

import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

public class SimilarArtistFragment extends SelectListFragment<Artist> {
	private static final String TAG = SimilarArtistFragment.class.getSimpleName();
	private ArtistInfo info;
	private String artistId;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		artist = true;

		artistId = getArguments().getString(Constants.INTENT_EXTRA_NAME_ARTIST);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_play_now:
				playAll(false);
				return true;
			case R.id.menu_shuffle:
				playAll(true);
				return true;
			case R.id.menu_show_missing:
				showMissingArtists();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Object entry = listView.getItemAtPosition(info.position);
		onCreateContextMenu(menu, view, menuInfo, entry);

		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Artist artist = (Artist) listView.getItemAtPosition(info.position);
		return onContextItemSelected(menuItem, artist);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Artist artist = (Artist) parent.getItemAtPosition(position);
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
		args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
		args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.similar_artists;
	}

	@Override
	public ArrayAdapter getAdapter(List<Artist> objects) {
		return new ArtistAdapter(context, objects);
	}

	@Override
	public List<Artist> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		info = musicService.getArtistInfo(artistId, refresh, context, listener);
		return info.getSimilarArtists();
	}

	@Override
	public int getTitleResource() {
		return R.string.menu_similar_artists;
	}

	private void showMissingArtists() {
		StringBuilder b = new StringBuilder();

		for(String name: info.getMissingArtists()) {
			b.append("<h3><a href=\"https://www.google.com/#q=" + URLEncoder.encode(name) + "\">" + name + "</a></h3> ");
		}

		Util.showHTMLDialog(context, R.string.menu_similar_artists, b.toString());
	}

	private void playAll(final boolean shuffle) {
		new RecursiveLoader(context) {
			@Override
			protected Boolean doInBackground() throws Throwable {
				musicService = MusicServiceFactory.getMusicService(context);

				MusicDirectory root = new MusicDirectory();
				for(Artist artist: objects) {
					if(Util.isTagBrowsing(context) && !Util.isOffline(context)) {
						root.addChildren(musicService.getArtist(artist.getId(), artist.getName(), false, context, this).getChildren());
					} else {
						root.addChildren(musicService.getMusicDirectory(artist.getId(), artist.getName(), false, context, this).getChildren());
					}
				}

				if(shuffle) {
					root.shuffleChildren();
				}

				songs = new LinkedList<MusicDirectory.Entry>();
				getSongsRecursively(root, songs);

				DownloadService downloadService = getDownloadService();
				if (!songs.isEmpty() && downloadService != null) {
					downloadService.clear();
					downloadService.download(songs, false, true, false, false);
				}

				return true;
			}
		}.execute();
	}
}
