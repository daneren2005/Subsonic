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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.view.BasicHeaderView;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.view.UpdateView.UpdateViewHolder;

public abstract class SectionAdapter<T> extends RecyclerView.Adapter<UpdateViewHolder<T>> {
	private static String TAG = SectionAdapter.class.getSimpleName();
	public static int VIEW_TYPE_HEADER = 0;

	protected Context context;
	protected List<String> headers;
	protected List<List<T>> sections;
	protected boolean singleSectionHeader;
	protected OnItemClickedListener<T> onItemClickedListener;
	protected List<T> selected = new ArrayList<>();
	protected ActionMode currentActionMode;
	protected boolean checkable = true;

	protected SectionAdapter() {}
	public SectionAdapter(Context context, List<T> section) {
		this(context, section, false);
	}
	public SectionAdapter(Context context, List<T> section, boolean singleSectionHeader) {
		this.context = context;
		this.headers = Arrays.asList("Section");
		this.sections = new ArrayList<>();
		this.sections.add(section);
		this.singleSectionHeader = singleSectionHeader;
	}
	public SectionAdapter(Context context, List<String> headers, List<List<T>> sections) {
		this(context, headers, sections, true);
	}
	public SectionAdapter(Context context, List<String> headers, List<List<T>> sections, boolean singleSectionHeader){
		this.context = context;
		this.headers = headers;
		this.sections = sections;
		this.singleSectionHeader = singleSectionHeader;
	}

	public void replaceExistingData(List<T> section) {
		this.sections = new ArrayList<>();
		this.sections.add(section);
		notifyDataSetChanged();
	}
	public void replaceExistingData(List<String> headers, List<List<T>> sections) {
		this.headers = headers;
		this.sections = sections;
		notifyDataSetChanged();
	}

	@Override
	public UpdateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if(viewType == VIEW_TYPE_HEADER) {
			return onCreateHeaderHolder(parent);
		} else {
			final UpdateViewHolder<T> holder = onCreateSectionViewHolder(parent, viewType);
			final UpdateView updateView = holder.getUpdateView();

			if(updateView != null) {
				updateView.getChildAt(0).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						T item = holder.getItem();
						updateView.onClick();
						if (updateView.isCheckable() && currentActionMode != null) {
							if (selected.contains(item)) {
								selected.remove(item);
								setChecked(updateView, false);
							} else {
								selected.add(item);
								setChecked(updateView, true);
							}

							if(selected.isEmpty()) {
								currentActionMode.finish();
							} else {
								currentActionMode.setTitle(context.getResources().getString(R.string.select_album_n_selected, selected.size()));
							}
						} else if (onItemClickedListener != null) {
							onItemClickedListener.onItemClicked(item);
						}
					}
				});

				View moreButton = updateView.findViewById(R.id.more_button);
				if(moreButton == null) {
					moreButton = updateView.findViewById(R.id.item_more);
				}
				if (moreButton != null) {
					moreButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							final T item = holder.getItem();
							if(onItemClickedListener != null) {
								PopupMenu popup = new PopupMenu(context, v);
								onItemClickedListener.onCreateContextMenu(popup.getMenu(), popup.getMenuInflater(), updateView, item);

								popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
									@Override
									public boolean onMenuItemClick(MenuItem menuItem) {
										return onItemClickedListener.onContextItemSelected(menuItem, updateView, item);
									}
								});
								popup.show();
							}
						}
					});

					if(checkable) {
						updateView.getChildAt(0).setOnLongClickListener(new View.OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								startActionMode(holder);
								return true;
							}
						});
					}
				}
			}

			return holder;
		}
	}

	@Override
	public void onBindViewHolder(UpdateViewHolder holder, int position) {
		UpdateView updateView = holder.getUpdateView();

		if(sections.size() == 1 && !singleSectionHeader) {
			T item = sections.get(0).get(position);
			onBindViewHolder(holder, item, getItemViewType(position));
			if(updateView.isCheckable()) {
				setChecked(updateView, selected.contains(item));
			}
			holder.setItem(item);
			return;
		}

		int subPosition = 0;
		int subHeader = 0;
		for(List<T> section: sections) {
			boolean validHeader = headers.get(subHeader) != null;
			if(position == subPosition && validHeader) {
				onBindHeaderHolder(holder, headers.get(subHeader));
				return;
			}

			int headerOffset = validHeader ? 1 : 0;
			if(position < (subPosition + section.size() + headerOffset)) {
				T item = section.get(position - subPosition - headerOffset);
				onBindViewHolder(holder, item, getItemViewType(item));

				if(updateView.isCheckable()) {
					setChecked(updateView, selected.contains(item));
				}
				holder.setItem(item);
				return;
			}

			subPosition += section.size();
			if(validHeader) {
				subPosition += 1;
			}
			subHeader++;
		}
	}

	@Override
	public int getItemCount() {
		if(sections.size() == 1 && !singleSectionHeader) {
			return sections.get(0).size();
		}

		int count = 0;
		for(String header: headers) {
			if(header != null) {
				count++;
			}
		}
		for(List<T> section: sections) {
			count += section.size();
		}

		return count;
	}

	@Override
	public int getItemViewType(int position) {
		if(sections.size() == 1 && !singleSectionHeader) {
			return getItemViewType(sections.get(0).get(position));
		}

		int subPosition = 0;
		int subHeader = 0;
		for(List<T> section: sections) {
			boolean validHeader = headers.get(subHeader) != null;
			if(position == subPosition && validHeader) {
				return VIEW_TYPE_HEADER;
			}

			int headerOffset = validHeader ? 1 : 0;
			if(position < (subPosition + section.size() + headerOffset)) {
				return getItemViewType(section.get(position - subPosition - headerOffset));
			}

			subPosition += section.size();
			if(validHeader) {
				subPosition += 1;
			}
			subHeader++;
		}

		return -1;
	}

	public UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		return new UpdateViewHolder(new BasicHeaderView(context));
	}
	public void onBindHeaderHolder(UpdateViewHolder holder, String header) {
		UpdateView view = holder.getUpdateView();
		if(view != null) {
			view.setObject(header);
		}
	}

	public T getItemForPosition(int position) {
		if(sections.size() == 1 && !singleSectionHeader) {
			return sections.get(0).get(position);
		}

		int subPosition = 0;
		for(List<T> section: sections) {
			if(position == subPosition) {
				return null;
			}

			if(position <= (subPosition + section.size())) {
				return section.get(position - subPosition - 1);
			}

			subPosition += section.size() + 1;
		}

		return null;
	}
	public int getItemPosition(T item) {
		if(sections.size() == 1 && !singleSectionHeader) {
			return sections.get(0).indexOf(item);
		}

		int subPosition = 0;
		for(List<T> section: sections) {
			subPosition += section.size() + 1;

			int position = section.indexOf(item);
			if(position != -1) {
				return position + subPosition;
			}
		}

		return -1;
	}

	public void setOnItemClickedListener(OnItemClickedListener<T> onItemClickedListener) {
		this.onItemClickedListener = onItemClickedListener;
	}

	public void addSelected(T item) {
		selected.add(item);
	}
	public List<T> getSelected() {
		List<T> selected = new ArrayList<>();
		selected.addAll(this.selected);
		return selected;
	}

	public void clearSelected() {
		// TODO: This needs to work with multiple sections
		for(T item: selected) {
			int index = sections.get(0).indexOf(item);

			if(singleSectionHeader) {
				index++;
			}

			this.notifyItemChanged(index);
		}
		selected.clear();
	}

	public void removeItem(T item) {
		int subPosition = 0;
		for(List<T> section: sections) {
			if(sections.size() > 1 || singleSectionHeader) {
				subPosition++;
			}

			int index = section.indexOf(item);
			if (index != -1) {
				section.remove(item);
				notifyItemRemoved(subPosition + index);
				break;
			}

			subPosition += section.size();
		}
	}

	public abstract UpdateView.UpdateViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType);
	public abstract void onBindViewHolder(UpdateViewHolder holder, T item, int viewType);
	public abstract int getItemViewType(T item);
	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
	}
	public void setChecked(UpdateView updateView, boolean checked) {
		updateView.setChecked(checked);
	}

	private void startActionMode(final UpdateView.UpdateViewHolder<T> holder) {
		final UpdateView<T> updateView = holder.getUpdateView();
		if (context instanceof SubsonicFragmentActivity && currentActionMode == null) {
			final SubsonicFragmentActivity fragmentActivity = (SubsonicFragmentActivity) context;
			Toolbar toolbar = fragmentActivity.getActiveToolbar();

			toolbar.startActionMode(new ActionMode.Callback() {
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					currentActionMode = mode;
					fragmentActivity.onCreateOptionsMenu(menu);

					T item = holder.getItem();
					selected.add(item);
					setChecked(updateView, true);

					notifyDataSetChanged();
					mode.setTitle(context.getResources().getString(R.string.select_album_n_selected, selected.size()));
					return true;
				}

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return false;
				}

				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					if (fragmentActivity.onOptionsItemSelected(item)) {
						currentActionMode.finish();
						return true;
					} else {
						return false;
					}
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					currentActionMode = null;
					selected.clear();
					notifyDataSetChanged();
				}
			});
		}
	}

	public interface OnItemClickedListener<T> {
		void onItemClicked(T item);
		void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<T> updateView, T item);
		boolean onContextItemSelected(MenuItem menuItem, UpdateView<T> updateView, T item);
	}
}
