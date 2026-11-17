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

package github.paroj.dsub2000.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Set;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.domain.MusicFolder;
import github.paroj.dsub2000.util.Util;
import github.paroj.dsub2000.view.AlbumListCountView;
import github.paroj.dsub2000.view.MainHeaderView;
import github.paroj.dsub2000.view.MainTileView;
import github.paroj.dsub2000.view.UpdateView;

public class MainAdapter extends SectionAdapter<Integer> {
	public static final int VIEW_TYPE_ALBUM_LIST = 1;
	public static final int VIEW_TYPE_ALBUM_COUNT_LIST = 2;

	/** Header of the lists: shows the page controls instead of a title */
	public static final String HEADER_LISTS = "lists";

	public interface OnControlsChanged {
		void onShowSongsChanged(boolean showSongs);
		/** @param musicFolder the folder to limit the lists to, null for all folders */
		void onMusicFolderChanged(MusicFolder musicFolder);
	}

	/** Alpha of the lists the server does not support, they keep their spot to show what is missing */
	private static final float UNSUPPORTED_ALPHA = 0.38f;

	private final Set<Integer> unsupported;
	private final boolean showSongs;
	private final List<MusicFolder> musicFolders;
	private final MusicFolder selectedFolder;
	private final OnControlsChanged onControlsChanged;

	/**
	 * @param musicFolders the folders to choose from, null to hide the folder choice
	 * @param selectedFolder the folder the lists are limited to, null for all folders
	 */
	public MainAdapter(Context context, List<String> headers, List<List<Integer>> sections, Set<Integer> unsupported, OnItemClickedListener onItemClickedListener,
					   boolean showSongs, List<MusicFolder> musicFolders, MusicFolder selectedFolder, OnControlsChanged onControlsChanged) {
		super(context, headers, sections);
		this.onItemClickedListener = onItemClickedListener;
		this.unsupported = unsupported;
		this.showSongs = showSongs;
		this.musicFolders = musicFolders;
		this.selectedFolder = selectedFolder;
		this.onControlsChanged = onControlsChanged;
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
		UpdateView updateView;
		if(viewType == VIEW_TYPE_ALBUM_LIST) {
			updateView = new MainTileView(context);
		} else  {
			updateView = new AlbumListCountView(context);
		}

		return new UpdateView.UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateView.UpdateViewHolder holder, Integer item, int viewType) {
		UpdateView view = holder.getUpdateView();
		view.setObject(item);
		view.setAlpha(unsupported.contains(item) ? UNSUPPORTED_ALPHA : 1f);
	}

	@Override
	public int getItemViewType(Integer item) {
		if(item == R.string.main_albums_newest) {
			return VIEW_TYPE_ALBUM_COUNT_LIST;
		} else {
			return VIEW_TYPE_ALBUM_LIST;
		}
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		return new UpdateView.UpdateViewHolder(new MainHeaderView(context));
	}
	@Override
	public void onBindHeaderHolder(UpdateView.UpdateViewHolder holder, String header, int sectionIndex) {
		bindControls(holder.getUpdateView());
	}

	private void bindControls(View view) {
		MaterialButtonToggleGroup mode = view.findViewById(R.id.main_mode);
		mode.clearOnButtonCheckedListeners();
		mode.check(showSongs ? R.id.main_mode_songs : R.id.main_mode_albums);
		mode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
			if(isChecked) {
				onControlsChanged.onShowSongsChanged(checkedId == R.id.main_mode_songs);
			}
		});

		final Chip folder = view.findViewById(R.id.main_folder);
		if(musicFolders == null) {
			folder.setVisibility(View.GONE);
			return;
		}

		folder.setVisibility(View.VISIBLE);
		folder.setText(selectedFolder == null ? context.getString(R.string.select_artist_all_folders) : selectedFolder.getName());
		View.OnClickListener showFolders = v -> {
			PopupMenu popup = new PopupMenu(context, folder);
			popup.getMenu().add(0, 0, 0, R.string.select_artist_all_folders);
			for(int i = 0; i < musicFolders.size(); i++) {
				popup.getMenu().add(0, i + 1, i + 1, musicFolders.get(i).getName());
			}
			popup.setOnMenuItemClickListener(item -> {
				int index = item.getItemId() - 1;
				MusicFolder chosen = index < 0 ? null : musicFolders.get(index);
				if(!Util.equals(chosen == null ? null : chosen.getId(), selectedFolder == null ? null : selectedFolder.getId())) {
					onControlsChanged.onMusicFolderChanged(chosen);
				}
				return true;
			});
			popup.show();
		};
		folder.setOnClickListener(showFolders);
		folder.setOnCloseIconClickListener(showFolders);
	}
}
