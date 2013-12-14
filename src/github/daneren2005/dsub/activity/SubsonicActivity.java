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
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.SearchFragment;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.DrawerAdapter;
import github.daneren2005.dsub.view.UpdateView;

import java.io.File;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SubsonicActivity extends ActionBarActivity implements OnItemSelectedListener {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	protected static boolean fullScreen;
	private String[] drawerItemsDescriptions;
	private String[] drawerItems;
	private boolean drawerIdle = true;
	private boolean[] enabledItems = {true, true, true};
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
	ListView drawerList;
	View lastSelectedView = null;
	int lastSelectedPosition = 0;
	boolean drawerOpen = false;

	@Override
	protected void onCreate(Bundle bundle) {
		setUncaughtExceptionHandler();
		applyTheme();
		super.onCreate(bundle);
		applyFullscreen();
		startService(new Intent(this, DownloadServiceImpl.class));
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
		getImageLoader().clear();
	}

	@Override
	public void finish() {
		super.finish();
		Util.disablePendingTransition(this);
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
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if("Settings".equals(drawerItemsDescriptions[position])) {
					startActivity(new Intent(SubsonicActivity.this, SettingsActivity.class));
					drawer.closeDrawers();
				} else {
					startFragmentActivity(drawerItemsDescriptions[position]);

					if(lastSelectedView != view) {
						if(lastSelectedView != null) {
							lastSelectedView.setBackgroundResource(android.R.color.transparent);
						}
						view.setBackgroundResource(R.color.dividerColor);
						lastSelectedView = view;
						lastSelectedPosition = position;
					}
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
				if(lastSelectedView == null) {
					lastSelectedView = drawerList.getChildAt(lastSelectedPosition);
					lastSelectedView.setBackgroundResource(R.color.dividerColor);
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
		for(int i = 1; i < size; i++) {
			SubsonicFragment frag = (SubsonicFragment)fm.findFragmentByTag(ids[i]);
			frag.setSupportTag(ids[i]);
			if(secondaryContainer != null) {
				frag.setPrimaryFragment(false, true);
			}
			backStack.add(frag);
		}

		// Current fragment is hidden in secondaryContainer
		if(secondaryContainer == null && findViewById(currentFragment.getRootId()) == null) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.remove(currentFragment);
			trans.commit();
			getSupportFragmentManager().executePendingTransactions();

			trans = getSupportFragmentManager().beginTransaction();
			trans.add(backStack.get(backStack.size() - 1).getRootId(), currentFragment, ids[0]);
			trans.commit();
		}
		// Current fragment needs to be moved over to secondaryContainer
		else if(secondaryContainer != null && secondaryContainer.findViewById(currentFragment.getRootId()) == null && backStack.size() > 0) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.remove(currentFragment);
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
		if(drawerOpen == true) {
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
			getDownloadService().setRemoteVolume(isVolumeUp);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		getSupportActionBar().setTitle(title);
		recreateSpinner();
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
		boolean bookmarksEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_BOOKMARKS_ENABLED, true) && !Util.isOffline(this);
		boolean chatEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_CHAT_ENABLED, true) && !Util.isOffline(this);
		
		if(drawerItems == null || !enabledItems[0] == podcastsEnabled || !enabledItems[1] == bookmarksEnabled || !enabledItems[2] == chatEnabled) {
			drawerItems = getResources().getStringArray(R.array.drawerItems);
			drawerItemsDescriptions = getResources().getStringArray(R.array.drawerItemsDescriptions);
	
			// Remove listings that user wants hidden
			int alreadyRemoved = 0;
			List<String> drawerItemsList = new ArrayList<String>(Arrays.asList(drawerItems));
			List<String> drawerItemsDescriptionsList = new ArrayList<String>(Arrays.asList(drawerItemsDescriptions));
			List<Integer> drawerItemsIconsList = new ArrayList<Integer>();

			int[] arrayAttr = {R.attr.drawerItemsIcons};
			TypedArray arrayType = obtainStyledAttributes(arrayAttr);
			int arrayId = arrayType.getResourceId(0, 0);
			TypedArray iconType = getResources().obtainTypedArray(arrayId);
			for(int i = 0; i < drawerItemsList.size(); i++) {
				drawerItemsIconsList.add(iconType.getResourceId(i, 0));
			}
			iconType.recycle();
			arrayType.recycle();
	
			// Selectively remove podcast listing [3]
			if(!podcastsEnabled) {
				drawerItemsList.remove(3 - alreadyRemoved);
				drawerItemsDescriptionsList.remove(3 - alreadyRemoved);
				drawerItemsIconsList.remove(3 - alreadyRemoved);
				alreadyRemoved++;
			}

			// Selectively remove bookmarks listing [4]
			if(!bookmarksEnabled) {
				drawerItemsList.remove(4 - alreadyRemoved);
				drawerItemsDescriptionsList.remove(4 - alreadyRemoved);
				drawerItemsIconsList.remove(4 - alreadyRemoved);
				alreadyRemoved++;
			}
	
			// Selectively remove chat listing: [5]
			if(!chatEnabled) {
				drawerItemsList.remove(5 - alreadyRemoved);
				drawerItemsDescriptionsList.remove(5 - alreadyRemoved);
				drawerItemsIconsList.remove(5 - alreadyRemoved);
				alreadyRemoved++;
			}
	
			// Put list back together
			if(alreadyRemoved > 0) {
				drawerItems = drawerItemsList.toArray(new String[0]);
				drawerItemsDescriptions = drawerItemsDescriptionsList.toArray(new String[0]);
			}
			
			drawerList.setAdapter(new DrawerAdapter(this, drawerItemsList, drawerItemsIconsList));
			enabledItems[0] = podcastsEnabled;
			enabledItems[1] = bookmarksEnabled;
			enabledItems[2] = chatEnabled;
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
		if(this.getClass() != SubsonicFragmentActivity.class) {
			Intent intent = new Intent(this, SubsonicFragmentActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
			Util.startActivityWithoutTransition(this, intent);
		} else {
			finished = true;
			this.stopService(new Intent(this, DownloadServiceImpl.class));
			this.finish();
		}
	}

	public boolean onBackPressedSupport() {
		if(backStack.size() > 0) {
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

	public void replaceFragment(SubsonicFragment fragment, int id, int tag) {
		replaceFragment(fragment, id, tag, false);
	}
	public void replaceFragment(SubsonicFragment fragment, int id, int tag, boolean replaceCurrent) {
		if(currentFragment != null) {
			currentFragment.setPrimaryFragment(false, secondaryContainer != null);
		}
		backStack.add(currentFragment);

		currentFragment = fragment;
		currentFragment.setPrimaryFragment(true);
		supportInvalidateOptionsMenu();

		if(secondaryContainer == null) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(id, fragment, tag + "");
			trans.commit();
		} else {
			// Make sure secondary container is visible now
			secondaryContainer.setVisibility(View.VISIBLE);

			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

			// Check to see if you need to put on top of old left or not
			if(backStack.size() > 1) {
				// Move old right to left if there is a backstack already
				SubsonicFragment newLeftFragment = backStack.get(backStack.size() - 1);
				trans.remove(newLeftFragment);

				// Only move right to left if replaceCurrent is false
				if(!replaceCurrent) {
					SubsonicFragment oldLeftFragment = backStack.get(backStack.size() - 2);
					oldLeftFragment.setSecondaryFragment(false);
					int leftId = oldLeftFragment.getRootId();

					// Make sure remove is finished before adding
					trans.commit();
					getSupportFragmentManager().executePendingTransactions();

					trans = getSupportFragmentManager().beginTransaction();
					trans.add(leftId, newLeftFragment, newLeftFragment.getSupportTag() + "");
				} else {
					backStack.remove(backStack.size() - 1);
				}
			}
			
			// Add fragment to the right container
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
		Fragment oldFrag = (Fragment)currentFragment;

		currentFragment = (SubsonicFragment) backStack.remove(backStack.size() - 1);
		currentFragment.setPrimaryFragment(true, false);
		supportInvalidateOptionsMenu();

		if(secondaryContainer == null) {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.remove(oldFrag);
			trans.commit();
		} else {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			
			// Remove old right fragment
			trans.remove(oldFrag);

			// Only switch places if there is a backstack, otherwise primary container is correct
			if(backStack.size() > 0) {
				// Add current left fragment to right side
				trans.remove(currentFragment);

				// Make sure remove is finished before adding
				trans.commit();
				getSupportFragmentManager().executePendingTransactions();

				trans = getSupportFragmentManager().beginTransaction();
				trans.add(R.id.fragment_second_container, currentFragment, currentFragment.getSupportTag() + "");
				
				backStack.get(backStack.size() - 1).setSecondaryFragment(true);
			} else {
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
	}
	
	protected void recreateSpinner() {
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
		Intent intent = new Intent(this, this.getClass());
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
		
		if ("dark".equals(theme)) {
			setTheme(R.style.Theme_DSub_Dark);
		} else if ("black".equals(theme)) {
			setTheme(R.style.Theme_DSub_Black);
		} else if ("light".equals(theme)) {
			setTheme(R.style.Theme_DSub_Light);
		} else {
			setTheme(R.style.Theme_DSub_Holo);
		}
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

	public boolean isDestroyed() {
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
			DownloadService downloadService = DownloadServiceImpl.getInstance();
			if (downloadService != null) {
				return downloadService;
			}
			Log.w(TAG, "DownloadService not running. Attempting to start it.");
			startService(new Intent(this, DownloadServiceImpl.class));
			Util.sleepQuietly(50L);
		}
		return DownloadServiceImpl.getInstance();
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
