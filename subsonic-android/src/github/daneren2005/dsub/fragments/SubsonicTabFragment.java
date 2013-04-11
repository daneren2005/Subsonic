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

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.activity.SubsonicTabActivity;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.ImageLoader;

public class SubsonicTabFragment extends SherlockFragment {
	private static final String TAG = SubsonicTabActivity.class.getSimpleName();
	protected SubsonicActivity context;
	protected View rootView;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = (SubsonicActivity)activity;
	}

	public DownloadService getDownloadService() {
		return context != null ? context.getDownloadService() : null;
	}

	protected void refresh() {

	}

	protected void exit() {
		context.stopService(new Intent(context, DownloadServiceImpl.class));
		context.finish();
	}

	public void setProgressVisible(boolean visible) {
		View view = rootView.findViewById(R.id.tab_progress);
		if (view != null) {
			view.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void updateProgress(String message) {
		TextView view = (TextView) rootView.findViewById(R.id.tab_progress_message);
		if (view != null) {
			view.setText(message);
		}
	}

	protected synchronized ImageLoader getImageLoader() {
		return context.getImageLoader();
	}
	public synchronized static ImageLoader getStaticImageLoader(Context context) {
		return SubsonicActivity.getStaticImageLoader(context);
	}
	
	public void setPrimaryFragment(boolean primary) {
		if(primary) {
			setHasOptionsMenu(true);
		} else {
			setHasOptionsMenu(false);
		}
	}

	protected void setTitle(CharSequence title) {
		context.setTitle(title);
	}
	protected void setTitle(int title) {
		context.setTitle(title);
	}
}
