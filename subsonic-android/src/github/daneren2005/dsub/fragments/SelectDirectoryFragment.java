package github.daneren2005.dsub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.view.EntryAdapter;
import java.util.List;
import com.mobeta.android.dslv.*;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.TabBackgroundTask;
import java.util.HashSet;
import java.util.Set;

public class SelectDirectoryFragment extends SubsonicTabFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectDirectoryFragment.class.getSimpleName();

	private DragSortListView entryList;
	private View footer;
	private View emptyView;
	private boolean hideButtons = false;
	private Button moreButton;
	private Boolean licenseValid;
	private boolean showHeader = true;
	private EntryAdapter entryAdapter;
	private List<MusicDirectory.Entry> entries;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.select_album, container, false);

		entryList = (DragSortListView) rootView.findViewById(R.id.select_album_entries);
		footer = LayoutInflater.from(context).inflate(R.layout.select_album_footer, entryList, false);
		entryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		entryList.setOnItemClickListener(this);
		entryList.setDropListener(new DragSortListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				int max = entries.size();
				if(to >= max) {
					to = max - 1;
				}
				else if(to < 0) {
					to = 0;
				}
				entries.add(to, entries.remove(from));
				entryAdapter.notifyDataSetChanged();
			}
		});

		moreButton = (Button) footer.findViewById(R.id.select_album_more);
		emptyView = rootView.findViewById(R.id.select_album_empty);

		registerForContextMenu(entryList);
		load(false);

		return rootView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position >= 0) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
			/*if (entry.isDirectory()) {
				Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
				Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
			} else if (entry.isVideo()) {
				if(entryExists(entry)) {
					playExternalPlayer(entry);
				} else {
					streamExternalPlayer(entry);
				}
			}*/
		}
	}

	@Override
	protected void refresh() {
		load(true);
	}

	private void load(boolean refresh) {
		Bundle args = getArguments();
		if(args != null) {
			String id = args.getString(Constants.INTENT_EXTRA_NAME_ID);
			String name = args.getString(Constants.INTENT_EXTRA_NAME_NAME);
			String playlistId = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
			String playlistName = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
			String albumListType = args.getString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
			int albumListSize = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
			int albumListOffset = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);

			if (playlistId != null) {
				getPlaylist(playlistId, playlistName);
			} else if (albumListType != null) {
				getAlbumList(albumListType, albumListSize, albumListOffset);
			} else {
				getMusicDirectory(id, name, refresh);
			}
		}
	}

	private void getMusicDirectory(final String id, final String name, final boolean refresh) {
		// setTitle(name);

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getMusicDirectory(id, name, refresh, context, this);
			}
		}.execute();
	}

	private void getPlaylist(final String playlistId, final String playlistName) {
		// setTitle(playlistName);

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getPlaylist(playlistId, playlistName, context, this);
			}
		}.execute();
	}

	private void getAlbumList(final String albumListType, final int size, final int offset) {
		showHeader = false;

		/*if ("newest".equals(albumListType)) {
			setTitle(R.string.main_albums_newest);
		} else if ("random".equals(albumListType)) {
			setTitle(R.string.main_albums_random);
		} else if ("highest".equals(albumListType)) {
			setTitle(R.string.main_albums_highest);
		} else if ("recent".equals(albumListType)) {
			setTitle(R.string.main_albums_recent);
		} else if ("frequent".equals(albumListType)) {
			setTitle(R.string.main_albums_frequent);
		} else if ("starred".equals(albumListType)) {
			setTitle(R.string.main_albums_starred);
		}*/

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				MusicDirectory result;
				if ("starred".equals(albumListType)) {
					result = service.getStarredList(context, this);
				} else {
					result = service.getAlbumList(albumListType, size, offset, context, this);
				}
				return result;
			}

			@Override
			protected void done(Pair<MusicDirectory, Boolean> result) {
				if (!result.getFirst().getChildren().isEmpty()) {
					if (!("starred".equals(albumListType))) {
						moreButton.setVisibility(View.VISIBLE);
						entryList.addFooterView(footer);
					}

					moreButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							/*Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
							String type = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
							int size = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
							int offset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0) + size;

							intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
							intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, size);
							intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, offset);
							Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);*/
						}
					});
				}
				super.done(result);
			}
		}.execute();
	}

	private abstract class LoadTask extends TabBackgroundTask<Pair<MusicDirectory, Boolean>> {

		public LoadTask() {
			super(SelectDirectoryFragment.this);
		}

		protected abstract MusicDirectory load(MusicService service) throws Exception;

		@Override
		protected Pair<MusicDirectory, Boolean> doInBackground() throws Throwable {
			MusicService musicService = MusicServiceFactory.getMusicService(context);
			MusicDirectory dir = load(musicService);
			boolean valid = musicService.isLicenseValid(context, this);
			return new Pair<MusicDirectory, Boolean>(dir, valid);
		}

		@Override
		protected void done(Pair<MusicDirectory, Boolean> result) {
			entries = result.getFirst().getChildren();

			int songCount = 0;
			for (MusicDirectory.Entry entry : entries) {
				if (!entry.isDirectory()) {
					songCount++;
				}
			}

			if (songCount > 0) {
				if(showHeader) {
					entryList.addHeaderView(createHeader(entries), null, false);
				}
			} else {
				hideButtons = true;
			}

			emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
			entryList.setAdapter(entryAdapter = new EntryAdapter(context, getImageLoader(), entries, true));
			licenseValid = result.getSecond();
			// invalidateOptionsMenu();

			/*boolean playAll = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
			if (playAll && songCount > 0) {
				playAll(getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false), false);
			}*/
		}
	}

	private View createHeader(List<MusicDirectory.Entry> entries) {
		View header = LayoutInflater.from(context).inflate(R.layout.select_album_header, entryList, false);

		View coverArtView = header.findViewById(R.id.select_album_art);
		getImageLoader().loadImage(coverArtView, entries.get(0), true, true);

		TextView titleView = (TextView) header.findViewById(R.id.select_album_title);
		// titleView.setText(getTitle());

		int songCount = 0;

		Set<String> artists = new HashSet<String>();
		for (MusicDirectory.Entry entry : entries) {
			if (!entry.isDirectory()) {
				songCount++;
				if (entry.getArtist() != null) {
					artists.add(entry.getArtist());
				}
			}
		}

		TextView artistView = (TextView) header.findViewById(R.id.select_album_artist);
		if (artists.size() == 1) {
			artistView.setText(artists.iterator().next());
			artistView.setVisibility(View.VISIBLE);
		} else {
			artistView.setVisibility(View.GONE);
		}

		TextView songCountView = (TextView) header.findViewById(R.id.select_album_song_count);
		String s = context.getResources().getQuantityString(R.plurals.select_album_n_songs, songCount, songCount);
		songCountView.setText(s.toUpperCase());

		return header;
	}
}