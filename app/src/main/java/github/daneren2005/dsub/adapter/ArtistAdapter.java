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
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ArtistView;
import github.daneren2005.dsub.view.FastScroller;
import github.daneren2005.dsub.view.UpdateView;

public class ArtistAdapter extends SectionAdapter<Artist> implements FastScroller.BubbleTextGetter {
	public static int VIEW_TYPE_ARTIST = 4;

	private List<MusicFolder> musicFolders;
	private OnMusicFolderChanged onMusicFolderChanged;

	public ArtistAdapter(Context context, List<Artist> artists, OnItemClickedListener listener) {
		this(context, artists, null, listener, null);
	}

	public ArtistAdapter(Context context, List<Artist> artists, List<MusicFolder> musicFolders, OnItemClickedListener onItemClickedListener, OnMusicFolderChanged onMusicFolderChanged) {
		super(context, artists);
		this.musicFolders = musicFolders;
		this.onItemClickedListener = onItemClickedListener;
		this.onMusicFolderChanged = onMusicFolderChanged;

		if(musicFolders != null) {
			this.singleSectionHeader = true;
		}
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		final View header = LayoutInflater.from(context).inflate(R.layout.select_artist_header, parent, false);
		header.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(context, header.findViewById(R.id.select_artist_folder_2));

				popup.getMenu().add(R.string.select_artist_all_folders);
				for (MusicFolder musicFolder : musicFolders) {
					popup.getMenu().add(musicFolder.getName());
				}

				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						for (MusicFolder musicFolder : musicFolders) {
							if(item.getTitle().equals(musicFolder.getName())) {
								if(onMusicFolderChanged != null) {
									onMusicFolderChanged.onMusicFolderChanged(musicFolder);
								}
								return true;
							}
						}

						if(onMusicFolderChanged != null) {
							onMusicFolderChanged.onMusicFolderChanged(null);
						}
						return true;
					}
				});
				popup.show();
			}
		});

		return new UpdateView.UpdateViewHolder(header, false);
	}
	@Override
	public void onBindHeaderHolder(UpdateView.UpdateViewHolder holder, String header) {
		TextView folderName = (TextView) holder.getView().findViewById(R.id.select_artist_folder_2);

		String musicFolderId = Util.getSelectedMusicFolderId(context);
		if(musicFolderId != null) {
			for (MusicFolder musicFolder : musicFolders) {
				if (musicFolder.getId().equals(musicFolderId)) {
					folderName.setText(musicFolder.getName());
					break;
				}
			}
		} else {
			folderName.setText(R.string.select_artist_all_folders);
		}
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		return new UpdateView.UpdateViewHolder(new ArtistView(context));
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Artist item, int viewType) {
		holder.getUpdateView().setObject(item);
	}

	@Override
	public int getItemViewType(Artist item) {
		return VIEW_TYPE_ARTIST;
	}

	@Override
	public String getTextToShowInBubble(int position) {
		Artist artist = getItemForPosition(position);

		if(artist == null) {
			return "";
		} else {
			return artist.getName().substring(0, 1);
		}
	}

	public interface OnMusicFolderChanged {
		void onMusicFolderChanged(MusicFolder musicFolder);
	}
}
