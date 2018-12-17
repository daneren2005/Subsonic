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

 Copyright 2009 (C) Sindre Mehus
 */
package github.vrih.xsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import github.vrih.xsub.R;
import github.vrih.xsub.domain.PodcastChannel;
import github.vrih.xsub.util.FileUtil;
import github.vrih.xsub.util.ImageLoader;
import github.vrih.xsub.util.SyncUtil;

public class PodcastChannelView extends UpdateView<PodcastChannel> {

	private File file;
	private final TextView titleView;
	private final ImageLoader imageLoader;

	public PodcastChannelView(Context context) {
		this(context, null, false);
	}
	public PodcastChannelView(Context context, ImageLoader imageLoader, boolean largeCell) {
		super(context);

		this.imageLoader = imageLoader;
		if(imageLoader != null) {
			LayoutInflater.from(context).inflate(largeCell ? R.layout.basic_cell_item : R.layout.basic_art_item, this, true);
		} else {
			LayoutInflater.from(context).inflate(R.layout.basic_list_item, this, true);
		}

		titleView = findViewById(R.id.item_name);
		starButton = findViewById(R.id.item_star);
		if(starButton != null) {
			starButton.setFocusable(false);
		}
		moreButton = findViewById(R.id.item_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
		coverArtView = findViewById(R.id.item_art);
	}

	protected void setObjectImpl(PodcastChannel channel) {
		if(channel.getName() != null) {
			titleView.setText(channel.getName());
		} else {
			titleView.setText(channel.getUrl());
		}
		file = FileUtil.getPodcastDirectory(context, channel);

		if(imageLoader != null) {
			imageTask = imageLoader.loadImage(coverArtView, channel, false, true);
		}
	}

	public void onUpdateImageView() {
		if(imageLoader != null) {
			imageTask = imageLoader.loadImage(coverArtView, item, false, true);
		}
	}
	
	@Override
	protected void updateBackground() {
		if(SyncUtil.isSyncedPodcast(context, item.getId())) {
			if(exists) {
				shaded = false;
				exists = false;
			}
			pinned = true;
		} else if(file.exists()) {
			if(pinned) {
				shaded = false;
				pinned = false;
			}
			exists = true;
		} else {
			pinned = false;
			exists = false;
		}
	}
}
