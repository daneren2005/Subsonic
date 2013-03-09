package github.daneren2005.dsub.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.updates.Updater;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.Util;
import java.io.File;
import java.io.PrintWriter;

public class SubsonicActivity extends SherlockFragmentActivity {
	private static final String TAG = SubsonicActivity.class.getSimpleName();
	private static ImageLoader IMAGE_LOADER;
	protected static String theme;
	private boolean destroyed = false;

	@Override
	protected void onCreate(Bundle bundle) {
		setUncaughtExceptionHandler();
		applyTheme();
		super.onCreate(bundle);
		startService(new Intent(this, DownloadServiceImpl.class));
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
}
