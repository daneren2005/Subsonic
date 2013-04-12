package github.daneren2005.dsub.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import android.support.v4.view.ViewPager;
import android.util.Log;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Util;

public class MainActivity extends SubsonicActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		viewPager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = new TabPagerAdapter(this, viewPager);
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
			pagerAdapter.removeCurrent();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater menuInflater = getSupportMenuInflater();
		pagerAdapter.onCreateOptionsMenu(menu, menuInflater);
		return true;
	}
	@Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		return pagerAdapter.onOptionsItemSelected(item);
	}

	private void showInfoDialog() {
		if (!infoDialogDisplayed) {
			infoDialogDisplayed = true;
			if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
				Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
			}
		}
	}
}
