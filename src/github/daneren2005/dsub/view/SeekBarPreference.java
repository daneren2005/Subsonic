/*
 * Copyright (C) 2012 Christopher Eby <kreed@kreed.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package github.daneren2005.dsub.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.Constants;

/**
 * SeekBar preference to set the shake force threshold.
 */
public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	/**
	 * The current value.
	 */
	private String mValue;

	/**
	 * Our context (needed for getResources())
	 */
	private Context mContext;

	/**
	 * TextView to display current threshold.
	 */
	private TextView mValueText;

	public SeekBarPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
	}

	@Override
	public CharSequence getSummary()
	{
		return getSummary(mValue);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
	{
		mValue = restoreValue ? getPersistedString((String) defaultValue) : (String)defaultValue;
	}

	/**
	 * Create the summary for the given value.
	 *
	 * @param value The force threshold.
	 * @return A string representation of the threshold.
	 */
	private String getSummary(String value) {
		int val = Integer.parseInt(value);
		if(Constants.PREFERENCES_KEY_REPLAY_GAIN_UNTAGGED.equals(getKey())) {
			return String.format("%+.1f dB", (val - 150) / 10f);
		} else if(Constants.PREFERENCES_KEY_REPLAY_GAIN_BUMP.equals(getKey())) {
			return String.format("%+.1f dB", 2 * (val - 75) / 10f);
		} else {
			return String.format("%+.1f", val);
		}
	}

	@Override
	protected View onCreateDialogView()
	{
		View view = super.onCreateDialogView();

		mValueText = (TextView)view.findViewById(R.id.value);
		mValueText.setText(getSummary(mValue));

		SeekBar seekBar = (SeekBar)view.findViewById(R.id.seek_bar);
		seekBar.setMax(150);
		seekBar.setProgress(Integer.parseInt(mValue));
		seekBar.setOnSeekBarChangeListener(this);

		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		persistString(mValue);
		notifyChanged();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		if (fromUser) {
			mValue = String.valueOf(progress);
			mValueText.setText(getSummary(mValue));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
	}
}
