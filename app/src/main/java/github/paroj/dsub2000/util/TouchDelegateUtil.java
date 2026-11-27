package github.paroj.dsub2000.util;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;
import github.paroj.dsub2000.R;

/**
 * Utility class to easily expand the touch area of any view without changing its visual layout.
 */
public class TouchDelegateUtil {

    /**
     * Expands the touch area of a view on both sides (left/right).
     * Reads the expansion value from dimens.xml using View.TouchExpand.
     * Can be called before or after setting an OnClickListener.
     */
    public static void expandTouchArea(final View view) {
        final View parent = (View) view.getParent();
        if (parent == null) return;

        float extraDp = view.getResources().getDimension(R.dimen.View_TouchExpand);
        final int extraPx = (int) extraDp;

        parent.post(() -> {
            Rect rect = new Rect();
            view.getHitRect(rect);

            rect.left -= extraPx;
            rect.right += extraPx;

            parent.setTouchDelegate(new TouchDelegate(rect, view));
        });
    }
}