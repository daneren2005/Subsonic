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
import android.os.Build;
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
public class ImageLoader {
	private static final String TAG = ImageLoader.class.getSimpleName();

	private Context context;
	private LruCache<String, Bitmap> cache;
	private Bitmap nowPlaying;
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
						oldBitmap.recycle();
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
		new ViewImageTask(view.getContext(), entry, size, imageSizeLarge, large, view, crossfade).execute();
	}

	public void loadImage(Context context, RemoteControlClient remoteControl, MusicDirectory.Entry entry) {
		if (largeUnknownImage != null && ((BitmapDrawable)largeUnknownImage).getBitmap().isRecycled()) {
			createLargeUnknownImage(context);
		}

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
		new RemoteControlClientImageTask(context, entry, imageSizeLarge, imageSizeLarge, false, remoteControl).execute();
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
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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

	private abstract class ImageTask extends SilentBackgroundTask<Void> {
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
				Bitmap bitmap = musicService.getCoverArt(mContext, mEntry, mSize, null);
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
}
