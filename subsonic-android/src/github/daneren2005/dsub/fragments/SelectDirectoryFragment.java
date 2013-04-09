package github.daneren2005.dsub.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
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
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SelectDirectoryFragment extends LibraryFunctionsFragment implements AdapterView.OnItemClickListener {
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

	String id;
	String name;
	String playlistId;
	String playlistName;
	String albumListType;
	int albumListSize;
	int albumListOffset;

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

		Bundle args = getArguments();
		if(args != null) {
			id = args.getString(Constants.INTENT_EXTRA_NAME_ID);
			name = args.getString(Constants.INTENT_EXTRA_NAME_NAME);
			playlistId = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
			playlistName = args.getString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
			albumListType = args.getString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
			albumListSize = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
			albumListOffset = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		}
		load(false);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater menuInflater) {
		if(licenseValid == null) {
			menuInflater.inflate(R.menu.empty, menu);
		}
		else if(hideButtons) {
			if(albumListType != null) {
				menuInflater.inflate(R.menu.select_album_list, menu);
			} else {
				menuInflater.inflate(R.menu.select_album, menu);
			}
			hideButtons = false;
		} else {
			if(Util.isOffline(context)) {
				menuInflater.inflate(R.menu.select_song_offline, menu);
			}
			else {
				menuInflater.inflate(R.menu.select_song, menu);

				if(playlistId == null) {
					menu.removeItem(R.id.menu_remove_playlist);
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_play_now:
				playNow(false, false);
				return true;
			case R.id.menu_play_last:
				playNow(false, true);
				return true;
			case R.id.menu_shuffle:
				playNow(true, false);
				return true;
			case R.id.menu_select:
				selectAllOrNone();
				return true;
			case R.id.menu_download:
				downloadBackground(false);
				selectAll(false, false);
				return true;
			case R.id.menu_cache:
				downloadBackground(true);
				selectAll(false, false);
				return true;
			case R.id.menu_delete:
				delete();
				selectAll(false, false);
				return true;
			case R.id.menu_add_playlist:
				addToPlaylist(getSelectedSongs());
				return true;
			case R.id.menu_remove_playlist:
				removeFromPlaylist(playlistId, playlistName, getSelectedIndexes());
				return true;
		}

		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position >= 0) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
			if (entry.isDirectory()) {
				SubsonicTabFragment fragment = new SelectDirectoryFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
				fragment.setArguments(args);

				final FragmentTransaction trans = getFragmentManager().beginTransaction();
				trans.replace(R.id.select_album_layout, fragment);
				trans.addToBackStack(null);
				trans.commit();
			} else if (entry.isVideo()) {
				if(entryExists(entry)) {
					playExternalPlayer(entry);
				} else {
					streamExternalPlayer(entry);
				}
			}
		}
	}

	@Override
	protected void refresh() {
		load(true);
	}

	private void load(boolean refresh) {
		entryList.setVisibility(View.INVISIBLE);
		emptyView.setVisibility(View.INVISIBLE);
		if (playlistId != null) {
			getPlaylist(playlistId, playlistName);
		} else if (albumListType != null) {
			getAlbumList(albumListType, albumListSize, albumListOffset);
		} else {
			getMusicDirectory(id, name, refresh);
		}
	}

	private void getMusicDirectory(final String id, final String name, final boolean refresh) {
		setTitle(name);

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getMusicDirectory(id, name, refresh, context, this);
			}
		}.execute();
	}

	private void getPlaylist(final String playlistId, final String playlistName) {
		setTitle(playlistName);

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getPlaylist(playlistId, playlistName, context, this);
			}
		}.execute();
	}

	private void getAlbumList(final String albumListType, final int size, final int offset) {
		showHeader = false;

		if ("newest".equals(albumListType)) {
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
		}

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
						if(entryList.getFooterViewsCount() == 0) {
							entryList.addFooterView(footer);
						}
					}

					moreButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							albumListOffset += albumListSize;
							refresh();
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
			entryList.setVisibility(View.VISIBLE);
			licenseValid = result.getSecond();
			context.invalidateOptionsMenu();

			Bundle args = getArguments();
			boolean playAll = args.getBoolean(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
			if (playAll && songCount > 0) {
				playAll(args.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, false), false);
			}
		}
	}

	private void playNow(final boolean shuffle, final boolean append) {
		if(getSelectedSongs().size() > 0) {
			download(append, false, !append, false, shuffle);
			selectAll(false, false);
		}
		else {
			playAll(shuffle, append);
		}
	}
	private void playAll(final boolean shuffle, final boolean append) {
		boolean hasSubFolders = false;
		for (int i = 0; i < entryList.getCount(); i++) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) entryList.getItemAtPosition(i);
			if (entry != null && entry.isDirectory()) {
				hasSubFolders = true;
				break;
			}
		}

		if (hasSubFolders && id != null) {
			downloadRecursively(id, false, append, !append, shuffle, false);
		} else {
			selectAll(true, false);
			download(append, false, !append, false, shuffle);
			selectAll(false, false);
		}
	}

	private void selectAllOrNone() {
		boolean someUnselected = false;
		int count = entryList.getCount();
		for (int i = 0; i < count; i++) {
			if (!entryList.isItemChecked(i) && entryList.getItemAtPosition(i) instanceof MusicDirectory.Entry) {
				someUnselected = true;
				break;
			}
		}
		selectAll(someUnselected, true);
	}

	private void selectAll(boolean selected, boolean toast) {
		int count = entryList.getCount();
		int selectedCount = 0;
		for (int i = 0; i < count; i++) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) entryList.getItemAtPosition(i);
			if (entry != null && !entry.isDirectory() && !entry.isVideo()) {
				entryList.setItemChecked(i, selected);
				selectedCount++;
			}
		}

		// Display toast: N tracks selected / N tracks unselected
		if (toast) {
			int toastResId = selected ? R.string.select_album_n_selected
									  : R.string.select_album_n_unselected;
			Util.toast(context, context.getString(toastResId, selectedCount));
		}
	}

	private List<MusicDirectory.Entry> getSelectedSongs() {
		List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
		int count = entryList.getCount();
		for (int i = 0; i < count; i++) {
			if (entryList.isItemChecked(i)) {
				songs.add((MusicDirectory.Entry) entryList.getItemAtPosition(i));
			}
		}
		return songs;
	}

	private List<Integer> getSelectedIndexes() {
		List<Integer> indexes = new ArrayList<Integer>();

		int count = entryList.getCount();
		for (int i = 0; i < count; i++) {
			if (entryList.isItemChecked(i)) {
				indexes.add(i - 1);
			}
		}

		return indexes;
	}

	private void download(final boolean append, final boolean save, final boolean autoplay, final boolean playNext, final boolean shuffle) {
		if (getDownloadService() == null) {
			return;
		}

		final List<MusicDirectory.Entry> songs = getSelectedSongs();
		Runnable onValid = new Runnable() {
			@Override
			public void run() {
				if (!append) {
					getDownloadService().clear();
				}

				warnIfNetworkOrStorageUnavailable();
				getDownloadService().download(songs, save, autoplay, playNext, shuffle);
				if (playlistName != null) {
					getDownloadService().setSuggestedPlaylistName(playlistName);
				}
				if (autoplay) {
					Util.startActivityWithoutTransition(context, DownloadActivity.class);
				} else if (save) {
					Util.toast(context,
							   context.getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
				} else if (append) {
					Util.toast(context,
							   context.getResources().getQuantityString(R.plurals.select_album_n_songs_added, songs.size(), songs.size()));
				}
			}
		};

		checkLicenseAndTrialPeriod(onValid);
	}
	private void downloadBackground(final boolean save) {
		List<MusicDirectory.Entry> songs = getSelectedSongs();
		if(songs.isEmpty()) {
			selectAll(true, false);
			songs = getSelectedSongs();
		}
		downloadBackground(save, songs);
	}
	private void downloadBackground(final boolean save, final List<MusicDirectory.Entry> songs) {
		if (getDownloadService() == null) {
			return;
		}

		Runnable onValid = new Runnable() {
			@Override
			public void run() {
				warnIfNetworkOrStorageUnavailable();
				getDownloadService().downloadBackground(songs, save);

				Util.toast(context,
					context.getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
			}
		};

		checkLicenseAndTrialPeriod(onValid);
	}

	private void delete() {
		List<MusicDirectory.Entry> songs = getSelectedSongs();
		if(songs.isEmpty()) {
			selectAll(true, false);
			songs = getSelectedSongs();
		}
		if (getDownloadService() != null) {
			getDownloadService().delete(songs);
		}
	}

	private boolean entryExists(MusicDirectory.Entry entry) {
		DownloadFile check = new DownloadFile(context, entry, false);
		return check.isCompleteFileAvailable();
	}

	private void playWebView(MusicDirectory.Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(context);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoUrl(maxBitrate, context, entry.getId())));

		startActivity(intent);
	}
	private void playExternalPlayer(MusicDirectory.Entry entry) {
		if(!entryExists(entry)) {
			Util.toast(context, R.string.download_need_download);
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(entry.getPath()), "video/*");

			List<ResolveInfo> intents = context.getPackageManager()
				.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if(intents != null && intents.size() > 0) {
				startActivity(intent);
			}else {
				Util.toast(context, R.string.download_no_streaming_player);
			}
		}
	}
	private void streamExternalPlayer(MusicDirectory.Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(context);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(context).getVideoStreamUrl(maxBitrate, context, entry.getId())), "video/*");

		List<ResolveInfo> intents = context.getPackageManager()
			.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if(intents != null && intents.size() > 0) {
			startActivity(intent);
		} else {
			Util.toast(context, R.string.download_no_streaming_player);
		}
	}

	public void deleteRecursively(MusicDirectory.Entry album) {
		File dir = FileUtil.getAlbumDirectory(context, album);
		Util.recursiveDelete(dir);
		if(Util.isOffline(context)) {
			refresh();
		}
	}

	public void removeFromPlaylist(final String id, final String name, final List<Integer> indexes) {
		new LoadingTask<Void>(context, true) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.removeFromPlaylist(id, indexes, context, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				for(int i = indexes.size() - 1; i >= 0; i--) {
					entryList.setItemChecked(indexes.get(i) + 1, false);
					entryAdapter.removeAt(indexes.get(i));
				}
				entryAdapter.notifyDataSetChanged();
				Util.toast(context, context.getResources().getString(R.string.removed_playlist, indexes.size(), name));
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.updated_playlist_error, name) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	private void checkLicenseAndTrialPeriod(Runnable onValid) {
		if (licenseValid) {
			onValid.run();
			return;
		}

		int trialDaysLeft = Util.getRemainingTrialDays(context);
		Log.i(TAG, trialDaysLeft + " trial days left.");

		if (trialDaysLeft == 0) {
			showDonationDialog(trialDaysLeft, null);
		} else if (trialDaysLeft < Constants.FREE_TRIAL_DAYS / 2) {
			showDonationDialog(trialDaysLeft, onValid);
		} else {
			Util.toast(context, context.getResources().getString(R.string.select_album_not_licensed, trialDaysLeft));
			onValid.run();
		}
	}

	private void showDonationDialog(int trialDaysLeft, final Runnable onValid) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(android.R.drawable.ic_dialog_info);

		if (trialDaysLeft == 0) {
			builder.setTitle(R.string.select_album_donate_dialog_0_trial_days_left);
		} else {
			builder.setTitle(context.getResources().getQuantityString(R.plurals.select_album_donate_dialog_n_trial_days_left,
															  trialDaysLeft, trialDaysLeft));
		}

		builder.setMessage(R.string.select_album_donate_dialog_message);

		builder.setPositiveButton(R.string.select_album_donate_dialog_now,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL)));
				}
			});

		builder.setNegativeButton(R.string.select_album_donate_dialog_later,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
					if (onValid != null) {
						onValid.run();
					}
				}
			});

		builder.create().show();
	}

	private View createHeader(List<MusicDirectory.Entry> entries) {
		View header = LayoutInflater.from(context).inflate(R.layout.select_album_header, entryList, false);

		View coverArtView = header.findViewById(R.id.select_album_art);
		getImageLoader().loadImage(coverArtView, entries.get(0), true, true);

		TextView titleView = (TextView) header.findViewById(R.id.select_album_title);
		if(playlistName != null) {
			titleView.setText(playlistName);
		} else if(name != null) {
			titleView.setText(name);
		}

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