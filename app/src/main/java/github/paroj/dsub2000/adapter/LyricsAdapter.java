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
 */
package github.paroj.dsub2000.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.domain.LyricsLine;

/**
 * Displays lyrics one line per row.  When the lyrics are synced the line
 * matching the current playback position is highlighted, and tapping a line
 * seeks playback to it.
 */
public class LyricsAdapter extends RecyclerView.Adapter<LyricsAdapter.LineViewHolder> {
	private static final float ACTIVE_ALPHA = 1.0f;
	private static final float INACTIVE_ALPHA = 0.5f;

	public interface OnLineClickListener {
		void onLineClick(LyricsLine line);
	}

	private List<LyricsLine> lines;
	private boolean synced;
	private int activeLine = -1;
	private OnLineClickListener onLineClickListener;

	public LyricsAdapter(List<LyricsLine> lines, boolean synced) {
		this.lines = lines;
		this.synced = synced;
	}

	public void setOnLineClickListener(OnLineClickListener listener) {
		this.onLineClickListener = listener;
	}

	/**
	 * Swap in the lyrics of another song without recreating the adapter.
	 */
	public void setLyrics(List<LyricsLine> lines, boolean synced) {
		this.lines = lines == null ? new ArrayList<LyricsLine>() : lines;
		this.synced = synced;
		this.activeLine = -1;
		notifyDataSetChanged();
	}

	@Override
	public LineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lyrics_line, parent, false);
		return new LineViewHolder(view);
	}

	@Override
	public void onBindViewHolder(LineViewHolder holder, int position) {
		final LyricsLine line = lines.get(position);
		holder.textView.setText(line.getValue());

		// Unsynced lyrics have no current line, so everything stays fully visible.
		boolean highlight = synced && position == activeLine;
		holder.textView.setAlpha((!synced || highlight) ? ACTIVE_ALPHA : INACTIVE_ALPHA);
		holder.textView.setTypeface(null, highlight ? Typeface.BOLD : Typeface.NORMAL);

		boolean seekable = synced && line.getStart() != null && onLineClickListener != null;
		holder.itemView.setClickable(seekable);
		if(seekable) {
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onLineClickListener.onLineClick(line);
				}
			});
		} else {
			holder.itemView.setOnClickListener(null);
		}
	}

	@Override
	public int getItemCount() {
		return lines.size();
	}

	public int getActiveLine() {
		return activeLine;
	}

	public void setActiveLine(int line) {
		if(line == activeLine) {
			return;
		}

		int previous = activeLine;
		activeLine = line;
		if(previous >= 0 && previous < lines.size()) {
			notifyItemChanged(previous);
		}
		if(activeLine >= 0 && activeLine < lines.size()) {
			notifyItemChanged(activeLine);
		}
	}

	public static class LineViewHolder extends RecyclerView.ViewHolder {
		private final TextView textView;

		public LineViewHolder(View itemView) {
			super(itemView);
			this.textView = (TextView) itemView.findViewById(R.id.lyrics_line);
		}
	}
}
