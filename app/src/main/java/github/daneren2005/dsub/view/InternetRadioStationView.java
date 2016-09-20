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
	Copyright 2016 (C) Scott Jackson
*/
package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.InternetRadioStation;

public class InternetRadioStationView extends UpdateView<InternetRadioStation> {
	private TextView titleView;

	public InternetRadioStationView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.basic_list_item, this, true);

		titleView = (TextView) findViewById(R.id.item_name);
		moreButton = (ImageView) findViewById(R.id.item_more);
	}

	protected void setObjectImpl(InternetRadioStation station) {
		titleView.setText(station.getTitle());
	}
}
