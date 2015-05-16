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
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.view.AlbumView;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.view.UpdateView.UpdateViewHolder;

public class EntryGridAdapter extends RecyclerView.Adapter<UpdateViewHolder> {
	private static String TAG = EntryGridAdapter.class.getSimpleName();

	public static int VIEW_TYPE_HEADER = 0;
	public static int VIEW_TYPE_ALBUM_CELL = 1;
	public static int VIEW_TYPE_ALBUM_LINE = 2;
	public static int VIEW_TYPE_SONG = 3;

	protected Context context;
	protected List<Entry> entries;
	private ImageLoader imageLoader;
	private boolean largeAlbums;
	private boolean showArtist = false;
	private boolean checkable = true;
	private OnEntryClickedListener onEntryClickedListener;

	private View header;
	private List<Entry> selected = new ArrayList<Entry>();
	private UpdateView contextView;
	private Entry contextEntry;

	public EntryGridAdapter(Context context, List<Entry> entries, ImageLoader imageLoader, boolean largeCell) {
		this.context = context;
		this.entries = entries;
		this.imageLoader = imageLoader;
		this.largeAlbums = largeCell;

		// Always show artist if they aren't all the same
		String artist = null;
		for(MusicDirectory.Entry entry: entries) {
			if(artist == null) {
				artist = entry.getArtist();
			}

			if(artist != null && !artist.equals(entry.getArtist())) {
				showArtist = true;
			}
		}
	}

	@Override
	public UpdateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if(viewType == VIEW_TYPE_HEADER) {
			return new UpdateViewHolder(header, false);
		}

		UpdateView updateView = null;
		if(viewType == VIEW_TYPE_ALBUM_LINE || viewType == VIEW_TYPE_ALBUM_CELL) {
			updateView = new AlbumView(context, viewType == VIEW_TYPE_ALBUM_CELL);
		} else if(viewType == VIEW_TYPE_SONG) {
			updateView = new SongView(context);
		}

		if(viewType != VIEW_TYPE_HEADER && updateView != null) {
			final UpdateView view = updateView;
			updateView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Entry entry = getEntryForView(view);

					if (view.isCheckable() && v instanceof SongView) {
						SongView songView = (SongView) v;

						if (selected.contains(entry)) {
							selected.remove(entry);
							songView.setChecked(false);
						} else {
							selected.add(entry);
							songView.setChecked(true);
						}
					} else if (onEntryClickedListener != null) {
						onEntryClickedListener.onEntryClicked(entry);
					}
				}
			});
			updateView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Entry entry = getEntryForView(view);

					setContextEntry(view, entry);
					v.showContextMenu();
					return false;
				}
			});

			View moreButton = updateView.findViewById(R.id.more_button);
			if(moreButton != null) {
				moreButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Entry entry = getEntryForView(view);
						setContextEntry(view, entry);
						v.showContextMenu();
					}
				});
			}
		}

		return new UpdateViewHolder(updateView);
	}

	@Override
	public void onBindViewHolder(UpdateViewHolder holder, int position) {
		// Header already created
		if(header != null && position == 0) {
			return;
		}

		UpdateView view = holder.getUpdateView();

		int viewType = getItemViewType(position);
		Entry entry = getEntryForPosition(position);
		if(viewType == VIEW_TYPE_ALBUM_CELL || viewType == VIEW_TYPE_ALBUM_LINE) {
			AlbumView albumView = (AlbumView) view;
			albumView.setShowArtist(showArtist);
			albumView.setObject(entry, imageLoader);
		} else if(viewType == VIEW_TYPE_SONG) {
			SongView songView = (SongView) view;
			songView.setObject(entry, checkable);
			songView.setChecked(selected.contains(entry));
		}
		view.setPosition(position);
	}

	@Override
	public int getItemCount() {
		int size = entries.size();

		if(header != null) {
			size++;
		}

		return size;
	}

	@Override
	public int getItemViewType(int position) {
		if(header != null && position == 0) {
			return VIEW_TYPE_HEADER;
		}

		Entry entry = getEntryForPosition(position);
		if(entry.isDirectory()) {
			if (largeAlbums) {
				return VIEW_TYPE_ALBUM_CELL;
			} else {
				return VIEW_TYPE_ALBUM_LINE;
			}
		} else {
			return VIEW_TYPE_SONG;
		}
	}

	public Entry getEntryForView(UpdateView view) {
		int position = view.getPosition();
		return getEntryForPosition(position);
	}
	public Entry getEntryForPosition(int position) {
		if(header != null) {
			position--;
		}

		return entries.get(position);
	}

	public void setHeader(View header) {
		this.header = header;
	}

	public void setShowArtist(boolean showArtist) {
		this.showArtist = showArtist;
	}
	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
	}

	public void setOnEntryClickedListener(OnEntryClickedListener listener) {
		this.onEntryClickedListener = listener;
	}

	public List<Entry> getSelected() {
		List<Entry> selected = new ArrayList<>();
		selected.addAll(this.selected);
		return selected;
	}
	public void clearSelected() {
		for(Entry entry: selected) {
			int index = entries.indexOf(entry);
			this.notifyItemChanged(index);
		}
		selected.clear();
	}

	public void removeEntry(Entry entry) {
		int index = entries.indexOf(entry);
		if(index != -1) {
			removeAt(index);
		}
	}
	public void removeAt(int index) {
		entries.remove(index);
		notifyItemRemoved(index);
	}

	public void setContextEntry(UpdateView view, Entry entry) {
		this.contextView = view;
		this.contextEntry = entry;
	}
	public UpdateView getContextView() {
		return contextView;
	}
	public Entry getContextEntry() {
		return contextEntry;
	}

	public interface OnEntryClickedListener {
		void onEntryClicked(Entry entry);
	}
}
