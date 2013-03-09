package github.daneren2005.dsub.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.view.EntryAdapter;
import java.util.List;
import com.mobeta.android.dslv.*;

public class SelectDirectoryFragment extends SubsonicTabFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectDirectoryFragment.class.getSimpleName();

	private DragSortListView entryList;
	private View footer;
	private View emptyView;
	private boolean hideButtons = false;
	private Button moreButton;
	private Boolean licenseValid;
	private boolean showHeader = true;
	private EntryAdapter entryAdapter;
	private List<MusicDirectory.Entry> entries;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.select_album, container, false);

		entryList = (DragSortListView) rootView.findViewById(R.id.select_album_entries);
		footer = LayoutInflater.from(context).inflate(R.layout.select_album_footer, entryList, false);
		entryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		entryList.setOnItemClickListener(this);
		entryList.setDropListener(new DragSortListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				int max = entries.size();
				if(to >= max) {
					to = max - 1;
				}
				else if(to < 0) {
					to = 0;
				}
				entries.add(to, entries.remove(from));
				entryAdapter.notifyDataSetChanged();
			}
		});

		moreButton = (Button) footer.findViewById(R.id.select_album_more);
		emptyView = rootView.findViewById(R.id.select_album_empty);

		registerForContextMenu(entryList);

		return rootView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position >= 0) {
			MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
			/*if (entry.isDirectory()) {
				Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
				intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
				Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
			} else if (entry.isVideo()) {
				if(entryExists(entry)) {
					playExternalPlayer(entry);
				} else {
					streamExternalPlayer(entry);
				}
			}*/
		}
	}

	@Override
	protected void refresh() {
		// load(true);
	}
}