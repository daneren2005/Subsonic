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

package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import github.daneren2005.dsub.R;

public class DetailsAdapter extends ArrayAdapter<String> {
	private List<String> headers;
	private List<String> details;

	public DetailsAdapter(Context context, int layout, List<String> headers, List<String> details) {
		super(context, layout, headers);

		this.headers = headers;
		this.details = details;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		View view;
		if(convertView == null) {
			view = LayoutInflater.from(getContext()).inflate(R.layout.details_item, null);
		} else {
			view = convertView;
		}

		TextView nameView = (TextView) view.findViewById(R.id.detail_name);
		TextView detailsView = (TextView) view.findViewById(R.id.detail_value);

		nameView.setText(headers.get(position));

		detailsView.setText(details.get(position));
		Linkify.addLinks(detailsView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

		return view;
	}
}
