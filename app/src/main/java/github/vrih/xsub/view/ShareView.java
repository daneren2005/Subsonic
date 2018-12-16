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

import java.text.SimpleDateFormat;
import java.util.Locale;

import github.vrih.xsub.R;
import github.vrih.xsub.domain.Share;

public class ShareView extends UpdateView<Share> {
	private static final String TAG = ShareView.class.getSimpleName();

	private final TextView titleView;
	private final TextView descriptionView;

	public ShareView(Context context) {
		super(context, false);
		LayoutInflater.from(context).inflate(R.layout.complex_list_item, this, true);

		titleView = findViewById(R.id.item_name);
		descriptionView = findViewById(R.id.item_description);
		starButton = findViewById(R.id.item_star);
		starButton.setFocusable(false);
		moreButton = findViewById(R.id.item_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
	}

	public void setObjectImpl(Share share) {
		titleView.setText(share.getName());
		if(share.getExpires() != null) {
			descriptionView.setText(context.getResources().getString(R.string.share_expires, new SimpleDateFormat("E MMM d, yyyy", Locale.ENGLISH).format(share.getExpires())));
		} else {
			descriptionView.setText(context.getResources().getString(R.string.share_expires_never));
		}
	}
}
