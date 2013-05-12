package github.daneren2005.dsub.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.updates.Updater;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SubsonicActivity extends SherlockFragmentActivity implements OnItemSelectedListener {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	private boolean destroyed = false;
	protected TabPagerAdapter pagerAdapter;
	protected ViewPager viewPager;
	protected List<SubsonicFragment> backStack = new ArrayList<SubsonicFragment>();
	protected List<Integer> backStackId = new ArrayList<Integer>();
	protected SubsonicFragment currentFragment;
	protected int currentFragmentId;
	Spinner actionBarSpinner;
	ArrayAdapter<CharSequence> spinnerAdapter;

	@Override
	protected void onCreate(Bundle bundle) {
		setUncaughtExceptionHandler();
		applyTheme();
		super.onCreate(bundle);
		startService(new Intent(this, DownloadServiceImpl.class));
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	@Override
	protected void onPostCreate(Bundle bundle) {
		super.onPostCreate(bundle);
		
		View actionbar = getLayoutInflater().inflate(R.layout.actionbar_spinner, null);
		actionBarSpinner = (Spinner)actionbar.findViewById(R.id.spinner);
		spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBarSpinner.setOnItemSelectedListener(this);
		actionBarSpinner.setAdapter(spinnerAdapter);

		getSupportActionBar().setCustomView(actionbar);
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if(viewPager == null) {
			super.onSaveInstanceState(savedInstanceState);
			int[] ids = new int[backStackId.size() + 1];
			ids[0] = currentFragmentId;
			int i = 1;
			for(Integer id: backStackId) {
				ids[i] = id;
				i++;
			}
			savedInstanceState.putIntArray(Constants.MAIN_BACK_STACK, ids);
			savedInstanceState.putInt(Constants.MAIN_BACK_STACK_SIZE, backStackId.size() + 1);
		}
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if(viewPager == null) {
			super.onRestoreInstanceState(savedInstanceState);
			
			int size = savedInstanceState.getInt(Constants.MAIN_BACK_STACK_SIZE);
			int[] ids = savedInstanceState.getIntArray(Constants.MAIN_BACK_STACK);
			FragmentManager fm = getSupportFragmentManager();
			currentFragment = (SubsonicFragment)fm.findFragmentById(ids[0]);
			currentFragmentId = ids[0];
			currentFragment.setPrimaryFragment(true);
			invalidateOptionsMenu();
			for(int i = 1; i < size; i++) {
				backStack.add((SubsonicFragment)fm.findFragmentById(ids[i]));
				backStackId.add(ids[i]);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater menuInflater = getSupportMenuInflater();
		if(pagerAdapter != null) {
			pagerAdapter.onCreateOptionsMenu(menu, menuInflater);
		} else if(currentFragment != null) {
			currentFragment.onCreateOptionsMenu(menu, menuInflater);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
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
		boolean isJukebox = getDownloadService() != null && getDownloadService().isJukeboxEnabled();

		if (isVolumeAdjust && isJukebox) {
			getDownloadService().adjustJukeboxVolume(isVolumeUp);
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
	
	public void replaceFragment(SubsonicFragment fragment, int id) {
		if(pagerAdapter != null) {
			pagerAdapter.replaceCurrent(fragment, id);
		} else {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			backStack.add(currentFragment);
			backStackId.add(currentFragmentId);
			
			currentFragment = fragment;
			currentFragment.setPrimaryFragment(true);
			currentFragmentId = id;
			invalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(id, fragment);
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
		currentFragmentId = backStackId.remove(backStackId.size() - 1);
		currentFragment.setPrimaryFragment(true);
		invalidateOptionsMenu();

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
		} else if ("light".equals(theme)) {
			setTheme(R.style.Theme_DSub_Light);
		} else if ("dark_fullscreen".equals(theme)) {
			setTheme(R.style.Theme_DSub_Dark_Fullscreen);
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
	
	public void checkUpdates() {
		try {
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			int ver = Integer.parseInt(version.replace(".", ""));
			Updater updater = new Updater(ver);
			updater.checkUpdates(SubsonicActivity.this);
		}
		catch(Exception e) {

		}
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
	
	public class TabPagerAdapter extends FragmentPagerAdapter implements TabListener, ViewPager.OnPageChangeListener {
		private SherlockFragmentActivity activity;
		private ViewPager pager;
		private ActionBar actionBar;
		private SubsonicFragment currentFragment;
		private List tabs = new ArrayList();
		private List frags = new ArrayList();
		private int currentPosition;

		public TabPagerAdapter(SherlockFragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			this.activity = activity;
			this.actionBar = activity.getSupportActionBar();
			this.pager = pager;
			this.currentPosition = 0;
		}

		@Override
		public Fragment getItem(int i) {
			final TabInfo tabInfo = (TabInfo)tabs.get(i);
			SherlockFragment frag = (SherlockFragment) Fragment.instantiate(activity, tabInfo.fragmentClass.getName(), tabInfo.args);
			List fragStack = new ArrayList();
			fragStack.add(frag);
			frags.add(i, fragStack);
			if(currentFragment == null) {
				currentFragment = (SubsonicFragment) frag;
				currentFragment.setPrimaryFragment(true);
			}
			return frag;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}
		
		public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater menuInflater) {
			if(currentFragment != null) {
				currentFragment.onCreateOptionsMenu(menu, menuInflater);
			}
		}
		public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
			if(currentFragment != null) {
				return currentFragment.onOptionsItemSelected(item);
			} else {
				return false;
			}
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			TabInfo tabInfo = (TabInfo) tab.getTag();
			for (int i = 0; i < tabs.size(); i++) {
				if ( tabs.get(i) == tabInfo ) {
					pager.setCurrentItem(i);
					break;
				}
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {}

		public void onPageScrollStateChanged(int arg0) {}

		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		public void onPageSelected(int position) {
			currentPosition = position;
			actionBar.setSelectedNavigationItem(position);
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			List fragStack = (List)frags.get(position);
			currentFragment = (SubsonicFragment) fragStack.get(fragStack.size() - 1);
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(true);
			}
			activity.invalidateOptionsMenu();
			recreateSpinner();
		}

		public void addTab(CharSequence title, Class fragmentClass, Bundle args) {
			final TabInfo tabInfo = new TabInfo(fragmentClass, args);

			Tab tab = actionBar.newTab();
			tab.setText(title);
			tab.setTabListener(this);
			tab.setTag(tabInfo);

			tabs.add(tabInfo);

			actionBar.addTab(tab);
			notifyDataSetChanged();
		}
		
		public void replaceCurrent(SubsonicFragment fragment, int id) {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			List fragStack = (List)frags.get(currentPosition);
			fragStack.add(fragment);
			
			currentFragment = fragment;
			currentFragment.setPrimaryFragment(true);
			activity.invalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.add(id, fragment);
			trans.commit();
			recreateSpinner();
		}
		
		public void removeCurrent() {
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			List fragStack = (List)frags.get(currentPosition);
			Fragment oldFrag = (Fragment)fragStack.remove(fragStack.size() - 1);
			
			currentFragment = (SubsonicFragment) fragStack.get(fragStack.size() - 1);
			currentFragment.setPrimaryFragment(true);
			activity.invalidateOptionsMenu();
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.remove(oldFrag);
			trans.commit();
		}
		
		public boolean onBackPressed() {
			List fragStack = (List)frags.get(currentPosition);
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
			List fragStack = (List)frags.get(currentPosition);
			if(fragStack.size() > 1) {
				spinnerAdapter.clear();
				for(int i = 0; i < fragStack.size(); i++) {
					SubsonicFragment frag = (SubsonicFragment)fragStack.get(i);
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
			for (int i = 0; i < frags.size(); i++) {
				List fragStack = (List)frags.get(i);
				SubsonicFragment frag = (SubsonicFragment)fragStack.get(fragStack.size() - 1);
				frag.invalidate();
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
	}
}
