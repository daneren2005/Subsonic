package github.daneren2005.dsub.adapter;


import android.content.Context;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.domain.MusicDirectory.Entry;


public class EntryGridAdapterTest extends AndroidTestCase {
	private EntryGridAdapter mAdapter;


	public EntryGridAdapterTest() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testRemoveAt() {
		Entry a = new Entry("a");
		Entry c = new Entry("c");

		List<Entry> section = new ArrayList<>(Arrays.asList(
				a,
				new Entry("b"),
				c,
				new Entry("d"),
				new Entry("e")));
		mAdapter = new EntryGridAdapter(null, section, null, false);

		mAdapter.removeAt(Arrays.asList(1, 3, 4));
		assertEquals(new ArrayList<>(Arrays.asList(a, c)), section);
	}
}
