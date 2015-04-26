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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends RecyclingImageView {
	public SquareImageView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onMeasure(final int widthSpec, final int heightSpec) {
		super.onMeasure(widthSpec, heightSpec);
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
	}
}
