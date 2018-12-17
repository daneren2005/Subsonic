package github.vrih.xsub.util;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import org.eclipse.jetty.util.ArrayQueue;

import java.util.Queue;

import github.vrih.xsub.adapter.SectionAdapter;
import github.vrih.xsub.fragments.SubsonicFragment;
import github.vrih.xsub.service.DownloadFile;
import github.vrih.xsub.service.DownloadService;
import github.vrih.xsub.view.SongView;
import github.vrih.xsub.view.UpdateView;

public class DownloadFileItemHelperCallback extends ItemTouchHelper.SimpleCallback {
	private static final String TAG = DownloadFileItemHelperCallback.class.getSimpleName();

	private final SubsonicFragment fragment;
	private final boolean mainList;

	private BackgroundTask pendingTask = null;
	private final Queue pendingOperations = new ArrayQueue();

	public DownloadFileItemHelperCallback(SubsonicFragment fragment, boolean mainList) {
		super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
		this.fragment = fragment;
		this.mainList = mainList;
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder fromHolder, RecyclerView.ViewHolder toHolder) {
		int from = fromHolder.getAdapterPosition();
		int to = toHolder.getAdapterPosition();
		getSectionAdapter().moveItem(from, to);

		synchronized (pendingOperations) {
			pendingOperations.add(new Pair<>(from, to));
			updateDownloadService();
		}
		return true;
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
		SongView songView = (SongView) ((UpdateView.UpdateViewHolder) viewHolder).getUpdateView();
		DownloadFile downloadFile = songView.getDownloadFile();

		getSectionAdapter().removeItem(downloadFile);
		synchronized (pendingOperations) {
			pendingOperations.add(downloadFile);
			updateDownloadService();
		}
	}

	private DownloadService getDownloadService() {
		return fragment.getDownloadService();
	}
	private SectionAdapter getSectionAdapter() {
		return fragment.getCurrentAdapter();
	}

	private void updateDownloadService() {
		if(pendingTask == null) {
			final DownloadService downloadService = getDownloadService();
			if(downloadService == null) {
				return;
			}

			pendingTask = new SilentBackgroundTask<Void>(downloadService) {
				@Override
				protected Void doInBackground() throws Throwable {
					boolean running = true;
					while(running) {
						Object nextOperation = null;
						synchronized (pendingOperations) {
							if(!pendingOperations.isEmpty()) {
								nextOperation = pendingOperations.remove();
							}
						}

						if(nextOperation != null) {
							if(nextOperation instanceof Pair) {
								Pair<Integer, Integer> swap = (Pair) nextOperation;
								downloadService.swap(mainList, swap.getFirst(), swap.getSecond());
							} else if(nextOperation instanceof DownloadFile) {
								DownloadFile downloadFile = (DownloadFile) nextOperation;
								if(mainList) {
									downloadService.remove(downloadFile);
								} else {
									downloadService.removeBackground(downloadFile);
								}
							}
						} else {
							running = false;
						}
					}

					synchronized (pendingOperations) {
						pendingTask = null;

						// Start a task if this is non-empty.  Means someone added while we were running operations
						if(!pendingOperations.isEmpty()) {
							updateDownloadService();
						}
					}
					return null;
				}
			};
			pendingTask.execute();
		}
	}
}
