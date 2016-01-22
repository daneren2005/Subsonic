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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.adapter.SimilarArtistAdapter;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SimilarArtistFragment extends SelectRecyclerFragment<Artist> {
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
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Artist> updateView, Artist item) {
		if(!Artist.MISSING_ID.equals(item.getId())) {
			onCreateContextMenuSupport(menu, menuInflater, updateView, item);
			recreateContextMenu(menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Artist> updateView, Artist artist) {
		return onContextItemSelected(menuItem, artist);
	}

	@Override
	public void onItemClicked(UpdateView<Artist> updateView, Artist artist) {
		if(Artist.MISSING_ID.equals(artist.getId())) {
			String url = "http://www.last.fm/music/" + URLEncoder.encode(artist.getName());
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			startActivity(intent);
		} else {
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
			args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
			fragment.setArguments(args);

			replaceFragment(fragment);
		}
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.similar_artists;
	}

	@Override
	public SectionAdapter getAdapter(List<Artist> artists) {
		if(info.getMissingArtists().isEmpty()) {
			return new SimilarArtistAdapter(context, artists, this);
		} else {
			List<String> headers = new ArrayList<>();
			headers.add(null);
			headers.add(context.getResources().getString(R.string.menu_similar_artists_missing));

			List<Artist> missingArtists = new ArrayList<>();
			for(String artistName: info.getMissingArtists()) {
				Artist artist = new Artist(Artist.MISSING_ID, artistName);
				missingArtists.add(artist);
			}

			return new SimilarArtistAdapter(context, headers, Arrays.asList(artists, missingArtists), this);
		}
	}

	@Override
	public List<Artist> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		info = musicService.getArtistInfo(artistId, refresh, true, context, listener);
		return info.getSimilarArtists();
	}

	@Override
	public int getTitleResource() {
		return R.string.menu_similar_artists;
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
