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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.DrawableTint;
import github.daneren2005.dsub.view.BasicHeaderView;
import github.daneren2005.dsub.view.UpdateView;

public abstract class ExpandableSectionAdapter<T> extends SectionAdapter<T> {
	private static final String TAG = ExpandableSectionAdapter.class.getSimpleName();
	private static final int DEFAULT_VISIBLE = 4;
	private static final int EXPAND_TOGGLE = R.attr.select_server;
	private static final int COLLAPSE_TOGGLE = R.attr.select_tabs;

	protected List<Integer> sectionsDefaultVisible;
	protected List<List<T>> sectionsExtras;
	protected int expandToggleRes;
	protected int collapseToggleRes;

	protected ExpandableSectionAdapter() {}
	public ExpandableSectionAdapter(Context context, List<T> section) {
		List<List<T>> sections = new ArrayList<>();
		sections.add(section);

		init(context, Arrays.asList("Section"), sections, Arrays.asList((Integer) null));
	}
	public ExpandableSectionAdapter(Context context, List<String> headers, List<List<T>> sections) {
		init(context, headers, sections, null);
	}
	public ExpandableSectionAdapter(Context context, List<String> headers, List<List<T>> sections, List<Integer> sectionsDefaultVisible) {
		init(context, headers, sections, sectionsDefaultVisible);
	}
	protected void init(Context context, List<String> headers, List<List<T>> fullSections, List<Integer> sectionsDefaultVisible) {
		this.context = context;
		this.headers = headers;
		this.sectionsDefaultVisible = sectionsDefaultVisible;
		if(sectionsDefaultVisible == null) {
			sectionsDefaultVisible = new ArrayList<>(fullSections.size());
			for(int i = 0; i < fullSections.size(); i++) {
				sectionsDefaultVisible.add(DEFAULT_VISIBLE);
			}
		}

		this.sections = new ArrayList<>();
		this.sectionsExtras = new ArrayList<>();
		int i = 0;
		for(List<T> fullSection: fullSections) {
			List<T> visibleSection = new ArrayList<>();

			Integer defaultVisible = sectionsDefaultVisible.get(i);
			if(defaultVisible == null || defaultVisible >= fullSection.size()) {
				visibleSection.addAll(fullSection);
				this.sectionsExtras.add(null);
			} else {
				visibleSection.addAll(fullSection.subList(0, defaultVisible));
				this.sectionsExtras.add(fullSection.subList(defaultVisible, fullSection.size()));
			}
			this.sections.add(visibleSection);

			i++;
		}

		expandToggleRes = DrawableTint.getDrawableRes(context, EXPAND_TOGGLE);
		collapseToggleRes = DrawableTint.getDrawableRes(context, COLLAPSE_TOGGLE);
	}

	@Override
	public UpdateView.UpdateViewHolder onCreateHeaderHolder(ViewGroup parent) {
		return new UpdateView.UpdateViewHolder(new BasicHeaderView(context, R.layout.expandable_header));
	}

	@Override
	public void onBindHeaderHolder(UpdateView.UpdateViewHolder holder, String header, final int sectionIndex) {
		UpdateView view = holder.getUpdateView();
		ImageView toggleSelectionView = (ImageView) view.findViewById(R.id.item_select);

		List<T> visibleSelection = sections.get(sectionIndex);
		List<T> sectionExtras = sectionsExtras.get(sectionIndex);

		if(sectionExtras != null && !sectionExtras.isEmpty()) {
			toggleSelectionView.setVisibility(View.VISIBLE);
			toggleSelectionView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					List<T> visibleSelection = sections.get(sectionIndex);
					List<T> sectionExtras = sectionsExtras.get(sectionIndex);

					// Update icon
					int selectToggleAttr;
					if (!visibleSelection.contains(sectionExtras.get(0))) {
						selectToggleAttr = COLLAPSE_TOGGLE;

						// Update how many are displayed
						int lastIndex = getItemPosition(visibleSelection.get(visibleSelection.size() - 1));
						visibleSelection.addAll(sectionExtras);
						notifyItemRangeInserted(lastIndex, sectionExtras.size());
					} else {
						selectToggleAttr = EXPAND_TOGGLE;

						// Update how many are displayed
						visibleSelection.removeAll(sectionExtras);
						int lastIndex = getItemPosition(visibleSelection.get(visibleSelection.size() - 1));
						notifyItemRangeRemoved(lastIndex, sectionExtras.size());
					}

					((ImageView) v).setImageResource(DrawableTint.getDrawableRes(context, selectToggleAttr));
				}
			});

			int selectToggleAttr;
			if (!visibleSelection.contains(sectionExtras.get(0))) {
				selectToggleAttr = EXPAND_TOGGLE;
			} else {
				selectToggleAttr = COLLAPSE_TOGGLE;
			}

			toggleSelectionView.setImageResource(DrawableTint.getDrawableRes(context, selectToggleAttr));
		} else {
			toggleSelectionView.setVisibility(View.GONE);
		}

		if(view != null) {
			view.setObject(header);
		}
	}
}
