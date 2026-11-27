package github.paroj.dsub2000.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import github.paroj.dsub2000.R;

import github.paroj.dsub2000.activity.SubsonicActivity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class LoadingTask<T> extends BackgroundTask<T> {

    private final Activity tabActivity;
	private AlertDialog loading;
	private TextView loadingMessage;
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
		View view = tabActivity.getLayoutInflater().inflate(R.layout.dialog_loading, null);
		loadingMessage = (TextView) view.findViewById(R.id.loading_message);
		loading = new MaterialAlertDialogBuilder(tabActivity)
				.setView(view)
				.setCancelable(cancellable)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						cancel();
					}
				})
				.show();

		queue.offer(task = new Task() {
			@Override
			public void onDone(T result) {
				if(loading.isShowing()) {
					loading.dismiss();
				}
				done(result);
			}

			@Override
			public void onError(Throwable t) {
				if(loading.isShowing()) {
					loading.dismiss();
				}
				error(t);
			}
		});
    }

	@Override
    public boolean isCancelled() {
        return (tabActivity instanceof SubsonicActivity && ((SubsonicActivity) tabActivity).isDestroyedCompat()) || cancelled.get();
    }
	
	@Override
    public void updateProgress(final String message) {
		if(!cancelled.get()) {
			getHandler().post(new Runnable() {
				@Override
				public void run() {
						loadingMessage.setText(message);
				}
			});
		}
    }
}
