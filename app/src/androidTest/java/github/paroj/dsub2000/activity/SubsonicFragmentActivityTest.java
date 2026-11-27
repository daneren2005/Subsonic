package github.paroj.dsub2000.activity;

import github.paroj.dsub2000.R;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SubsonicFragmentActivityTest {
	private SubsonicFragmentActivity activity;
	@Rule
	public ActivityTestRule<SubsonicFragmentActivity> activityRule = new ActivityTestRule<>(SubsonicFragmentActivity.class, true, true);

	@Before
	public void setUp() throws Exception {
	    activity = activityRule.getActivity();
	}

	/**
	 * Test the main layout.
	 */
	@Test
	public void testLayout() {
		assertNotNull(activity.findViewById(R.id.content_frame));
	}
	
	/**
	 * Test the bottom bar.
	 */
	@Test
	public void testBottomBar() {
		assertNotNull(activity.findViewById(R.id.bottom_bar));
	}
}
