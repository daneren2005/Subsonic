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
import android.util.AttributeSet;
import android.widget.ViewFlipper;

/**
 * Work-around for Android Issue 6191 (http://code.google.com/p/android/issues/detail?id=6191)
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class MyViewFlipper extends ViewFlipper {

    public MyViewFlipper(Context context) {
        super(context);
    }

    public MyViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        }
        catch (IllegalArgumentException e) {
            // Call stopFlipping() in order to kick off updateRunning()
            stopFlipping();
        }
    }
}

