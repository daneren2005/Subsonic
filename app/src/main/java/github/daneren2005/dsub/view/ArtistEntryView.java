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
package github.daneren2005.dsub.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.FileUtil;

import java.io.File;
/**
 * Used to display albums in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class ArtistEntryView extends UpdateView<MusicDirectory.Entry> {
	private static final String TAG = ArtistEntryView.class.getSimpleName();

	private File file;
    private TextView titleView;

    public ArtistEntryView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.basic_list_item, this, true);

        titleView = (TextView) findViewById(R.id.item_name);
		starButton = (ImageButton) findViewById(R.id.item_star);
		starButton.setFocusable(false);
		moreButton = (ImageView) findViewById(R.id.item_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
    }
    
    protected void setObjectImpl(MusicDirectory.Entry artist) {
    	titleView.setText(artist.getTitle());
		file = FileUtil.getArtistDirectory(context, artist);
    }
    
	@Override
	protected void updateBackground() {
		exists = file.exists();
		isStarred = item.isStarred();
	}

	public File getFile() {
		return file;
	}
}
