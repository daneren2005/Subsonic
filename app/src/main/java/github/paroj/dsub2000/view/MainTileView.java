package github.paroj.dsub2000.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import github.paroj.dsub2000.R;

/**
 * A tile on the overview page: an icon followed by the name of the list it opens.
 * The object is the string resource of that name.
 */
public class MainTileView extends UpdateView<Integer> {
	private final ImageView iconView;
	private final TextView titleView;

	public MainTileView(Context context) {
		super(context, false);
		LayoutInflater.from(context).inflate(R.layout.main_tile_item, this, true);

		iconView = findViewById(R.id.item_icon);
		titleView = findViewById(R.id.basic_count_name);
	}

	public static void bind(ImageView iconView, TextView titleView, int item) {
		titleView.setText(item);
		iconView.setImageResource(getIcon(item));
	}

	public static int getIcon(int item) {
		if(item == R.string.main_albums_newest || item == R.string.main_songs_newest) {
			return R.drawable.ic_main_recently_added;
		} else if(item == R.string.main_albums_recent || item == R.string.main_songs_recent) {
			return R.drawable.ic_main_recently_played;
		} else if(item == R.string.main_albums_frequent || item == R.string.main_songs_frequent
				|| item == R.string.main_songs_top_played) {
			return R.drawable.ic_main_most_played;
		} else if(item == R.string.main_albums_highest || item == R.string.main_songs_highest) {
			return R.drawable.ic_action_rating_good;
		} else if(item == R.string.main_albums_starred || item == R.string.main_songs_starred) {
			return R.drawable.ic_toggle_star;
		} else if(item == R.string.main_albums_random || item == R.string.main_songs_random) {
			return R.drawable.ic_menu_shuffle;
		} else if(item == R.string.main_albums_alphabetical || item == R.string.main_songs_alphabetical) {
			return R.drawable.ic_main_alphabetical;
		} else if(item == R.string.main_albums_genres || item == R.string.main_songs_genres) {
			return R.drawable.ic_main_genres;
		} else if(item == R.string.main_albums_year || item == R.string.main_songs_year) {
			return R.drawable.ic_action_calendar;
		} else {
			return R.drawable.ic_action_song;
		}
	}

	@Override
	protected void setObjectImpl(Integer item) {
		bind(iconView, titleView, item);
	}
}
