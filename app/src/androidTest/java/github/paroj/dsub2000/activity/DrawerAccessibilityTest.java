package github.paroj.dsub2000.activity;

import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.google.android.material.navigation.NavigationView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import github.paroj.dsub2000.R;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NavigationMenuPresenter stores the adapter position in each menu item view's accessibility
 * delegate.  After the menu shrank, the still attached views report their old position until a
 * layout pass rebinds them, which made an accessibility prefetch of the drawer throw an
 * IndexOutOfBoundsException from getItemViewType().
 */
public class DrawerAccessibilityTest {
	@Rule
	public ActivityTestRule<SubsonicFragmentActivity> rule = new ActivityTestRule<>(SubsonicFragmentActivity.class);

	private SubsonicFragmentActivity activity;
	private NavigationView drawer;
	private RecyclerView menuList;

	@Before
	public void setUp() {
		activity = rule.getActivity();
		drawer = activity.findViewById(R.id.left_drawer);
		menuList = findRecyclerView(drawer);
		assertNotNull("drawer menu list", menuList);
		InstrumentationRegistry.getInstrumentation().waitForIdleSync();
	}

	@Test
	public void staleMenuItemsDoNotReportTheirOldPosition() throws Exception {
		final boolean[] menuShrank = {false};
		final Throwable[] error = {null};

		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
			// The server list is shorter than the tab list, so switching shrinks the menu
			drawer.getHeaderView(0).performClick();

			// Until the next layout pass the views of the removed items are still attached.
			// An accessibility prefetch would query them at exactly this point.
			for(int i = 0; i < menuList.getChildCount(); i++) {
				View child = menuList.getChildAt(i);
				if(menuList.getChildViewHolder(child).getLayoutPosition() >= menuList.getAdapter().getItemCount()) {
					menuShrank[0] = true;
				}

				try {
					child.createAccessibilityNodeInfo();
				} catch(Throwable t) {
					error[0] = t;
				}
			}
		});

		assertTrue("menu shrank before it was laid out", menuShrank[0]);
		if(error[0] != null) {
			throw new AssertionError("stale menu item reported its old accessibility position", error[0]);
		}
	}

	private static RecyclerView findRecyclerView(ViewGroup group) {
		for(int i = 0; i < group.getChildCount(); i++) {
			View child = group.getChildAt(i);
			if(child instanceof RecyclerView) {
				return (RecyclerView) child;
			}
			if(child instanceof ViewGroup) {
				RecyclerView found = findRecyclerView((ViewGroup) child);
				if(found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
