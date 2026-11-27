/*
	 This file is part of Subsonic.

	 Subsonic is free software: you can redistribute it and/or modify
	 it under the terms of the GNU General Public License as published by
	 the Free Software Foundation, either version 3 of the License, or
	 (at your option) any later version.

	 Subsonic is distributed in the hope that it will be useful,
	 but WITHOUT ANY WARRANTY; without even the implied warranty of
	 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 GNU General Public License for more details.

	 You should have received a copy of the GNU General Public License
	 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
*/

package github.paroj.dsub2000.view;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

/**
 * Text field whose floating label is one of the form labels that used to sit next to the field.
 * Those labels and their translations end in a colon (":", " :" or "："), which does not belong
 * in a floating label, so it is dropped.
 */
public class FormTextInputLayout extends TextInputLayout {
	private static final Pattern TRAILING_COLON = Pattern.compile("\\s*[:：]\\s*$");

	public FormTextInputLayout(Context context) {
		super(context);
	}

	public FormTextInputLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FormTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setHint(CharSequence hint) {
		super.setHint(hint == null ? null : stripLabel(hint));
	}

	/** A form label without its trailing colon, for places that show it on its own. */
	public static String stripLabel(CharSequence label) {
		return TRAILING_COLON.matcher(label).replaceFirst("");
	}
}
