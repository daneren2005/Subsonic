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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.fragments.ChatFragment;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.SearchFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectBookmarkFragment;
import github.daneren2005.dsub.fragments.SelectDirectoryFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.fragments.SelectPodcastsFragment;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.updates.Updater;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ChangeLog;

/**
 * Created by Scott on 10/14/13.
 */
public class SubsonicFragmentActivity extends SubsonicActivity {
	private static String TAG = SubsonicFragmentActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;
	private ScheduledExecutorService executorService;
	private View bottomBar;
	private View coverArtView;
	private TextView trackView;
	private TextView artistView;
	private ImageButton startButton;
	private long lastBackPressTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
			stopService(new Intent(this, DownloadServiceImpl.class));
			finish();
		} else if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD)) {
			getIntent().removeExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD);
			Intent intent = new Intent();
			intent.setClass(this, DownloadActivity.class);
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW)) {
				intent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW, true);
			}
			startActivity(intent);
		}
		setContentView(R.layout.abstract_fragment_activity);

		if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {
			String fragmentType = getIntent().getStringExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE);
			currentFragment = getNewFragment(fragmentType);
			
			if("".equals(fragmentType) || fragmentType == null) {
				// Initial startup stuff
				loadSettings();
			}
			
			currentFragment.setPrimaryFragment(true);
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currentFragment, currentFragment.getSupportTag() + "").commit();
			
			if(getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_QUERY) != null) {
				SearchFragment fragment = new SearchFragment();
				replaceFragment(fragment, R.id.home_layout, fragment.getSupportTag());
			}
		}

		bottomBar = findViewById(R.id.bottom_bar);
		bottomBar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(v.getContext(), DownloadActivity.class);
				startActivity(intent);
			}
		});
		coverArtView = bottomBar.findViewById(R.id.album_art);
		trackView = (TextView) bottomBar.findViewById(R.id.track_name);
		artistView = (TextView) bottomBar.findViewById(R.id.artist_name);

		ImageButton previousButton = (ImageButton) findViewById(R.id.download_previous);
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

					@Override
					protected void done(Void result) {
						update();
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
						} else {
							getDownloadService().start();
						}

						return null;
					}

					@Override
					protected void done(Void result) {
						update();
					}
				}.execute();
			}
		});

		ImageButton nextButton = (ImageButton) findViewById(R.id.download_next);
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new SilentBackgroundTask<Void>(SubsonicFragmentActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						if(getDownloadService() == null) {
							return null;
						}

						if (getDownloadService().getCurrentPlayingIndex() < getDownloadService().size() - 1) {
							getDownloadService().next();
						}
						return null;
					}

					@Override
					protected void done(Void result) {
						update();
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
			changeLog.getLogDialog().show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		};

		if(getIntent().hasExtra(Constants.INTENT_EXTRA_VIEW_ALBUM)) {
			int fragmentID = R.id.fragment_list_layout;
			if(getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_PARENT_ID)) {
				SubsonicFragment fragment = new SelectDirectoryFragment();
				Bundle args = new Bundle();
				args.putString(Constants.INTENT_EXTRA_NAME_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PARENT_ID));
				args.putString(Constants.INTENT_EXTRA_NAME_NAME, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PARENT_NAME));
				fragment.setArguments(args);

				replaceFragment(fragment, R.id.fragment_list_layout, currentFragment.getSupportTag());
				fragmentID = fragment.getRootId();
			}

			SubsonicFragment fragment = new SelectDirectoryFragment();
			Bundle args = new Bundle();
			args.putString(Constants.INTENT_EXTRA_NAME_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID));
			args.putString(Constants.INTENT_EXTRA_NAME_NAME, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_NAME));
			fragment.setArguments(args);

			replaceFragment(fragment, fragmentID, currentFragment.getSupportTag());
			getIntent().removeExtra(Constants.INTENT_EXTRA_VIEW_ALBUM);
		}

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onPause() {
		super.onPause();
		executorService.shutdown();
	}

	@Override
	public void setContentView(int viewId) {
		super.setContentView(viewId);
		drawerToggle.setDrawerIndicatorEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onBackPressed() {
		if(onBackPressedSupport()) {
			if(lastBackPressTime < (System.currentTimeMillis() - 4000)) {
				lastBackPressTime = System.currentTimeMillis();
				Util.toast(this, R.string.main_back_confirm);
			} else {
				finish();
			}
		}
	}

	@Override
	public void replaceFragment(SubsonicFragment fragment, int id, int tag, boolean replaceCurrent) {
		super.replaceFragment(fragment, id, tag, replaceCurrent);
		drawerToggle.setDrawerIndicatorEnabled(false);
	}
	@Override
	public void removeCurrent() {
		super.removeCurrent();
		if(backStack.isEmpty()) {
			drawerToggle.setDrawerIndicatorEnabled(true);
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
		drawer.closeDrawers();

		if(secondaryContainer != null) {
			secondaryContainer.setVisibility(View.GONE);
		}
		drawerToggle.setDrawerIndicatorEnabled(true);
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
		} else {
			return new MainFragment();
		}
	} 

	private void update() {
		if (getDownloadService() == null) {
			return;
		}

		DownloadFile current = getDownloadService().getCurrentPlaying();
		if(current == null) {
			trackView.setText("Title");
			artistView.setText("Artist");
			getImageLoader().loadImage(coverArtView, null, false, false);
			return;
		}

		MusicDirectory.Entry song = current.getSong();
		trackView.setText(song.getTitle());
		artistView.setText(song.getArtist());
		getImageLoader().loadImage(coverArtView, song, false, false);
		int[] attrs = new int[] {(getDownloadService().getPlayerState() == PlayerState.STARTED) ?  R.attr.media_button_pause : R.attr.media_button_start};
		TypedArray typedArray = this.obtainStyledAttributes(attrs);
		startButton.setImageResource(typedArray.getResourceId(0, 0));
		typedArray.recycle();
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

	private void loadSettings() {
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		SharedPreferences prefs = Util.getPreferences(this);
		if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, FileUtil.getDefaultMusicDirectory().getPath());
			editor.commit();
		}

		if (!prefs.contains(Constants.PREFERENCES_KEY_OFFLINE)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCES_KEY_OFFLINE, false);

			editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + 1, "Demo Server");
			editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + 1, "http://demo.subsonic.org");
			editor.putString(Constants.PREFERENCES_KEY_USERNAME + 1, "android-guest");
			editor.putString(Constants.PREFERENCES_KEY_PASSWORD + 1, "guest");
			editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
			editor.commit();
		}
		if(!prefs.contains(Constants.PREFERENCES_KEY_SERVER_COUNT)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 3);
			editor.commit();
		}
	}

	private void showInfoDialog() {
		if (!infoDialogDisplayed) {
			infoDialogDisplayed = true;
			Log.i(TAG, Util.getRestUrl(this, null));
			if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
				Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
			}
		}
	}
}
