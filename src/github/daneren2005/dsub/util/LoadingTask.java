package github.daneren2005.dsub.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import github.daneren2005.dsub.activity.SubsonicActivity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class LoadingTask<T> extends BackgroundTask<T> {

    private final Activity tabActivity;
	private ProgressDialog loading;
	private final boolean cancellable;

	public LoadingTask(Activity activity) {
		super(activity);
		tabActivity = activity;
		this.cancellable = true;
	}
    public LoadingTask(Activity activity, final boolean cancellable) {
        super(activity);
        tabActivity = activity;
		this.cancellable = cancellable;
    }

    @Override
    public void execute() {
        loading = ProgressDialog.show(tabActivity, "", "Loading. Please Wait...", true, cancellable, new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cancel();
			}
		});

		queue.offer(task = new Task() {
			@Override
			public void onDone(T result) {
				loading.cancel();
				done(result);
			}

			@Override
			public void onError(Throwable t) {
				loading.cancel();
				error(t);
			}
		});
    }

	@Override
    protected boolean isCancelled() {
        return (tabActivity instanceof SubsonicActivity && ((SubsonicActivity)tabActivity).isDestroyed()) || cancelled;
    }
	
	@Override
    public void updateProgress(final String message) {
		if(!cancelled) {
			getHandler().post(new Runnable() {
				@Override
				public void run() {
						loading.setMessage(message);
				}
			});
		}
    }
}
