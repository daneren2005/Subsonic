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
package github.daneren2005.dsub.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.v4.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.domain.InternetRadioStation;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.domain.PodcastChannel;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.compat.RemoteControlClientBase;

/**
 * Asynchronous loading of images, with caching.
 * <p/>
 * There should normally be only one instance of this class.
 *
 * @author Sindre Mehus
 */
public class ImageLoader {
	private static final String TAG = ImageLoader.class.getSimpleName();
	public static final String PLAYLIST_PREFIX = "pl-";
	public static final String PODCAST_PREFIX = "pc-";

	private Context context;
	private LruCache<String, Bitmap> cache;
	private Handler handler;
	private Bitmap nowPlaying;
	private Bitmap nowPlayingSmall;
	private final int imageSizeDefault;
	private final int imageSizeLarge;
	private final int avatarSizeDefault;
	private boolean clearingCache = false;
	private final int cacheSize;

	private final static int[] COLORS = {0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444};

	public ImageLoader(Context context) {
		this.context = context;
		handler = new Handler(Looper.getMainLooper());
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		cacheSize = maxMemory / 4;

		// Determine the density-dependent image sizes.
		imageSizeDefault = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		imageSizeLarge = Math.round(Math.min(metrics.widthPixels, metrics.heightPixels));
		avatarSizeDefault = context.getResources().getDrawable(R.drawable.ic_social_person).getIntrinsicHeight();

		cache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
			}

			@Override
			protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
				if(evicted) {
					if((oldBitmap != nowPlaying && oldBitmap != nowPlayingSmall) || clearingCache) {
						oldBitmap.recycle();
					} else if(oldBitmap != newBitmap) {
						cache.put(key, oldBitmap);
					}
				}
			}
		};
	}

	public void clearCache() {
		nowPlaying = null;
		nowPlayingSmall = null;
		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				clearingCache = true;
				cache.evictAll();
				clearingCache = false;
				return null;
			}
		}.execute();
	}
	public void onLowMemory(float percent) {
		Log.i(TAG, "Cache size: " + cache.size() + " => " + Math.round(cacheSize * (1 - percent)) + " out of " + cache.maxSize());
		cache.resize(Math.round(cacheSize * (1 - percent)));
	}
	public void onUIVisible() {
		if(cache.maxSize() != cacheSize) {
			Log.i(TAG, "Returned to full cache size");
			cache.resize(cacheSize);
		}
	}

	public void setNowPlayingSmall(Bitmap bitmap) {
		nowPlayingSmall = bitmap;
	}

	private Bitmap getUnknownImage(MusicDirectory.Entry entry, int size) {
		String key;
		int color;
		if(entry == null) {
			key = getKey("unknown", size);
			color = COLORS[0];

			return getUnknownImage(key, size, color, null, null);
		} else {
			key = getKey(entry.getId() + "unknown", size);
			String hash;
			if(entry.getAlbum() != null) {
				hash = entry.getAlbum();
			} else if(entry.getArtist() != null) {
				hash = entry.getArtist();
			} else {
				hash = entry.getId();
			}
			color = COLORS[Math.abs(hash.hashCode()) % COLORS.length];

			return getUnknownImage(key, size, color, entry.getAlbum(), entry.getArtist());
		}
	}
	private Bitmap getUnknownImage(String key, int size, int color, String topText, String bottomText) {
		Bitmap bitmap = cache.get(key);
		if(bitmap == null) {
			bitmap = createUnknownImage(size, color, topText, bottomText);
			cache.put(key, bitmap);
		}

		return bitmap;
	}
	private Bitmap createUnknownImage(int size, int primaryColor, String topText, String bottomText) {
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Paint color = new Paint();
		color.setColor(primaryColor);
		canvas.drawRect(0, 0, size, size * 2.0f / 3.0f, color);

		color.setShader(new LinearGradient(0, 0, 0, size / 3.0f, Color.rgb(82, 82, 82), Color.BLACK, Shader.TileMode.MIRROR));
		canvas.drawRect(0, size * 2.0f / 3.0f, size, size, color);

		if(topText != null || bottomText != null) {
			Paint font = new Paint();
			font.setFlags(Paint.ANTI_ALIAS_FLAG);
			font.setColor(Color.WHITE);
			font.setTextSize(3.0f + size * 0.07f);

			if(topText != null) {
				canvas.drawText(topText, size * 0.05f, size * 0.6f, font);
			}

			if(bottomText != null) {
				canvas.drawText(bottomText, size * 0.05f, size * 0.8f, font);
			}
		}

		return bitmap;
	}

	public Bitmap getCachedImage(Context context, MusicDirectory.Entry entry, boolean large) {
		int size = large ? imageSizeLarge : imageSizeDefault;
		if(entry == null || entry.getCoverArt() == null) {
			return getUnknownImage(entry, size);
		}

		Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), size));
		if(bitmap == null || bitmap.isRecycled()) {
			bitmap = FileUtil.getAlbumArtBitmap(context, entry, size);
			String key = getKey(entry.getCoverArt(), size);
			cache.put(key, bitmap);
			cache.get(key);
		}

		if(bitmap != null && bitmap.isRecycled()) {
			bitmap = null;
		}
		return bitmap;
	}

	public SilentBackgroundTask loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade) {
		int size = large ? imageSizeLarge : imageSizeDefault;
		return loadImage(view, entry, large, size, crossfade);
	}
	public SilentBackgroundTask loadImage(View view, MusicDirectory.Entry entry, boolean large, int size, boolean crossfade) {
		if(entry != null && entry instanceof InternetRadioStation) {
			// Continue on and load a null bitmap
		}
		// If we know this a artist, try to load artist info instead
		else if(entry != null && !entry.isAlbum() && ServerInfo.checkServerVersion(context, "1.11")  && !Util.isOffline(context)) {
			SilentBackgroundTask task = new ArtistImageTask(view.getContext(), entry, size, imageSizeLarge, large, view, crossfade);
			task.execute();
			return task;
		} else if(entry != null && entry.getCoverArt() == null && entry.isDirectory() && !Util.isOffline(context)) {
			// Try to lookup child cover art
			MusicDirectory.Entry firstChild = FileUtil.lookupChild(context, entry, true);
			if(firstChild != null) {
				entry.setCoverArt(firstChild.getCoverArt());
			}
		}

		Bitmap bitmap;
		if (entry == null || entry.getCoverArt() == null) {
			bitmap = getUnknownImage(entry, size);
			setImage(view, Util.createDrawableFromBitmap(context, bitmap), crossfade);
			return null;
		}

		bitmap = cache.get(getKey(entry.getCoverArt(), size));
		if (bitmap != null && !bitmap.isRecycled()) {
			final Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(view, drawable, crossfade);
			if(large) {
				nowPlaying = bitmap;
			}
			return null;
		}

		if (!large) {
			setImage(view, null, false);
		}
		ImageTask task = new ViewImageTask(view.getContext(), entry, size, imageSizeLarge, large, view, crossfade);
		task.execute();
		return task;
	}

	public SilentBackgroundTask<Void> loadImage(View view, String url, boolean large) {
		Bitmap bitmap;
		int size = large ? imageSizeLarge : imageSizeDefault;
		if (url == null) {
			String key = getKey(url + "unknown", size);
			int color = COLORS[Math.abs(key.hashCode()) % COLORS.length];
			bitmap = getUnknownImage(key, size, color, null, null);
			setImage(view, Util.createDrawableFromBitmap(context, bitmap), true);
			return null;
		}

		bitmap = cache.get(getKey(url, size));
		if (bitmap != null && !bitmap.isRecycled()) {
			final Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(view, drawable, true);
			return null;
		}
		setImage(view, null, false);

		SilentBackgroundTask<Void> task = new ViewUrlTask(view.getContext(), view, url, size);
		task.execute();
		return task;
	}

	public SilentBackgroundTask<Void> loadImage(Context context, RemoteControlClientBase remoteControl, MusicDirectory.Entry entry) {
		Bitmap bitmap;
		if (entry == null || entry.getCoverArt() == null) {
			bitmap = getUnknownImage(entry, imageSizeLarge);
			setImage(entry, remoteControl, Util.createDrawableFromBitmap(context, bitmap));
			return null;
		}

		bitmap = cache.get(getKey(entry.getCoverArt(), imageSizeLarge));
		if (bitmap != null && !bitmap.isRecycled()) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(entry, remoteControl, drawable);
			return null;
		}

		setImage(entry, remoteControl, Util.createDrawableFromBitmap(context, null));
		ImageTask task = new RemoteControlClientImageTask(context, entry, imageSizeLarge, imageSizeLarge, false, remoteControl);
		task.execute();
		return task;
	}

	public SilentBackgroundTask<Void> loadAvatar(Context context, ImageView view, String username) {
		if(username == null) {
			view.setImageResource(R.drawable.ic_social_person);
			return null;
		}

		Bitmap bitmap = cache.get(username);
		if (bitmap != null && !bitmap.isRecycled()) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			view.setImageDrawable(drawable);
			return null;
		}
		view.setImageDrawable(null);

		SilentBackgroundTask<Void> task = new AvatarTask(context, view, username);
		task.execute();
		return task;
	}

	public SilentBackgroundTask loadImage(View view, Playlist playlist, boolean large, boolean crossfade) {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		String id;
		if(Util.isOffline(context)) {
			id = PLAYLIST_PREFIX + playlist.getName();
			entry.setTitle(playlist.getComment());
		} else {
			id = PLAYLIST_PREFIX + playlist.getId();
			entry.setTitle(playlist.getName());
		}
		entry.setId(id);
		entry.setCoverArt(id);
		// So this isn't treated as a artist
		entry.setParent("");

		return loadImage(view, entry, large, crossfade);
	}

	public SilentBackgroundTask loadImage(View view, PodcastChannel channel, boolean large, boolean crossfade) {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		entry.setId(PODCAST_PREFIX + channel.getId());
		entry.setTitle(channel.getName());
		entry.setCoverArt(channel.getCoverArt());
		// So this isn't treated as a artist
		entry.setParent("");

		return loadImage(view, entry, large, crossfade);
	}

	private String getKey(String coverArtId, int size) {
		return coverArtId + size;
	}

	private void setImage(View view, final Drawable drawable, boolean crossfade) {
		if (view instanceof TextView) {
			// Cross-fading is not implemented for TextView since it's not in use.  It would be easy to add it, though.
			TextView textView = (TextView) view;
			textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
		} else if (view instanceof ImageView) {
			final ImageView imageView = (ImageView) view;
			if (crossfade && drawable != null) {
				Drawable existingDrawable = imageView.getDrawable();
				if (existingDrawable == null) {
					Bitmap emptyImage;
					if(drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
						emptyImage = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
					} else {
						emptyImage = Bitmap.createBitmap(imageSizeDefault, imageSizeDefault, Bitmap.Config.ARGB_8888);
					}
					existingDrawable = new BitmapDrawable(context.getResources(), emptyImage);
				} else if(existingDrawable instanceof TransitionDrawable) {
					// This should only ever be used if user is skipping through many songs quickly
					TransitionDrawable tmp = (TransitionDrawable) existingDrawable;
					existingDrawable = tmp.getDrawable(tmp.getNumberOfLayers() - 1);
				}
				if(existingDrawable != null && drawable != null) {
					Drawable[] layers = new Drawable[]{existingDrawable, drawable};
					final TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
					imageView.setImageDrawable(transitionDrawable);
					transitionDrawable.startTransition(250);

					// Get rid of transition drawable after transition occurs
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							// Only execute if still on same transition drawable
							if (imageView.getDrawable() == transitionDrawable) {
								imageView.setImageDrawable(drawable);
							}
						}
					}, 500L);
				} else {
					imageView.setImageDrawable(drawable);
				}
			} else {
				imageView.setImageDrawable(drawable);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setImage(MusicDirectory.Entry entry, RemoteControlClientBase remoteControl, Drawable drawable) {
		if(remoteControl != null && drawable != null) {
			Bitmap origBitmap = ((BitmapDrawable)drawable).getBitmap();
			if ( origBitmap != null && !origBitmap.isRecycled()) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && origBitmap != null) {
					origBitmap = origBitmap.copy(origBitmap.getConfig(), false);
				}

				remoteControl.updateAlbumArt(entry, origBitmap);
			} else  {
				if(origBitmap != null) {
					Log.e(TAG, "Tried to load a recycled bitmap.");
				}

				remoteControl.updateAlbumArt(entry, null);
			}
		}
	}

	public abstract class ImageTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		protected final MusicDirectory.Entry mEntry;
		private final int mSize;
		private final int mSaveSize;
		private final boolean mIsNowPlaying;
		protected Drawable mDrawable;

		public ImageTask(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying) {
			super(context);
			mContext = context;
			mEntry = entry;
			mSize = size;
			mSaveSize = saveSize;
			mIsNowPlaying = isNowPlaying;
		}

		@Override
		protected Void doInBackground() throws Throwable {
			try {
				MusicService musicService = MusicServiceFactory.getMusicService(mContext);
				Bitmap bitmap = musicService.getCoverArt(mContext, mEntry, mSize, null, this);
				if(bitmap != null) {
					String key = getKey(mEntry.getCoverArt(), mSize);
					cache.put(key, bitmap);
					// Make sure key is the most recently "used"
					cache.get(key);
					if (mIsNowPlaying) {
						nowPlaying = bitmap;
					}
				} else {
					bitmap = getUnknownImage(mEntry, mSize);
				}

				mDrawable = Util.createDrawableFromBitmap(mContext, bitmap);
			} catch (Throwable x) {
				Log.e(TAG, "Failed to download album art.", x);
				cancelled.set(true);
			}

			return null;
		}
	}

	private class ViewImageTask extends ImageTask {
		protected boolean mCrossfade;
		private View mView;

		public ViewImageTask(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying, View view, boolean crossfade) {
			super(context, entry, size, saveSize, isNowPlaying);

			mView = view;
			mCrossfade = crossfade;
		}

		@Override
		protected void done(Void result) {
			setImage(mView, mDrawable, mCrossfade);
		}
	}

	private class RemoteControlClientImageTask extends ImageTask {
		private RemoteControlClientBase mRemoteControl;

		public RemoteControlClientImageTask(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying, RemoteControlClientBase remoteControl) {
			super(context, entry, size, saveSize, isNowPlaying);

			mRemoteControl = remoteControl;
		}

		@Override
		protected void done(Void result) {
			setImage(mEntry, mRemoteControl, mDrawable);
		}
	}

	private class ArtistImageTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		private final MusicDirectory.Entry mEntry;
		private final int mSize;
		private final int mSaveSize;
		private final boolean mIsNowPlaying;
		private Drawable mDrawable;
		private boolean mCrossfade;
		private View mView;

		private SilentBackgroundTask subTask;

		public ArtistImageTask(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying, View view, boolean crossfade) {
			super(context);
			mContext = context;
			mEntry = entry;
			mSize = size;
			mSaveSize = saveSize;
			mIsNowPlaying = isNowPlaying;
			mView = view;
			mCrossfade = crossfade;
		}

		@Override
		protected Void doInBackground() throws Throwable {
			try {
				MusicService musicService = MusicServiceFactory.getMusicService(mContext);
				ArtistInfo artistInfo = musicService.getArtistInfo(mEntry.getId(), false, true, mContext, null);
				String url = artistInfo.getImageUrl();

				// Figure out whether we are going to get a artist image or the standard image
				if (url != null && !"".equals(url.trim())) {
					// If getting the artist image fails for any reason, retry for the standard version
					subTask = new ViewUrlTask(mContext, mView, url, mSize) {
						@Override
						protected void failedToDownload() {
							// Call loadImage so we can take advantage of all of it's logic checks
							loadImage(mView, mEntry, mSize == imageSizeLarge, mCrossfade);

							// Delete subTask so it doesn't get called in done
							subTask = null;
						}
					};
				} else {
					if (mEntry != null && mEntry.getCoverArt() == null && mEntry.isDirectory() && !Util.isOffline(context)) {
						// Try to lookup child cover art
						MusicDirectory.Entry firstChild = FileUtil.lookupChild(context, mEntry, true);
						if (firstChild != null) {
							mEntry.setCoverArt(firstChild.getCoverArt());
						}
					}

					if (mEntry != null && mEntry.getCoverArt() != null) {
						subTask = new ViewImageTask(mContext, mEntry, mSize, mSaveSize, mIsNowPlaying, mView, mCrossfade);
					} else {
						// If entry is null as well, we need to just set as a blank image
						Bitmap bitmap = getUnknownImage(mEntry, mSize);
						mDrawable = Util.createDrawableFromBitmap(mContext, bitmap);
						return null;
					}
				}

				// Execute whichever way we decided to go
				subTask.doInBackground();
			} catch (Throwable x) {
				Log.e(TAG, "Failed to get artist info", x);
				cancelled.set(true);
			}
			return null;
		}

		@Override
		public void done(Void result) {
			if(subTask != null) {
				subTask.done(result);
			} else if(mDrawable != null) {
				setImage(mView, mDrawable, mCrossfade);
			}
		}
	}

	private class ViewUrlTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		private final String mUrl;
		private final ImageView mView;
		private Drawable mDrawable;
		private int mSize;

		public ViewUrlTask(Context context, View view, String url, int size) {
			super(context);
			mContext = context;
			mView = (ImageView) view;
			mUrl = url;
			mSize = size;
		}

		@Override
		protected Void doInBackground() throws Throwable {
			try {
				MusicService musicService = MusicServiceFactory.getMusicService(mContext);
				Bitmap bitmap = musicService.getBitmap(mUrl, mSize, mContext, null, this);
				if(bitmap != null) {
					String key = getKey(mUrl, mSize);
					cache.put(key, bitmap);
					// Make sure key is the most recently "used"
					cache.get(key);

					mDrawable = Util.createDrawableFromBitmap(mContext, bitmap);
				}
			} catch (Throwable x) {
				Log.e(TAG, "Failed to download from url " + mUrl, x);
				cancelled.set(true);
			}

			return null;
		}

		@Override
		protected void done(Void result) {
			if(mDrawable != null) {
				mView.setImageDrawable(mDrawable);
			} else {
				failedToDownload();
			}
		}

		protected void failedToDownload() {

		}
	}

	private class AvatarTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		private final String mUsername;
		private final ImageView mView;
		private Drawable mDrawable;

		private AvatarTask(Context context, ImageView view, String username) {
			super(context);
			mContext = context;
			mView = view;
			mUsername = username;
		}

		@Override
		protected Void doInBackground() throws Throwable {
			try {
				MusicService musicService = MusicServiceFactory.getMusicService(mContext);
				Bitmap bitmap = musicService.getAvatar(mUsername, avatarSizeDefault, mContext, null, this);
				if(bitmap != null) {
					cache.put(mUsername, bitmap);
					// Make sure key is the most recently "used"
					cache.get(mUsername);

					mDrawable = Util.createDrawableFromBitmap(mContext, bitmap);
				}
			} catch (java.io.FileNotFoundException  x) {
				Log.i(TAG, "Avatar not available for download.");
			} catch (Throwable x) {
				Log.e(TAG, "Failed to download avatar.", x);
			}

			return null;
		}

		@Override
		protected void done(Void result) {
			if(mDrawable != null) {
				mView.setImageDrawable(mDrawable);
			} else {
				mView.setImageResource(R.drawable.ic_social_person);
			}
		}
	}
}
