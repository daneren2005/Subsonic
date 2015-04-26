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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.view.ShareView;

/**
 * @author Sindre Mehus
*/
public class ShareAdapter extends ArrayAdapter<Share>{
	private Context activity;
	private List<Share> shares;

    public ShareAdapter(Context context, List<Share> shares) {
        super(context, android.R.layout.simple_list_item_1, shares);
		this.activity = context;
		this.shares = shares;
    }
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		Share share = shares.get(position);
		ShareView view;
		if (convertView != null && convertView instanceof ShareView) {
			view = (ShareView) convertView;
		} else {
			view = new ShareView(activity);
		}
		view.setObject(share);
		return view;
    }
}
