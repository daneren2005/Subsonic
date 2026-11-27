package github.paroj.dsub2000.activity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import github.paroj.dsub2000.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The bars have to look final during the slide, so that nothing jumps when it ends. */
public class NowPlayingPanelTest {
    @Rule
    public ActivityTestRule<SubsonicFragmentActivity> rule = new ActivityTestRule<>(SubsonicFragmentActivity.class);

    private SubsonicFragmentActivity activity;
    private SlidingUpPanelLayout panel;
    private Toolbar mainToolbar;
    private Toolbar nowPlayingToolbar;
    private View bottomBar;

    @Before
    public void setUp() {
        activity = rule.getActivity();
        panel = activity.findViewById(R.id.slide_up_panel);
        mainToolbar = activity.findViewById(R.id.main_toolbar);
        nowPlayingToolbar = activity.findViewById(R.id.now_playing_toolbar);
        bottomBar = activity.findViewById(R.id.bottom_bar);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void toolbarsAreReadyBeforeTheSlideEnds() throws Exception {
        Object mainIcon = mainToolbar.getNavigationIcon();
        CharSequence mainTitle = mainToolbar.getTitle();
        assertNotNull(mainIcon);

        AtomicReference<String> opening = new AtomicReference<>();
        slideTo(PanelState.EXPANDED, offset -> offset > 0.75f, () -> opening.set(
                "alpha=" + (nowPlayingToolbar.getAlpha() > 0f)
                        + " toolbar=" + (nowPlayingToolbar.getVisibility() == View.VISIBLE)
                        + " bar=" + (bottomBar.getVisibility() == View.VISIBLE)
                        + " menu=" + (nowPlayingToolbar.getMenu().size() > 0)
                        + " title=" + nowPlayingToolbar.getTitle()
                        + " icon=" + (nowPlayingToolbar.getNavigationIcon() != null)
                        + " mainIcon=" + (mainToolbar.getNavigationIcon() == mainIcon)));
        assertEquals("alpha=true toolbar=true bar=false menu=true title="
                + activity.getString(R.string.button_bar_now_playing) + " icon=true mainIcon=true", opening.get());
        assertEquals(1f, nowPlayingToolbar.getAlpha(), 0f);
        assertEquals(View.INVISIBLE, bottomBar.getVisibility());
        assertSame(mainIcon, mainToolbar.getNavigationIcon());

        AtomicReference<String> closing = new AtomicReference<>();
        slideTo(PanelState.COLLAPSED, offset -> offset < 0.25f, () -> closing.set(
                "toolbar=" + (nowPlayingToolbar.getVisibility() == View.VISIBLE)
                        + " bar=" + (bottomBar.getVisibility() == View.VISIBLE)
                        + " menu=" + (mainToolbar.getMenu().size() > 0)
                        + " title=" + mainToolbar.getTitle()
                        + " mainIcon=" + (mainToolbar.getNavigationIcon() == mainIcon)));
        assertEquals("toolbar=false bar=true menu=true title=" + mainTitle + " mainIcon=true", closing.get());
        assertEquals(1f, bottomBar.getAlpha(), 0f);
        assertEquals(View.INVISIBLE, nowPlayingToolbar.getVisibility());
        assertEquals(mainTitle, mainToolbar.getTitle());
    }

    @Test
    public void nowPlayingHomeButtonClosesThePanelInsteadOfOpeningTheDrawer() throws Exception {
        slideTo(PanelState.EXPANDED, offset -> false, () -> { });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        CountDownLatch collapsed = collapsedLatch();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> navigationButton(nowPlayingToolbar).performClick());
        assertTrue("Panel collapses", collapsed.await(10, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
        assertFalse("Drawer stays closed", drawer.isDrawerOpen(activity.findViewById(R.id.left_drawer)));
    }

    private interface OffsetCondition {
        boolean test(float offset);
    }

    private void slideTo(PanelState target, OffsetCondition when, Runnable probe) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        boolean[] probed = {false};
        SlidingUpPanelLayout.PanelSlideListener listener = new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float offset) {
                if(!probed[0] && when.test(offset)) {
                    probed[0] = true;
                    probe.run();
                }
            }

            @Override
            public void onPanelStateChanged(View view, PanelState previous, PanelState state) {
                if(state == target) {
                    done.countDown();
                }
            }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            panel.addPanelSlideListener(listener);
            panel.setPanelState(target);
        });
        assertTrue("Panel reaches " + target, done.await(10, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> panel.removePanelSlideListener(listener));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private CountDownLatch collapsedLatch() {
        CountDownLatch latch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> panel.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelStateChanged(View view, PanelState previous, PanelState state) {
                if(state == PanelState.COLLAPSED) {
                    latch.countDown();
                }
            }
        }));
        return latch;
    }

    private static View navigationButton(ViewGroup toolbar) {
        for(int i = 0; i < toolbar.getChildCount(); i++) {
            if(toolbar.getChildAt(i) instanceof ImageButton) {
                return toolbar.getChildAt(i);
            }
        }
        throw new AssertionError("Navigation button not found");
    }
}
