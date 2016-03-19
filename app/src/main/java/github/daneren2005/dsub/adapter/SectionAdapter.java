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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.MenuUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.BasicHeaderView;
import github.daneren2005.dsub.view.UpdateView;
import github.daneren2005.dsub.view.UpdateView.UpdateViewHolder;

public abstract class SectionAdapter<T> extends RecyclerView.Adapter<UpdateViewHolder<T>> {
	private static String TAG = SectionAdapter.class.getSimpleName();
	public static int VIEW_TYPE_HEADER = 0;
	public static String[] ignoredArticles;

	protected Context context;
	protected List<String> headers;
	protected List<List<T>> sections;
	protected boolean singleSectionHeader;
	protected OnItemClickedListener<T> onItemClickedListener;
	protected List<T> selected = new ArrayList<>();
	protected List<UpdateView> selectedViews = new ArrayList<>();
	protected ActionMode currentActionMode;
	protected boolean checkable = false;

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
						if (currentActionMode != null) {
							if(updateView.isCheckable()) {
								if (selected.contains(item)) {
									selected.remove(item);
									selectedViews.remove(updateView);
									setChecked(updateView, false);
								} else {
									selected.add(item);
									selectedViews.add(updateView);
									setChecked(updateView, true);
								}

								if (selected.isEmpty()) {
									currentActionMode.finish();
								} else {
									currentActionMode.setTitle(context.getResources().getString(R.string.select_album_n_selected, selected.size()));
								}
							}
						} else if (onItemClickedListener != null) {
							onItemClickedListener.onItemClicked(updateView, item);
						}
					}
				});

				View moreButton = updateView.findViewById(R.id.item_more);
				if (moreButton != null) {
					moreButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							try {
								final T item = holder.getItem();
								if (onItemClickedListener != null) {
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
							} catch(Exception e) {
								Log.w(TAG, "Failed to show popup", e);
							}
						}
					});

					if(checkable) {
						updateView.getChildAt(0).setOnLongClickListener(new View.OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								if(updateView.isCheckable()) {
									if (currentActionMode == null) {
										startActionMode(holder);
									} else {
										updateView.getChildAt(0).performClick();
									}
								}
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
			postBindView(updateView, item);
			holder.setItem(item);
			return;
		}

		int subPosition = 0;
		int subHeader = 0;
		for(List<T> section: sections) {
			boolean validHeader = headers.get(subHeader) != null;
			if(position == subPosition && validHeader) {
				onBindHeaderHolder(holder, headers.get(subHeader), subHeader);
				return;
			}

			int headerOffset = validHeader ? 1 : 0;
			if(position < (subPosition + section.size() + headerOffset)) {
				T item = section.get(position - subPosition - headerOffset);
				onBindViewHolder(holder, item, getItemViewType(item));

				postBindView(updateView, item);
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

	private void postBindView(UpdateView updateView, T item) {
		if(updateView.isCheckable()) {
			setChecked(updateView, selected.contains(item));
		}

		View moreButton = updateView.findViewById(R.id.item_more);
		if(moreButton != null) {
			if(onItemClickedListener != null) {
				PopupMenu popup = new PopupMenu(context, moreButton);
				Menu menu = popup.getMenu();
				onItemClickedListener.onCreateContextMenu(popup.getMenu(), popup.getMenuInflater(), updateView, item);
				if (menu.size() == 0) {
					moreButton.setVisibility(View.GONE);
				} else {
					moreButton.setVisibility(View.VISIBLE);
				}
			} else {
				moreButton.setVisibility(View.VISIBLE);
			}
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
	public void onBindHeaderHolder(UpdateViewHolder holder, String header, int sectionIndex) {
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
		}
		selected.clear();

		for(UpdateView updateView: selectedViews) {
			updateView.setChecked(false);
		}
	}

	public void moveItem(int from, int to) {
		List<T> section = sections.get(0);
		int max = section.size();
		if(to >= max) {
			to = max - 1;
		} else if(to < 0) {
			to = 0;
		}

		T moved = section.remove(from);
		section.add(to, moved);

		notifyItemMoved(from, to);
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
	public void onCreateActionModeMenu(Menu menu, MenuInflater menuInflater) {}

	private void startActionMode(final UpdateView.UpdateViewHolder<T> holder) {
		final UpdateView<T> updateView = holder.getUpdateView();
		if (context instanceof SubsonicFragmentActivity && currentActionMode == null) {
			final SubsonicFragmentActivity fragmentActivity = (SubsonicFragmentActivity) context;
			fragmentActivity.startSupportActionMode(new ActionMode.Callback() {
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					currentActionMode = mode;

					T item = holder.getItem();
					selected.add(item);
					selectedViews.add(updateView);
					setChecked(updateView, true);

					onCreateActionModeMenu(menu, mode.getMenuInflater());
					MenuUtil.hideMenuItems(context, menu, updateView);

					mode.setTitle(context.getResources().getString(R.string.select_album_n_selected, selected.size()));
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true)) {
						TypedValue typedValue = new TypedValue();
						Resources.Theme theme = context.getTheme();
						theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
						int colorPrimaryDark = typedValue.data;

						Window window = ((SubsonicFragmentActivity) context).getWindow();
						window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
						window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
						window.setStatusBarColor(colorPrimaryDark);
					}
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
					for (UpdateView<T> updateView : selectedViews) {
						updateView.setChecked(false);
					}
					selectedViews.clear();

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_COLOR_ACTION_BAR, true)) {
						Window window = ((SubsonicFragmentActivity) context).getWindow();
						window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					}
				}
			});
		}
	}
	public void stopActionMode() {
		if(currentActionMode != null) {
			currentActionMode.finish();
		}
	}

	public String getNameIndex(String name) {
		return getNameIndex(name, false);
	}
	public String getNameIndex(String name, boolean removeIgnoredArticles) {
		if(name == null) {
			return "*";
		}

		if(removeIgnoredArticles) {
			if (ignoredArticles == null) {
				SharedPreferences prefs = Util.getPreferences(context);
				String ignoredArticlesString = prefs.getString(Constants.CACHE_KEY_IGNORE, "The El La Los Las Le Les");
				ignoredArticles = ignoredArticlesString.split(" ");
			}

			name = name.toLowerCase();
			for (String article : ignoredArticles) {
				int index = name.indexOf(article.toLowerCase() + " ");
				if (index == 0) {
					name = name.substring(article.length() + 1);
				}
			}
		}

		String index = name.substring(0, 1).toUpperCase();
		if (!Character.isLetter(index.charAt(0))) {
			index = "#";
		}

		return index;
	}

	public interface OnItemClickedListener<T> {
		void onItemClicked(UpdateView<T> updateView, T item);
		void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<T> updateView, T item);
		boolean onContextItemSelected(MenuItem menuItem, UpdateView<T> updateView, T item);
	}
}
