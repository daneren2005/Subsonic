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

import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.fragments.AdminFragment;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.fragments.UserFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.HeadphoneListenerService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.ThemeUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.util.UserUtil;

import static android.Manifest.*;

public class SubsonicActivity extends AppCompatActivity implements OnItemSelectedListener {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	protected static boolean fullScreen;
	protected static boolean actionbarColored;
	private static final int MENU_GROUP_SERVER = 10;
	private static final int MENU_ITEM_SERVER_BASE = 100;
	public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
	public static final int PERMISSIONS_REQUEST_LOCATION = 2;

	private final List<Runnable> afterServiceAvailable = new ArrayList<>();
	private boolean drawerIdle = true;
	private boolean destroyed = false;
	private boolean finished = false;
	protected List<SubsonicFragment> backStack = new ArrayList<SubsonicFragment>();
	protected SubsonicFragment currentFragment;
	protected View primaryContainer;
	protected View secondaryContainer;
	protected boolean tv = false;
	protected boolean touchscreen = true;
	protected Handler handler = new Handler();
	Spinner actionBarSpinner;
	ArrayAdapter<CharSequence> spinnerAdapter;
	ViewGroup rootView;
	DrawerLayout drawer;
	ActionBarDrawerToggle drawerToggle;
	NavigationView drawerList;
	View drawerHeader;
	ImageView drawerUserAvatar;
	ImageView drawerHeaderToggle;
	TextView drawerServerName;
	TextView drawerUserName;
	int lastSelectedPosition = 0;
	boolean showingTabs = true;
	boolean drawerOpen = false;
	SharedPreferences.OnSharedPreferenceChangeListener preferencesListener;

	static {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
	}

	@Override
	protected void onCreate(Bundle bundle) {
		UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
		if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
			// tv = true;
		}
		PackageManager pm = getPackageManager();
		if(!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
			touchscreen = false;
		}

		setUncaughtExceptionHandler();
		applyTheme();
		applyFullscreen();
		super.onCreate(bundle);
		DownloadService.startService(this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		if(getIntent().hasExtra(Constants.FRAGMENT_POSITION)) {
			lastSelectedPosition = getIntent().getIntExtra(Constants.FRAGMENT_POSITION, 0);
		}

		if(preferencesListener == null) {
			preferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
					// When changing drawer settings change visibility
					switch(key) {
						case Constants.PREFERENCES_KEY_PODCASTS_ENABLED:
							setDrawerItemVisible(R.id.drawer_podcasts, false);
							break;
						case Constants.PREFERENCES_KEY_BOOKMARKS_ENABLED:
							setDrawerItemVisible(R.id.drawer_bookmarks, false);
							break;
						case Constants.PREFERENCES_KEY_INTERNET_RADIO_ENABLED:
							setDrawerItemVisible(R.id.drawer_internet_radio_stations, false);
							break;
						case Constants.PREFERENCES_KEY_SHARED_ENABLED:
							setDrawerItemVisible(R.id.drawer_shares, false);
							break;
						case Constants.PREFERENCES_KEY_CHAT_ENABLED:
							setDrawerItemVisible(R.id.drawer_chat, false);
							break;
						case Constants.PREFERENCES_KEY_ADMIN_ENABLED:
							setDrawerItemVisible(R.id.drawer_admin, false);
							break;
					}
				}
			};
			Util.getPreferences(this).registerOnSharedPreferenceChangeListener(preferencesListener);
		}

		if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{ permission.WRITE_EXTERNAL_STORAGE }, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

				} else {
					Util.toast(this, R.string.permission_external_storage_failed);
					finish();
				}
			}
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		if(spinnerAdapter == null) {
			createCustomActionBarView();
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Sync the toggle state after onRestoreInstanceState has occurred.
		if(drawerToggle != null) {
			drawerToggle.syncState();
		}

		if(Util.shouldStartOnHeadphones(this)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setClassName(this.getPackageName(), HeadphoneListenerService.class.getName());
			this.startService(serviceIntent);
		}
	}

	protected void createCustomActionBarView() {
		actionBarSpinner = (Spinner) getLayoutInflater().inflate(R.layout.actionbar_spinner, null);
		if((this instanceof SubsonicFragmentActivity || this instanceof SettingsActivity) && (Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true) || ThemeUtil.getThemeRes(this) != R.style.Theme_DSub_Light_No_Color)) {
			actionBarSpinner.setBackgroundDrawable(DrawableTint.getTintedDrawableFromColor(this, R.drawable.abc_spinner_mtrl_am_alpha, android.R.color.white));
		}
		spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBarSpinner.setOnItemSelectedListener(this);
		actionBarSpinner.setAdapter(spinnerAdapter);

		getSupportActionBar().setCustomView(actionBarSpinner);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Util.registerMediaButtonEventReceiver(this);

		// Make sure to update theme
		SharedPreferences prefs = Util.getPreferences(this);
		if (theme != null && !theme.equals(ThemeUtil.getTheme(this)) || fullScreen != prefs.getBoolean(Constants.PREFERENCES_KEY_FULL_SCREEN, false) || actionbarColored != prefs.getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true)) {
			restart();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			DrawableTint.clearCache();
			return;
		}

		getImageLoader().onUIVisible();
		UpdateView.addActiveActivity();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// If this is in onStart is causes crashes when rotating screen in offline mode
		// Actual root cause of error is `drawerItemSelected(newFragment);` in the offline mode branch of code
		populateTabs();
	}

	@Override
	protected void onStop() {
		super.onStop();

		UpdateView.removeActiveActivity();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyed = true;
		Util.getPreferences(this).unregisterOnSharedPreferenceChangeListener(preferencesListener);
	}

	@Override
	public void finish() {
		super.finish();
		Util.disablePendingTransition(this);
	}

	@Override
	public void setContentView(int viewId) {
		if(isTv()) {
			super.setContentView(R.layout.static_drawer_activity);
		} else {
			super.setContentView(R.layout.abstract_activity);
		}
		rootView = (ViewGroup) findViewById(R.id.content_frame);

		if(viewId != 0) {
			LayoutInflater layoutInflater = getLayoutInflater();
			layoutInflater.inflate(viewId, rootView);
		}

		drawerList = (NavigationView) findViewById(R.id.left_drawer);
		drawerList.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(final MenuItem menuItem) {
				if(showingTabs) {
					// Settings are on a different selectable track
					if (menuItem.getItemId() != R.id.drawer_settings && menuItem.getItemId() != R.id.drawer_admin && menuItem.getItemId() != R.id.drawer_offline) {
						menuItem.setChecked(true);
						lastSelectedPosition = menuItem.getItemId();
					}

					switch (menuItem.getItemId()) {
						case R.id.drawer_home:
							drawerItemSelected("Home");
							return true;
						case R.id.drawer_library:
							drawerItemSelected("Artist");
							return true;
						case R.id.drawer_playlists:
							drawerItemSelected("Playlist");
							return true;
						case R.id.drawer_podcasts:
							drawerItemSelected("Podcast");
							return true;
						case R.id.drawer_bookmarks:
							drawerItemSelected("Bookmark");
							return true;
						case R.id.drawer_internet_radio_stations:
							drawerItemSelected("Internet Radio");
							return true;
						case R.id.drawer_shares:
							drawerItemSelected("Share");
							return true;
						case R.id.drawer_chat:
							drawerItemSelected("Chat");
							return true;
						case R.id.drawer_admin:
							if (UserUtil.isCurrentAdmin()) {
								UserUtil.confirmCredentials(SubsonicActivity.this, new Runnable() {
									@Override
									public void run() {
										drawerItemSelected("Admin");
										menuItem.setChecked(true);
										lastSelectedPosition = menuItem.getItemId();
									}
								});
							} else {
								drawerItemSelected("Admin");
								menuItem.setChecked(true);
								lastSelectedPosition = menuItem.getItemId();
							}
							return true;
						case R.id.drawer_downloading:
							drawerItemSelected("Download");
							return true;
						case R.id.drawer_offline:
							toggleOffline();
							return true;
						case R.id.drawer_settings:
							startActivity(new Intent(SubsonicActivity.this, SettingsActivity.class));
							drawer.closeDrawers();
							return true;
					}
				} else {
					int activeServer = menuItem.getItemId() - MENU_ITEM_SERVER_BASE;
					SubsonicActivity.this.setActiveServer(activeServer);
					populateTabs();
					return true;
				}

				return false;
			}
		});

		drawerHeader = drawerList.inflateHeaderView(R.layout.drawer_header);
		drawerHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(showingTabs) {
					populateServers();
				} else {
					populateTabs();
				}
			}
		});

		drawerHeaderToggle = (ImageView) drawerHeader.findViewById(R.id.header_select_image);
		drawerServerName = (TextView) drawerHeader.findViewById(R.id.header_server_name);
		drawerUserName = (TextView) drawerHeader.findViewById(R.id.header_user_name);

		drawerUserAvatar = (ImageView) drawerHeader.findViewById(R.id.header_user_avatar);

		updateDrawerHeader();

		if(!isTv()) {
			drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

			// Pass in toolbar if it exists
			Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
			drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.common_appname, R.string.common_appname) {
				@Override
				public void onDrawerClosed(View view) {
					drawerIdle = true;
					drawerOpen = false;

					if(!showingTabs) {
						populateTabs();
					}
				}

				@Override
				public void onDrawerOpened(View view) {
					DownloadService downloadService = getDownloadService();
					boolean downloadingVisible = downloadService != null && !downloadService.getBackgroundDownloads().isEmpty();
					if(lastSelectedPosition == R.id.drawer_downloading) {
						downloadingVisible = true;
					}
					setDrawerItemVisible(R.id.drawer_downloading, downloadingVisible);

					drawerIdle = true;
					drawerOpen = true;
				}

				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					super.onDrawerSlide(drawerView, slideOffset);
					drawerIdle = false;
				}
			};
			drawer.setDrawerListener(drawerToggle);
			drawerToggle.setDrawerIndicatorEnabled(true);

			drawer.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (drawerIdle && currentFragment != null && currentFragment.getGestureDetector() != null) {
						return currentFragment.getGestureDetector().onTouchEvent(event);
					} else {
						return false;
					}
				}
			});
		}

		// Check whether this is a tablet or not
		secondaryContainer = findViewById(R.id.fragment_second_container);
		if(secondaryContainer != null) {
			primaryContainer = findViewById(R.id.fragment_container);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		String[] ids = new String[backStack.size() + 1];
		ids[0] = currentFragment.getTag();
		int i = 1;
		for(SubsonicFragment frag: backStack) {
			ids[i] = frag.getTag();
			i++;
		}
		savedInstanceState.putStringArray(Constants.MAIN_BACK_STACK, ids);
		savedInstanceState.putInt(Constants.MAIN_BACK_STACK_SIZE, backStack.size() + 1);
		savedInstanceState.putInt(Constants.FRAGMENT_POSITION, lastSelectedPosition);
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int size = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_SIZE);
		String[] ids = savedInstanceState.getStringArray(Constants.MAIN_BACK_STACK);
		FragmentManager fm = getSupportFragmentManager();
		currentFragment = (SubsonicFragment)fm.findFragmentByTag(ids[0]);
		currentFragment.setPrimaryFragment(true);
		currentFragment.setSupportTag(ids[0]);
		supportInvalidateOptionsMenu();
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		for(int i = 1; i < size; i++) {
			SubsonicFragment frag = (SubsonicFragment)fm.findFragmentByTag(ids[i]);
			frag.setSupportTag(ids[i]);
			if(secondaryContainer != null) {
				frag.setPrimaryFragment(false, true);
			}
			trans.hide(frag);
			backStack.add(frag);
		}
		trans.commit();

		// Current fragment is hidden in secondaryContainer
		if(secondaryContainer == null && !currentFragment.isVisible()) {
			trans = getSupportFragmentManager().beginTransaction();
			trans.remove(currentFragment);
			trans.commit();
			getSupportFragmentManager().executePendingTransactions();

			trans = getSupportFragmentManager().beginTransaction();
			trans.add(R.id.fragment_container, currentFragment, ids[0]);
			trans.commit();
		}
		// Current fragment needs to be moved over to secondaryContainer
		else if(secondaryContainer != null && secondaryContainer.findViewById(currentFragment.getRootId()) == null && backStack.size() > 0) {
			trans = getSupportFragmentManager().beginTransaction();
			trans.remove(currentFragment);
			trans.show(backStack.get(backStack.size() - 1));
			trans.commit();
			getSupportFragmentManager().executePendingTransactions();

			trans = getSupportFragmentManager().beginTransaction();
			trans.add(R.id.fragment_second_container, currentFragment, ids[0]);
			trans.commit();

			secondaryContainer.setVisibility(View.VISIBLE);
		}

		lastSelectedPosition = savedInstanceState.getInt(Constants.FRAGMENT_POSITION);
		if(lastSelectedPosition != 0) {
			MenuItem item = drawerList.getMenu().findItem(lastSelectedPosition);
			if(item != null) {
				item.setChecked(true);
			}
		}
		recreateSpinner();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		SubsonicFragment currentFragment = getCurrentFragment();
		if(currentFragment != null) {
			try {
				SubsonicFragment fragment = getCurrentFragment();
				fragment.setContext(this);
				fragment.onCreateOptionsMenu(menu, menuInflater);

				if(isTouchscreen()) {
					menu.setGroupVisible(R.id.not_touchscreen, false);
				}
			} catch(Exception e) {
				Log.w(TAG, "Error on creating options menu", e);
			}
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
			return true;
		} else if(item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}

		return getCurrentFragment().onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
		boolean isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP;
		boolean isVolumeAdjust = isVolumeDown || isVolumeUp;
		boolean isJukebox = getDownloadService() != null && getDownloadService().isRemoteEnabled();

		if (isVolumeAdjust && isJukebox) {
			getDownloadService().updateRemoteVolume(isVolumeUp);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void setTitle(CharSequence title) {
		if(title != null && getSupportActionBar() != null && !title.equals(getSupportActionBar().getTitle())) {
			getSupportActionBar().setTitle(title);
			recreateSpinner();
		}
	}
	public void setSubtitle(CharSequence title) {
		getSupportActionBar().setSubtitle(title);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		int top = spinnerAdapter.getCount() - 1;
		if(position < top) {
			for(int i = top; i > position && i >= 0; i--) {
				removeCurrent();
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	private void populateTabs() {
		drawerList.getMenu().clear();
		drawerList.inflateMenu(R.menu.drawer_navigation);

		SharedPreferences prefs = Util.getPreferences(this);
		boolean podcastsEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_PODCASTS_ENABLED, true);
		boolean bookmarksEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_BOOKMARKS_ENABLED, true) && !Util.isOffline(this) && ServerInfo.canBookmark(this);
		boolean internetRadioEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_INTERNET_RADIO_ENABLED, true) && !Util.isOffline(this) && ServerInfo.canInternetRadio(this);
		boolean sharedEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_SHARED_ENABLED, true) && !Util.isOffline(this);
		boolean chatEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_CHAT_ENABLED, true) && !Util.isOffline(this);
		boolean adminEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_ADMIN_ENABLED, true) && !Util.isOffline(this);

		MenuItem offlineMenuItem = drawerList.getMenu().findItem(R.id.drawer_offline);
		if(Util.isOffline(this)) {
			setDrawerItemVisible(R.id.drawer_home, false);

			if(lastSelectedPosition == 0 || lastSelectedPosition == R.id.drawer_home) {
				String newFragment = Util.openToTab(this);
				if(newFragment == null || "Home".equals(newFragment)) {
					newFragment = "Artist";
				}

				lastSelectedPosition = getDrawerItemId(newFragment);
				drawerItemSelected(newFragment);
			}

			offlineMenuItem.setTitle(R.string.main_online);
		} else {
			offlineMenuItem.setTitle(R.string.main_offline);
		}

		if(!podcastsEnabled) {
			setDrawerItemVisible(R.id.drawer_podcasts, false);
		}
		if(!bookmarksEnabled) {
			setDrawerItemVisible(R.id.drawer_bookmarks, false);
		}
		if(!internetRadioEnabled) {
			setDrawerItemVisible(R.id.drawer_internet_radio_stations, false);
		}
		if(!sharedEnabled) {
			setDrawerItemVisible(R.id.drawer_shares, false);
		}
		if(!chatEnabled) {
			setDrawerItemVisible(R.id.drawer_chat, false);
		}
		if(!adminEnabled) {
			setDrawerItemVisible(R.id.drawer_admin, false);
		}

		if(lastSelectedPosition != 0) {
			MenuItem item = drawerList.getMenu().findItem(lastSelectedPosition);
			if(item != null) {
				item.setChecked(true);
			}
		}
		drawerHeaderToggle.setImageResource(R.drawable.main_select_server_dark);

		showingTabs = true;
	}
	private void populateServers() {
		drawerList.getMenu().clear();

		int serverCount = Util.getServerCount(this);
		int activeServer = Util.getActiveServer(this);
		for(int i = 1; i <= serverCount; i++) {
			MenuItem item = drawerList.getMenu().add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_BASE + i, MENU_ITEM_SERVER_BASE + i, Util.getServerName(this, i));
			if(activeServer == i) {
				item.setChecked(true);
			}
		}
		drawerList.getMenu().setGroupCheckable(MENU_GROUP_SERVER, true, true);
		drawerHeaderToggle.setImageResource(R.drawable.main_select_tabs_dark);

		showingTabs = false;
	}
	private void setDrawerItemVisible(int id, boolean visible) {
		MenuItem item = drawerList.getMenu().findItem(id);
		if(item != null) {
			item.setVisible(visible);
		}
	}

	protected void drawerItemSelected(String fragmentType) {
		if(currentFragment != null) {
			currentFragment.stopActionMode();
		}
		startFragmentActivity(fragmentType);
	}

	public void startFragmentActivity(String fragmentType) {
		Intent intent = new Intent();
		intent.setClass(SubsonicActivity.this, SubsonicFragmentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(!"".equals(fragmentType)) {
			intent.putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, fragmentType);
		}
		if(lastSelectedPosition != 0) {
			intent.putExtra(Constants.FRAGMENT_POSITION, lastSelectedPosition);
		}
		startActivity(intent);
		finish();
	}

	protected void exit() {
		if(((Object) this).getClass() != SubsonicFragmentActivity.class) {
			Intent intent = new Intent(this, SubsonicFragmentActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
			Util.startActivityWithoutTransition(this, intent);
		} else {
			finished = true;
			this.stopService(new Intent(this, DownloadService.class));
			this.finish();
		}
	}

	public boolean onBackPressedSupport() {
		if(drawerOpen) {
			drawer.closeDrawers();
			return false;
		} else if(backStack.size() > 0) {
			removeCurrent();
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onBackPressed() {
		if(onBackPressedSupport()) {
			super.onBackPressed();
		}
	}

	public SubsonicFragment getCurrentFragment() {
		return this.currentFragment;
	}

	public void replaceFragment(SubsonicFragment fragment, int tag) {
		replaceFragment(fragment, tag, false);
	}
	public void replaceFragment(SubsonicFragment fragment, int tag, boolean replaceCurrent) {
		SubsonicFragment oldFragment = currentFragment;
		if(currentFragment != null) {
			currentFragment.setPrimaryFragment(false, secondaryContainer != null);
		}
		backStack.add(currentFragment);

		currentFragment = fragment;
		currentFragment.setPrimaryFragment(true);
		supportInvalidateOptionsMenu();

		if(secondaryContainer == null || oldFragment.isAlwaysFullscreen() || currentFragment.isAlwaysStartFullscreen()) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
			trans.hide(oldFragment);
			trans.add(R.id.fragment_container, fragment, tag + "");
			trans.commit();
		} else {
			// Make sure secondary container is visible now
			secondaryContainer.setVisibility(View.VISIBLE);

			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

			// Check to see if you need to put on top of old left or not
			if(backStack.size() > 1) {
				// Move old right to left if there is a backstack already
				SubsonicFragment newLeftFragment = backStack.get(backStack.size() - 1);
				if(replaceCurrent) {
					// trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
				}
				trans.remove(newLeftFragment);

				// Only move right to left if replaceCurrent is false
				if(!replaceCurrent) {
					SubsonicFragment oldLeftFragment = backStack.get(backStack.size() - 2);
					oldLeftFragment.setSecondaryFragment(false);
					// trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
					trans.hide(oldLeftFragment);

					// Make sure remove is finished before adding
					trans.commit();
					getSupportFragmentManager().executePendingTransactions();

					trans = getSupportFragmentManager().beginTransaction();
					// trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
					trans.add(R.id.fragment_container, newLeftFragment, newLeftFragment.getSupportTag() + "");
				} else {
					backStack.remove(backStack.size() - 1);
				}
			}

			// Add fragment to the right container
			trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
			trans.add(R.id.fragment_second_container, fragment, tag + "");

			// Commit it all
			trans.commit();

			oldFragment.setIsOnlyVisible(false);
			currentFragment.setIsOnlyVisible(false);
		}
		recreateSpinner();
	}
	public void removeCurrent() {
		// Don't try to remove current if there is no backstack to remove from
		if(backStack.isEmpty()) {
			return;
		}

		if(currentFragment != null) {
			currentFragment.setPrimaryFragment(false);
		}
		SubsonicFragment oldFragment = currentFragment;

		currentFragment = backStack.remove(backStack.size() - 1);
		currentFragment.setPrimaryFragment(true, false);
		supportInvalidateOptionsMenu();

		if(secondaryContainer == null || currentFragment.isAlwaysFullscreen() || oldFragment.isAlwaysStartFullscreen()) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
			trans.remove(oldFragment);
			trans.show(currentFragment);
			trans.commit();
		} else {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

			// Remove old right fragment
			trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
			trans.remove(oldFragment);

			// Only switch places if there is a backstack, otherwise primary container is correct
			if(backStack.size() > 0 && !backStack.get(backStack.size() - 1).isAlwaysFullscreen() && !currentFragment.isAlwaysStartFullscreen()) {
				trans.setCustomAnimations(0, 0, 0, 0);
				// Add current left fragment to right side
				trans.remove(currentFragment);

				// Make sure remove is finished before adding
				trans.commit();
				getSupportFragmentManager().executePendingTransactions();

				trans = getSupportFragmentManager().beginTransaction();
				// trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
				trans.add(R.id.fragment_second_container, currentFragment, currentFragment.getSupportTag() + "");

				SubsonicFragment newLeftFragment = backStack.get(backStack.size() - 1);
				newLeftFragment.setSecondaryFragment(true);
				trans.show(newLeftFragment);
			} else {
				secondaryContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.exit_to_right));
				secondaryContainer.setVisibility(View.GONE);

				currentFragment.setIsOnlyVisible(true);
			}

			trans.commit();
		}
		recreateSpinner();
	}
	public void replaceExistingFragment(SubsonicFragment fragment, int tag) {
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.remove(currentFragment);
		trans.add(R.id.fragment_container, fragment, tag + "");
		trans.commit();

		currentFragment = fragment;
		currentFragment.setPrimaryFragment(true);
		supportInvalidateOptionsMenu();
	}

	public void invalidate() {
		if(currentFragment != null) {
			while(backStack.size() > 0) {
				removeCurrent();
			}

			if(currentFragment instanceof UserFragment || currentFragment instanceof AdminFragment) {
				restart(false);
			} else {
				currentFragment.invalidate();
			}
			populateTabs();
		}

		supportInvalidateOptionsMenu();
	}

	protected void recreateSpinner() {
		if(currentFragment == null || currentFragment.getTitle() == null) {
			return;
		}
		if(spinnerAdapter == null || getSupportActionBar().getCustomView() == null) {
			createCustomActionBarView();
		}

		if(backStack.size() > 0) {
			createCustomActionBarView();
			spinnerAdapter.clear();
			for(int i = 0; i < backStack.size(); i++) {
				CharSequence title = backStack.get(i).getTitle();
				if(title != null) {
					spinnerAdapter.add(title);
				} else {
					spinnerAdapter.add("null");
				}
			}
			if(currentFragment.getTitle() != null) {
				spinnerAdapter.add(currentFragment.getTitle());
			} else {
				spinnerAdapter.add("null");
			}
			spinnerAdapter.notifyDataSetChanged();
			actionBarSpinner.setSelection(spinnerAdapter.getCount() - 1);
			if(!isTv()) {
				getSupportActionBar().setDisplayShowTitleEnabled(false);
				getSupportActionBar().setDisplayShowCustomEnabled(true);
			}

			if(drawerToggle.isDrawerIndicatorEnabled()) {
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
				drawerToggle.setDrawerIndicatorEnabled(false);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		} else if(!isTv()) {
			getSupportActionBar().setDisplayShowTitleEnabled(true);
			getSupportActionBar().setTitle(currentFragment.getTitle());
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			drawerToggle.setDrawerIndicatorEnabled(true);
		}
	}

	protected void restart() {
		restart(true);
	}
	protected void restart(boolean resumePosition) {
		Intent intent = new Intent(this, this.getClass());
		intent.putExtras(getIntent());
		if(resumePosition) {
			intent.putExtra(Constants.FRAGMENT_POSITION, lastSelectedPosition);
		} else {
			String fragmentType = Util.openToTab(this);
			intent.putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, fragmentType);
			intent.putExtra(Constants.FRAGMENT_POSITION, getDrawerItemId(fragmentType));
		}
		finish();
		Util.startActivityWithoutTransition(this, intent);
	}

	private void applyTheme() {
		theme = ThemeUtil.getTheme(this);

		if(theme != null && theme.indexOf("fullscreen") != -1) {
			theme = theme.substring(0, theme.indexOf("_fullscreen"));
			ThemeUtil.setTheme(this, theme);
		}

		ThemeUtil.applyTheme(this, theme);
		actionbarColored = Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true);
	}
	private void applyFullscreen() {
		fullScreen = Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_FULL_SCREEN, false);
		if(fullScreen || isTv()) {
			// Hide additional elements on higher Android versions
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_FULLSCREEN |
						View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

				getWindow().getDecorView().setSystemUiVisibility(flags);
			} else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			}
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	public boolean isDestroyedCompat() {
		return destroyed;
	}

	public synchronized ImageLoader getImageLoader() {
		if (IMAGE_LOADER == null) {
			IMAGE_LOADER = new ImageLoader(this);
		}
		return IMAGE_LOADER;
	}
	public synchronized static ImageLoader getStaticImageLoader(Context context) {
		if (IMAGE_LOADER == null) {
			IMAGE_LOADER = new ImageLoader(context);
		}
		return IMAGE_LOADER;
	}

	public DownloadService getDownloadService() {
		if(finished) {
			return null;
		}

		// If service is not available, request it to start and wait for it.
		for (int i = 0; i < 5; i++) {
			DownloadService downloadService = DownloadService.getInstance();
			if (downloadService != null) {
				break;
			}
			Log.w(TAG, "DownloadService not running. Attempting to start it.");
			DownloadService.startService(this);
			Util.sleepQuietly(50L);
		}

		final DownloadService downloadService = DownloadService.getInstance();
		if(downloadService != null && afterServiceAvailable.size() > 0) {
			for(Runnable runnable: afterServiceAvailable) {
				handler.post(runnable);
			}
			afterServiceAvailable.clear();
		}
		return downloadService;
	}
	public void runWhenServiceAvailable(Runnable runnable) {
		if(getDownloadService() != null) {
			runnable.run();
		} else {
			afterServiceAvailable.add(runnable);
			checkIfServiceAvailable();
		}
	}
	private void checkIfServiceAvailable() {
		if(getDownloadService() == null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					checkIfServiceAvailable();
				}
			}, 50);
		} else if(afterServiceAvailable.size() > 0) {
			for(Runnable runnable: afterServiceAvailable) {
				handler.post(runnable);
			}
			afterServiceAvailable.clear();
		}
	}

	public static String getThemeName() {
		return theme;
	}

	public boolean isTv() {
		return tv;
	}
	public boolean isTouchscreen() {
		return touchscreen;
	}

	public void openNowPlaying() {

	}
	public void closeNowPlaying() {

	}

	public void setActiveServer(int instance) {
		if (Util.getActiveServer(this) != instance) {
			final DownloadService service = getDownloadService();
			if (service != null) {
				new SilentBackgroundTask<Void>(this) {
					@Override
					protected Void doInBackground() throws Throwable {
						service.clearIncomplete();
						return null;
					}
				}.execute();

			}
			Util.setActiveServer(this, instance);
			invalidate();
			UserUtil.refreshCurrentUser(this, false, true);
			updateDrawerHeader();
		}
	}
	public void updateDrawerHeader() {
		if(Util.isOffline(this)) {
			drawerServerName.setText(R.string.select_album_offline);
			drawerUserName.setText("");
			drawerUserAvatar.setVisibility(View.GONE);
			drawerHeader.setClickable(false);
			drawerHeaderToggle.setVisibility(View.GONE);
		} else {
			drawerServerName.setText(Util.getServerName(this));
			drawerUserName.setText(UserUtil.getCurrentUsername(this));
			drawerUserAvatar.setVisibility(View.VISIBLE);
			getImageLoader().loadAvatar(this, drawerUserAvatar, UserUtil.getCurrentUsername(this));
			drawerHeader.setClickable(true);
			drawerHeaderToggle.setVisibility(View.VISIBLE);
		}
	}

	public void toggleOffline() {
		boolean isOffline = Util.isOffline(this);
		Util.setOffline(this, !isOffline);
		invalidate();
		DownloadService service = getDownloadService();
		if (service != null) {
			service.setOnline(isOffline);
		}

		// Coming back online
		if(isOffline) {
			int scrobblesCount = Util.offlineScrobblesCount(this);
			int starsCount = Util.offlineStarsCount(this);
			if(scrobblesCount > 0 || starsCount > 0){
				showOfflineSyncDialog(scrobblesCount, starsCount);
			}
		}

		UserUtil.seedCurrentUser(this);
		this.updateDrawerHeader();
		drawer.closeDrawers();
	}

	private void showOfflineSyncDialog(final int scrobbleCount, final int starsCount) {
		String syncDefault = Util.getSyncDefault(this);
		if(syncDefault != null) {
			if("sync".equals(syncDefault)) {
				syncOffline(scrobbleCount, starsCount);
				return;
			} else if("delete".equals(syncDefault)) {
				deleteOffline();
				return;
			}
		}

		View checkBoxView = this.getLayoutInflater().inflate(R.layout.sync_dialog, null);
		final CheckBox checkBox = (CheckBox)checkBoxView.findViewById(R.id.sync_default);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.offline_sync_dialog_title)
				.setMessage(this.getResources().getString(R.string.offline_sync_dialog_message, scrobbleCount, starsCount))
				.setView(checkBoxView)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						if(checkBox.isChecked()) {
							Util.setSyncDefault(SubsonicActivity.this, "sync");
						}
						syncOffline(scrobbleCount, starsCount);
					}
				}).setNeutralButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		}).setNegativeButton(R.string.common_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				if (checkBox.isChecked()) {
					Util.setSyncDefault(SubsonicActivity.this, "delete");
				}
				deleteOffline();
			}
		});

		builder.create().show();
	}

	private void syncOffline(final int scrobbleCount, final int starsCount) {
		new SilentBackgroundTask<Integer>(this) {
			@Override
			protected Integer doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(SubsonicActivity.this);
				return musicService.processOfflineSyncs(SubsonicActivity.this, null);
			}

			@Override
			protected void done(Integer result) {
				if(result == scrobbleCount) {
					Util.toast(SubsonicActivity.this, getResources().getString(R.string.offline_sync_success, result));
				} else {
					Util.toast(SubsonicActivity.this, getResources().getString(R.string.offline_sync_partial, result, scrobbleCount + starsCount));
				}
			}

			@Override
			protected void error(Throwable error) {
				Log.w(TAG, "Failed to sync offline stats", error);
				String msg = getResources().getString(R.string.offline_sync_error) + " " + getErrorMessage(error);
				Util.toast(SubsonicActivity.this, msg);
			}
		}.execute();
	}
	private void deleteOffline() {
		SharedPreferences.Editor offline = Util.getOfflineSync(this).edit();
		offline.putInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
		offline.putInt(Constants.OFFLINE_STAR_COUNT, 0);
		offline.commit();
	}
	
	public int getDrawerItemId(String fragmentType) {
		if(fragmentType == null) {
			return R.id.drawer_home;
		}

		switch(fragmentType) {
			case "Home":
				return R.id.drawer_home;
			case "Artist":
				return R.id.drawer_library;
			case "Playlist":
				return R.id.drawer_playlists;
			case "Podcast":
				return R.id.drawer_podcasts;
			case "Bookmark":
				return R.id.drawer_bookmarks;
			case "Internet Radio":
				return R.id.drawer_internet_radio_stations;
			case "Share":
				return R.id.drawer_shares;
			case "Chat":
				return R.id.drawer_chat;
			default:
				return R.id.drawer_home;
		}
	}

	private void setUncaughtExceptionHandler() {
		Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
		if (!(handler instanceof SubsonicActivity.SubsonicUncaughtExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new SubsonicActivity.SubsonicUncaughtExceptionHandler(this));
		}
	}

	/**
	 * Logs the stack trace of uncaught exceptions to a file on the SD card.
	 */
	private static class SubsonicUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		private final Thread.UncaughtExceptionHandler defaultHandler;
		private final Context context;

		private SubsonicUncaughtExceptionHandler(Context context) {
			this.context = context;
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		}

		@Override
		public void uncaughtException(Thread thread, Throwable throwable) {
			File file = null;
			PrintWriter printWriter = null;
			try {

				PackageInfo packageInfo = context.getPackageManager().getPackageInfo("github.daneren2005.dsub", 0);
				file = new File(Environment.getExternalStorageDirectory(), "dsub-stacktrace.txt");
				printWriter = new PrintWriter(file);
				printWriter.println("Android API level: " + Build.VERSION.SDK);
				printWriter.println("Subsonic version name: " + packageInfo.versionName);
				printWriter.println("Subsonic version code: " + packageInfo.versionCode);
				printWriter.println();
				throwable.printStackTrace(printWriter);
				Log.i(TAG, "Stack trace written to " + file);
			} catch (Throwable x) {
				Log.e(TAG, "Failed to write stack trace to " + file, x);
			} finally {
				Util.close(printWriter);
				if (defaultHandler != null) {
					defaultHandler.uncaughtException(thread, throwable);
				}

			}
		}
	}
}
