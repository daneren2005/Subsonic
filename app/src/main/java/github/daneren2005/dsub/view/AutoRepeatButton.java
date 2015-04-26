package github.daneren2005.dsub.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class AutoRepeatButton extends ImageButton {

	private static final long initialRepeatDelay = 1000;
	private static final long repeatIntervalInMilliseconds = 300;
	private boolean doClick = true;
	private Runnable repeatEvent = null;

	private Runnable repeatClickWhileButtonHeldRunnable = new Runnable() {
		@Override
		public void run() {
			doClick = false;
			//Perform the present repetition of the click action provided by the user
			// in setOnClickListener().
			if(repeatEvent != null)
				repeatEvent.run();

			//Schedule the next repetitions of the click action, using a faster repeat
			// interval than the initial repeat delay interval.
			postDelayed(repeatClickWhileButtonHeldRunnable, repeatIntervalInMilliseconds);
		}
	};

	private void commonConstructorCode() {
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction(); 
				if(action == MotionEvent.ACTION_DOWN) 
				{
					doClick = true;
					//Just to be sure that we removed all callbacks, 
					// which should have occurred in the ACTION_UP
					removeCallbacks(repeatClickWhileButtonHeldRunnable);

					//Schedule the start of repetitions after a one half second delay.
					postDelayed(repeatClickWhileButtonHeldRunnable, initialRepeatDelay);
					
					setPressed(true);
				}
				else if(action == MotionEvent.ACTION_UP) {
					//Cancel any repetition in progress.
					removeCallbacks(repeatClickWhileButtonHeldRunnable);

					if(doClick || repeatEvent == null) {
						performClick();
					}
					
					setPressed(false);
				}

				//Returning true here prevents performClick() from getting called 
				// in the usual manner, which would be redundant, given that we are 
				// already calling it above.
				return true;
			}
		});
	}
	
	public void setOnRepeatListener(Runnable runnable) {
		repeatEvent = runnable;
	}

	public AutoRepeatButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		commonConstructorCode();
	}


	public AutoRepeatButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		commonConstructorCode();
	}

	public AutoRepeatButton(Context context) {
		super(context);
		commonConstructorCode();
	}
}
