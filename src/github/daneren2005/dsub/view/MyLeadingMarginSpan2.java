package github.daneren2005.dsub.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * Created by Scott on 1/13/2015.
 */
public class MyLeadingMarginSpan2 implements LeadingMarginSpan.LeadingMarginSpan2 {
	private int margin;
	private int lines;

	public MyLeadingMarginSpan2(int lines, int margin) {
		this.margin = margin;
		this.lines = lines;
	}

	@Override
	public int getLeadingMargin(boolean first) {
		return first ? margin : 0;
	}

	@Override
	public int getLeadingMarginLineCount() {
		return lines;
	}

	@Override
	public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
								  int top, int baseline, int bottom, CharSequence text,
								  int start, int end, boolean first, Layout layout) {}
}
