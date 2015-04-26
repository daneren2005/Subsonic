package github.daneren2005.dsub.activity;

import github.daneren2005.dsub.R;
import android.test.*;
import android.view.View;

public class DownloadActivityTest extends
		ActivityInstrumentationTestCase2<DownloadActivity> {

	private DownloadActivity activity;

	public DownloadActivityTest() {
		super(DownloadActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	    activity = getActivity();
	}

	/**
	 * Test the main layout.
	 */
	public void testLayout() {
		View view = activity.findViewById(R.layout.download_activity);
		assertNotNull(view);
		assertNotNull(view.findViewById(R.layout.download_activity));
		assertNotNull(activity.findViewById(R.id.fragment_container));
	}

}
