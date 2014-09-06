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
package github.daneren2005.dsub.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.SilentBackgroundTask;

public class UpdateView extends LinearLayout {
	private static final String TAG = UpdateView.class.getSimpleName();
	private static final WeakHashMap<UpdateView, ?> INSTANCES = new WeakHashMap<UpdateView, Object>();

	private static Handler backgroundHandler;
	private static Handler uiHandler;
	private static Runnable updateRunnable;
	private static int activeActivities = 0;

	protected Context context;
	protected ImageButton starButton;
	protected ImageView moreButton;
	
	protected boolean exists = false;
	protected boolean pinned = false;
	protected boolean shaded = false;
	protected boolean starred = false;
	protected boolean isStarred = false;
	protected int isRated = 0;
	protected int rating = 0;
	protected SilentBackgroundTask<Void> imageTask = null;
	
	protected final boolean autoUpdate;
	
	public UpdateView(Context context) {
		this(context, true);
	}
	public UpdateView(Context context, boolean autoUpdate) {
		super(context);
		this.context = context;
		this.autoUpdate = autoUpdate;
		
		setLayoutParams(new AbsListView.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT));
		
		if(autoUpdate) {
			INSTANCES.put(this, null);
		}
		startUpdater();
	}
	
	@Override
	public void setPressed(boolean pressed) {
		
	}
	
	public void setObject(Object obj) {
		setObjectImpl(obj);
		updateBackground();
		update();
	}
	public void setObject(Object obj1, Object obj2) {
		if(imageTask != null) {
			imageTask.cancel();
			imageTask = null;
		}
		
		setObjectImpl(obj1, obj2);
		backgroundHandler.post(new Runnable() {
			@Override
			public void run() {
				updateBackground();
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		});
	}
	protected void setObjectImpl(Object obj) {
		
	}
	protected void setObjectImpl(Object obj1, Object obj2) {

	}
	
	private static synchronized void startUpdater() {
		if(uiHandler != null) {
			return;
		}
		
		uiHandler = new Handler();
		// Needed so handler is never null until thread creates it
		backgroundHandler = uiHandler;
		updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateAll();
            }
        };
		
		new Thread(new Runnable() {
			public void run() {
				Looper.prepare();
				backgroundHandler = new Handler(Looper.myLooper());
				uiHandler.post(updateRunnable);
				Looper.loop();
			}
		}, "UpdateView").start();
    }

	public static synchronized void triggerUpdate() {
		if(backgroundHandler != null) {
			uiHandler.removeCallbacksAndMessages(null);
			backgroundHandler.removeCallbacksAndMessages(null);
			uiHandler.post(updateRunnable);
		}
	}

    private static void updateAll() {
        try {
			// If nothing can see this, stop updating
			if(activeActivities == 0) {
				activeActivities--;
				return;
			}

			List<UpdateView> views = new ArrayList<UpdateView>();
            for (UpdateView view : INSTANCES.keySet()) {
                if (view.isShown()) {
					views.add(view);
                }
            }
			if(views.size() > 0) {
				updateAllLive(views);
			} else {
				uiHandler.postDelayed(updateRunnable, 2000L);
			}
        } catch (Throwable x) {
            Log.w(TAG, "Error when updating song views.", x);
        }
    }
	private static void updateAllLive(final List<UpdateView> views) {
		final Runnable runnable = new Runnable() {
			@Override
            public void run() {
				try {
					for(UpdateView view: views) {
						view.update();
					}
				} catch (Throwable x) {
					Log.w(TAG, "Error when updating song views.", x);
				}
				uiHandler.postDelayed(updateRunnable, 1000L);
			}
		};
		
		backgroundHandler.post(new Runnable() {
			@Override
            public void run() {
				try {
					for(UpdateView view: views) {
						view.updateBackground();
					}
					uiHandler.post(runnable);
				} catch (Throwable x) {
					Log.w(TAG, "Error when updating song views.", x);
				}
			}
		});
	}

	public static void addActiveActivity() {
		activeActivities++;

		if(activeActivities == 0 && uiHandler != null && updateRunnable != null) {
			activeActivities++;
			uiHandler.post(updateRunnable);
		}
	}
	public static void removeActiveActivity() {
		activeActivities--;
	}

	public static MusicDirectory.Entry findEntry(MusicDirectory.Entry entry) {
		for(UpdateView view: INSTANCES.keySet()) {
			if(view instanceof SongView) {
				MusicDirectory.Entry check = ((SongView) view).getEntry();
				if(check != null && entry != check && check.getId().equals(entry.getId())) {
					return check;
				}
			}
		}

		return null;
	}
	
	protected void updateBackground() {
		
	}
	protected void update() {
		if(moreButton != null) {
			if(exists || pinned) {
				if(!shaded) {
					moreButton.setImageResource(exists ? R.drawable.download_cached : R.drawable.download_pinned);
					shaded = true;
				}
			} else {
				if(shaded) {
					int[] attrs = new int[] {R.attr.download_none};
					TypedArray typedArray = context.obtainStyledAttributes(attrs);
					moreButton.setImageResource(typedArray.getResourceId(0, 0));
					shaded = false;
				}
			}
		}
		
		if(starButton != null) {
			if(isStarred) {
				if(!starred) {
					starButton.setVisibility(View.VISIBLE);
					starred = true;
				}
			} else {
				if(starred) {
					starButton.setVisibility(View.GONE);
					starred = false;
				}
			}
		}

		if(isRated != rating) {
			// Color the entire row based on rating
			if(isRated < 3 && isRated > 0) {
				this.setBackgroundColor(Color.RED);
				// Use darker colors the lower the rating goes
				this.getBackground().setAlpha(10 * (3 - isRated));
			} else {
				this.setBackgroundColor(0x00000000);
			}

			rating = isRated;
		}
	}
}
