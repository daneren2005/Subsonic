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

package github.vrih.xsub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;

import androidx.appcompat.widget.PopupMenu;
import github.vrih.xsub.R;
import github.vrih.xsub.domain.Artist;
import github.vrih.xsub.domain.MusicDirectory.Entry;
import github.vrih.xsub.domain.MusicFolder;
import github.vrih.xsub.util.Util;
import github.vrih.xsub.view.ArtistView;
import github.vrih.xsub.view.FastScroller;
import github.vrih.xsub.view.SongView;
import github.vrih.xsub.view.UpdateView;

public class ArtistAdapter extends SectionAdapter<Serializable> implements FastScroller.BubbleTextGetter {
	private static final int VIEW_TYPE_SONG = 3;
	public static final int VIEW_TYPE_ARTIST = 4;

	private final List<MusicFolder> musicFolders;
	private final OnMusicFolderChanged onMusicFolderChanged;

	public ArtistAdapter(Context context, List<Serializable> artists, List<MusicFolder> musicFolders, OnItemClickedListener onItemClickedListener, OnMusicFolderChanged onMusicFolderChanged) {
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

		return new UpdateView.UpdateViewHolder(header);
	}
	@Override
	public void onBindHeaderHolder(UpdateView.UpdateViewHolder holder, String header, int sectionIndex) {
		TextView folderName = holder.getView().findViewById(R.id.select_artist_folder_2);

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
		UpdateView updateView = null;
		if(viewType == VIEW_TYPE_ARTIST) {
			updateView = new ArtistView(context);
		} else if(viewType == VIEW_TYPE_SONG) {
			updateView = new SongView(context);
		}

		return new UpdateView.UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Serializable item, int viewType) {
		UpdateView view = holder.getUpdateView();
		if(viewType == VIEW_TYPE_ARTIST) {
			view.setObject(item);
		} else if(viewType == VIEW_TYPE_SONG) {
			SongView songView = (SongView) view;
			Entry entry = (Entry) item;
			songView.setObject(entry, checkable && !entry.isVideo());
		}
	}

	@Override
	public int getItemViewType(Serializable item) {
		if(item instanceof Artist) {
			return VIEW_TYPE_ARTIST;
		} else {
			return VIEW_TYPE_SONG;
		}
	}

	@Override
	public String getTextToShowInBubble(int position) {
		Object item = getItemForPosition(position);
		if(item instanceof Artist) {
			return getNameIndex(((Artist) item).getName(), true);
		} else {
			return null;
		}
	}

	public interface OnMusicFolderChanged {
		void onMusicFolderChanged(MusicFolder musicFolder);
	}
}
