package github.daneren2005.dsub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.fragments.SubsonicTabFragment;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

public class MusicActivity extends SubsonicActivity {
	private static final String TAG = MusicActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;
	private MainActivityPagerAdapter pagerAdapter;
	private ViewPager viewPager;
	private SubsonicTabFragment prevPageFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
			exit();
		}
		setContentView(R.layout.music);

		pagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager());
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				notifyFragmentOnPageSelected(position);
			}
		});

		if (savedInstanceState == null) {
			int position = Util.isOffline(this) ? 0 : 2;
			if (position == viewPager.getCurrentItem()) {
				notifyFragmentOnPageSelected(position);
			} else {
				viewPager.setCurrentItem(position);
			}
		}

		handleIntentAction(getIntent());
	}

	@Override
	protected void onPostCreate(Bundle bundle) {
		super.onPostCreate(bundle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		showInfoDialog();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntentAction(intent);
		// mPagerAdapter.notifyDataSetChanged();
	}

	private void handleIntentAction(Intent intent) {
		if (intent.hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
			exit();
		}
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

	private void notifyFragmentOnPageSelected(int position) {
		/*if (prevPageFragment != null) {
			prevPageFragment.onPageDeselected();
		}
		prevPageFragment = (SubsonicTabFragment) pagerAdapter.instantiateItem(viewPager, position);
		prevPageFragment.onPageSelected();*/
	}

	public class MainActivityPagerAdapter extends FragmentPagerAdapter {

		private final String [] titles = new String [] {
				"Home", 
				"Library",
				"Playlists"
		};

		public MainActivityPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment;
			Bundle args = new Bundle();
			switch(i) {
				case 0:
					fragment = new MainFragment();
					break;
				case 1:
					fragment = new SelectArtistFragment();
					break;
				case 2:
					fragment = new SelectPlaylistFragment();
					break;
				default:
					fragment = null;
			}

			if (fragment != null) {
				fragment.setArguments(args);
			}

			return fragment;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}
	}
}
