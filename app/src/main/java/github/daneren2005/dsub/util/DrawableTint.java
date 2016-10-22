/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.util.TypedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import github.daneren2005.dsub.R;

public class DrawableTint {
	private static final Map<Integer, Integer> attrMap = new HashMap<>();
	private static final WeakHashMap<Integer, Drawable> tintedDrawables = new WeakHashMap<>();

	public static Drawable getTintedDrawable(Context context, @DrawableRes int drawableRes) {
		return getTintedDrawable(context, drawableRes, R.attr.colorAccent);
	}
	public static Drawable getTintedDrawable(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttr) {
		if(tintedDrawables.containsKey(drawableRes)) {
			return tintedDrawables.get(drawableRes);
		}

		int color = getColorRes(context, colorAttr);
		Drawable background = context.getResources().getDrawable(drawableRes);
		background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		tintedDrawables.put(drawableRes, background);
		return background;
	}
	public static Drawable getTintedDrawableFromColor(Context context, @DrawableRes int drawableRes, @ColorRes int colorRes) {
		if(tintedDrawables.containsKey(drawableRes)) {
			return tintedDrawables.get(drawableRes);
		}

		int color = context.getResources().getColor(colorRes);
		Drawable background = context.getResources().getDrawable(drawableRes);
		background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		tintedDrawables.put(drawableRes, background);
		return background;
	}
	public static int getColorRes(Context context, @AttrRes int colorAttr) {
		int color;
		if(attrMap.containsKey(colorAttr)) {
			color = attrMap.get(colorAttr);
		} else {
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = context.getTheme();
			theme.resolveAttribute(colorAttr, typedValue, true);
			color = typedValue.data;
			attrMap.put(colorAttr, color);
		}

		return color;
	}
	public static int getDrawableRes(Context context, @AttrRes int drawableAttr) {
		if(attrMap.containsKey(drawableAttr)) {
			return attrMap.get(drawableAttr);
		} else {
			int[] attrs = new int[]{drawableAttr};
			TypedArray typedArray = context.obtainStyledAttributes(attrs);
			@DrawableRes int drawableRes = typedArray.getResourceId(0, 0);
			typedArray.recycle();
			attrMap.put(drawableAttr, drawableRes);
			return drawableRes;
		}
	}
	public static Drawable getTintedAttrDrawable(Context context, @AttrRes int drawableAttr, @AttrRes int colorAttr) {
		if(tintedDrawables.containsKey(drawableAttr)) {
			return getTintedDrawable(context, attrMap.get(drawableAttr), colorAttr);
		}

		@DrawableRes int drawableRes = getDrawableRes(context, drawableAttr);
		return getTintedDrawable(context, drawableRes, colorAttr);
	}

	public static void clearCache() {
		attrMap.clear();
		tintedDrawables.clear();
	}
}
