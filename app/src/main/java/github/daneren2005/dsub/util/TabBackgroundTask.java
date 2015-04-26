package github.daneren2005.dsub.util;

import github.daneren2005.dsub.fragments.SubsonicFragment;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class TabBackgroundTask<T> extends BackgroundTask<T> {

    private final SubsonicFragment tabFragment;

    public TabBackgroundTask(SubsonicFragment fragment) {
        super(fragment.getActivity());
        tabFragment = fragment;
    }

    @Override
    public void execute() {
        tabFragment.setProgressVisible(true);

		queue.offer(task = new Task() {
			@Override
			public void onDone(T result) {
				tabFragment.setProgressVisible(false);
				done(result);
			}

			@Override
			public void onError(Throwable t) {
				tabFragment.setProgressVisible(false);
				error(t);
			}
		});
    }

	@Override
    public boolean isCancelled() {
        return !tabFragment.isAdded() || cancelled.get();
    }

    @Override
    public void updateProgress(final String message) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                tabFragment.updateProgress(message);
            }
        });
    }
}
