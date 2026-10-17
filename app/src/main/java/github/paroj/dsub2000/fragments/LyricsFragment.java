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

package github.paroj.dsub2000.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.adapter.LyricsAdapter;
import github.paroj.dsub2000.domain.Lyrics;
import github.paroj.dsub2000.domain.LyricsLine;
import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.domain.PlayerState;
import github.paroj.dsub2000.domain.StructuredLyrics;
import github.paroj.dsub2000.service.DownloadFile;
import github.paroj.dsub2000.service.DownloadService;
import github.paroj.dsub2000.service.MusicService;
import github.paroj.dsub2000.service.MusicServiceFactory;
import github.paroj.dsub2000.util.BackgroundTask;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.TabBackgroundTask;
import github.paroj.dsub2000.util.Util;

/**
 * Displays song lyrics.
 *
 * Prefers the OpenSubsonic getLyricsBySongId endpoint, which is keyed by song
 * id and can return timestamped lines.  Synced lyrics follow playback: the
 * current line is highlighted, the list scrolls to keep it in view, and tapping
 * a line seeks to it.  Servers without that extension fall back to the legacy
 * artist/title based getLyrics endpoint.
 *
 * @author Sindre Mehus
 */
public final class LyricsFragment extends SubsonicFragment implements DownloadService.OnSongChangedListener {
	private static final String TAG = LyricsFragment.class.getSimpleName();
	private static final String STATE_VERSIONS = "lyrics.versions";
	private static final String STATE_SELECTED_VERSION = "lyrics.selectedVersion";

	// The service only refreshes its cached position once a second, so the
	// position is interpolated between updates to keep highlighting tight.
	private static final long INTERPOLATE_INTERVAL = 200L;
	// A jump larger than this between the expected and reported position is a seek.
	private static final long SEEK_THRESHOLD = 2000L;
	// Beyond this many lines, jump rather than animate.
	private static final int SNAP_DISTANCE = 4;

	private TextView artistView;
	private TextView titleView;
	private TextView textView;
	private ScrollView scrollView;
	private RecyclerView lineList;
	private LinearLayoutManager layoutManager;
	private LyricsAdapter adapter;

	private Lyrics lyrics;
	private ArrayList<StructuredLyrics> lyricsVersions;
	private int selectedVersion = -1;
	private StructuredLyrics structuredLyrics;

	private String songId;
	private String songArtist;
	private String songTitle;
	private int loadGeneration = 0;

	private boolean autoScroll = true;
	private boolean userScrolling = false;
	private boolean started = false;
	private boolean playing = false;
	private boolean hasPosition = false;
	private long lastKnownPosition = 0L;
	private long lastKnownAt = 0L;

	private final Handler handler = new Handler();
	private Runnable interpolateRunnable;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null) {
			lyrics = (Lyrics) bundle.getSerializable(Constants.FRAGMENT_LIST);
			lyricsVersions = (ArrayList<StructuredLyrics>) bundle.getSerializable(STATE_VERSIONS);
			selectedVersion = bundle.getInt(STATE_SELECTED_VERSION, -1);
			structuredLyrics = versionAt(selectedVersion);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.FRAGMENT_LIST, lyrics);
		outState.putSerializable(STATE_VERSIONS, lyricsVersions);
		outState.putInt(STATE_SELECTED_VERSION, selectedVersion);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		setTitle(R.string.download_menu_lyrics);
		setSubtitle(null);
		rootView = inflater.inflate(R.layout.lyrics, container, false);
		artistView = (TextView) rootView.findViewById(R.id.lyrics_artist);
		titleView = (TextView) rootView.findViewById(R.id.lyrics_title);
		textView = (TextView) rootView.findViewById(R.id.lyrics_text);
		scrollView = (ScrollView) rootView.findViewById(R.id.lyrics_scroll);
		lineList = (RecyclerView) rootView.findViewById(R.id.lyrics_list);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(this);
		setupScrollList(lineList);

		layoutManager = getLinearLayoutManager();
		lineList.setLayoutManager(layoutManager);
		// The default change animation cross-fades item alpha, which is also what
		// marks the current line. Without this, rows that scroll off during a
		// highlight change keep the animator's alpha and stay lit.
		lineList.setItemAnimator(null);
		lineList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				if(newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					// A drag opens a user scroll session; a fling keeps it open
					// through SETTLING until the list comes to rest.
					userScrolling = true;
				} else if(newState == RecyclerView.SCROLL_STATE_IDLE && userScrolling) {
					updateAutoScroll();
					userScrolling = false;
				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				// Only react to scrolling the user drove. Programmatic scrolling
				// moves towards a line that is off screen by definition, and
				// would otherwise immediately switch following off.
				if(userScrolling) {
					updateAutoScroll();
				}
			}
		});

		songId = getArguments().getString(Constants.INTENT_EXTRA_NAME_ID);
		songArtist = getArguments().getString(Constants.INTENT_EXTRA_NAME_ARTIST);
		songTitle = getArguments().getString(Constants.INTENT_EXTRA_NAME_TITLE);

		if(lyrics == null && structuredLyrics == null) {
			load(songId, songArtist, songTitle);
		} else {
			setLyrics();
		}

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		started = true;

		context.runWhenServiceAvailable(new Runnable() {
			@Override
			public void run() {
				// The service can come up only after the fragment has stopped
				// again; a listener registered then would never be removed.
				if(!started) {
					return;
				}

				DownloadService downloadService = getDownloadService();
				if(downloadService == null) {
					return;
				}

				downloadService.addOnSongChangedListener(LyricsFragment.this, true);

				// Reading along is hard if the screen turns off. Only honours the
				// existing preference; NowPlayingFragment owns clearing the flag.
				if(downloadService.getKeepScreenOn()) {
					context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}
			}
		});

		startInterpolating();
	}

	@Override
	public void onStop() {
		super.onStop();
		started = false;

		DownloadService downloadService = getDownloadService();
		if(downloadService != null) {
			downloadService.removeOnSongChangeListener(this);
		}

		stopInterpolating();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		// Only worth offering when the server actually returned alternatives.
		if(lyricsVersions != null && lyricsVersions.size() > 1) {
			menuInflater.inflate(R.menu.lyrics, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu_lyrics_version) {
			showVersionDialog();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void refresh(boolean refresh) {
		lyrics = null;
		lyricsVersions = null;
		selectedVersion = -1;
		structuredLyrics = null;
		autoScroll = true;

		load(songId, songArtist, songTitle);
	}

	// OnSongChangedListener

	@Override
	public void onSongChanged(DownloadFile currentPlaying, int currentPlayingIndex, boolean shouldFastForward) {
		if(currentPlaying == null) {
			return;
		}

		// Lyrics can be opened for any song, not just the one playing, so whether
		// they stay anchored to that song or move with the queue is a preference.
		if(!Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_LYRICS_FOLLOW_QUEUE, true)) {
			return;
		}

		MusicDirectory.Entry song = currentPlaying.getSong();
		if(song == null || song.getId() == null || song.getId().equals(songId)) {
			return;
		}

		songId = song.getId();
		songArtist = song.getArtist();
		songTitle = song.getTitle();

		lyrics = null;
		lyricsVersions = null;
		selectedVersion = -1;
		structuredLyrics = null;
		autoScroll = true;
		hasPosition = false;
		lastKnownPosition = 0L;

		load(songId, songArtist, songTitle);
	}

	@Override
	public void onSongsChanged(List<DownloadFile> songs, DownloadFile currentPlaying, int currentPlayingIndex, boolean shouldFastForward) {

	}

	@Override
	public void onSongProgress(DownloadFile currentPlaying, int millisPlayed, Integer duration, boolean isSeekable) {
		long now = SystemClock.elapsedRealtime();
		boolean seeked = hasPosition && Math.abs(millisPlayed - estimatePosition(now)) > SEEK_THRESHOLD;

		lastKnownPosition = millisPlayed;
		lastKnownAt = now;
		hasPosition = true;

		if(seeked) {
			// Jumping deliberately is a request to follow along again.
			autoScroll = true;
		}

		updateActiveLine(seeked);
	}

	@Override
	public void onStateUpdate(DownloadFile downloadFile, PlayerState playerState) {
		// Collapse the interpolation into the stored position before the playing
		// flag changes. Otherwise time spent paused would count into the estimate,
		// and the first real position after resuming would look like a seek.
		if(hasPosition) {
			long now = SystemClock.elapsedRealtime();
			lastKnownPosition = estimatePosition(now);
			lastKnownAt = now;
		}

		playing = playerState == PlayerState.STARTED;
	}

	@Override
	public void onMetadataUpdate(MusicDirectory.Entry entry, int fieldChange) {

	}

	private void load(final String id, final String artist, final String title) {
		final int generation = ++loadGeneration;

		BackgroundTask<LoadResult> task = new TabBackgroundTask<LoadResult>(this) {
			@Override
			protected LoadResult doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				LoadResult result = new LoadResult();

				if(id != null) {
					try {
						result.versions = usableVersions(musicService.getLyricsBySongId(id, context, this));
					} catch(Exception e) {
						// Server does not support the songLyrics extension, or the
						// lookup failed. Fall back to the legacy endpoint below.
						Log.d(TAG, "getLyricsBySongId failed, falling back to getLyrics", e);
					}
				}

				result.selected = selectVersion(result.versions);
				if(result.selected < 0) {
					result.plain = musicService.getLyrics(artist, title, context, this);
				}

				return result;
			}

			@Override
			protected void done(LoadResult result) {
				// Skipping tracks quickly can finish loads out of order. Loads run
				// concurrently, so the fields are only assigned here on the main
				// thread, where the generation check and the assignment are atomic.
				if(generation != loadGeneration) {
					return;
				}

				lyricsVersions = result.versions;
				selectedVersion = result.selected;
				structuredLyrics = versionAt(result.selected);
				lyrics = result.plain;

				setLyrics();
				updateActiveLine(true);

				// The version picker only exists when there is something to pick.
				context.supportInvalidateOptionsMenu();
			}
		};
		task.execute();
	}

	private static class LoadResult {
		private ArrayList<StructuredLyrics> versions;
		private int selected = -1;
		private Lyrics plain;
	}

	private ArrayList<StructuredLyrics> usableVersions(List<StructuredLyrics> candidates) {
		if(candidates == null) {
			return null;
		}

		ArrayList<StructuredLyrics> usable = new ArrayList<StructuredLyrics>();
		for(StructuredLyrics candidate: candidates) {
			if(!candidate.getLines().isEmpty()) {
				usable.add(candidate);
			}
		}

		return usable.isEmpty() ? null : usable;
	}

	/**
	 * A song can carry several sets of lyrics. Prefer synced ones, and within
	 * that prefer the device language when the server tells us the language.
	 */
	private int selectVersion(List<StructuredLyrics> candidates) {
		if(candidates == null) {
			return -1;
		}

		int syncedInLanguage = -1;
		int syncedAny = -1;
		int plainInLanguage = -1;
		int plainAny = -1;

		for(int i = 0; i < candidates.size(); i++) {
			StructuredLyrics candidate = candidates.get(i);
			boolean matchesLanguage = matchesDeviceLanguage(candidate.getLang());

			if(candidate.isSynced()) {
				if(matchesLanguage && syncedInLanguage < 0) {
					syncedInLanguage = i;
				}
				if(syncedAny < 0) {
					syncedAny = i;
				}
			} else {
				if(matchesLanguage && plainInLanguage < 0) {
					plainInLanguage = i;
				}
				if(plainAny < 0) {
					plainAny = i;
				}
			}
		}

		if(syncedInLanguage >= 0) {
			return syncedInLanguage;
		} else if(syncedAny >= 0) {
			return syncedAny;
		} else if(plainInLanguage >= 0) {
			return plainInLanguage;
		} else {
			return plainAny;
		}
	}

	private StructuredLyrics versionAt(int index) {
		if(lyricsVersions == null || index < 0 || index >= lyricsVersions.size()) {
			return null;
		}
		return lyricsVersions.get(index);
	}

	private boolean matchesDeviceLanguage(String lang) {
		if(!hasKnownLanguage(lang)) {
			return false;
		}

		// The server may use either ISO 639-1 ("en") or 639-2/3 ("eng"), so
		// compare against both forms exactly; prefix matching would make a
		// device set to "es" claim Estonian ("est").
		Locale device = Locale.getDefault();
		if(lang.equalsIgnoreCase(device.getLanguage())) {
			return true;
		}

		try {
			return lang.equalsIgnoreCase(device.getISO3Language());
		} catch(MissingResourceException e) {
			// The device language has no three letter form to compare against.
			return false;
		}
	}

	private boolean hasKnownLanguage(String lang) {
		// "xxx" and "und" both mean the server does not know the language.
		return lang != null && !"xxx".equalsIgnoreCase(lang) && !"und".equalsIgnoreCase(lang);
	}

	private void showVersionDialog() {
		if(lyricsVersions == null || lyricsVersions.size() < 2) {
			return;
		}

		CharSequence[] labels = new CharSequence[lyricsVersions.size()];
		for(int i = 0; i < lyricsVersions.size(); i++) {
			labels[i] = versionLabel(lyricsVersions.get(i));
		}

		new AlertDialog.Builder(context)
			.setTitle(R.string.lyrics_select_version)
			.setSingleChoiceItems(labels, selectedVersion, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					if(which == selectedVersion) {
						return;
					}

					selectedVersion = which;
					structuredLyrics = versionAt(which);
					autoScroll = true;
					setLyrics();
					updateActiveLine(true);
				}
			})
			.show();
	}

	private String versionLabel(StructuredLyrics version) {
		String lang = version.getLang();
		String name;

		if(!hasKnownLanguage(lang)) {
			name = getString(R.string.lyrics_unknown_language);
		} else {
			// Three letter codes often have no display name; fall back to the code.
			String display = new Locale(lang).getDisplayLanguage();
			name = (display == null || display.length() == 0 || display.equalsIgnoreCase(lang)) ? lang : display;
		}

		String kind = getString(version.isSynced() ? R.string.lyrics_synced : R.string.lyrics_unsynced);
		return getString(R.string.lyrics_version_format, name, kind);
	}

	private void setLyrics() {
		if(structuredLyrics != null) {
			artistView.setText(firstNonEmpty(structuredLyrics.getDisplayArtist(), songArtist));
			titleView.setText(firstNonEmpty(structuredLyrics.getDisplayTitle(), songTitle));

			if(adapter == null) {
				adapter = new LyricsAdapter(structuredLyrics.getLines(), structuredLyrics.isSynced());
				adapter.setOnLineClickListener(new LyricsAdapter.OnLineClickListener() {
					@Override
					public void onLineClick(LyricsLine line) {
						seekToLine(line);
					}
				});
			} else {
				adapter.setLyrics(structuredLyrics.getLines(), structuredLyrics.isSynced());
			}
			// The view can be recreated while the fragment instance and its adapter
			// survive, so a non-null adapter is not necessarily attached to this list.
			if(lineList.getAdapter() != adapter) {
				lineList.setAdapter(adapter);
			}

			// These are different lyrics, so the old scroll position is meaningless.
			// Without this the next song opens wherever the last one was left.
			layoutManager.scrollToPositionWithOffset(0, 0);

			scrollView.setVisibility(View.GONE);
			lineList.setVisibility(View.VISIBLE);
		} else if(lyrics != null && lyrics.getArtist() != null) {
			artistView.setText(lyrics.getArtist());
			titleView.setText(lyrics.getTitle());
			textView.setText(lyrics.getText());
			lineList.setVisibility(View.GONE);
			scrollView.setVisibility(View.VISIBLE);
		} else {
			artistView.setText(R.string.lyrics_nomatch);
			titleView.setText(songTitle);
			textView.setText(null);
			lineList.setVisibility(View.GONE);
			scrollView.setVisibility(View.VISIBLE);
		}

		// setupScrollList only re-enables pull to refresh from the line list's
		// scroll events; when the plain text view is showing instead, the gate
		// must not stay closed at wherever the list was last left.
		if(scrollView.getVisibility() == View.VISIBLE && context.isTouchscreen()) {
			refreshLayout.setEnabled(true);
		}
	}

	private String firstNonEmpty(String first, String second) {
		if(first != null && first.length() > 0) {
			return first;
		}
		return second;
	}

	private void seekToLine(LyricsLine line) {
		DownloadService downloadService = getDownloadService();
		if(downloadService == null || line.getStart() == null || !isShowingCurrentSong()) {
			return;
		}

		downloadService.seekTo((int) Math.max(0L, line.getStart() - lyricsOffset()));
		autoScroll = true;
	}

	/**
	 * Lyrics can be opened for any song in the queue, not just the one playing.
	 * Playback position only describes the song actually playing, so following
	 * and seeking are limited to that case.
	 */
	private boolean isShowingCurrentSong() {
		DownloadService downloadService = getDownloadService();
		if(downloadService == null || songId == null) {
			return false;
		}

		DownloadFile currentPlaying = downloadService.getCurrentPlaying();
		return currentPlaying != null && currentPlaying.getSong() != null
				&& songId.equals(currentPlaying.getSong().getId());
	}

	private long lyricsOffset() {
		if(structuredLyrics == null || structuredLyrics.getOffset() == null) {
			return 0L;
		}
		return structuredLyrics.getOffset();
	}

	private long estimatePosition(long now) {
		if(!playing) {
			return lastKnownPosition;
		}
		return lastKnownPosition + (now - lastKnownAt);
	}

	private void startInterpolating() {
		if(interpolateRunnable != null) {
			return;
		}

		interpolateRunnable = new Runnable() {
			@Override
			public void run() {
				updateActiveLine(false);
				handler.postDelayed(this, INTERPOLATE_INTERVAL);
			}
		};
		handler.postDelayed(interpolateRunnable, INTERPOLATE_INTERVAL);
	}

	private void stopInterpolating() {
		if(interpolateRunnable != null) {
			handler.removeCallbacks(interpolateRunnable);
			interpolateRunnable = null;
		}
	}

	private void updateActiveLine(boolean snap) {
		if(adapter == null || lineList == null || structuredLyrics == null || !structuredLyrics.isSynced()) {
			return;
		}

		if(!isShowingCurrentSong()) {
			// Showing another song's lyrics: no line is current.
			adapter.setActiveLine(-1);
			return;
		}

		long position = estimatePosition(SystemClock.elapsedRealtime());
		long offset = lyricsOffset();

		List<LyricsLine> lines = structuredLyrics.getLines();
		int active = -1;
		for(int i = 0; i < lines.size(); i++) {
			Long start = lines.get(i).getStart();
			if(start == null) {
				continue;
			}

			if(position >= (start - offset)) {
				active = i;
			} else {
				break;
			}
		}

		int previous = adapter.getActiveLine();
		if(active == previous) {
			return;
		}
		adapter.setActiveLine(active);

		// Never scroll while the user's gesture is still in progress; when it
		// ends, the scroll session decides whether following is still on.
		if(!autoScroll || userScrolling || active < 0 || !canCenter(active)) {
			return;
		}

		if(snap || previous < 0 || Math.abs(active - previous) > SNAP_DISTANCE) {
			layoutManager.scrollToPositionWithOffset(active, centerOffset());
		} else {
			smoothScrollToCenter(active);
		}
	}

	/**
	 * Whether the line can actually be moved to the middle.
	 *
	 * Near the start of the lyrics the list is already scrolled to the top, so
	 * centring would mean scrolling past the beginning; the same applies at the
	 * end. Asking for that scroll does not move anything, it just triggers the
	 * overscroll bounce, so the early and late lines are left where they are and
	 * scrolling begins once the current line actually reaches the middle.
	 */
	private boolean canCenter(int position) {
		View child = layoutManager.findViewByPosition(position);
		if(child == null) {
			// Off screen, so scrolling is both possible and needed.
			return true;
		}

		int listCenter = lineList.getHeight() / 2;
		int childCenter = (child.getTop() + child.getBottom()) / 2;
		int delta = childCenter - listCenter;

		if(delta > 0) {
			return lineList.canScrollVertically(1);
		} else if(delta < 0) {
			return lineList.canScrollVertically(-1);
		}

		return false;
	}

	/**
	 * Offset that leaves the current line in the middle of the list.
	 */
	private int centerOffset() {
		int listHeight = lineList.getHeight();
		if(listHeight <= 0) {
			return 0;
		}

		int rowHeight = 0;
		View child = layoutManager.findViewByPosition(adapter.getActiveLine());
		if(child != null) {
			rowHeight = child.getHeight();
		} else if(lineList.getChildCount() > 0) {
			rowHeight = lineList.getChildAt(0).getHeight();
		}

		return Math.max(0, (listHeight - rowHeight) / 2);
	}

	/**
	 * smoothScrollToPosition only scrolls far enough to bring a line into view,
	 * which leaves it pinned to the bottom edge with nothing visible ahead of it.
	 * Centring instead keeps the upcoming lines readable.
	 */
	private void smoothScrollToCenter(int position) {
		LinearSmoothScroller scroller = new LinearSmoothScroller(context) {
			@Override
			public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
				return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
			}
		};
		scroller.setTargetPosition(position);
		layoutManager.startSmoothScroll(scroller);
	}

	/**
	 * Following stops once the user scrolls the current line out of view, and
	 * resumes when they bring it back.  The thresholds differ on purpose:
	 * leaving needs the line fully gone, returning needs it fully visible, so
	 * that a line resting on the edge does not toggle repeatedly.
	 */
	private void updateAutoScroll() {
		if(adapter == null || layoutManager == null) {
			return;
		}

		int active = adapter.getActiveLine();
		if(active < 0) {
			// Nothing is highlighted yet, so there is no line to measure the
			// scroll against; treat any deliberate scroll as parking the list.
			// Otherwise reading ahead during an intro would be yanked back to
			// the first line the moment it lights up.
			autoScroll = false;
			return;
		}

		int first = layoutManager.findFirstVisibleItemPosition();
		int last = layoutManager.findLastVisibleItemPosition();
		if(first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
			return;
		}

		if(active < first || active > last) {
			autoScroll = false;
		} else if(!autoScroll) {
			int firstComplete = layoutManager.findFirstCompletelyVisibleItemPosition();
			int lastComplete = layoutManager.findLastCompletelyVisibleItemPosition();
			if(firstComplete != RecyclerView.NO_POSITION && lastComplete != RecyclerView.NO_POSITION
					&& active >= firstComplete && active <= lastComplete) {
				// Resume quietly: the next line change scrolls, so the list does
				// not jump underneath the finger that just brought it back.
				autoScroll = true;
			}
		}
	}
}
