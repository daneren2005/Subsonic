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
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.textfield.TextInputLayout;

/**
 * EditTextPreference whose field is shown as a Material 3 outlined text field.
 *
 * The platform preference creates its EditText in code and puts it straight into the dialog, so it
 * cannot be styled from a layout. Once the platform has added it, the field is moved into a
 * TextInputLayout at the same position.
 */
public class MaterialEditTextPreference extends EditTextPreference {
	private TextInputLayout fieldLayout;

	public MaterialEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public MaterialEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public MaterialEditTextPreference(Context context) {
		super(context);
	}

	@Override
	protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
		super.onAddEditTextToDialogView(dialogView, editText);

		ViewGroup container = (ViewGroup) editText.getParent();
		if(container == null) {
			return;
		}
		int index = container.indexOfChild(editText);
		container.removeView(editText);

		fieldLayout = new TextInputLayout(getContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
		int variation = editText.getInputType() & InputType.TYPE_MASK_VARIATION;
		if(variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
			fieldLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
		}
		// The layout only draws its outline as the field background when the field has none,
		// and the platform padding is narrower than the Material 3 one
		editText.setBackground(null);
		int padding = Math.round(16 * getContext().getResources().getDisplayMetrics().density);
		editText.setPadding(padding, padding, padding, padding);
		fieldLayout.addView(editText,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		container.addView(fieldLayout, index, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	/** The text field around the EditText of the dialog that is currently shown. */
	protected View getFieldLayout() {
		return fieldLayout;
	}
}
