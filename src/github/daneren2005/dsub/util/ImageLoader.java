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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.RemoteControlClient;
import android.os.Handler;
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronous loading of images, with caching.
 * <p/>
 * There should normally be only one instance of this class.
 *
 * @author Sindre Mehus
 */
@TargetApi(14)
public class ImageLoader implements Runnable {
	private static final String TAG = ImageLoader.class.getSimpleName();
	private static final int CONCURRENCY = 5;

	private Handler mHandler = new Handler();
	private Context context;
	private LruCache<String, Bitmap> cache;
	private Bitmap nowPlaying;
	private final BlockingQueue<Task> queue;
	private final int imageSizeDefault;
	private final int imageSizeLarge;
	private Drawable largeUnknownImage;

	public ImageLoader(Context context) {
		this.context = context;
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
						// oldBitmap.recycle();
					} else {
						cache.put(key, oldBitmap);
					}
				}
			}
		};

		queue = new LinkedBlockingQueue<Task>(500);

		// Determine the density-dependent image sizes.
		imageSizeDefault = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		imageSizeLarge = (int) Math.round(Math.min(metrics.widthPixels, metrics.heightPixels));

		for (int i = 0; i < CONCURRENCY; i++) {
			new Thread(this, "ImageLoader").start();
		}

		createLargeUnknownImage(context);
	}

	private void createLargeUnknownImage(Context context) {
		BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.unknown_album_large);
		Bitmap bitmap = Bitmap.createScaledBitmap(drawable.getBitmap(), imageSizeLarge, imageSizeLarge, true);
		largeUnknownImage = Util.createDrawableFromBitmap(context, bitmap);
	}

	public void loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade) {
		if (largeUnknownImage != null && ((BitmapDrawable)largeUnknownImage).getBitmap().isRecycled()) {
			createLargeUnknownImage(view.getContext());
		}

		if (entry == null || entry.getCoverArt() == null) {
			setUnknownImage(view, large);
			return;
		}

		int size = large ? imageSizeLarge : imageSizeDefault;
		Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), size));
		if (bitmap != null && !bitmap.isRecycled()) {
			final Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(view, drawable, large);
			if(large) {
				nowPlaying = bitmap;
			}
			return;
		}

		if (!large) {
			setUnknownImage(view, large);
		}
		queue.offer(new Task(view.getContext(), entry, size, imageSizeLarge, large, new ViewTaskHandler(view, crossfade)));
	}

	public void loadImage(Context context, RemoteControlClient remoteControl, MusicDirectory.Entry entry) {
		if (largeUnknownImage != null && ((BitmapDrawable)largeUnknownImage).getBitmap().isRecycled())
		createLargeUnknownImage(context);

		if (entry == null || entry.getCoverArt() == null) {
			setUnknownImage(remoteControl);
			return;
		}

		Bitmap bitmap = cache.get(getKey(entry.getCoverArt(), imageSizeLarge));
		if (bitmap != null && !bitmap.isRecycled()) {
			Drawable drawable = Util.createDrawableFromBitmap(this.context, bitmap);
			setImage(remoteControl, drawable);
			return;
		}

		setUnknownImage(remoteControl);
		queue.offer(new Task(context, entry, imageSizeLarge, imageSizeLarge, false, new RemoteControlClientTaskHandler(remoteControl)));
	}

	private String getKey(String coverArtId, int size) {
		return coverArtId + size;
	}

	private void setImage(View view, Drawable drawable, boolean crossfade) {
		if (view instanceof TextView) {
			// Cross-fading is not implemented for TextView since it's not in use.  It would be easy to add it, though.
			TextView textView = (TextView) view;
			textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
		} else if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
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
				} else {
					// Try to get rid of old transitions
					try {
						TransitionDrawable tmp = (TransitionDrawable) existingDrawable;
						int layers = tmp.getNumberOfLayers();
						existingDrawable = tmp.getDrawable(layers - 1);
					} catch(Exception e) {
						// Do nothing, just means that the drawable is a flat image
					}
				}
				if (!(((BitmapDrawable)existingDrawable).getBitmap().isRecycled()))
				{ // We will flow through to the non-transition if the old image is recycled... Yay 4.3
					Drawable[] layers = new Drawable[]{existingDrawable, drawable};

					TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
					imageView.setImageDrawable(transitionDrawable);
					transitionDrawable.startTransition(250);
					return;
				}
			}
			imageView.setImageDrawable(drawable);
			return;
		}
	}

	private void setImage(RemoteControlClient remoteControl, Drawable drawable) {
		if(remoteControl != null && drawable != null) {
			Bitmap origBitmap = ((BitmapDrawable)drawable).getBitmap();
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

	private void setUnknownImage(View view, boolean large) {
		if (large) {
			setImage(view, largeUnknownImage, false);
		} else {
			if (view instanceof TextView) {
				((TextView) view).setCompoundDrawablesWithIntrinsicBounds(R.drawable.unknown_album, 0, 0, 0);
			} else if (view instanceof ImageView) {
				((ImageView) view).setImageResource(R.drawable.unknown_album);
			}
		}
	}

	private void setUnknownImage(RemoteControlClient remoteControl) {
		setImage(remoteControl, largeUnknownImage);
	}

	public void clear() {
		queue.clear();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Task task = queue.take();
				task.execute();
			} catch (Throwable x) {
				Log.e(TAG, "Unexpected exception in ImageLoader.", x);
			}
		}
	}

	private class Task {
		private final Context mContext;
		private final MusicDirectory.Entry mEntry;
		private final int mSize;
		private final int mSaveSize;
		private final boolean mIsNowPlaying;
		private ImageLoaderTaskHandler mTaskHandler;

		public Task(Context context, MusicDirectory.Entry entry, int size, int saveSize, boolean isNowPlaying, ImageLoaderTaskHandler taskHandler) {
			mContext = context;
			mEntry = entry;
			mSize = size;
			mSaveSize = saveSize;
			mIsNowPlaying = isNowPlaying;
			mTaskHandler = taskHandler;
		}

		public void execute() {
			try {
				loadImage();
			} catch(OutOfMemoryError e) {
				Log.w(TAG, "Ran out of memory trying to load image, try cleanup and retry");
				cache.evictAll();
				System.gc();
			}
		}
		public void loadImage() {
			try {
				MusicService musicService = MusicServiceFactory.getMusicService(mContext);
				Bitmap bitmap = musicService.getCoverArt(mContext, mEntry, mSize, mSaveSize, null);
				String key = getKey(mEntry.getCoverArt(), mSize);
				cache.put(key, bitmap);
				// Make sure key is the most recently "used"
				cache.get(key);
				if(mIsNowPlaying) {
					nowPlaying = bitmap;
				}

				final Drawable drawable = Util.createDrawableFromBitmap(mContext, bitmap);
				mTaskHandler.setDrawable(drawable);
				mHandler.post(mTaskHandler);
			} catch (Throwable x) {
				Log.e(TAG, "Failed to download album art.", x);
			}
		}
	}

	private abstract class ImageLoaderTaskHandler implements Runnable {

		protected Drawable mDrawable;

		public void setDrawable(Drawable drawable) {
			mDrawable = drawable;
		}

	}

	private class ViewTaskHandler extends ImageLoaderTaskHandler {

		protected boolean mCrossfade;
		private View mView;

		public ViewTaskHandler(View view, boolean crossfade) {
			mCrossfade = crossfade;
			mView = view;
		}

		@Override
		public void run() {
			setImage(mView, mDrawable, mCrossfade);
		}
	}

	private class RemoteControlClientTaskHandler extends ImageLoaderTaskHandler {

		private RemoteControlClient mRemoteControl;

		public RemoteControlClientTaskHandler(RemoteControlClient remoteControl) {
			mRemoteControl = remoteControl;
		}

		@Override
		public void run() {
			setImage(mRemoteControl, mDrawable);
		}
	}
}
