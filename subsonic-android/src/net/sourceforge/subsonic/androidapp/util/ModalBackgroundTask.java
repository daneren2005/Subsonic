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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import net.sourceforge.subsonic.androidapp.R;

/**
 * @author Sindre Mehus
 */
public abstract class ModalBackgroundTask<T> extends BackgroundTask<T> {

    private static final String TAG = ModalBackgroundTask.class.getSimpleName();

    private final AlertDialog progressDialog;
    private Thread thread;
    private final boolean finishActivityOnCancel;
    private boolean cancelled;

    public ModalBackgroundTask(Activity activity, boolean finishActivityOnCancel) {
        super(activity);
        this.finishActivityOnCancel = finishActivityOnCancel;
        progressDialog = createProgressDialog();
    }

    public ModalBackgroundTask(Activity activity) {
        this(activity, true);
    }

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.background_task_wait);
        builder.setMessage(R.string.background_task_loading);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                cancel();
            }
        });
        builder.setPositiveButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancel();
            }
        });

        return builder.create();
    }

    public void execute() {
        cancelled = false;
        progressDialog.show();

        thread = new Thread() {
            @Override
            public void run() {
                try {
                    final T result = doInBackground();
                    if (cancelled) {
                        progressDialog.dismiss();
                        return;
                    }

                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            done(result);
                        }
                    });

                } catch (final Throwable t) {
                    if (cancelled) {
                        return;
                    }
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            error(t);
                        }
                    });
                }
            }
        };
        thread.start();
    }

    protected void cancel() {
        cancelled = true;
        if (thread != null) {
            thread.interrupt();
        }

        if (finishActivityOnCancel) {
            getActivity().finish();
        }
    }

    protected boolean isCancelled() {
        return cancelled;
    }

    protected void error(Throwable error) {
        Log.w(TAG, "Got exception: " + error, error);
        new ErrorDialog(getActivity(), getErrorMessage(error), finishActivityOnCancel);
    }

    @Override
    public void updateProgress(final String message) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }
}
