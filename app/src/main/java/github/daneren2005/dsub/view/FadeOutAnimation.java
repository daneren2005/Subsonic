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

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Fades a view out by changing its alpha value.
 *
 * @author Sindre Mehus
 * @version $Id: Util.java 3203 2012-10-04 09:12:08Z sindre_mehus $
 */
public class FadeOutAnimation extends AlphaAnimation {

    private boolean cancelled;

    /**
     * Creates and starts the fade out animation.
     *
     * @param view           The view to fade out (or display).
     * @param fadeOut        If true, the view is faded out. Otherwise it is immediately made visible.
     * @param durationMillis Fade duration.
     */
    public static void createAndStart(View view, boolean fadeOut, long durationMillis) {
        if (fadeOut) {
            view.clearAnimation();
            view.startAnimation(new FadeOutAnimation(view, durationMillis));
        } else {
            Animation animation = view.getAnimation();
            if (animation instanceof FadeOutAnimation) {
                ((FadeOutAnimation) animation).cancelFadeOut();
            }
            view.clearAnimation();
            view.setVisibility(View.VISIBLE);
        }
    }

    FadeOutAnimation(final View view, long durationMillis) {
        super(1.0F, 0.0F);
        setDuration(durationMillis);
        setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                if (!cancelled) {
                    view.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void cancelFadeOut() {
        cancelled = true;
    }
}
