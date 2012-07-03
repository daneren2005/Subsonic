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
package net.sourceforge.subsonic.androidapp.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.util.Log;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class CancellableTask {

    private static final String TAG = CancellableTask.class.getSimpleName();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<Thread> thread = new AtomicReference<Thread>();
    private final AtomicReference<OnCancelListener> cancelListener = new AtomicReference<OnCancelListener>();

    public void cancel() {
        Log.d(TAG, "Cancelling " + CancellableTask.this);
        cancelled.set(true);

        OnCancelListener listener = cancelListener.get();
        if (listener != null) {
            try {
                listener.onCancel();
            } catch (Throwable x) {
                Log.w(TAG, "Error when invoking OnCancelListener.", x);
            }
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setOnCancelListener(OnCancelListener listener) {
        cancelListener.set(listener);
    }

    public boolean isRunning() {
        return running.get();
    }

    public abstract void execute();

    public void start() {
        thread.set(new Thread() {
            @Override
            public void run() {
                running.set(true);
                Log.d(TAG, "Starting thread for " + CancellableTask.this);
                try {
                    execute();
                } finally {
                    running.set(false);
                    Log.d(TAG, "Stopping thread for " + CancellableTask.this);
                }
            }
        });
        thread.get().start();
    }

    public static interface OnCancelListener {
        void onCancel();
    }
}
