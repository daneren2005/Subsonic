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
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.v4.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

/**
 * Asynchronous loading of images, with caching.
 * <p/>
 * There should normally be only one instance of this class.
 *
 * @author Sindre Mehus
 */
public class ImageLoader {
	private static final String TAG = ImageLoader.class.getSimpleName();

	private Context context;
	private LruCache<String, Bitmap> cache;
	private Handler handler;
	private Bitmap nowPlaying;
	private final int imageSizeDefault;
	private final int imageSizeLarge;
	private final int avatarSizeDefault;

	private final static int[] COLORS = {0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444};

	public ImageLoader(Context context) {
		this.context = context;
		handler = new Handler(Looper.getMainLooper());
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 4;
		cache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
			}

			@Override
			protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
				if(evicted) {
					if(oldBitmap != nowPlaying) {
						if(sizeOf("", oldBitmap) > 500) {
							oldBitmap.recycle();
						}
					} else {
						cache.put(key, oldBitmap);
					}
				}
			}
		};

		// Determine the density-dependent image sizes.
		imageSizeDefault = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		imageSizeLarge = Math.round(Math.min(metrics.widthPixels, metrics.heightPixels));
		avatarSizeDefault = context.getResources().getDrawable(R.drawable.ic_social_person).getIntrinsicHeight();
	}

	public void clearCache() {
		nowPlaying = null;
		cache.evictAll();
	}

	private Bitmap getUnknownImage(MusicDirectory.Entry entry, int size) {
		String key;
		int color;
		if(entry == null) {
			key = getKey("unknown", size);
			color = COLORS[0];
		} else {
			key = getKey(entry.getId() + "unknown", size);
			color = COLORS[Math.abs(entry.getAlbum().hashCode()) % COLORS.length];
		}
		Bitmap bitmap = cache.get(key);
		if(bitmap == null) {
			bitmap = createUnknownImage(entry, size, color);
			cache.put(key, bitmap);
		}

		return bitmap;
	}
	private Bitmap createUnknownImage(MusicDirectory.Entry entry, int size, int primaryColor) {
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Paint color = new Paint();
		color.setColor(primaryColor);
		canvas.drawRect(0, 0, size, size * 2.0f / 3.0f, color);

		color.setShader(new LinearGradient(0, 0, 0, size / 3.0f, Color.rgb(82, 82, 82), Color.BLACK, Shader.TileMode.MIRROR));
		canvas.drawRect(0, size * 2.0f / 3.0f, size, size, color);

		if(entry != null) {
			Paint font = new Paint();
			font.setFlags(Paint.ANTI_ALIAS_FLAG);
			font.setColor(Color.WHITE);
			font.setTextSize(3.0f + size * 0.07f);

			if(entry.getAlbum() != null) {
				canvas.drawText(entry.getAlbum(), size * 0.05f, size * 0.6f, font);
			}

			if(entry.getArtist() != null) {
				canvas.drawText(entry.getArtist(), size * 0.05f, size * 0.8f, font);
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

		return bitmap;
	}

	public ImageTask loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade) {
		if(entry != null && entry.getCoverArt() == null && entry.isDirectory() && !Util.isOffline(context)) {
			// Try to lookup child cover art
			MusicDirectory.Entry firstChild = FileUtil.lookupChild(context, entry, true);
			if(firstChild != null) {
				entry.setCoverArt(firstChild.getCoverArt());
			}
		}

		Bitmap bitmap;
		int size = large ? imageSizeLarge : imageSizeDefault;
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
			setImage(view, Util.createDrawableFromBitmap(context, null), false);
		}
		ImageTask task = new ViewImageTask(view.getContext(), entry, size, imageSizeLarge, large, view, crossfade);
		task.execute();
		return task;
	}

	public SilentBackgroundTask<Void> loadImage(Context context, RemoteControlClient remoteControl, MusicDirectory.Entry entry) {
		Bitmap bitmap;
		if (entry == null || entry.getCoverArt() == null) {
			bitmap = getUnknownImage(entry, imageSizeLarge);
			setImage(remoteControl, Util.createDrawableFromBitmap(context, bitmap));
			return null;
		}

		bitmap = cache.get(getKey(entry.getCoverArt(), imageSizeLarge));
		if (bitmap != null && !bitmap.isRecycled()) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(remoteControl, drawable);
			return null;
		}

		setImage(remoteControl, Util.createDrawableFromBitmap(context, null));
		ImageTask task = new RemoteControlClientImageTask(context, entry, imageSizeLarge, imageSizeLarge, false, remoteControl);
		task.execute();
		return task;
	}

	public SilentBackgroundTask<Void> loadAvatar(Context context, ImageView view, String username) {
		Bitmap bitmap = cache.get(username);
		if (bitmap != null && !bitmap.isRecycled()) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			view.setImageDrawable(drawable);
			return null;
		}

		SilentBackgroundTask<Void> task = new AvatarTask(context, view, username);
		task.execute();
		return task;
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
			if (crossfade) {
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
				
				Drawable[] layers = new Drawable[] {existingDrawable, drawable};
				final TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
				imageView.setImageDrawable(transitionDrawable);
				transitionDrawable.startTransition(250);
				
				// Get rid of transition drawable after transition occurs
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// Only execute if still on same transition drawable
						if(imageView.getDrawable() == transitionDrawable) {
							imageView.setImageDrawable(drawable);
						}
					}
				}, 500L);
			} else {
				imageView.setImageDrawable(drawable);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setImage(RemoteControlClient remoteControl, Drawable drawable) {
		if(remoteControl != null && drawable != null) {
			Bitmap origBitmap = ((BitmapDrawable)drawable).getBitmap();
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && origBitmap != null) {
				origBitmap = origBitmap.copy(origBitmap.getConfig(), false);
			}
			if ( origBitmap != null && !origBitmap.isRecycled()) {
				remoteControl.editMetadata(false).putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, origBitmap).apply();
			} else  {
				Log.e(TAG, "Tried to load a recycled bitmap.");
				remoteControl.editMetadata(false)
					.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null)
					.apply();
			}
		}
	}

	public abstract class ImageTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		private final MusicDirectory.Entry mEntry;
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
				String key = getKey(mEntry.getCoverArt(), mSize);
				cache.put(key, bitmap);
				// Make sure key is the most recently "used"
				cache.get(key);
				if(mIsNowPlaying) {
					nowPlaying = bitmap;
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
		private RemoteControlClient mRemoteControl;

		public RemoteControlClientImageTask(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying, RemoteControlClient remoteControl) {
			super(context, entry, size, saveSize, isNowPlaying);

			mRemoteControl = remoteControl;
		}

		@Override
		protected void done(Void result) {
			setImage(mRemoteControl, mDrawable);
		}
	}

	private class AvatarTask extends SilentBackgroundTask<Void> {
		private final Context mContext;
		private final String mUsername;
		private final ImageView mView;
		private Drawable mDrawable;

		public AvatarTask(Context context, ImageView view, String username) {
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
			} catch (Throwable x) {
				Log.e(TAG, "Failed to download album art.", x);
				cancelled.set(true);
			}

			return null;
		}

		@Override
		protected void done(Void result) {
			if(mDrawable != null) {
				mView.setImageDrawable(mDrawable);
			}
		}
	}
}
