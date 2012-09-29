/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */

package github.daneren2005.dsub.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.net.Uri;
import com.actionbarsherlock.view.Menu;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.SearchCritera;
import github.daneren2005.dsub.domain.SearchResult;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.ArtistAdapter;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.EntryAdapter;
import github.daneren2005.dsub.util.MergeAdapter;
import github.daneren2005.dsub.util.TabActivityBackgroundTask;
import github.daneren2005.dsub.util.Util;

/**
 * Performs searches and displays the matching artists, albums and songs.
 *
 * @author Sindre Mehus
 */
public class SearchActivity extends SubsonicTabActivity {

    private static final int DEFAULT_ARTISTS = 3;
    private static final int DEFAULT_ALBUMS = 5;
    private static final int DEFAULT_SONGS = 10;

    private static final int MAX_ARTISTS = 10;
    private static final int MAX_ALBUMS = 20;
    private static final int MAX_SONGS = 25;
    private ListView list;

    private View artistsHeading;
    private View albumsHeading;
    private View songsHeading;
    private TextView searchButton;
    private View moreArtistsButton;
    private View moreAlbumsButton;
    private View moreSongsButton;
    private SearchResult searchResult;
    private MergeAdapter mergeAdapter;
    private ArtistAdapter artistAdapter;
    private ListAdapter moreArtistsAdapter;
    private EntryAdapter albumAdapter;
    private ListAdapter moreAlbumsAdapter;
    private ListAdapter moreSongsAdapter;
    private EntryAdapter songAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        setTitle(R.string.search_title);

        View buttons = LayoutInflater.from(this).inflate(R.layout.search_buttons, null);

        artistsHeading = buttons.findViewById(R.id.search_artists);
        albumsHeading = buttons.findViewById(R.id.search_albums);
        songsHeading = buttons.findViewById(R.id.search_songs);

        searchButton = (TextView) buttons.findViewById(R.id.search_search);
        moreArtistsButton = buttons.findViewById(R.id.search_more_artists);
        moreAlbumsButton = buttons.findViewById(R.id.search_more_albums);
        moreSongsButton = buttons.findViewById(R.id.search_more_songs);

        list = (ListView) findViewById(R.id.search_list);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view == searchButton) {
                    onSearchRequested();
                } else if (view == moreArtistsButton) {
                    expandArtists();
                } else if (view == moreAlbumsButton) {
                    expandAlbums();
                } else if (view == moreSongsButton) {
                    expandSongs();
                } else {
                    Object item = parent.getItemAtPosition(position);
                    if (item instanceof Artist) {
                        onArtistSelected((Artist) item);
                    } else if (item instanceof MusicDirectory.Entry) {
                        MusicDirectory.Entry entry = (MusicDirectory.Entry) item;
                        if (entry.isDirectory()) {
                            onAlbumSelected(entry, false);
                        } else if (entry.isVideo()) {
                            onVideoSelected(entry);
                        } else {
                            onSongSelected(entry, false, true, true, false);
                        }

                    }
                }
            }
        });
        registerForContextMenu(list);
        onNewIntent(getIntent());
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		Intent intent;
        switch (item.getItemId()) {
			case R.id.menu_search:
				onSearchRequested();
				return true;
            case R.id.menu_exit:
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
                Util.startActivityWithoutTransition(this, intent);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String query = intent.getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY);
        boolean autoplay = intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
        boolean requestsearch = intent.getBooleanExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, false);

        if (query != null) {
            mergeAdapter = new MergeAdapter();
            list.setAdapter(mergeAdapter);
            search(query, autoplay);
        } else {
            populateList();
            if (requestsearch)
                onSearchRequested();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Object selectedItem = list.getItemAtPosition(info.position);

        boolean isArtist = selectedItem instanceof Artist;
        boolean isAlbum = selectedItem instanceof MusicDirectory.Entry && ((MusicDirectory.Entry) selectedItem).isDirectory();
        boolean isSong = selectedItem instanceof MusicDirectory.Entry && (!((MusicDirectory.Entry) selectedItem).isDirectory())
                && (!((MusicDirectory.Entry) selectedItem).isVideo());

        if (isArtist || isAlbum) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.select_album_context, menu);
        } else if (isSong) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.select_song_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Object selectedItem = list.getItemAtPosition(info.position);

        Artist artist = selectedItem instanceof Artist ? (Artist) selectedItem : null;
        MusicDirectory.Entry entry = selectedItem instanceof MusicDirectory.Entry ? (MusicDirectory.Entry) selectedItem : null;
        String id = artist != null ? artist.getId() : entry.getId();

        switch (menuItem.getItemId()) {
            case R.id.album_menu_play_now:
                downloadRecursively(id, false, false, true, false, false);
                break;
			case R.id.album_menu_play_shuffled:
				downloadRecursively(id, false, false, true, true, false);
				break;
            case R.id.album_menu_play_last:
                downloadRecursively(id, false, true, false, false, false);
                break;
			case R.id.album_menu_download:
                downloadRecursively(id, false, true, false, false, true);
                break;
            case R.id.album_menu_pin:
                downloadRecursively(id, true, true, false, false, true);
                break;
            case R.id.song_menu_play_now:
                onSongSelected(entry, false, false, true, false);
                break;
            case R.id.song_menu_play_next:
                onSongSelected(entry, false, true, false, true);
                break;
            case R.id.song_menu_play_last:
                onSongSelected(entry, false, true, false, false);
                break;
            default:
                return super.onContextItemSelected(menuItem);
        }

        return true;
    }

    private void search(final String query, final boolean autoplay) {
        BackgroundTask<SearchResult> task = new TabActivityBackgroundTask<SearchResult>(this) {
            @Override
            protected SearchResult doInBackground() throws Throwable {
                SearchCritera criteria = new SearchCritera(query, MAX_ARTISTS, MAX_ALBUMS, MAX_SONGS);
                MusicService service = MusicServiceFactory.getMusicService(SearchActivity.this);
                return service.search(criteria, SearchActivity.this, this);
            }

            @Override
            protected void done(SearchResult result) {
                searchResult = result;
                populateList();
                if (autoplay) {
                    autoplay();
                }

            }
        };
        task.execute();
    }

    private void populateList() {
        mergeAdapter = new MergeAdapter();
        mergeAdapter.addView(searchButton, true);

        if (searchResult != null) {
            List<Artist> artists = searchResult.getArtists();
            if (!artists.isEmpty()) {
                mergeAdapter.addView(artistsHeading);
                List<Artist> displayedArtists = new ArrayList<Artist>(artists.subList(0, Math.min(DEFAULT_ARTISTS, artists.size())));
                artistAdapter = new ArtistAdapter(this, displayedArtists);
                mergeAdapter.addAdapter(artistAdapter);
                if (artists.size() > DEFAULT_ARTISTS) {
                    moreArtistsAdapter = mergeAdapter.addView(moreArtistsButton, true);
                }
            }

            List<MusicDirectory.Entry> albums = searchResult.getAlbums();
            if (!albums.isEmpty()) {
                mergeAdapter.addView(albumsHeading);
                List<MusicDirectory.Entry> displayedAlbums = new ArrayList<MusicDirectory.Entry>(albums.subList(0, Math.min(DEFAULT_ALBUMS, albums.size())));
                albumAdapter = new EntryAdapter(this, getImageLoader(), displayedAlbums, false);
                mergeAdapter.addAdapter(albumAdapter);
                if (albums.size() > DEFAULT_ALBUMS) {
                    moreAlbumsAdapter = mergeAdapter.addView(moreAlbumsButton, true);
                }
            }

            List<MusicDirectory.Entry> songs = searchResult.getSongs();
            if (!songs.isEmpty()) {
                mergeAdapter.addView(songsHeading);
                List<MusicDirectory.Entry> displayedSongs = new ArrayList<MusicDirectory.Entry>(songs.subList(0, Math.min(DEFAULT_SONGS, songs.size())));
                songAdapter = new EntryAdapter(this, getImageLoader(), displayedSongs, false);
                mergeAdapter.addAdapter(songAdapter);
                if (songs.size() > DEFAULT_SONGS) {
                    moreSongsAdapter = mergeAdapter.addView(moreSongsButton, true);
                }
            }

            boolean empty = searchResult.getArtists().isEmpty() && searchResult.getAlbums().isEmpty() && searchResult.getSongs().isEmpty();
            searchButton.setText(empty ? R.string.search_no_match : R.string.search_search);
        }

        list.setAdapter(mergeAdapter);
    }

    private void expandArtists() {
        artistAdapter.clear();
        for (Artist artist : searchResult.getArtists()) {
            artistAdapter.add(artist);
        }
        artistAdapter.notifyDataSetChanged();
        mergeAdapter.removeAdapter(moreArtistsAdapter);
        mergeAdapter.notifyDataSetChanged();
    }

    private void expandAlbums() {
        albumAdapter.clear();
        for (MusicDirectory.Entry album : searchResult.getAlbums()) {
            albumAdapter.add(album);
        }
        albumAdapter.notifyDataSetChanged();
        mergeAdapter.removeAdapter(moreAlbumsAdapter);
        mergeAdapter.notifyDataSetChanged();
    }

    private void expandSongs() {
        songAdapter.clear();
        for (MusicDirectory.Entry song : searchResult.getSongs()) {
            songAdapter.add(song);
        }
        songAdapter.notifyDataSetChanged();
        mergeAdapter.removeAdapter(moreSongsAdapter);
        mergeAdapter.notifyDataSetChanged();
    }

    private void onArtistSelected(Artist artist) {
        Intent intent = new Intent(this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
        Util.startActivityWithoutTransition(this, intent);
    }

    private void onAlbumSelected(MusicDirectory.Entry album, boolean autoplay) {
        Intent intent = new Intent(SearchActivity.this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, album.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, album.getTitle());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, autoplay);
        Util.startActivityWithoutTransition(SearchActivity.this, intent);
    }

    private void onSongSelected(MusicDirectory.Entry song, boolean save, boolean append, boolean autoplay, boolean playNext) {
        DownloadService downloadService = getDownloadService();
        if (downloadService != null) {
            if (!append) {
                downloadService.clear();
            }
            downloadService.download(Arrays.asList(song), save, false, playNext, false);
            if (autoplay) {
                downloadService.play(downloadService.size() - 1);
            }

            Util.toast(SearchActivity.this, getResources().getQuantityString(R.plurals.select_album_n_songs_added, 1, 1));
        }
    }

    private void onVideoSelected(MusicDirectory.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MusicServiceFactory.getMusicService(this).getVideoUrl(this, entry.getId())));
        startActivity(intent);
    }

    private void autoplay() {
        if (!searchResult.getSongs().isEmpty()) {
            onSongSelected(searchResult.getSongs().get(0), false, false, true, false);
        } else if (!searchResult.getAlbums().isEmpty()) {
            onAlbumSelected(searchResult.getAlbums().get(0), true);
        }
    }
}