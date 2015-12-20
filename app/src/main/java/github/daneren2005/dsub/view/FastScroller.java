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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import github.daneren2005.dsub.R;

import static android.support.v7.widget.RecyclerView.OnScrollListener;

public class FastScroller extends LinearLayout {
	private static final String TAG = FastScroller.class.getSimpleName();
	private static final int BUBBLE_ANIMATION_DURATION = 100;
	private static final int TRACK_SNAP_RANGE = 5;

	private TextView bubble;
	private View handle;
	private RecyclerView recyclerView;
	private final ScrollListener scrollListener = new ScrollListener();
	private int height;
	private int visibleRange = -1;
	private RecyclerView.Adapter adapter;
	private AdapterDataObserver adapterObserver;
	private boolean visibleBubble = true;
	private boolean hasScrolled = false;

	private ObjectAnimator currentAnimator = null;

	public FastScroller(final Context context,final AttributeSet attrs,final int defStyleAttr) {
		super(context,attrs,defStyleAttr);
		initialise(context);
	}

	public FastScroller(final Context context) {
		super(context);
		initialise(context);
	}

	public FastScroller(final Context context,final AttributeSet attrs) {
		super(context, attrs);
		initialise(context);
	}

	private void initialise(Context context) {
		setOrientation(HORIZONTAL);
		setClipChildren(false);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.fast_scroller,this,true);
		bubble = (TextView)findViewById(R.id.fastscroller_bubble);
		handle = findViewById(R.id.fastscroller_handle);
		bubble.setVisibility(INVISIBLE);
		setVisibility(GONE);
	}

	@Override
	protected void onSizeChanged(int w,int h,int oldw,int oldh) {
		super.onSizeChanged(w,h,oldw,oldh);
		height = h;
		visibleRange = -1;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		final int action = event.getAction();
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				if(event.getX() < (handle.getX() - 30)) {
					return false;
				}

				if(currentAnimator != null)
					currentAnimator.cancel();
				if(bubble.getVisibility() == INVISIBLE) {
					if(visibleBubble) {
						showBubble();
					}
				} else if(!visibleBubble) {
					hideBubble();
				}
				handle.setSelected(true);
			case MotionEvent.ACTION_MOVE:
				setRecyclerViewPosition(event.getY());
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				handle.setSelected(false);
				hideBubble();
				return true;
		}
		return super.onTouchEvent(event);
	}

	public void attachRecyclerView(RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
		recyclerView.addOnScrollListener(scrollListener);
		registerAdapter();
		visibleRange = -1;
	}
	public void detachRecyclerView() {
		recyclerView.removeOnScrollListener(scrollListener);
		recyclerView.setVerticalScrollBarEnabled(true);
		unregisterAdapter();
		recyclerView = null;
		setVisibility(View.GONE);
	}
	public boolean isAttached() {
		return recyclerView != null;
	}

	private void setRecyclerViewPosition(float y) {
		if(recyclerView != null) {
			if(recyclerView.getChildCount() == 0) {
				return;
			}

			int itemCount = recyclerView.getAdapter().getItemCount();
			float proportion = getValueInRange(0, 1f, y / (float) height);

			float targetPosFloat = getValueInRange(0, itemCount - 1, proportion * (float)itemCount);
			int targetPos = (int) targetPosFloat;

			// Immediately make sure that the target is visible
			LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			// layoutManager.scrollToPositionWithOffset(targetPos, 0);
			View firstVisibleView = recyclerView.getChildAt(0);

			// Calculate how far through this position we are
			int columns = Math.round(recyclerView.getWidth() / firstVisibleView.getWidth());
			int firstVisiblePosition = recyclerView.getChildPosition(firstVisibleView);
			int remainder = (targetPos - firstVisiblePosition) % columns;
			float offsetPercentage = (targetPosFloat - targetPos + remainder) / columns;
			if(offsetPercentage < 0) {
				offsetPercentage = 1 + offsetPercentage;
			}
			int firstVisibleHeight = firstVisibleView.getHeight();
			if(columns > 1) {
				firstVisibleHeight += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GridSpacingDecoration.SPACING, firstVisibleView.getResources().getDisplayMetrics());
			}
			int offset = (int) (offsetPercentage * firstVisibleHeight);

			layoutManager.scrollToPositionWithOffset(targetPos, -offset);
			onUpdateScroll(1, 1);

			try {
				String bubbleText = null;
				RecyclerView.Adapter adapter = recyclerView.getAdapter();
				if(adapter instanceof BubbleTextGetter) {
					bubbleText = ((BubbleTextGetter) adapter).getTextToShowInBubble(targetPos);
				}

				if(bubbleText == null) {
					visibleBubble = false;
					bubble.setVisibility(View.INVISIBLE);
				} else {
					bubble.setText(bubbleText);
					bubble.setVisibility(View.VISIBLE);
					visibleBubble = true;
				}
			} catch(Exception e) {
				Log.e(TAG, "Error getting text for bubble", e);
			}
		}
	}

	private float getValueInRange(float min, float max, float value) {
		float minimum = Math.max(min, value);
		return Math.min(minimum,max);
	}

	private void setBubbleAndHandlePosition(float y) {
		int bubbleHeight = bubble.getHeight();
		int handleHeight = handle.getHeight();
		handle.setY(getValueInRange(0,height-handleHeight,(int)(y-handleHeight/2)));
		bubble.setY(getValueInRange(0, height - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
	}

	private void showBubble() {
		bubble.setVisibility(VISIBLE);
		if(currentAnimator != null)
			currentAnimator.cancel();
		currentAnimator = ObjectAnimator.ofFloat(bubble,"alpha",0f,1f).setDuration(BUBBLE_ANIMATION_DURATION);
		currentAnimator.start();
	}

	private void hideBubble() {
		if(currentAnimator != null)
			currentAnimator.cancel();
		currentAnimator = ObjectAnimator.ofFloat(bubble,"alpha",1f,0f).setDuration(BUBBLE_ANIMATION_DURATION);
		currentAnimator.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				bubble.setVisibility(INVISIBLE);
				currentAnimator = null;
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				super.onAnimationCancel(animation);
				bubble.setVisibility(INVISIBLE);
				currentAnimator = null;
			}
		});
		currentAnimator.start();
	}

	private void registerAdapter() {
		RecyclerView.Adapter newAdapter = recyclerView.getAdapter();
		if(newAdapter != adapter) {
			unregisterAdapter();
		}

		if(newAdapter != null) {
			adapterObserver = new AdapterDataObserver() {
				@Override
				public void onChanged() {
					visibleRange = -1;
				}

				@Override
				public void onItemRangeChanged(int positionStart, int itemCount) {
					visibleRange = -1;
				}

				@Override
				public void onItemRangeInserted(int positionStart, int itemCount) {
					visibleRange = -1;
				}

				@Override
				public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
					visibleRange = -1;
				}

				@Override
				public void onItemRangeRemoved(int positionStart, int itemCount) {
					visibleRange = -1;
				}
			};
			newAdapter.registerAdapterDataObserver(adapterObserver);
			adapter = newAdapter;
		}
	}
	private void unregisterAdapter() {
		if(adapter != null) {
			adapter.unregisterAdapterDataObserver(adapterObserver);
			adapter = null;
			adapterObserver = null;
		}
	}

	private class ScrollListener extends OnScrollListener {
		@Override
		public void onScrolled(RecyclerView rv,int dx,int dy) {
			onUpdateScroll(dx, dy);
		}
	}

	private void onUpdateScroll(int dx, int dy) {
		if(recyclerView.getWidth() == 0) {
			return;
		}
		registerAdapter();

		View firstVisibleView = recyclerView.getChildAt(0);
		if(firstVisibleView == null) {
			return;
		}
		int firstVisiblePosition = recyclerView.getChildPosition(firstVisibleView);

		int itemCount = recyclerView.getAdapter().getItemCount();
		int columns = Math.round(recyclerView.getWidth() / firstVisibleView.getWidth());
		if(visibleRange == -1) {
			visibleRange = recyclerView.getChildCount();
		}

		// Add the percentage of the item the user has scrolled past already
		float pastFirst = -firstVisibleView.getY() / firstVisibleView.getHeight() * columns;
		float position = firstVisiblePosition + pastFirst;

		// Scale this so as we move down the visible range gets added to position from 0 -> visible range
		float scaledVisibleRange = position / (float) (itemCount - visibleRange) * visibleRange;
		position += scaledVisibleRange;

		float proportion = position / itemCount;
		setBubbleAndHandlePosition(height * proportion);

		if((visibleRange * 2) < itemCount) {
			if (!hasScrolled && (dx > 0 || dy > 0)) {
				setVisibility(View.VISIBLE);
				hasScrolled = true;
				recyclerView.setVerticalScrollBarEnabled(false);
			}
		} else if(hasScrolled) {
			setVisibility(View.GONE);
			hasScrolled = false;
			recyclerView.setVerticalScrollBarEnabled(true);
		}
	}

	public interface BubbleTextGetter {
		String getTextToShowInBubble(int position);
	}
}
