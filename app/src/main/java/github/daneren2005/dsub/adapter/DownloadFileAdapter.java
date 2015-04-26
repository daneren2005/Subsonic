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

package github.daneren2005.dsub.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.view.SongView;

public class DownloadFileAdapter extends ArrayAdapter<DownloadFile> {
	Context context;

	public DownloadFileAdapter(Context context, List<DownloadFile> entries) {
		super(context, android.R.layout.simple_list_item_1, entries);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SongView view;
		if (convertView != null && convertView instanceof SongView) {
			view = (SongView) convertView;
		} else {
			view = new SongView(context);
		}
		DownloadFile downloadFile = getItem(position);
		view.setObject(downloadFile.getSong(), false);
		view.setDownloadFile(downloadFile);
		return view;
	}
}
