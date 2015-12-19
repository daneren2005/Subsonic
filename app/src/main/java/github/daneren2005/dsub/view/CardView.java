package github.daneren2005.dsub.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import github.daneren2005.dsub.R;

public class CardView extends FrameLayout{
	public CardView(Context context) {
		super(context);
		this.setClipChildren(true);
		this.setBackgroundResource(R.drawable.card_rounded_corners);
	}

	public CardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setClipChildren(true);
		this.setBackgroundResource(R.drawable.card_rounded_corners);
	}

	public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.setClipChildren(true);
		this.setBackgroundResource(R.drawable.card_rounded_corners);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.setClipChildren(true);
		this.setBackgroundResource(R.drawable.card_rounded_corners);
	}

	@Override
	public void onDraw(Canvas canvas) {
		Path clipPath = new Path();

		float roundedDp = getResources().getDimension(R.dimen.Card_Radius);
		clipPath.addRoundRect(new RectF(canvas.getClipBounds()), roundedDp, roundedDp, Path.Direction.CW);
		canvas.clipPath(clipPath);
		super.onDraw(canvas);
	}
}
