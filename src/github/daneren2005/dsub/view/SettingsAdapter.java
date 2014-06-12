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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;

import static github.daneren2005.dsub.domain.User.Setting;

public class SettingsAdapter extends ArrayAdapter<Setting> {
	private final Context context;
	private final boolean editable;

	public SettingsAdapter(Context context, User user, boolean editable) {
		super(context, R.layout.basic_list_item, user.getSettings());
		this.context = context;
		this.editable = editable;
	}

	public SettingsAdapter(Context context, List<Setting> settings, boolean editable) {
		super(context, R.layout.basic_list_item, settings);
		this.context = context;
		this.editable = editable;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Setting entry = getItem(position);
		SettingView view;
		if (convertView != null && convertView instanceof SettingView) {
			view = (SettingView) convertView;
		} else {
			view = new SettingView(context);
		}
		view.setObject(entry, editable);
		return view;
	}
}
