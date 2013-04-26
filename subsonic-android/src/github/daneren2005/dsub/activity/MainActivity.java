package github.daneren2005.dsub.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.fragments.MainFragment;
import github.daneren2005.dsub.fragments.SelectArtistFragment;
import github.daneren2005.dsub.fragments.SelectPlaylistFragment;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Util;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends SubsonicActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static boolean infoDialogDisplayed;
	private ScheduledExecutorService executorService;
	private View coverArtView;
	private TextView trackView;
	private TextView artistView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		View bottomBar = findViewById(R.id.bottom_bar);
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

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onPause() {
		super.onPause();
		executorService.shutdown();
	}

	@Override
	public void onBackPressed() {
		if(pagerAdapter.onBackPressed()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle(R.string.menu_exit)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						MainActivity.super.onBackPressed();
					}
				})
				.setNegativeButton(R.string.common_cancel, null);
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(pagerAdapter != null) {
			com.actionbarsherlock.view.MenuInflater menuInflater = getSupportMenuInflater();
			pagerAdapter.onCreateOptionsMenu(menu, menuInflater);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		return pagerAdapter.onOptionsItemSelected(item);
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
