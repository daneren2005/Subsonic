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
import android.util.TypedValue;
import android.view.View;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);

		int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, view.getResources().getDisplayMetrics());
		int halfSpacing = spacing / 2;

		int childCount = parent.getChildCount();
		int childIndex = parent.getChildPosition(view);
		int spanCount = getTotalSpan(view, parent);
		int spanIndex = childIndex % spanCount;
		int spanSize = getSpanSize(parent, childIndex);

        /* INVALID SPAN */
		if (spanCount < 1 || spanSize > 1) return;

		outRect.top = halfSpacing;
		outRect.bottom = halfSpacing;
		outRect.left = halfSpacing;
		outRect.right = halfSpacing;

		if (isTopEdge(childIndex, spanCount)) {
			outRect.top = spacing;
		}

		if (isLeftEdge(spanIndex, spanCount)) {
			outRect.left = spacing;
		}

		if (isRightEdge(spanIndex, spanCount)) {
			outRect.right = spacing;
		}

		if (isBottomEdge(childIndex, childCount, spanCount)) {
			outRect.bottom = spacing;
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

	protected boolean isTopEdge(int childIndex, int spanCount) {
		return childIndex < spanCount;
	}

	protected boolean isBottomEdge(int childIndex, int childCount, int spanCount) {
		return childIndex >= childCount - spanCount;
	}
}
