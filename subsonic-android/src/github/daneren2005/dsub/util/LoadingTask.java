package github.daneren2005.dsub.util;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import github.daneren2005.dsub.activity.SubsonicTabActivity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class LoadingTask<T> extends BackgroundTask<T> {

    private final SubsonicTabActivity tabActivity;
	private final boolean cancellable;
	private boolean cancelled = false;

    public LoadingTask(SubsonicTabActivity activity, final boolean cancellable) {
        super(activity);
        tabActivity = activity;
		this.cancellable = cancellable;
    }

    @Override
    public void execute() {
        final ProgressDialog loading = ProgressDialog.show(tabActivity, "", "Loading. Please Wait...", true, cancellable, new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cancelled = true;
			}
			
		});

        new Thread() {
            @Override
            public void run() {
                try {
                    final T result = doInBackground();
                    if (isCancelled()) {
                        return;
                    }

                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            loading.cancel();
                            done(result);
                        }
                    });
                } catch (final Throwable t) {
					if (isCancelled()) {
                        return;
                    }
					
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            loading.cancel();
                            error(t);
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isCancelled() {
        return tabActivity.isDestroyed() || cancelled;
    }
	
	@Override
    public void updateProgress(final String message) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                
            }
        });
    }
}
