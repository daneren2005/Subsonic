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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import net.sourceforge.subsonic.androidapp.R;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class HorizontalSlider extends ProgressBar {

    private final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slider_knob);
    private boolean slidingEnabled;
    private OnSliderChangeListener listener;
    private static final int PADDING = 2;
    private boolean sliding;
    private int sliderPosition;
    private int startPosition;

    public interface OnSliderChangeListener {
        void onSliderChanged(View view, int position, boolean inProgress);
    }

    public HorizontalSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HorizontalSlider(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.progressBarStyleHorizontal);
    }

    public HorizontalSlider(Context context) {
        super(context);
    }

    public void setSlidingEnabled(boolean slidingEnabled) {
        if (this.slidingEnabled != slidingEnabled) {
            this.slidingEnabled = slidingEnabled;
            invalidate();
        }
    }

    public boolean isSlidingEnabled() {
        return slidingEnabled;
    }

    public void setOnSliderChangeListener(OnSliderChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int max = getMax();
        if (!slidingEnabled || max == 0) {
            return;
        }

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingLeft - paddingRight;
        int h = getHeight() - paddingTop - paddingBottom;
        int position = sliding ? sliderPosition : getProgress();

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getWidth();
        float x = paddingLeft + w * ((float) position / max) - bitmapWidth / 2.0F;
        x = Math.max(x, paddingLeft);
        x = Math.min(x, paddingLeft + w - bitmapWidth);
        float y = paddingTop + h / 2.0F - bitmapHeight / 2.0F;

        canvas.drawBitmap(bitmap, x, y, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!slidingEnabled) {
            return false;
        }

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {

            if (action == MotionEvent.ACTION_DOWN) {
                sliding = true;
                startPosition = getProgress();
            }

            float x = event.getX() - PADDING;
            float width = getWidth() - 2 * PADDING;
            sliderPosition = Math.round((float) getMax() * (x / width));
            sliderPosition = Math.max(sliderPosition, 0);

            setProgress(Math.min(startPosition, sliderPosition));
            setSecondaryProgress(Math.max(startPosition, sliderPosition));
            if (listener != null) {
                listener.onSliderChanged(this, sliderPosition, true);
            }

        } else if (action == MotionEvent.ACTION_UP) {
            sliding = false;
            setProgress(sliderPosition);
            setSecondaryProgress(0);
            if (listener != null) {
                listener.onSliderChanged(this, sliderPosition, false);
            }
        }

        return true;
    }
}