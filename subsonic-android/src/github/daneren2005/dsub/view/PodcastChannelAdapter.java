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
package github.daneren2005.dsub.view;

import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.PodcastChannel;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

/**
 * @author Sindre Mehus
*/
public class PodcastChannelAdapter extends ArrayAdapter<PodcastChannel>{
	private Context activity;
	private List<PodcastChannel> podcasts;

    public PodcastChannelAdapter(Context context, List<PodcastChannel> podcasts) {
        super(context, android.R.layout.simple_list_item_1, podcasts);
		this.activity = context;
		this.podcasts = podcasts;
    }
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PodcastChannel podcast = podcasts.get(position);
		PodcastChannelView view;
		if (convertView != null && convertView instanceof PodcastChannelView) {
			view = (PodcastChannelView) convertView;
		} else {
			view = new PodcastChannelView(activity);
		}
		view.setPodcastChannel(podcast);
		return view;
    }
}
