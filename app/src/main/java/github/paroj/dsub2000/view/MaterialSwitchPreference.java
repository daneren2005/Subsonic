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

	 Copyright 2014 (C) Scott Jackson
*/

package github.paroj.dsub2000.view;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.material.materialswitch.MaterialSwitch;

import github.paroj.dsub2000.R;

/**
 * A {@link SwitchPreference} that renders a Material 3
 * {@link MaterialSwitch} as its widget instead of the platform switch.
 *
 * The platform inflater cannot resolve the internal id the base class uses
 * to locate its widget, so the switch is synchronized here explicitly.
 */
public class MaterialSwitchPreference extends SwitchPreference {
	private final Listener mListener = new Listener();

	public MaterialSwitchPreference(Context context) {
		this(context, null);
	}

	public MaterialSwitchPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.switchPreferenceStyle);
	}

	public MaterialSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setWidgetLayoutResource(R.layout.preference_widget_material_switch);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		MaterialSwitch switchView = (MaterialSwitch) view.findViewById(R.id.material_switch_widget);
		if (switchView != null) {
			// Clear the listener while re-binding so setting the checked
			// state does not feed back into the preference.
			switchView.setOnCheckedChangeListener(null);
			switchView.setChecked(isChecked());
			switchView.setEnabled(isEnabled());
			switchView.setOnCheckedChangeListener(mListener);
		}
	}

	private class Listener implements CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean desiredState) {
			if (!callChangeListener(desiredState)) {
				// Listener didn't like it, change it back.
				buttonView.setChecked(!desiredState);
			} else {
				MaterialSwitchPreference.this.setChecked(desiredState);
			}
		}
	}
}
