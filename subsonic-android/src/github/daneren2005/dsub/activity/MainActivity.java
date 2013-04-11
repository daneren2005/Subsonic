package github.daneren2005.dsub.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.fragments.SubsonicTabFragment;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Util;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends SubsonicActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;
	private MainActivityPagerAdapter pagerAdapter;
	private ViewPager viewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		viewPager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = new MainActivityPagerAdapter(this, viewPager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOnPageChangeListener(pagerAdapter);

		addTab("Home", MainFragment.class, null);
		addTab("Library", SelectArtistFragment.class, null);
		addTab("Playlists", SelectPlaylistFragment.class, null);
	}

	@Override
	protected void onPostCreate(Bundle bundle) {
		super.onPostCreate(bundle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		showInfoDialog();
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onBackPressed() {
		int backStack = getSupportFragmentManager().getBackStackEntryCount();
		int currentTab = viewPager.getCurrentItem();
		
		if(backStack == 0) {
			if(currentTab == 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.common_confirm)
					.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							MainActivity.super.onBackPressed();
						}
					})
					.setNegativeButton(R.string.common_cancel, null);
				AlertDialog dialog = builder.create();
				dialog.show();
			} else {
				viewPager.setCurrentItem(0);
			}
		} else {
			super.onBackPressed();
		}
	}

	protected void addTab(int titleRes, Class fragmentClass, Bundle args) {
		pagerAdapter.addTab(getString(titleRes), fragmentClass, args);
	}
	protected void addTab(CharSequence title, Class fragmentClass, Bundle args) {
		pagerAdapter.addTab(title, fragmentClass, args);
	}

	private void exit() {
		stopService(new Intent(this, DownloadServiceImpl.class));
		finish();
	}

	private void showInfoDialog() {
		if (!infoDialogDisplayed) {
			infoDialogDisplayed = true;
			if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
				Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
			}
		}
	}



	public class MainActivityPagerAdapter extends FragmentPagerAdapter implements TabListener, ViewPager.OnPageChangeListener {
		private SherlockFragmentActivity activity;
		private ViewPager pager;
		private ActionBar actionBar;
		private SubsonicTabFragment currentFragment;
		private List tabs = new ArrayList();
		private List frags = new ArrayList();

		public MainActivityPagerAdapter(SherlockFragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			this.activity = activity;
			this.actionBar = activity.getSupportActionBar();
			this.pager = pager;
		}

		@Override
		public Fragment getItem(int i) {
			final TabInfo tabInfo = (TabInfo)tabs.get(i);
			SherlockFragment frag = (SherlockFragment) Fragment.instantiate(activity, tabInfo.fragmentClass.getName(), tabInfo.args);
			frags.add(i, frag);
			if(currentFragment == null) {
				currentFragment = (SubsonicTabFragment) frag;
				currentFragment.setPrimaryFragment(true);
			}
			return frag;
		}

		@Override
		public int getCount() {
			return tabs.size();
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
			actionBar.setSelectedNavigationItem(position);
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(false);
			}
			currentFragment = (SubsonicTabFragment) frags.get(position);
			if(currentFragment != null) {
				currentFragment.setPrimaryFragment(true);
			}
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
