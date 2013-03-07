package github.daneren2005.dsub.util;

import github.daneren2005.dsub.activity.SubsonicTabActivity;
import github.daneren2005.dsub.fragments.SubsonicTabFragment;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class TabBackgroundTask<T> extends BackgroundTask<T> {

    private final SubsonicTabFragment tabFragment;

    public TabBackgroundTask(SubsonicTabFragment fragment) {
        super(fragment.getActivity());
        tabFragment = fragment;
    }

    @Override
    public void execute() {
        tabFragment.setProgressVisible(true);

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
                            tabFragment.setProgressVisible(false);
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
                            tabFragment.setProgressVisible(false);
                            error(t);
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isCancelled() {
        return !tabFragment.isAdded();
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
