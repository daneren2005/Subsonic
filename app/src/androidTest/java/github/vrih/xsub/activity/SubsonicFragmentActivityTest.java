package github.vrih.xsub.activity;

import github.vrih.xsub.R;

public class SubsonicFragmentActivityTest extends
		ActivityInstrumentationTestCase2<SubsonicFragmentActivity> {

	private SubsonicFragmentActivity activity;

	public SubsonicFragmentActivityTest() {
		super(SubsonicFragmentActivity.class);
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
		assertNotNull(activity.findViewById(R.id.content_frame));
	}
	
	/**
	 * Test the bottom bar.
	 */
	public void testBottomBar() {
		assertNotNull(activity.findViewById(R.id.bottom_bar));
	}
}
