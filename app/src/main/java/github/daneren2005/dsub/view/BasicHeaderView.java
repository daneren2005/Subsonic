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
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import github.daneren2005.dsub.R;

public class BasicHeaderView extends UpdateView<String> {
	final TextView nameView;

	public BasicHeaderView(Context context) {
		this(context, R.layout.basic_header);
	}
	public BasicHeaderView(Context context, int layout) {
		super(context, false);

		LayoutInflater.from(context).inflate(layout, this, true);
		nameView = (TextView) findViewById(R.id.item_name);
	}

	protected void setObjectImpl(String string) {
		nameView.setText(string);
	}
}
