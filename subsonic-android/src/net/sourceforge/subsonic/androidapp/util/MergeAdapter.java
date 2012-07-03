/***
 Copyright (c) 2008-2009 CommonsWare, LLC
 Portions (c) 2009 Google, Inc.

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package net.sourceforge.subsonic.androidapp.util;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Adapter that merges multiple child adapters and views
 * into a single contiguous whole.
 * <p/>
 * Adapters used as pieces within MergeAdapter must
 * have view type IDs monotonically increasing from 0. Ideally,
 * adapters also have distinct ranges for their row ids, as
 * returned by getItemId().
 */
public class MergeAdapter extends BaseAdapter {

    private final CascadeDataSetObserver observer = new CascadeDataSetObserver();
    private final ArrayList<ListAdapter> pieces = new ArrayList<ListAdapter>();

    /**
     * Stock constructor, simply chaining to the superclass.
     */
    public MergeAdapter() {
        super();
    }

    /**
     * Adds a new adapter to the roster of things to appear
     * in the aggregate list.
     *
     * @param adapter Source for row views for this section
     */
    public void addAdapter(ListAdapter adapter) {
        pieces.add(adapter);
        adapter.registerDataSetObserver(observer);
    }

    public void removeAdapter(ListAdapter adapter) {
        adapter.unregisterDataSetObserver(observer);
        pieces.remove(adapter);
    }

    /**
    * Adds a new View to the roster of things to appear
    * in the aggregate list.
    *
    * @param view Single view to add
    */
    public ListAdapter addView(View view) {
        return addView(view, false);
    }

    /**
     * Adds a new View to the roster of things to appear
     * in the aggregate list.
     *
     * @param view    Single view to add
     * @param enabled false if views are disabled, true if enabled
     */
    public ListAdapter addView(View view, boolean enabled) {
        return addViews(Arrays.asList(view), enabled);
    }

    /**
     * Adds a list of views to the roster of things to appear
     * in the aggregate list.
     *
     * @param views List of views to add
     */
    public ListAdapter addViews(List<View> views) {
        return addViews(views, false);
    }

    /**
     * Adds a list of views to the roster of things to appear
     * in the aggregate list.
     *
     * @param views   List of views to add
     * @param enabled false if views are disabled, true if enabled
     */
    public ListAdapter addViews(List<View> views, boolean enabled) {
        ListAdapter adapter = enabled ? new EnabledSackAdapter(views) : new SackOfViewsAdapter(views);
        addAdapter(adapter);
        return adapter;
    }

    /**
     * Get the data item associated with the specified
     * position in the data set.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public Object getItem(int position) {
        for (ListAdapter piece : pieces) {
            int size = piece.getCount();

            if (position < size) {
                return (piece.getItem(position));
            }

            position -= size;
        }

        return (null);
    }

    /**
     * How many items are in the data set represented by this
     * Adapter.
     */
    @Override
    public int getCount() {
        int total = 0;

        for (ListAdapter piece : pieces) {
            total += piece.getCount();
        }

        return (total);
    }

    /**
     * Returns the number of types of Views that will be
     * created by getView().
     */
    @Override
    public int getViewTypeCount() {
        int total = 0;

        for (ListAdapter piece : pieces) {
            total += piece.getViewTypeCount();
        }

        return (Math.max(total, 1));        // needed for setListAdapter() before content add'
    }

    /**
     * Get the type of View that will be created by getView()
     * for the specified item.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public int getItemViewType(int position) {
        int typeOffset = 0;
        int result = -1;

        for (ListAdapter piece : pieces) {
            int size = piece.getCount();

            if (position < size) {
                result = typeOffset + piece.getItemViewType(position);
                break;
            }

            position -= size;
            typeOffset += piece.getViewTypeCount();
        }

        return (result);
    }

    /**
     * Are all items in this ListAdapter enabled? If yes it
     * means all items are selectable and clickable.
     */
    @Override
    public boolean areAllItemsEnabled() {
        return (false);
    }

    /**
     * Returns true if the item at the specified position is
     * not a separator.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public boolean isEnabled(int position) {
        for (ListAdapter piece : pieces) {
            int size = piece.getCount();

            if (position < size) {
                return (piece.isEnabled(position));
            }

            position -= size;
        }

        return (false);
    }

    /**
     * Get a View that displays the data at the specified
     * position in the data set.
     *
     * @param position    Position of the item whose data we want
     * @param convertView View to recycle, if not null
     * @param parent      ViewGroup containing the returned View
     */
    @Override
    public View getView(int position, View convertView,
                        ViewGroup parent) {
        for (ListAdapter piece : pieces) {
            int size = piece.getCount();

            if (position < size) {

                return (piece.getView(position, convertView, parent));
            }

            position -= size;
        }

        return (null);
    }

    /**
     * Get the row id associated with the specified position
     * in the list.
     *
     * @param position Position of the item whose data we want
     */
    @Override
    public long getItemId(int position) {
        for (ListAdapter piece : pieces) {
            int size = piece.getCount();

            if (position < size) {
                return (piece.getItemId(position));
            }

            position -= size;
        }

        return (-1);
    }

    private static class EnabledSackAdapter extends SackOfViewsAdapter {
        public EnabledSackAdapter(List<View> views) {
            super(views);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return (true);
        }

        @Override
        public boolean isEnabled(int position) {
            return (true);
        }
    }

    private class CascadeDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			notifyDataSetInvalidated();
		}
	}
}

