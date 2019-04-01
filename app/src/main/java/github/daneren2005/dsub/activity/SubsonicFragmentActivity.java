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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.Date;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerQueue;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.fragments.AdminFragment;
import github.daneren2005.dsub.fragments.ChatFragment;
import github.daneren2005.dsub.fragments.DownloadFragment;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.NowPlayingFragment;
import github.daneren2005.dsub.fragments.SearchFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectBookmarkFragment;
import github.daneren2005.dsub.fragments.SelectDirectoryFragment;
import github.daneren2005.dsub.fragments.SelectInternetRadioStationFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.fragments.SelectPodcastsFragment;
import github.daneren2005.dsub.fragments.SelectShareFragment;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.updates.Updater;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.KeyStoreUtil;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ChangeLog;

/**
 * Created by Scott on 10/14/13.
 */
public class SubsonicFragmentActivity extends SubsonicActivity implements DownloadService.OnSongChangedListener {
	private static String TAG = SubsonicFragmentActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;
	private static boolean sessionInitialized = false;
	private static long ALLOWED_SKEW = 30000L;

	private SlidingUpPanelLayout slideUpPanel;
	private SlidingUpPanelLayout.PanelSlideListener panelSlideListener;
	private boolean isPanelClosing = false;
	private NowPlayingFragment nowPlayingFragment;
	private SubsonicFragment secondaryFragment;
	private Toolbar mainToolbar;
	private Toolbar nowPlayingToolbar;

	private View bottomBar;
	private ImageView coverArtView;
	private TextView trackView;
	private TextView artistView;
	private ImageButton startButton;
	private long lastBackPressTime = 0;
	private DownloadFile currentPlaying;
	private PlayerState currentState;
	private ImageButton previousButton;
	private ImageButton nextButton;
	private ImageButton rewindButton;
	private ImageButton fastforwardButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState == null) {
			String fragmentType = getIntent().getStringExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE);
			boolean firstRun = false;
			if (fragmentType == null) {
				fragmentType = Util.openToTab(this);
				if (fragmentType != null) {
					firstRun = true;
				}
			}

			if ("".equals(fragmentType) || fragmentType == null || firstRun) {
				// Initial startup stuff
				if (!sessionInitialized) {
					loadSession();
				}
			}
		}

		super.onCreate(savedInstanceState);
		if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
			stopService(new Intent(this, DownloadService.class));
			finish();
			getImageLoader().clearCache();
			DrawableTint.clearCache();
		} else if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW)) {
			getIntent().putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, "Download");
			lastSelectedPosition = R.id.drawer_downloading;
		}
		setContentView(R.layout.abstract_fragment_activity);

		if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {
			String fragmentType = getIntent().getStringExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE);
			if(fragmentType == null) {
				fragmentType = Util.openToTab(this);
				if(fragmentType != null) {
					getIntent().putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, fragmentType);
					lastSelectedPosition = getDrawerItemId(fragmentType);
				} else {
					lastSelectedPosition = R.id.drawer_home;
				}

				MenuItem item = drawerList.getMenu().findItem(lastSelectedPosition);
				if(item != null) {
					item.setChecked(true);
				}
			} else {
				lastSelectedPosition = getDrawerItemId(fragmentType);
			}

			currentFragment = getNewFragment(fragmentType);
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_ID)) {
				Bundle currentArguments = currentFragment.getArguments();
				if(currentArguments == null) {
					currentArguments = new Bundle();
				}
				currentArguments.putString(Constants.INTENT_EXTRA_NAME_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID));
				currentFragment.setArguments(currentArguments);
			}
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currentFragment, currentFragment.getSupportTag() + "").commit();

			if(getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY) != null) {
				SearchFragment fragment = new SearchFragment();
				replaceFragment(fragment, fragment.getSupportTag());
			}

			// If a album type is set, switch to that album type view
			String albumType = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
			if(albumType != null) {
				SubsonicFragment fragment = new SelectDirectoryFragment();

				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, albumType);
				args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
				args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);

				fragment.setArguments(args);
				replaceFragment(fragment, fragment.getSupportTag());
			}
		}

		slideUpPanel = (SlidingUpPanelLayout) findViewById(R.id.slide_up_panel);
		panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {

			}

			@Override
			public void onPanelCollapsed(View panel) {
				isPanelClosing = false;
				if(bottomBar.getVisibility() == View.GONE) {
					bottomBar.setVisibility(View.VISIBLE);
					nowPlayingToolbar.setVisibility(View.GONE);
					nowPlayingFragment.setPrimaryFragment(false);
					setSupportActionBar(mainToolbar);
					recreateSpinner();
				}
			}

			@Override
			public void onPanelExpanded(View panel) {
				isPanelClosing = false;
				currentFragment.stopActionMode();

				// Disable custom view before switching
				getSupportActionBar().setDisplayShowCustomEnabled(false);
				getSupportActionBar().setDisplayShowTitleEnabled(true);

				bottomBar.setVisibility(View.GONE);
				nowPlayingToolbar.setVisibility(View.VISIBLE);
				setSupportActionBar(nowPlayingToolbar);

				if(secondaryFragment == null) {
					nowPlayingFragment.setPrimaryFragment(true);
				} else {
					secondaryFragment.setPrimaryFragment(true);
				}

				drawerToggle.setDrawerIndicatorEnabled(false);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}

			@Override
			public void onPanelAnchored(View panel) {

			}

			@Override
			public void onPanelHidden(View panel) {

			}
		};
		slideUpPanel.setPanelSlideListener(panelSlideListener);

		if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD)) {
			// Post this later so it actually runs
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					openNowPlaying();
				}
			}, 200);

			getIntent().removeExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD);
		}

		bottomBar = findViewById(R.id.bottom_bar);
		mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		nowPlayingToolbar = (Toolbar) findViewById(R.id.now_playing_toolbar);
		coverArtView = (ImageView) bottomBar.findViewById(R.id.album_art);
		trackView = (TextView) bottomBar.findViewById(R.id.track_name);
		artistView = (TextView) bottomBar.findViewById(R.id.artist_name);

		setSupportActionBar(mainToolbar);

		if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {
			nowPlayingFragment = new NowPlayingFragment();
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(R.id.now_playing_fragment_container, nowPlayingFragment, nowPlayingFragment.getTag() + "");
			trans.commit();
		}

		rewindButton = (ImageButton) findViewById(R.id.download_rewind);
		rewindButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						if (getDownloadService() == null) {
							return null;
						}

						getDownloadService().rewind();
						return null;
					}
				}.execute();
			}
		});

		previousButton = (ImageButton) findViewById(R.id.download_previous);
		previousButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						if(getDownloadService() == null) {
							return null;
						}

						getDownloadService().previous();
						return null;
					}
				}.execute();
			}
		});

		startButton = (ImageButton) findViewById(R.id.download_start);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						PlayerState state = getDownloadService().getPlayerState();
						if(state == PlayerState.STARTED) {
							getDownloadService().pause();
						} else if(state == PlayerState.IDLE) {
							getDownloadService().play();
						} else {
							getDownloadService().start();
						}

						return null;
					}
				}.execute();
			}
		});

		nextButton = (ImageButton) findViewById(R.id.download_next);
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						if(getDownloadService() == null) {
							return null;
						}

						getDownloadService().next();
						return null;
					}
				}.execute();
			}
		});

		fastforwardButton = (ImageButton) findViewById(R.id.download_fastforward);
		fastforwardButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						if (getDownloadService() == null) {
							return null;
						}

						getDownloadService().fastForward();
						return null;
					}
				}.execute();
			}
		});
	}

	@Override
	protected void onPostCreate(Bundle bundle) {
		super.onPostCreate(bundle);

		showInfoDialog();
		checkUpdates();

		ChangeLog changeLog = new ChangeLog(this, Util.getPreferences(this));
		if(changeLog.isFirstRun()) {
			if(changeLog.isFirstRunEver()) {
				changeLog.updateVersionInPreferences();
			} else {
				Dialog log = changeLog.getLogDialog();
				if (log != null) {
					log.show();
				}
			}
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if(currentFragment != null && intent.getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY) != null) {
			if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
				closeNowPlaying();
			}

			if(currentFragment instanceof SearchFragment) {
				String query = intent.getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY);
				boolean autoplay = intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
				String artist = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST);
				String album = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM);
				String title = intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE);

				if (query != null) {
					((SearchFragment)currentFragment).search(query, autoplay, artist, album, title);
				}
				getIntent().removeExtra(Constants.INTENT_EXTRA_NAME_QUERY);
			} else {
				setIntent(intent);

				SearchFragment fragment = new SearchFragment();
				replaceFragment(fragment, fragment.getSupportTag());
			}
		} else if(intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, false)) {
			if(slideUpPanel.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
				openNowPlaying();
			}
		} else {
			if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
				closeNowPlaying();
			}
			setIntent(intent);
		}
		if(drawer != null) {
			drawer.closeDrawers();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if(getIntent().hasExtra(Constants.INTENT_EXTRA_VIEW_ALBUM)) {
			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID));
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_NAME));
			args.putString(Constants.INTENT_EXTRA_SEARCH_SONG, getIntent().getStringExtra(Constants.INTENT_EXTRA_SEARCH_SONG));
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_ARTIST)) {
				args.putBoolean(Constants.INTENT_EXTRA_NAME_ARTIST, true);
			}
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_CHILD_ID)) {
				args.putString(Constants.INTENT_EXTRA_NAME_CHILD_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_CHILD_ID));
			}
			fragment.setArguments(args);

			replaceFragment(fragment, fragment.getSupportTag());
			getIntent().removeExtra(Constants.INTENT_EXTRA_VIEW_ALBUM);
		}

		UserUtil.seedCurrentUser(this);
		createAccount();
		runWhenServiceAvailable(new Runnable() {
			@Override
			public void run() {
				getDownloadService().addOnSongChangedListener(SubsonicFragmentActivity.this, true);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		DownloadService downloadService = getDownloadService();
		if(downloadService != null) {
			downloadService.removeOnSongChangeListener(this);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putString(Constants.MAIN_NOW_PLAYING, nowPlayingFragment.getTag());
		if(secondaryFragment != null) {
			savedInstanceState.putString(Constants.MAIN_NOW_PLAYING_SECONDARY, secondaryFragment.getTag());
		}
		savedInstanceState.putInt(Constants.MAIN_SLIDE_PANEL_STATE, slideUpPanel.getPanelState().hashCode());
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		String id = savedInstanceState.getString(Constants.MAIN_NOW_PLAYING);
		FragmentManager fm = getSupportFragmentManager();
		nowPlayingFragment = (NowPlayingFragment) fm.findFragmentByTag(id);

		String secondaryId = savedInstanceState.getString(Constants.MAIN_NOW_PLAYING_SECONDARY);
		if(secondaryId != null) {
			secondaryFragment = (SubsonicFragment) fm.findFragmentByTag(secondaryId);

			nowPlayingFragment.setPrimaryFragment(false);
			secondaryFragment.setPrimaryFragment(true);

			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.hide(nowPlayingFragment);
			trans.commit();
		}

		if(drawerToggle != null && backStack.size() > 0) {
			drawerToggle.setDrawerIndicatorEnabled(false);
		}

		if(savedInstanceState.getInt(Constants.MAIN_SLIDE_PANEL_STATE, -1) == SlidingUpPanelLayout.PanelState.EXPANDED.hashCode()) {
			panelSlideListener.onPanelExpanded(null);
		}
	}

	@Override
	public void setContentView(int viewId) {
		super.setContentView(viewId);
		if(drawerToggle != null){
			drawerToggle.setDrawerIndicatorEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED && secondaryFragment == null) {
			slideUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		} else if(onBackPressedSupport()) {
			if(!Util.disableExitPrompt(this) && lastBackPressTime < (System.currentTimeMillis() - 4000)) {
				lastBackPressTime = System.currentTimeMillis();
				Util.toast(this, R.string.main_back_confirm);
			} else {
				finish();
			}
		}
	}

	@Override
	public boolean onBackPressedSupport() {
		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			removeCurrent();
			return false;
		} else {
			return super.onBackPressedSupport();
		}
	}

	@Override
	public SubsonicFragment getCurrentFragment() {
		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			if(secondaryFragment == null) {
				return nowPlayingFragment;
			} else {
				return secondaryFragment;
			}
		} else {
			return super.getCurrentFragment();
		}
	}

	@Override
	public void replaceFragment(SubsonicFragment fragment, int tag, boolean replaceCurrent) {
		if(slideUpPanel != null && slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED && !isPanelClosing) {
			secondaryFragment = fragment;
			nowPlayingFragment.setPrimaryFragment(false);
			secondaryFragment.setPrimaryFragment(true);
			supportInvalidateOptionsMenu();

			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
			trans.hide(nowPlayingFragment);
			trans.add(R.id.now_playing_fragment_container, secondaryFragment, tag + "");
			trans.commit();
		} else {
			super.replaceFragment(fragment, tag, replaceCurrent);
		}
	}
	@Override
	public void removeCurrent() {
		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED && secondaryFragment != null) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
			trans.remove(secondaryFragment);
			trans.show(nowPlayingFragment);
			trans.commit();

			secondaryFragment = null;
			nowPlayingFragment.setPrimaryFragment(true);
			supportInvalidateOptionsMenu();
		} else {
			super.removeCurrent();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			getSupportActionBar().setTitle(title);
		} else {
			super.setTitle(title);
		}
	}

	@Override
	protected void drawerItemSelected(String fragmentType) {
		super.drawerItemSelected(fragmentType);

		if(slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			slideUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		}
	}

	@Override
	public void startFragmentActivity(String fragmentType) {
		// Create a transaction that does all of this
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

		// Clear existing stack
		for(int i = backStack.size() - 1; i >= 0; i--) {
			trans.remove(backStack.get(i));
		}
		trans.remove(currentFragment);
		backStack.clear();

		// Create new stack
		currentFragment = getNewFragment(fragmentType);
		currentFragment.setPrimaryFragment(true);
		trans.add(R.id.fragment_container, currentFragment, currentFragment.getSupportTag() + "");

		// Done, cleanup
		trans.commit();
		supportInvalidateOptionsMenu();
		recreateSpinner();
		if(drawer != null) {
			drawer.closeDrawers();
		}

		if(secondaryContainer != null) {
			secondaryContainer.setVisibility(View.GONE);
		}
		if(drawerToggle != null) {
			drawerToggle.setDrawerIndicatorEnabled(true);
		}
	}

	@Override
	public void openNowPlaying() {
		slideUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
	}
	@Override
	public void closeNowPlaying() {
		slideUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		isPanelClosing = true;
	}

	private SubsonicFragment getNewFragment(String fragmentType) {
		if("Artist".equals(fragmentType)) {
			return new SelectArtistFragment();
		} else if("Playlist".equals(fragmentType)) {
			return new SelectPlaylistFragment();
		} else if("Chat".equals(fragmentType)) {
			return new ChatFragment();
		} else if("Podcast".equals(fragmentType)) {
			return new SelectPodcastsFragment();
		} else if("Bookmark".equals(fragmentType)) {
			return new SelectBookmarkFragment();
		} else if("Internet Radio".equals(fragmentType)) {
			return new SelectInternetRadioStationFragment();
		} else if("Share".equals(fragmentType)) {
			return new SelectShareFragment();
		} else if("Admin".equals(fragmentType)) {
			return new AdminFragment();
		} else if("Download".equals(fragmentType)) {
			return new DownloadFragment();
		} else {
			return new MainFragment();
		}
	}

	public void checkUpdates() {
		try {
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			int ver = Integer.parseInt(version.replace(".", ""));
			Updater updater = new Updater(ver);
			updater.checkUpdates(this);
		}
		catch(Exception e) {

		}
	}

	private void loadSession() {
		if (Build.VERSION.SDK_INT >= 23) {
			try {
				KeyStoreUtil.loadKeyStore();
			} catch (Exception e) {
				Log.w(TAG, "Error loading keystore");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		}

		loadSettings();
		if(!Util.isOffline(this) && ServerInfo.canBookmark(this)) {
			loadBookmarks();
		}
		// If we are on Subsonic 5.2+, save play queue
		if(ServerInfo.canSavePlayQueue(this) && !Util.isOffline(this)) {
			loadRemotePlayQueue();
		}

		sessionInitialized = true;
	}
	private void loadSettings() {
		PreferenceManager.setDefaultValues(this, R.xml.settings_appearance, false);
		PreferenceManager.setDefaultValues(this, R.xml.settings_cache, false);
		PreferenceManager.setDefaultValues(this, R.xml.settings_drawer, false);
		PreferenceManager.setDefaultValues(this, R.xml.settings_sync, false);
		PreferenceManager.setDefaultValues(this, R.xml.settings_playback, false);

		SharedPreferences prefs = Util.getPreferences(this);
		if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION) || prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null) == null) {
			resetCacheLocation(prefs);
		} else {
			String path = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
			File cacheLocation = new File(path);
			if(!FileUtil.verifyCanWrite(cacheLocation)) {
				// Only warn user if there is a difference saved
				if(resetCacheLocation(prefs)) {
					Util.info(this, R.string.common_warning, R.string.settings_cache_location_reset);
				}
			}
		}

		if (!prefs.contains(Constants.PREFERENCES_KEY_OFFLINE)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCES_KEY_OFFLINE, false);

			editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + 1, "Demo Server");
			editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + 1, "http://demo.subsonic.org");
			editor.putString(Constants.PREFERENCES_KEY_USERNAME + 1, "guest2");
			if (Build.VERSION.SDK_INT < 23) {
				editor.putString(Constants.PREFERENCES_KEY_PASSWORD + 1, "guest");
			} else {
				// Attempt to encrypt password
				String encryptedDefaultPassword = KeyStoreUtil.encrypt("guest");

				if (encryptedDefaultPassword != null) {
					// If encryption succeeds, store encrypted password and flag password as encrypted
					editor.putString(Constants.PREFERENCES_KEY_PASSWORD + 1, encryptedDefaultPassword);
					editor.putBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + 1, true);
				} else {
					// Fall back to plaintext if Keystore is having issue
					editor = editor.putString(Constants.PREFERENCES_KEY_PASSWORD + 1, "guest");
					editor.putBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + 1, false);
				}
			}
			editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
			editor.commit();
		}
		if(!prefs.contains(Constants.PREFERENCES_KEY_SERVER_COUNT)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 1);
			editor.commit();
		}
	}

	private boolean resetCacheLocation(SharedPreferences prefs) {
		String newDirectory = FileUtil.getDefaultMusicDirectory(this).getPath();
		String oldDirectory = prefs.getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, null);
		if(newDirectory == null || (oldDirectory != null && newDirectory.equals(oldDirectory))) {
			return false;
		} else {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, newDirectory);
			editor.commit();
			return true;
		}
	}

	private void loadBookmarks() {
		final Context context = this;
		new SilentBackgroundTask<Void>(context) {
			@Override
			public Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.getBookmarks(true, context, null);

				return null;
			}

			@Override
			public void error(Throwable error) {
				Log.e(TAG, "Failed to get bookmarks", error);
			}
		}.execute();
	}
	private void loadRemotePlayQueue() {
		if(Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_RESUME_PLAY_QUEUE_NEVER, false)) {
			return;
		}

		final SubsonicActivity context = this;
		new SilentBackgroundTask<Void>(this) {
			private PlayerQueue playerQueue;

			@Override
			protected Void doInBackground() throws Throwable {
				try {
					MusicService musicService = MusicServiceFactory.getMusicService(context);
					PlayerQueue remoteState = musicService.getPlayQueue(context, null);

					// Make sure we wait until download service is ready
					DownloadService downloadService = getDownloadService();
					while(downloadService == null || !downloadService.isInitialized()) {
						Util.sleepQuietly(100L);
						downloadService = getDownloadService();
					}

					// If we had a remote state and it's changed is more recent than our existing state
					if(remoteState != null && remoteState.changed != null) {
						// Check if changed + 30 seconds since some servers have slight skew
						Date remoteChange = new Date(remoteState.changed.getTime() - ALLOWED_SKEW);
						Date localChange = downloadService.getLastStateChanged();
						if(localChange == null || localChange.before(remoteChange)) {
							playerQueue = remoteState;
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Failed to get playing queue to server", e);
				}

				return null;
			}

			@Override
			protected void done(Void arg) {
				if(!context.isDestroyedCompat() && playerQueue != null) {
					promptRestoreFromRemoteQueue(playerQueue);
				}
			}
		}.execute();
	}
	private void promptRestoreFromRemoteQueue(final PlayerQueue remoteState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String message = getResources().getString(R.string.common_confirm_message, getResources().getString(R.string.download_restore_play_queue).toLowerCase(), Util.formatDate(remoteState.changed));
		builder.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.common_confirm)
				.setMessage(message)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
							@Override
							protected Void doInBackground() throws Throwable {
								DownloadService downloadService = getDownloadService();
								downloadService.clear();
								downloadService.download(remoteState.songs, false, false, false, false, remoteState.currentPlayingIndex, remoteState.currentPlayingPosition);
								return null;
							}
						}.execute();
					}
				})
				.setNeutralButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
							@Override
							protected Void doInBackground() throws Throwable {
								DownloadService downloadService = getDownloadService();
								downloadService.serializeQueue(false);
								return null;
							}
						}.execute();
					}
				})
				.setNegativeButton(R.string.common_never, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
							@Override
							protected Void doInBackground() throws Throwable {
								DownloadService downloadService = getDownloadService();
								downloadService.serializeQueue(false);

								SharedPreferences.Editor editor = Util.getPreferences(SubsonicFragmentActivity.this).edit();
								editor.putBoolean(Constants.PREFERENCES_KEY_RESUME_PLAY_QUEUE_NEVER, true);
								editor.commit();
								return null;
							}
						}.execute();
					}
				});

		builder.create().show();
	}

	private void createAccount() {
		final Context context = this;

		new SilentBackgroundTask<Void>(this) {
			@Override
			protected Void doInBackground() throws Throwable {
				AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
				Account account = new Account(Constants.SYNC_ACCOUNT_NAME, Constants.SYNC_ACCOUNT_TYPE);
				accountManager.addAccountExplicitly(account, null, null);

				SharedPreferences prefs = Util.getPreferences(context);
				boolean syncEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_SYNC_ENABLED, true);
				int syncInterval = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_SYNC_INTERVAL, "60"));

				// Add enabled/frequency to playlist/podcasts syncing
				ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_PLAYLIST_AUTHORITY, syncEnabled);
				ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_PLAYLIST_AUTHORITY, new Bundle(), 60L * syncInterval);
				ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_PODCAST_AUTHORITY, syncEnabled);
				ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_PODCAST_AUTHORITY, new Bundle(), 60L * syncInterval);

				// Add for starred/recently added
				ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_STARRED_AUTHORITY, (syncEnabled && prefs.getBoolean(Constants.PREFERENCES_KEY_SYNC_STARRED, false)));
				ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_STARRED_AUTHORITY, new Bundle(), 60L * syncInterval);
				ContentResolver.setSyncAutomatically(account, Constants.SYNC_ACCOUNT_MOST_RECENT_AUTHORITY, (syncEnabled && prefs.getBoolean(Constants.PREFERENCES_KEY_SYNC_MOST_RECENT, false)));
				ContentResolver.addPeriodicSync(account, Constants.SYNC_ACCOUNT_MOST_RECENT_AUTHORITY, new Bundle(), 60L * syncInterval);
				return null;
			}

			@Override
			protected void done(Void result) {

			}
		}.execute();
	}

	private void showInfoDialog() {
		if (!infoDialogDisplayed) {
			infoDialogDisplayed = true;
			if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
				Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
			}
		}
	}

	public Toolbar getActiveToolbar() {
		return slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ? nowPlayingToolbar : mainToolbar;
	}

	@Override
	public void onSongChanged(DownloadFile currentPlaying, int currentPlayingIndex, boolean shouldFastForward) {
		this.currentPlaying = currentPlaying;

		MusicDirectory.Entry song = null;
		if (currentPlaying != null) {
			song = currentPlaying.getSong();
			trackView.setText(song.getTitle());

			if(song.getArtist() != null) {
				artistView.setVisibility(View.VISIBLE);
				artistView.setText(song.getArtist());
			} else {
				artistView.setVisibility(View.GONE);
			}
		} else {
			trackView.setText(R.string.main_title);
			artistView.setText(R.string.main_artist);
		}

		if (coverArtView != null) {
			int height = coverArtView.getHeight();
			if (height <= 0) {
				int[] attrs = new int[]{R.attr.actionBarSize};
				TypedArray typedArray = this.obtainStyledAttributes(attrs);
				height = typedArray.getDimensionPixelSize(0, 0);
				typedArray.recycle();
			}
			getImageLoader().loadImage(coverArtView, song, false, height, false);
		}

		updateMediaButtons(shouldFastForward);
	}

	private void updateMediaButtons(boolean shouldFastForward) {
		DownloadService downloadService = getDownloadService();
		if(downloadService.isCurrentPlayingSingle()) {
			previousButton.setVisibility(View.GONE);
			nextButton.setVisibility(View.GONE);
			rewindButton.setVisibility(View.GONE);
			fastforwardButton.setVisibility(View.GONE);
		} else {
			if (shouldFastForward) {
				previousButton.setVisibility(View.GONE);
				nextButton.setVisibility(View.GONE);

				rewindButton.setVisibility(View.VISIBLE);
				fastforwardButton.setVisibility(View.VISIBLE);
			} else {
				previousButton.setVisibility(View.VISIBLE);
				nextButton.setVisibility(View.VISIBLE);

				rewindButton.setVisibility(View.GONE);
				fastforwardButton.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onSongsChanged(List<DownloadFile> songs, DownloadFile currentPlaying, int currentPlayingIndex, boolean shouldFastForward) {
		if(this.currentPlaying != currentPlaying || this.currentPlaying == null) {
			onSongChanged(currentPlaying, currentPlayingIndex, shouldFastForward);
		} else {
			updateMediaButtons(shouldFastForward);
		}
	}

	@Override
	public void onSongProgress(DownloadFile currentPlaying, int millisPlayed, Integer duration, boolean isSeekable) {

	}

	@Override
	public void onStateUpdate(DownloadFile downloadFile, PlayerState playerState) {
		int[] attrs = new int[]{(playerState == PlayerState.STARTED) ? R.attr.actionbar_pause : R.attr.actionbar_start};
		TypedArray typedArray = this.obtainStyledAttributes(attrs);
		startButton.setImageResource(typedArray.getResourceId(0, 0));
		typedArray.recycle();
	}

	@Override
	public void onMetadataUpdate(MusicDirectory.Entry song, int fieldChange) {
		if(song != null && coverArtView != null && fieldChange == DownloadService.METADATA_UPDATED_COVER_ART) {
			int height = coverArtView.getHeight();
			if (height <= 0) {
				int[] attrs = new int[]{R.attr.actionBarSize};
				TypedArray typedArray = this.obtainStyledAttributes(attrs);
				height = typedArray.getDimensionPixelSize(0, 0);
				typedArray.recycle();
			}
			getImageLoader().loadImage(coverArtView, song, false, height, false);

			// We need to update it immediately since it won't update if updater is not running for it
			if(nowPlayingFragment != null && slideUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
				nowPlayingFragment.onMetadataUpdate(song, fieldChange);
			}
		}
	}
}
