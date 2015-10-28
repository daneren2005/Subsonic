package github.daneren2005.dsub.view;

import android.content.Context;
import android.widget.ImageView;

public abstract class UpdateView2<T1, T2> extends UpdateView<T1> {
	protected T2 item2;

	public UpdateView2(Context context) {
		super(context);
	}

	public UpdateView2(Context context, boolean autoUpdate) {
		super(context, autoUpdate);
	}

	public final void setObject(T1 obj1) {
		setObject(obj1, null);
	}
	@Override
	public void setObject(T1 obj1, Object obj2) {
		if(item == obj1 && item2 == obj2) {
			return;
		}

		item = obj1;
		item2 = (T2) obj2;
		if(imageTask != null) {
			imageTask.cancel();
			imageTask = null;
		}
		if(coverArtView != null && coverArtView instanceof ImageView) {
			((ImageView) coverArtView).setImageDrawable(null);
		}

		setObjectImpl(item, item2);
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

	protected final void setObjectImpl(T1 obj1) {
		setObjectImpl(obj1, null);
	}
	protected abstract void setObjectImpl(T1 obj1, T2 obj2);
}
