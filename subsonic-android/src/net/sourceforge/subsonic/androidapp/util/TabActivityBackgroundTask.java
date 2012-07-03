package net.sourceforge.subsonic.androidapp.util;

import net.sourceforge.subsonic.androidapp.activity.SubsonicTabActivity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class TabActivityBackgroundTask<T> extends BackgroundTask<T> {

    private final SubsonicTabActivity tabActivity;

    public TabActivityBackgroundTask(SubsonicTabActivity activity) {
        super(activity);
        tabActivity = activity;
    }

    @Override
    public void execute() {
        tabActivity.setProgressVisible(true);

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
                            tabActivity.setProgressVisible(false);
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
                            tabActivity.setProgressVisible(false);
                            error(t);
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isCancelled() {
        return tabActivity.isDestroyed();
    }

    @Override
    public void updateProgress(final String message) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                tabActivity.updateProgress(message);
            }
        });
    }
}
