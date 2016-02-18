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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.domain.User.MusicFolderSetting;

import static github.daneren2005.dsub.domain.User.Setting;

public class SettingView extends UpdateView2<Setting, Boolean> {
	private final TextView titleView;
	private final CheckBox checkBox;

	public SettingView(Context context) {
		super(context, false);
		this.context = context;
		LayoutInflater.from(context).inflate(R.layout.basic_choice_item, this, true);

		titleView = (TextView) findViewById(R.id.item_name);
		checkBox = (CheckBox) findViewById(R.id.item_checkbox);
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(item != null) {
					item.setValue(isChecked);
				}
			}
		});
		checkBox.setClickable(false);
	}

	protected void setObjectImpl(Setting setting, Boolean isEditable) {
		// Can't edit non-role parts
		String name = setting.getName();
		if(name.indexOf("Role") == -1 && !(setting instanceof MusicFolderSetting)) {
			item2 = false;
		}
		
		int res = -1;
		if(setting instanceof MusicFolderSetting) {
			titleView.setText(((MusicFolderSetting) setting).getLabel());
		} else if(User.SCROBBLING.equals(name)) {
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
		} else if(User.VIDEO_CONVERSION.equals(name)) {
			res = R.string.admin_role_video_conversion;
		} else if(User.LASTFM.equals(name)) {
			res = R.string.admin_role_lastfm;
		} else {
			// Last resort to display the raw value
			titleView.setText(name);
		}
		
		if(res != -1) {
			titleView.setText(res);
		}

		if(setting.getValue()) {
			checkBox.setChecked(setting.getValue());
		} else {
			checkBox.setChecked(false);
		}

		checkBox.setEnabled(item2);
	}

	@Override
	public boolean isCheckable() {
		return item2;
	}

	public void setChecked(boolean checked) {
		checkBox.setChecked(checked);
	}
}
