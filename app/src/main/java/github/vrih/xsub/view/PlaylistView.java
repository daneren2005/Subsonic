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
import android.widget.TextView;
import github.vrih.xsub.R;
import github.vrih.xsub.domain.Playlist;
import github.vrih.xsub.util.ImageLoader;
import github.vrih.xsub.util.SyncUtil;

/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class PlaylistView extends UpdateView<Playlist> {

	private final TextView titleView;
	private final ImageLoader imageLoader;

	public PlaylistView(Context context, ImageLoader imageLoader, boolean largeCell) {
		super(context);
		LayoutInflater.from(context).inflate(largeCell ? R.layout.basic_cell_item : R.layout.basic_art_item, this, true);

		coverArtView = findViewById(R.id.item_art);
		titleView = findViewById(R.id.item_name);
		moreButton = findViewById(R.id.item_more);

		this.imageLoader = imageLoader;
	}

	protected void setObjectImpl(Playlist playlist) {
		titleView.setText(playlist.getName());
		imageTask = imageLoader.loadImage(coverArtView, playlist, false, true);
	}

	public void onUpdateImageView() {
		imageTask = imageLoader.loadImage(coverArtView, item, false, true);
	}

	@Override
	protected void updateBackground() {
		pinned = SyncUtil.isSyncedPlaylist(context, item.getId());
	}
}
