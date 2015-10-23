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
package github.daneren2005.dsub.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.media.MediaBrowserService;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.SilentServiceTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.util.compat.RemoteControlClientLP;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoMediaBrowserService extends MediaBrowserService {
	private static final String TAG = AutoMediaBrowserService.class.getSimpleName();
	private static final String BROWSER_ROOT = "root";
	private static final String BROWSER_ALBUM_LISTS = "albumLists";
	private static final String BROWSER_LIBRARY = "library";
	private static final String BROWSER_PLAYLISTS = "playlists";
	private static final String PLAYLIST_PREFIX = "pl-";
	private static final String ALBUM_TYPE_PREFIX = "ty-";

	private DownloadService downloadService;
	private Handler handler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();
		getDownloadService();
	}

	@Nullable
	@Override
	public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
		BrowserRoot root = new BrowserRoot(BROWSER_ROOT, null);
		return root;
	}

	@Override
	public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
		if(BROWSER_ROOT.equals(parentId)) {
			getRootFolders(result);
		} else if(BROWSER_ALBUM_LISTS.equals(parentId)) {
			getAlbumLists(result);
		} else if(parentId.startsWith(ALBUM_TYPE_PREFIX)) {
			int id = Integer.valueOf(parentId.substring(ALBUM_TYPE_PREFIX.length()));
			getAlbumList(result, id);
		} else if(BROWSER_LIBRARY.equals(parentId)) {
			getLibrary(result);
		} else if(BROWSER_PLAYLISTS.equals(parentId)) {
			getPlaylists(result);
		} else if(parentId.startsWith(PLAYLIST_PREFIX)) {
			getPlayOptions(result, parentId.substring(PLAYLIST_PREFIX.length()), Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
		} else {
			// No idea what it is, send empty result
			result.sendResult(new ArrayList<MediaBrowser.MediaItem>());
		}
	}

	private void getRootFolders(Result<List<MediaBrowser.MediaItem>> result) {
		List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

		/*MediaDescription.Builder albumLists = new MediaDescription.Builder();
		albumLists.setTitle(downloadService.getString(R.string.main_albums_title))
				.setMediaId(BROWSER_ALBUM_LISTS);
		mediaItems.add(new MediaBrowser.MediaItem(albumLists.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE));

		MediaDescription.Builder library = new MediaDescription.Builder();
		library.setTitle(downloadService.getString(R.string.button_bar_browse))
			.setMediaId(BROWSER_LIBRARY);
		mediaItems.add(new MediaBrowser.MediaItem(library.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE));*/

		MediaDescription.Builder playlists = new MediaDescription.Builder();
		playlists.setTitle(downloadService.getString(R.string.button_bar_playlists))
				.setMediaId(BROWSER_PLAYLISTS);
		mediaItems.add(new MediaBrowser.MediaItem(playlists.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE));

		result.sendResult(mediaItems);
	}

	private void getAlbumLists(Result<List<MediaBrowser.MediaItem>> result) {
		List<Integer> albums = new ArrayList<>();
		albums.add(R.string.main_albums_newest);
		albums.add(R.string.main_albums_random);
		if(ServerInfo.checkServerVersion(downloadService, "1.8")) {
			albums.add(R.string.main_albums_alphabetical);
		}
		if(!Util.isTagBrowsing(downloadService)) {
			albums.add(R.string.main_albums_highest);
		}
		// albums.add(R.string.main_albums_starred);
		// albums.add(R.string.main_albums_genres);
		// albums.add(R.string.main_albums_year);
		albums.add(R.string.main_albums_recent);
		albums.add(R.string.main_albums_frequent);

		List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

		for(Integer id: albums) {
			MediaDescription description = new MediaDescription.Builder()
					.setTitle(downloadService.getResources().getString(id))
					.setMediaId(ALBUM_TYPE_PREFIX + id)
					.build();

			mediaItems.add(new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_BROWSABLE));
		}

		result.sendResult(mediaItems);
	}
	private void getAlbumList(Result<List<MediaBrowser.MediaItem>> result, int id) {

	}

	private void getLibrary(Result<List<MediaBrowser.MediaItem>> result) {

	}

	private void getPlaylists(final Result<List<MediaBrowser.MediaItem>> result) {
		new SilentServiceTask<List<Playlist>>(downloadService) {
			@Override
			protected List<Playlist> doInBackground(MusicService musicService) throws Throwable {
				return musicService.getPlaylists(false, downloadService, null);
			}

			@Override
			protected void done(List<Playlist> playlists) {
				List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

				for(Playlist playlist: playlists) {
					MediaDescription description = new MediaDescription.Builder()
							.setTitle(playlist.getName())
							.setMediaId(PLAYLIST_PREFIX + playlist.getId())
							.build();

					mediaItems.add(new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_BROWSABLE));
				}

				result.sendResult(mediaItems);
			}
		}.execute();

		result.detach();
	}
	private void getPlayOptions(Result<List<MediaBrowser.MediaItem>> result, String id, String idConstant) {
		List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

		Bundle playAllExtras = new Bundle();
		playAllExtras.putString(idConstant, id);

		MediaDescription.Builder playAll = new MediaDescription.Builder();
		playAll.setTitle(downloadService.getString(R.string.menu_play))
				.setMediaId("play-" + id)
				.setExtras(playAllExtras);
		mediaItems.add(new MediaBrowser.MediaItem(playAll.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE));

		Bundle shuffleExtras = new Bundle();
		shuffleExtras.putString(idConstant, id);
		shuffleExtras.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);

		MediaDescription.Builder shuffle = new MediaDescription.Builder();
		shuffle.setTitle(downloadService.getString(R.string.menu_shuffle))
				.setMediaId("shuffle-" + id)
				.setExtras(shuffleExtras);
		mediaItems.add(new MediaBrowser.MediaItem(shuffle.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE));

		/*Bundle playLastExtras = new Bundle();
		playLastExtras.putString(idConstant, id);
		playLastExtras.putBoolean(Constants.INTENT_EXTRA_PLAY_LAST, true);

		MediaDescription.Builder playLast = new MediaDescription.Builder();
		playLast.setTitle(downloadService.getString(R.string.menu_play_last))
				.setMediaId("playLast-" + id)
				.setExtras(playLastExtras);
		mediaItems.add(new MediaBrowser.MediaItem(playLast.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE));*/

		result.sendResult(mediaItems);
	}

	public void getDownloadService() {
		if(DownloadService.getInstance() == null) {
			startService(new Intent(this, DownloadService.class));
		}

		waitForDownloadService();
	}
	public void waitForDownloadService() {
		downloadService = DownloadService.getInstance();
		if(downloadService == null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					waitForDownloadService();
				}
			}, 100);
		} else {
			RemoteControlClientLP remoteControlClient = (RemoteControlClientLP) downloadService.getRemoteControlClient();
			setSessionToken(remoteControlClient.getMediaSession().getSessionToken());
		}
	}
}
