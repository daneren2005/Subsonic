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
import android.content.pm.PackageInfo;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SubsonicActivity extends ActionBarActivity implements OnItemSelectedListener {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	private boolean destroyed = false;
	protected TabPagerAdapter pagerAdapter;
	protected ViewPager viewPager;
	protected List<SubsonicFragment> backStack = new ArrayList<SubsonicFragment>();
	protected SubsonicFragment currentFragment;
	Spinner actionBarSpinner;
	ArrayAdapter<CharSequence> spinnerAdapter;
	ViewGroup rootView;
	DrawerLayout drawer;
	ActionBarDrawerToggle drawerToggle;
	ListView drawerList;

	@Override
	protected void onCreate(Bundle bundle) {
		setUncaughtExceptionHandler();
		applyTheme();
		super.onCreate(bundle);
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
		if (theme != null && !theme.equals(Util.getTheme(this))) {
			restart();
		}
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

		String[] drawerListItems = getResources().getStringArray(R.array.drawerItems);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, drawerListItems));

		drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent;

				switch(position) {
					case 0:
						startActivity(MainActivity.class);
						break;
					case 1:
						startFragmentActivity("Podcast");
						break;
					case 2:
						startFragmentActivity("Chat");
						break;
					case 3:
						startActivity(DownloadActivity.class);
						break;
					case 4:
						startActivity(SettingsActivity.class);
						break;
					case 5:
						exit();
						break;
				}
			}
		});

		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawer, R.drawable.ic_drawer, R.string.common_appname, R.string.common_appname) {
			@Override
			public void onDrawerClosed(View view) {
				/*if(pagerAdapter != null) {

				} else {
					getActionBar().setTitle(currentFragment.getTitle());
				}*/
			}

			@Override
			public void onDrawerOpened(View view) {
				// getActionBar().setTitle(R.string.common_appname);
			}
		};
		drawer.setDrawerListener(drawerToggle);
		if(this.getClass() != MainActivity.class) {
			drawerToggle.setDrawerIndicatorEnabled(false);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		if(viewPager == null) {
			String[] ids = new String[backStack.size() + 1];
			ids[0] = currentFragment.getTag();
			int i = 1;
			for(SubsonicFragment frag: backStack) {
				ids[i] = frag.getTag();
				i++;
			}
			savedInstanceState.putStringArray(Constants.MAIN_BACK_STACK, ids);
			savedInstanceState.putInt(Constants.MAIN_BACK_STACK_SIZE, backStack.size() + 1);
		} else {
			pagerAdapter.onSaveInstanceState(savedInstanceState);
		}
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if(viewPager == null) {
			super.onRestoreInstanceState(savedInstanceState);
			int size = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_SIZE);
			String[] ids = savedInstanceState.getStringArray(Constants.MAIN_BACK_STACK);
			FragmentManager fm = getSupportFragmentManager();
			currentFragment = (SubsonicFragment)fm.findFragmentByTag(ids[0]);
			currentFragment.setPrimaryFragment(true);
			supportInvalidateOptionsMenu();
			for(int i = 1; i < size; i++) {
				SubsonicFragment frag = (SubsonicFragment)fm.findFragmentByTag(ids[i]);
				backStack.add(frag);
			}
			recreateSpinner();
		} else {
			pagerAdapter.onRestoreInstanceState(savedInstanceState);
			super.onRestoreInstanceState(savedInstanceState);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		if(pagerAdapter != null) {
			pagerAdapter.onCreateOptionsMenu(menu, menuInflater);
		} else if(currentFragment != null) {
			currentFragment.onCreateOptionsMenu(menu, menuInflater);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		if(pagerAdapter != null) {
			return pagerAdapter.onOptionsItemSelected(item);
		} else if(currentFragment != null) {
			return currentFragment.onOptionsItemSelected(item);
		}
		return true;
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
		super.setTitle(title);
		if(pagerAdapter != null) {
			pagerAdapter.recreateSpinner();
		} else {
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
				if(pagerAdapter != null) {
					pagerAdapter.removeCurrent();
				} else {
					removeCurrent();
				}
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		
	}

	public void startActivity(Class t) {
		Intent intent = new Intent();
		intent.setClass(SubsonicActivity.this, t);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		if(this.getClass() != MainActivity.class) {
			finish();
		}
	}
	public void startFragmentActivity(String fragmentType) {
		Intent intent = new Intent();
		intent.setClass(SubsonicActivity.this, SubsonicFragmentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(Constants.INTENT_EXTRA_FRAGMENT_TYPE, fragmentType);
		startActivity(intent);
		if(this.getClass() != MainActivity.class) {
			finish();
		}
	}

	protected void exit() {
		if(this.getClass() != MainActivity.class) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
			Util.startActivityWithoutTransition(this, intent);
		} else {
			this.stopService(new Intent(this, DownloadServiceImpl.class));
			this.finish();
		}
	}

	public boolean onBackPressedSupport() {
		if(pagerAdapter != null) {
			return pagerAdapter.onBackPressed();
		} else {
			if(backStack.size() > 0) {
				removeCurrent();
				return false;
			} else {
				return true;
			}
		}
	}
	
	public void replaceFragment(SubsonicFragment fragment, int id, int tag) {
		if(pagerAdapter != null) {
			pagerAdapter.replaceCurrent(fragment, id, tag);
		} else {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			backStack.add(currentFragment);
			
			currentFragment = fragment;
			currentFragment.setPrimaryFragment(true);
			supportInvalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(id, fragment, tag + "");
			trans.commit();
			recreateSpinner();
		}
	}
	private void removeCurrent() {
		if(currentFragment != null) {
			currentFragment.setPrimaryFragment(false);
		}
		Fragment oldFrag = (Fragment)currentFragment;

		currentFragment = (SubsonicFragment) backStack.remove(backStack.size() - 1);
		currentFragment.setPrimaryFragment(true);
		supportInvalidateOptionsMenu();

		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.remove(oldFrag);
		trans.commit();
		recreateSpinner();
	}
	
	private void recreateSpinner() {
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
	
	protected void addTab(int titleRes, Class fragmentClass, Bundle args) {
		pagerAdapter.addTab(getString(titleRes), fragmentClass, args);
	}
	protected void addTab(CharSequence title, Class fragmentClass, Bundle args) {
		pagerAdapter.addTab(title, fragmentClass, args);
	}

	protected void restart() {
		Intent intent = new Intent(this, this.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(getIntent());
		Util.startActivityWithoutTransition(this, intent);
	}

	private void applyTheme() {
		theme = Util.getTheme(this);
		if ("dark".equals(theme)) {
			setTheme(R.style.Theme_DSub_Dark);
		} else if ("black".equals(theme)) {
			setTheme(R.style.Theme_DSub_Black);
		} else if ("light".equals(theme)) {
			setTheme(R.style.Theme_DSub_Light);
		} else if ("dark_fullscreen".equals(theme)) {
			setTheme(R.style.Theme_DSub_Dark_Fullscreen);
		} else if ("black_fullscreen".equals(theme)) {
			setTheme(R.style.Theme_DSub_Black_Fullscreen);
		} else if ("light_fullscreen".equals(theme)) {
			setTheme(R.style.Theme_DSub_Light_Fullscreen);
		} else if("holo".equals(theme)) {
			setTheme(R.style.Theme_DSub_Holo);
		} else if("holo_fullscreen".equals(theme)) {
			setTheme(R.style.Theme_DSub_Holo_Fullscreen);
		}else {
			setTheme(R.style.Theme_DSub_Holo);
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
	
	public ViewPager getViewPager() {
		return viewPager;
	}
	public TabPagerAdapter getPagerAdapter() {
		return pagerAdapter;
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
	
	public class TabPagerAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
		private ActionBarActivity activity;
		private ViewPager pager;
		private ActionBar actionBar;
		private SubsonicFragment currentFragment;
		private List<TabInfo> tabs = new ArrayList<TabInfo>();
		private List<List<SubsonicFragment>> frags = new ArrayList<List<SubsonicFragment>>();
		private List<QueuedFragment> queue = new ArrayList<QueuedFragment>();
		private int currentPosition;
		private boolean dontRecreate = false;

		public TabPagerAdapter(ActionBarActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			this.activity = activity;
			this.actionBar = activity.getSupportActionBar();
			this.pager = pager;
			this.currentPosition = 0;
		}

		@Override
		public Fragment getItem(int i) {
			final TabInfo tabInfo = tabs.get(i);
			SubsonicFragment frag = (SubsonicFragment) Fragment.instantiate(activity, tabInfo.fragmentClass.getName(), tabInfo.args);
			List<SubsonicFragment> fragStack = new ArrayList<SubsonicFragment>();
			fragStack.add(frag);
			while(i > frags.size()) {
				frags.add(null);
			}
			if(i == frags.size()) {
				frags.add(i, fragStack);
			} else {
				frags.set(i, fragStack);
			}
			if(currentFragment == null || currentPosition == i) {
				currentFragment = frag;
				currentFragment.setPrimaryFragment(true);
			}
			return frag;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}
		
		public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
			if(currentFragment != null && !dontRecreate) {
				for(QueuedFragment addFragment: queue) {
					// Let Pager know not to try to create options menu in replaceFragment
					dontRecreate = true;
					replaceFragment(addFragment.fragment, addFragment.id, currentFragment.getSupportTag());
					currentFragment = addFragment.fragment;
				}
				dontRecreate = false;
				queue.clear();
				
				currentFragment.setPrimaryFragment(true);
				currentFragment.onCreateOptionsMenu(menu, menuInflater);
			}
		}
		public boolean onOptionsItemSelected(MenuItem item) {
			if(currentFragment != null) {
				return currentFragment.onOptionsItemSelected(item);
			} else {
				return false;
			}
		}

		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
			TabInfo tabInfo = (TabInfo) tab.getTag();
			for (int i = 0; i < tabs.size(); i++) {
				if ( tabs.get(i) == tabInfo ) {
					pager.setCurrentItem(i);
					break;
				}
			}
		}

		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {}

		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {}

		public void onPageScrollStateChanged(int arg0) {}

		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		public void onPageSelected(int position) {
			currentPosition = position;
			actionBar.setSelectedNavigationItem(position);
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			if(position <= frags.size()) {
				List<SubsonicFragment> fragStack = frags.get(position);
				currentFragment = fragStack.get(fragStack.size() - 1);
				if(currentFragment != null) {
					currentFragment.setPrimaryFragment(true);
				}
				activity.supportInvalidateOptionsMenu();
				recreateSpinner();
			}
		}

		public void addTab(CharSequence title, Class fragmentClass, Bundle args) {
			final TabInfo tabInfo = new TabInfo(fragmentClass, args);

			ActionBar.Tab tab = actionBar.newTab();
			tab.setText(title);
			tab.setTabListener(this);
			tab.setTag(tabInfo);

			tabs.add(tabInfo);

			actionBar.addTab(tab);
			notifyDataSetChanged();
		}
		public void queueFragment(SubsonicFragment fragment, int id) {
			QueuedFragment frag = new QueuedFragment();
			frag.fragment = fragment;
			frag.id = id;
			queue.add(frag);
		}
		public void replaceCurrent(SubsonicFragment fragment, int id, int tag) {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			List<SubsonicFragment> fragStack = frags.get(currentPosition);
			fragStack.add(fragment);
			
			currentFragment = fragment;
			currentFragment.setPrimaryFragment(true);
			activity.supportInvalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(id, fragment, tag + "");
			trans.commit();
			recreateSpinner();
		}
		
		public void removeCurrent() {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			List<SubsonicFragment> fragStack = frags.get(currentPosition);
			Fragment oldFrag = (Fragment)fragStack.remove(fragStack.size() - 1);
			
			currentFragment = fragStack.get(fragStack.size() - 1);
			currentFragment.setPrimaryFragment(true);
			activity.supportInvalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.remove(oldFrag);
			trans.commit();
		}
		
		public boolean onBackPressed() {
			List<SubsonicFragment> fragStack = frags.get(currentPosition);
			if(fragStack.size() > 1) {
				removeCurrent();
				recreateSpinner();
				return false;
			} else {
				if(currentPosition == 0) {
					return true;
				} else {
					viewPager.setCurrentItem(0);
					return false;
				}
			}
		}
		
		private void recreateSpinner() {
			if(frags.isEmpty()) {
				return;
			}
			
			List<SubsonicFragment> fragStack = frags.get(currentPosition);
			if(fragStack.size() > 1) {
				spinnerAdapter.clear();
				for(int i = 0; i < fragStack.size(); i++) {
					SubsonicFragment frag = fragStack.get(i);
					spinnerAdapter.add(frag.getTitle());
				}
				spinnerAdapter.notifyDataSetChanged();
				actionBarSpinner.setSelection(spinnerAdapter.getCount() - 1);
				actionBar.setDisplayShowCustomEnabled(true);
			} else {
				actionBar.setDisplayShowCustomEnabled(false);
			}
		}
		
		public void invalidate() {
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			for (int i = 0; i < frags.size(); i++) {
				List<SubsonicFragment> fragStack = frags.get(i);
				
				for(int j = fragStack.size() - 1; j > 0; j--) {
					SubsonicFragment oldFrag = fragStack.remove(j);
					trans.remove((Fragment)oldFrag);
				}
				
				SubsonicFragment frag = (SubsonicFragment)fragStack.get(0);
				frag.invalidate();
			}
			trans.commit();
		}
		
		public void onSaveInstanceState(Bundle savedInstanceState) {
			for(int i = 0; i < frags.size(); i++) {
				List<SubsonicFragment> fragStack = frags.get(i);
				String[] ids = new String[fragStack.size()];
				
				for(int j = 0; j < fragStack.size(); j++) {
					ids[j] = fragStack.get(j).getTag();
				}
				savedInstanceState.putStringArray(Constants.MAIN_BACK_STACK + i, ids);
				savedInstanceState.putInt(Constants.MAIN_BACK_STACK_SIZE + i, fragStack.size());
			}
			savedInstanceState.putInt(Constants.MAIN_BACK_STACK_TABS, frags.size());
			savedInstanceState.putInt(Constants.MAIN_BACK_STACK_POSITION, currentPosition);
		}
		
		public void onRestoreInstanceState(Bundle savedInstanceState) {
			int tabCount = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_TABS);
			FragmentManager fm = activity.getSupportFragmentManager();
			for(int i = 0; i < tabCount; i++) {
				int stackSize = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_SIZE + i);
				String[] ids = savedInstanceState.getStringArray(Constants.MAIN_BACK_STACK + i);
				List<SubsonicFragment> fragStack = new ArrayList<SubsonicFragment>();
				
				for(int j = 0; j < stackSize; j++) {
					SubsonicFragment frag = (SubsonicFragment)fm.findFragmentByTag(ids[j]);
					fragStack.add(frag);
				}
				
				frags.add(i, fragStack);
			}
			
			if(tabCount > 0) {
				currentPosition = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_POSITION);
				List<SubsonicFragment> fragStack = frags.get(currentPosition);
				currentFragment = fragStack.get(fragStack.size() - 1);
				currentFragment.setPrimaryFragment(true);
				activity.supportInvalidateOptionsMenu();
			}
		}

		private class TabInfo {
			public final Class fragmentClass;
			public final Bundle args;
			public TabInfo(Class fragmentClass, Bundle args) {
				this.fragmentClass = fragmentClass;
				this.args = args;
			}
		}
		private class QueuedFragment {
			public SubsonicFragment fragment;
			public int id;
		}
	}
}
