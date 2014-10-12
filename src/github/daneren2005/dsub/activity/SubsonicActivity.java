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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.DrawerAdapter;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.util.UserUtil;

public class SubsonicActivity extends ActionBarActivity implements OnItemSelectedListener {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	protected static boolean fullScreen;
	private String[] drawerItemsDescriptions;
	private String[] drawerItems;
	private boolean drawerIdle = true;
	private boolean[] enabledItems = {true, true, true, true, true};
	private boolean destroyed = false;
	private boolean finished = false;
	protected List<SubsonicFragment> backStack = new ArrayList<SubsonicFragment>();
	protected SubsonicFragment currentFragment;
	protected View primaryContainer;
	protected View secondaryContainer;
	Spinner actionBarSpinner;
	ArrayAdapter<CharSequence> spinnerAdapter;
	ViewGroup rootView;
	DrawerLayout drawer;
	ActionBarDrawerToggle drawerToggle;
	DrawerAdapter drawerAdapter;
	ListView drawerList;
	TextView lastSelectedView = null;
	int lastSelectedPosition = 0;
	boolean drawerOpen = false;

	@Override
	protected void onCreate(Bundle bundle) {
		setUncaughtExceptionHandler();
		applyTheme();
		super.onCreate(bundle);
		applyFullscreen();
		startService(new Intent(this, DownloadService.class));
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		View actionbar = getLayoutInflater().inflate(R.layout.actionbar_spinner, null);
		actionBarSpinner = (Spinner)actionbar.findViewById(R.id.spinner);
		spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBarSpinner.setOnItemSelectedListener(this);
		actionBarSpinner.setAdapter(spinnerAdapter);

		getSupportActionBar().setCustomView(actionbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		if(getIntent().hasExtra(Constants.FRAGMENT_POSITION)) {
			lastSelectedPosition = getIntent().getIntExtra(Constants.FRAGMENT_POSITION, 0);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Util.registerMediaButtonEventReceiver(this);

		// Make sure to update theme
		if (theme != null && !theme.equals(Util.getTheme(this)) || fullScreen != Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_FULL_SCREEN, false)) {
			restart();
		}
		
		populateDrawer();
		UpdateView.addActiveActivity();
	}

	@Override
	protected void onPause() {
		super.onPause();

		UpdateView.removeActiveActivity();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyed = true;
	}

	@Override
	public void finish() {
		super.finish();
		Util.disablePendingTransition(this);
	}

	@Override
	public void startActivity(Intent intent) {
		if(intent.getComponent() != null && "github.daneren2005.dsub.activity.DownloadActivity".equals(intent.getComponent().getClassName())) {
			intent.putExtra(Constants.FRAGMENT_POSITION, lastSelectedPosition);
		}
		super.startActivity(intent);
	}

	@Override
	public void setContentView(int viewId) {
		super.setContentView(R.layout.abstract_activity);
		rootView = (ViewGroup) findViewById(R.id.content_frame);
		LayoutInflater layoutInflater = getLayoutInflater();
		layoutInflater.inflate(viewId, rootView);

		
		drawerList = (ListView) findViewById(R.id.left_drawer);

		drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
				final int actualPosition = drawerAdapter.getActualPosition(position);
				if("Settings".equals(drawerItemsDescriptions[actualPosition])) {
					startActivity(new Intent(SubsonicActivity.this, SettingsActivity.class));
					drawer.closeDrawers();
				} else if("Admin".equals(drawerItemsDescriptions[actualPosition]) && UserUtil.isCurrentAdmin()) {
					UserUtil.confirmCredentials(SubsonicActivity.this, new Runnable() {
						@Override
						public void run() {
							drawerItemSelected(actualPosition, view);
						}
					});
				} else {
					drawerItemSelected(actualPosition, view);
				}
			}
		});

		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawer, R.drawable.ic_drawer, R.string.common_appname, R.string.common_appname) {
			@Override
			public void onDrawerClosed(View view) {
				setTitle(currentFragment.getTitle());
				
				drawerIdle = true;
				drawerOpen = false;

				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View view) {
				DownloadService downloadService = getDownloadService();
				if(downloadService == null || downloadService.getBackgroundDownloads().isEmpty()) {
					drawerAdapter.setDownloadVisible(false);
				} else {
					drawerAdapter.setDownloadVisible(true);
				}

				if(lastSelectedView == null) {
					lastSelectedView = (TextView) drawerList.getChildAt(lastSelectedPosition).findViewById(R.id.drawer_name);
					if(lastSelectedView != null) {
						lastSelectedView.setTextAppearance(SubsonicActivity.this, R.style.DSub_TextViewStyle_Bold);
					}
				}

				getSupportActionBar().setTitle(R.string.common_appname);
				getSupportActionBar().setDisplayShowCustomEnabled(false);
				
				drawerIdle = true;
				drawerOpen = true;

				supportInvalidateOptionsMenu();
			}
			
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
				drawerIdle = false;
			}
		};
		drawer.setDrawerListener(drawerToggle);
		drawerToggle.setDrawerIndicatorEnabled(false);
		
		drawer.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (drawerIdle && currentFragment != null && currentFragment.getGestureDetector() != null) {
					return currentFragment.getGestureDetector().onTouchEvent(event);
				} else {
					return false;
				}
			}
		});

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
		recreateSpinner();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		if(drawerOpen) {
			menuInflater.inflate(R.menu.drawer_menu, menu);
		} else if(currentFragment != null) {
			currentFragment.onCreateOptionsMenu(menu, menuInflater);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(drawerToggle.onOptionsItemSelected(item)) {
			return true;
		} else if(item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}

		return currentFragment.onOptionsItemSelected(item);
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
		if(!title.equals(getSupportActionBar().getTitle())) {
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
			for(int i = top; i > position; i--) {
				removeCurrent();
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}
	
	private void populateDrawer() {
		SharedPreferences prefs = Util.getPreferences(this);
		boolean podcastsEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_PODCASTS_ENABLED, true);
		boolean bookmarksEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_BOOKMARKS_ENABLED, true) && !Util.isOffline(this) && ServerInfo.canBookmark(this);
		boolean sharedEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_SHARED_ENABLED, true) && !Util.isOffline(this);
		boolean chatEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_CHAT_ENABLED, true) && !Util.isOffline(this);
		boolean adminEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_ADMIN_ENABLED, true) && !Util.isOffline(this);
		
		if(drawerItems == null || !enabledItems[0] == podcastsEnabled || !enabledItems[1] == bookmarksEnabled || !enabledItems[2] == sharedEnabled || !enabledItems[3] == chatEnabled || !enabledItems[4] == adminEnabled) {
			drawerItems = getResources().getStringArray(R.array.drawerItems);
			drawerItemsDescriptions = getResources().getStringArray(R.array.drawerItemsDescriptions);

			List<String> drawerItemsList = new ArrayList<String>(Arrays.asList(drawerItems));
			List<Integer> drawerItemsIconsList = new ArrayList<Integer>();
			List<Boolean> drawerItemsVisibleList = new ArrayList<Boolean>();

			int[] arrayAttr = {R.attr.drawerItemsIcons};
			TypedArray arrayType = obtainStyledAttributes(arrayAttr);
			int arrayId = arrayType.getResourceId(0, 0);
			TypedArray iconType = getResources().obtainTypedArray(arrayId);
			for(int i = 0; i < drawerItemsList.size(); i++) {
				drawerItemsIconsList.add(iconType.getResourceId(i, 0));
				drawerItemsVisibleList.add(true);
			}
			iconType.recycle();
			arrayType.recycle();

			// Hide listings user doesn't want to see
			if(!podcastsEnabled) {
				drawerItemsVisibleList.set(3, false);
			}
			if(!bookmarksEnabled) {
				drawerItemsVisibleList.set(4, false);
			}
			if(!sharedEnabled) {
				drawerItemsVisibleList.set(5, false);
			}
			if(!chatEnabled) {
				drawerItemsVisibleList.set(6, false);
			}
			if(!adminEnabled) {
				drawerItemsVisibleList.set(7, false);
			}
			if(!getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW)) {
				drawerItemsVisibleList.set(8, false);
			}
			
			drawerList.setAdapter(drawerAdapter = new DrawerAdapter(this, drawerItemsList, drawerItemsIconsList, drawerItemsVisibleList));
			enabledItems[0] = podcastsEnabled;
			enabledItems[1] = bookmarksEnabled;
			enabledItems[2] = sharedEnabled;
			enabledItems[3] = chatEnabled;
			enabledItems[4] = adminEnabled;

			String fragmentType = getIntent().getStringExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE);
			if(fragmentType != null && lastSelectedPosition == 0) {
				for(int i = 0; i < drawerItemsDescriptions.length; i++) {
					if(fragmentType.equals(drawerItemsDescriptions[i])) {
						lastSelectedPosition = drawerAdapter.getAdapterPosition(i);
						break;
					}
				}
			}

			if(drawerList.getChildAt(lastSelectedPosition) == null) {
				lastSelectedView = null;
				drawerAdapter.setSelectedPosition(lastSelectedPosition);
			} else {
				lastSelectedView = (TextView) drawerList.getChildAt(lastSelectedPosition).findViewById(R.id.drawer_name);
				if(lastSelectedView != null) {
					lastSelectedView.setTextAppearance(SubsonicActivity.this, R.style.DSub_TextViewStyle_Bold);
				}
			}
		}
	}
	
	private void drawerItemSelected(int position, View view) {
		startFragmentActivity(drawerItemsDescriptions[position]);
		
		if(lastSelectedView != view) {
			if(lastSelectedView != null) {
				lastSelectedView.setTextAppearance(this, R.style.DSub_TextViewStyle);
			}
			
			lastSelectedView = (TextView) view.findViewById(R.id.drawer_name);
			lastSelectedView.setTextAppearance(this, R.style.DSub_TextViewStyle_Bold);
			lastSelectedPosition = position;
		}
	}

	public void startFragmentActivity(String fragmentType) {
		Intent intent = new Intent();
		intent.setClass(SubsonicActivity.this, SubsonicFragmentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(!"".equals(fragmentType)) {
			intent.putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, fragmentType);
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

		if(secondaryContainer == null) {
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
		}
		recreateSpinner();
	}
	public void removeCurrent() {
		if(currentFragment != null) {
			currentFragment.setPrimaryFragment(false);
		}
		Fragment oldFrag = currentFragment;

		currentFragment = backStack.remove(backStack.size() - 1);
		currentFragment.setPrimaryFragment(true, false);
		supportInvalidateOptionsMenu();

		if(secondaryContainer == null) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
			trans.remove(oldFrag);
			trans.show(currentFragment);
			trans.commit();
		} else {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			
			// Remove old right fragment
			trans.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
			trans.remove(oldFrag);

			// Only switch places if there is a backstack, otherwise primary container is correct
			if(backStack.size() > 0) {
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
			}
			
			trans.commit();
		}
		recreateSpinner();
	}

	public void invalidate() {
		if(currentFragment != null) {
			while(backStack.size() > 0) {
				removeCurrent();
			}

			currentFragment.invalidate();
			populateDrawer();
		}
		
		supportInvalidateOptionsMenu();
	}
	
	protected void recreateSpinner() {
		if(currentFragment == null || currentFragment.getTitle() == null) {
			return;
		}

		if(backStack.size() > 0) {
			spinnerAdapter.clear();
			for(int i = 0; i < backStack.size(); i++) {
				spinnerAdapter.add(backStack.get(i).getTitle());
			}
			spinnerAdapter.add(currentFragment.getTitle());
			spinnerAdapter.notifyDataSetChanged();
			actionBarSpinner.setSelection(spinnerAdapter.getCount() - 1);
			getSupportActionBar().setDisplayShowCustomEnabled(true);
		} else {
			getSupportActionBar().setDisplayShowCustomEnabled(false);
		}
	}

	protected void restart() {
		Intent intent = new Intent(this, ((Object) this).getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(getIntent());
		Util.startActivityWithoutTransition(this, intent);
	}

	private void applyTheme() {
		theme = Util.getTheme(this);

		if(theme != null && theme.indexOf("fullscreen") != -1) {
			theme = theme.substring(0, theme.indexOf("_fullscreen"));
			Util.setTheme(this, theme);
		}
		
		Util.applyTheme(this, theme);
	}
	private void applyFullscreen() {
		fullScreen = Util.getPreferences(this).getBoolean(Constants.PREFERENCES_KEY_FULL_SCREEN, false);
		if(fullScreen) {
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
				return downloadService;
			}
			Log.w(TAG, "DownloadService not running. Attempting to start it.");
			startService(new Intent(this, DownloadService.class));
			Util.sleepQuietly(50L);
		}
		return DownloadService.getInstance();
	}
	
	public static String getThemeName() {
		return theme;
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
				file = new File(Environment.getExternalStorageDirectory(), "subsonic-stacktrace.txt");
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
