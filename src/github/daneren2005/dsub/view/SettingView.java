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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;

import static github.daneren2005.dsub.domain.User.Setting;

public class SettingView extends UpdateView {
	Setting setting;

	CheckedTextView view;

	public SettingView(Context context) {
		super(context);
		this.context = context;
		LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, this, true);

		view = (CheckedTextView) findViewById(android.R.id.text1);
	}

	protected void setObjectImpl(Object obj, Object editable) {
		this.setting = (Setting) obj;

		String display = setting.getName();
		// Can't edit non-role parts
		if(display.indexOf("Role") == -1) {
			editable = false;
		}
		display = display.replace("Role", "");
		display = Character.toUpperCase(display.charAt(0)) + display.substring(1);

		view.setText(display);
		if(setting.getValue()) {
			view.setChecked(setting.getValue());
		} else {
			view.setChecked(false);
		}

		if((Boolean) editable) {
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					view.toggle();
					setting.setValue(view.isChecked());
				}
			});
		} else {
			view.setOnClickListener(null);
		}
	}
}
