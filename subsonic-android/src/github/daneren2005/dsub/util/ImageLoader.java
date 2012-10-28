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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.actionbarsherlock.app.ActionBar;

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

    private final LRUCache<String, Drawable> cache = new LRUCache<String, Drawable>(100);
    private final BlockingQueue<Task> queue;
    private final int imageSizeDefault;
    private final int imageSizeLarge;
    private Drawable largeUnknownImage;

    public ImageLoader(Context context) {
        queue = new LinkedBlockingQueue<Task>(500);

        // Determine the density-dependent image sizes.
        imageSizeDefault = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        imageSizeLarge = (int) Math.round(Math.min(metrics.widthPixels, metrics.heightPixels) * 0.6);

        for (int i = 0; i < CONCURRENCY; i++) {
            new Thread(this, "ImageLoader").start();
        }

        createLargeUnknownImage(context);
    }

    private void createLargeUnknownImage(Context context) {
		BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.unknown_album_large);
		Bitmap bitmap = Bitmap.createScaledBitmap(drawable.getBitmap(), imageSizeLarge, imageSizeLarge, true);
		bitmap = createReflection(bitmap);
		largeUnknownImage = Util.createDrawableFromBitmap(context, bitmap);
    }

    public void loadImage(View view, MusicDirectory.Entry entry, boolean large, boolean crossfade) {
        if (entry == null || entry.getCoverArt() == null) {
            setUnknownImage(view, large);
            return;
        }

        int size = large ? imageSizeLarge : imageSizeDefault;
        Drawable drawable = cache.get(getKey(entry.getCoverArt(), size));
        if (drawable != null) {
            setImage(view, drawable, large);
            return;
        }

        if (!large) {
            setUnknownImage(view, large);
        }
        queue.offer(new Task(view.getContext(), entry, size, large, large, new ViewTaskHandler(view, crossfade)));
    }

    public void loadImage(Context context, RemoteControlClient remoteControl, MusicDirectory.Entry entry) {
        if (entry == null || entry.getCoverArt() == null) {
            setUnknownImage(remoteControl);
            return;
        }
        
        Drawable drawable = cache.get(getKey(entry.getCoverArt(), imageSizeLarge));
        if (drawable != null) {
            setImage(remoteControl, drawable);
            return;
        }

        setUnknownImage(remoteControl);
        queue.offer(new Task(context, entry, imageSizeLarge, false, false, new RemoteControlClientTaskHandler(remoteControl)));
    }

    public void loadImage(ActionBar actionBar, MusicDirectory.Entry entry) {
        if (entry == null || entry.getCoverArt() == null) {
            setUnknownImage(actionBar);
            return;
        }
        
        Drawable drawable = cache.get(getKey(entry.getCoverArt(), imageSizeDefault));
        if (drawable != null) {
            setImage(actionBar, drawable);
            return;
        }

        setUnknownImage(actionBar);
        queue.offer(new Task(actionBar.getThemedContext(), entry, imageSizeDefault, false, false, new ActionBarTaskHandler(actionBar)));
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
                    Bitmap emptyImage = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    existingDrawable = new BitmapDrawable(emptyImage);
                }

                Drawable[] layers = new Drawable[]{existingDrawable, drawable};

                TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(250);
            } else {
                imageView.setImageDrawable(drawable);
            }
        }
    }
    
	private void setImage(RemoteControlClient remoteControl, Drawable drawable) {
		if(remoteControl != null) {
			Bitmap origBitmap = ((BitmapDrawable)drawable).getBitmap();
			remoteControl.editMetadata(false)
			.putBitmap(
					RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
					origBitmap.copy(origBitmap.getConfig(), true))
			.apply();
		}
    }
    
	private void setImage(ActionBar actionBar, Drawable drawable) {
    	actionBar.setIcon(drawable);
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
    
    private void setUnknownImage(ActionBar actionBar) {
        setImage(actionBar, largeUnknownImage);
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

    private Bitmap createReflection(Bitmap originalImage) {

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // The gap we want between the reflection and the original image
        final int reflectionGap = 4;

        // This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        // Create a Bitmap with the flip matix applied to it.
        // We only want the bottom half of the image
        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false);

        // Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 2), Bitmap.Config.ARGB_8888);

        // Create a new Canvas with the bitmap that's big enough for
        // the image plus gap plus reflection
        Canvas canvas = new Canvas(bitmapWithReflection);

        // Draw in the original image
        canvas.drawBitmap(originalImage, 0, 0, null);

        // Draw in the gap
        Paint defaultPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);

        // Draw in the reflection
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

        // Create a shader that is a linear gradient that covers the reflection
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0,
                bitmapWithReflection.getHeight() + reflectionGap, 0x70000000, 0xff000000,
                Shader.TileMode.CLAMP);

        // Set the paint to use this shader (linear gradient)
        paint.setShader(shader);

        // Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }
    
	private class Task {
    	private final Context mContext;
        private final MusicDirectory.Entry mEntry;
        private final Handler mHandler;
        private final int mSize;
        private final boolean mReflection;
        private final boolean mSaveToFile;
        private ImageLoaderTaskHandler mTaskHandler;

        public Task(Context context, MusicDirectory.Entry entry, int size, boolean reflection, boolean saveToFile, ImageLoaderTaskHandler taskHandler) {
        	mContext = context;
            mEntry = entry;
            mSize = size;
            mReflection = reflection;
            mSaveToFile = saveToFile;
            mTaskHandler = taskHandler;
            mHandler = new Handler();
        }

        public void execute() {
            try {
                MusicService musicService = MusicServiceFactory.getMusicService(mContext);
                Bitmap bitmap = musicService.getCoverArt(mContext, mEntry, mSize, mSaveToFile, null);

                if (mReflection) {
                    bitmap = createReflection(bitmap);
                }

                final Drawable drawable = Util.createDrawableFromBitmap(mContext, bitmap);
                cache.put(getKey(mEntry.getCoverArt(), mSize), drawable);
                
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
	
	private class ActionBarTaskHandler extends ImageLoaderTaskHandler {
		
		private ActionBar mActionBar;
		
		public ActionBarTaskHandler(ActionBar actionBar) {
			mActionBar = actionBar;
		}
		
		@Override
		public void run() {
			setImage(mActionBar, mDrawable);
		}
	}
}
