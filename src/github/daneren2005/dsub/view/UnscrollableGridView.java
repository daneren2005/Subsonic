package github.daneren2005.dsub.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.lang.reflect.Field;

/**
 * Created by Scott on 4/26/2014.
 */
public class UnscrollableGridView extends GridView {
	private static final String TAG = UnscrollableGridView.class.getSimpleName();

	public UnscrollableGridView(Context context) {
		super(context);
	}

	public UnscrollableGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UnscrollableGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public int getColumnWidth() {
		// This method will be called from onMeasure() too.
		// It's better to use getMeasuredWidth(), as it is safe in this case.

		int hSpacing = 20;
		try {
			Field field = GridView.class.getDeclaredField("mHorizontalSpacing");
			field.setAccessible(true);
			hSpacing = field.getInt(this);
		} catch(Exception e) {

		}

		final int totalHorizontalSpacing = getNumColumnsCompat() > 0 ? (getNumColumnsCompat() - 1) * hSpacing : 0;
		return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - totalHorizontalSpacing) / getNumColumnsCompat();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Sets the padding for this view.
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int measuredWidth = getMeasuredWidth();
		final int childWidth = getColumnWidth();
		int childHeight = 0;

		// If there's an adapter, use it to calculate the height of this view.
		final ListAdapter adapter = getAdapter();
		final int count;

		// There shouldn't be any inherent size (due to padding) if there are no child views.
		if (adapter == null || (count = adapter.getCount()) == 0) {
			setMeasuredDimension(0, 0);
			return;
		}

		// Get the first child from the adapter.
		final View child = adapter.getView(0, null, this);
		if (child != null) {
			// Set a default LayoutParams on the child, if it doesn't have one on its own.
			AbsListView.LayoutParams params = (AbsListView.LayoutParams) child.getLayoutParams();
			if (params == null) {
				params = new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT,
						AbsListView.LayoutParams.WRAP_CONTENT);
				child.setLayoutParams(params);
			}

			// Measure the exact width of the child, and the height based on the width.
			// Note: the child takes care of calculating its height.
			int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
			int childHeightSpec = MeasureSpec.makeMeasureSpec(0,  MeasureSpec.UNSPECIFIED);
			child.measure(childWidthSpec, childHeightSpec);
			childHeight = child.getMeasuredHeight();
		}

		int vSpacing = 10;
		try {
			Field field = GridView.class.getDeclaredField("mVerticalSpacing");
			field.setAccessible(true);
			vSpacing = field.getInt(this);
		} catch(Exception e) {

		}

		// Number of rows required to 'mTotal' items.
		final int rows = (int) Math.ceil((double) getCount() / getNumColumnsCompat());
		final int childrenHeight = childHeight * rows;
		final int totalVerticalSpacing = rows > 0 ? (rows - 1) * vSpacing : 0;

		// Total height of this view.
		final int measuredHeight = Math.abs(childrenHeight + getPaddingTop() + getPaddingBottom() + totalVerticalSpacing);
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	private int getNumColumnsCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return getNumColumnsCompat11();
		} else {
			int columns = 0;
			int children = getChildCount();
			if (children > 0) {
				int width = getChildAt(0).getMeasuredWidth();
				if (width > 0) {
					columns = getWidth() / width;
				}
			}
			return columns > 0 ? columns : AUTO_FIT;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private int getNumColumnsCompat11() {
		return getNumColumns();
	}
}
