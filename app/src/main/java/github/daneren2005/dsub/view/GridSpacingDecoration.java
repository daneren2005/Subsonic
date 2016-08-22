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

package github.daneren2005.dsub.view;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import static android.widget.LinearLayout.*;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {
	private static final String TAG = GridSpacingDecoration.class.getSimpleName();
	public static final int SPACING = 10;

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);

		int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SPACING, view.getResources().getDisplayMetrics());
		int halfSpacing = spacing / 2;

		int childCount = parent.getChildCount();
		int childIndex = parent.getChildPosition(view);
		// Not an actual child (ie: during delete event)
		if(childIndex == -1) {
			return;
		}
		int spanCount = getTotalSpan(view, parent);
		int spanIndex = childIndex % spanCount;

		// If we can, use the SpanSizeLookup since headers screw up the index calculation
		RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
		if(layoutManager instanceof GridLayoutManager) {
			GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
			GridLayoutManager.SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();
			if(spanSizeLookup != null) {
				spanIndex = spanSizeLookup.getSpanIndex(childIndex, spanCount);
			}
		}
		int spanSize = getSpanSize(parent, childIndex);

        /* INVALID SPAN */
		if (spanCount < 1 || spanSize > 1) return;

		int margins = 0;
		if(view instanceof UpdateView) {
			View firstChild = ((ViewGroup) view).getChildAt(0);
			ViewGroup.LayoutParams layoutParams = firstChild.getLayoutParams();
			if (layoutParams instanceof LinearLayout.LayoutParams) {
				margins = ((LinearLayout.LayoutParams) layoutParams).bottomMargin;
			} else if (layoutParams instanceof FrameLayout.LayoutParams) {
				margins = ((FrameLayout.LayoutParams) layoutParams).bottomMargin;
			}
		}
		int doubleMargins = margins * 2;

		outRect.top = halfSpacing - margins;
		outRect.bottom = halfSpacing - margins;
		outRect.left = halfSpacing - margins;
		outRect.right = halfSpacing - margins;

		if (isTopEdge(childIndex, spanIndex, spanCount)) {
			outRect.top = spacing - doubleMargins;
		}

		if (isLeftEdge(spanIndex, spanCount)) {
			outRect.left = spacing - doubleMargins;
		}

		if (isRightEdge(spanIndex, spanCount)) {
			outRect.right = spacing - doubleMargins;
		}

		if (isBottomEdge(childIndex, childCount, spanCount)) {
			outRect.bottom = spacing - doubleMargins;
		}
	}

	protected int getTotalSpan(View view, RecyclerView parent) {
		RecyclerView.LayoutManager mgr = parent.getLayoutManager();
		if (mgr instanceof GridLayoutManager) {
			return ((GridLayoutManager) mgr).getSpanCount();
		}

		return -1;
	}
	protected int getSpanSize(RecyclerView parent, int childIndex) {
		RecyclerView.LayoutManager mgr = parent.getLayoutManager();
		if (mgr instanceof GridLayoutManager) {
			GridLayoutManager.SpanSizeLookup lookup = ((GridLayoutManager) mgr).getSpanSizeLookup();
			if(lookup != null) {
				return lookup.getSpanSize(childIndex);
			}
		}

		return 1;
	}

	protected boolean isLeftEdge(int spanIndex, int spanCount) {
		return spanIndex == 0;
	}

	protected boolean isRightEdge(int spanIndex, int spanCount) {
		return spanIndex == spanCount - 1;
	}

	protected boolean isTopEdge(int childIndex, int spanIndex, int spanCount) {
		return childIndex < spanCount && childIndex == spanIndex;
	}

	protected boolean isBottomEdge(int childIndex, int childCount, int spanCount) {
		return childIndex >= childCount - spanCount;
	}
}
