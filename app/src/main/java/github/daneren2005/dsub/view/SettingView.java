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

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;

import static github.daneren2005.dsub.domain.User.Setting;

public class SettingView extends UpdateView {
	Setting setting;

	CheckedTextView view;

	public SettingView(Context context) {
		super(context, false);
		this.context = context;
		LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, this, true);

		view = (CheckedTextView) findViewById(android.R.id.text1);
	}

	protected void setObjectImpl(Object obj, Object editable) {
		this.setting = (Setting) obj;

		// Can't edit non-role parts
		String name = setting.getName();
		if(name.indexOf("Role") == -1) {
			editable = false;
		}
		
		int res = -1;
		if(User.SCROBBLING.equals(name)) {
			res = R.string.admin_scrobblingEnabled;
		} else if(User.ADMIN.equals(name)) {
			res = R.string.admin_role_admin;
		} else if(User.SETTINGS.equals(name)) {
			res = R.string.admin_role_settings;
		} else if(User.DOWNLOAD.equals(name)) {
			res = R.string.admin_role_download;
		} else if(User.UPLOAD.equals(name)) {
			res = R.string.admin_role_upload;
		} else if(User.COVERART.equals(name)) {
			res = R.string.admin_role_coverArt;
		} else if(User.COMMENT.equals(name)) {
			res = R.string.admin_role_comment;
		} else if(User.PODCAST.equals(name)) {
			res = R.string.admin_role_podcast;
		} else if(User.STREAM.equals(name)) {
			res = R.string.admin_role_stream;
		} else if(User.JUKEBOX.equals(name)) {
			res = R.string.admin_role_jukebox;
		} else if(User.SHARE.equals(name)) {
			res = R.string.admin_role_share;
		} else if(User.LASTFM.equals(name)) {
			res = R.string.admin_role_lastfm;
		} else {
			// Last resort to display the raw value
			view.setText(name);
		}
		
		if(res != -1) {
			view.setText(res);
		}

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
