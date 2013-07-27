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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class UpdateView extends LinearLayout {
	private static final String TAG = UpdateView.class.getSimpleName();
	private static final WeakHashMap<UpdateView, ?> INSTANCES = new WeakHashMap<UpdateView, Object>();
	
	private static Handler backgroundHandler;
	private static Handler uiHandler;
	private static Runnable updateRunnable;
	
	public UpdateView(Context context) {
		super(context);
		
		setLayoutParams(new AbsListView.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT));
		
		INSTANCES.put(this, null);
        int instanceCount = INSTANCES.size();
        if (instanceCount > 50) {
            Log.w(TAG, instanceCount + " live UpdateView instances");
        }
		
		startUpdater();
	}
	
	@Override
	public void setPressed(boolean pressed) {
		
	}
	
	private static synchronized void startUpdater() {
		if(uiHandler != null) {
			return;
		}
		
		uiHandler = new Handler();
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
		}).start();
    }

    private static void updateAll() {
        try {
			List<UpdateView> views = new ArrayList<UpdateView>();;
            for (UpdateView view : INSTANCES.keySet()) {
                if (view.isShown()) {
					views.add(view);
                }
            }
			updateAllLive(views);
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
	
	protected void updateBackground() {
		
	}
	protected void update() {
		
	}
}
