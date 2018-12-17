package github.vrih.xsub.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.material.card.MaterialCardView;

import github.vrih.xsub.R;
import github.vrih.xsub.util.DrawableTint;

public class CardView extends MaterialCardView {
	private static final String TAG = CardView.class.getSimpleName();
	Path clipPath;
	Rect bounds;
	RectF roundedRect;

	public CardView(Context context) {
		super(context);
		init(context);
	}

	public CardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
//
//	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//	public CardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//		super(context, attrs, defStyleAttr, defStyleRes);
//		init(context);
//	}

	@Override
	public void onDraw(Canvas canvas) {
		try {
			float roundedDp = getResources().getDimension(R.dimen.Card_Radius);
			canvas.getClipBounds(bounds);
			roundedRect.set(bounds);
			clipPath.addRoundRect(roundedRect, roundedDp, roundedDp, Path.Direction.CW);
			canvas.clipPath(clipPath);
		} catch(Exception e) {
			Log.e(TAG, "Failed to clip path on canvas", e);
		}
		super.onDraw(canvas);
	}

	private void init(Context context) {
		setClipChildren(true);
		clipPath = new Path();
		bounds = new Rect();
		roundedRect = new RectF();
		setBackgroundResource(DrawableTint.getDrawableRes(context, R.attr.cardBackgroundDrawable));
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setElevation(getResources().getInteger(R.integer.Card_Elevation));
		}
	}
}
