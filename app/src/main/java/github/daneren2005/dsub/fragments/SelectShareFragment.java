/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.fragments;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.SectionAdapter;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.adapter.ShareAdapter;
import github.daneren2005.dsub.view.UpdateView;

public class SelectShareFragment extends SelectRecyclerFragment<Share> {
	private static final String TAG = SelectShareFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(Menu menu, MenuInflater menuInflater, UpdateView<Share> updateView, Share item) {
		menuInflater.inflate(R.menu.select_share_context, menu);
		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem, UpdateView<Share> updateView, Share share) {
		switch (menuItem.getItemId()) {
			case R.id.share_menu_share:
				shareExternal(share);
				break;
			case R.id.share_menu_info:
				displayShareInfo(share);
				break;
			case R.id.share_menu_delete:
				deleteShare(share);
				break;
			case R.id.share_update_info:
				updateShareInfo(share);
				break;
		}

		return true;
	}

	@Override
	public int getOptionsMenu() {
		return R.menu.abstract_top_menu;
	}

	@Override
	public SectionAdapter getAdapter(List<Share> objs) {
		return new ShareAdapter(context, objs, this);
	}

	@Override
	public List<Share> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception {
		return musicService.getShares(context, listener);
	}

	@Override
	public int getTitleResource() {
		return R.string.button_bar_shares;
	}

	@Override
	public void onItemClicked(UpdateView<Share> updateView, Share share) {
		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.INTENT_EXTRA_NAME_SHARE, share);
		fragment.setArguments(args);

		replaceFragment(fragment);
	}

	private void displayShareInfo(final Share share) {
		List<Integer> headers = new ArrayList<>();
		List<String> details = new ArrayList<>();

		headers.add(R.string.details_title);
		details.add(share.getName());

		headers.add(R.string.details_owner);
		details.add(share.getUsername());

		headers.add(R.string.details_description);
		details.add(share.getDescription());

		headers.add(R.string.details_url);
		details.add(share.getUrl());

		headers.add(R.string.details_created);
		details.add(Util.formatDate(share.getCreated()));

		headers.add(R.string.details_last_played);
		details.add(Util.formatDate(share.getLastVisited()));

		headers.add(R.string.details_expiration);
		details.add(Util.formatDate(share.getExpires(), false));

		headers.add(R.string.details_played_count);
		details.add(Long.toString(share.getVisitCount()));

		Util.showDetailsDialog(context, R.string.details_title_playlist, headers, details);
	}

	private void updateShareInfo(final Share share) {
		View dialogView = context.getLayoutInflater().inflate(R.layout.update_share, null);
		final EditText nameBox = (EditText)dialogView.findViewById(R.id.get_share_name);
		final DatePicker expireBox = (DatePicker)dialogView.findViewById(R.id.get_share_expire);
		final CheckBox noExpiresBox = (CheckBox)dialogView.findViewById(R.id.get_share_no_expire);

		nameBox.setText(share.getDescription());
		Date expires = share.getExpires();
		if(expires != null) {
			expireBox.updateDate(expires.getYear() + 1900, expires.getMonth(), expires.getDate());
		}

		boolean noExpires = share.getExpires() == null;
		if(noExpires) {
			expireBox.setEnabled(false);
			noExpiresBox.setChecked(true);
		}

		noExpiresBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				expireBox.setEnabled(!isChecked);
			}
		});

		new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.playlist_update_info)
				.setView(dialogView)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new LoadingTask<Void>(context, false) {
							@Override
							protected Void doInBackground() throws Throwable {
								Long expiresIn = 0L;
								if (!noExpiresBox.isChecked()) {
									Date expires = new Date(expireBox.getYear() - 1900, expireBox.getMonth(), expireBox.getDayOfMonth());
									expiresIn = expires.getTime();
								}

								MusicService musicService = MusicServiceFactory.getMusicService(context);
								musicService.updateShare(share.getId(), nameBox.getText().toString(), expiresIn, context, null);
								return null;
							}

							@Override
							protected void done(Void result) {
								refresh();
								Util.toast(context, context.getResources().getString(R.string.share_updated_info, share.getName()));
							}

							@Override
							protected void error(Throwable error) {
								String msg;
								if (error instanceof OfflineException || error instanceof ServerTooOldException) {
									msg = getErrorMessage(error);
								} else {
									msg = context.getResources().getString(R.string.share_updated_info_error, share.getName()) + " " + getErrorMessage(error);
								}

								Util.toast(context, msg, false);
							}
						}.execute();
					}

				})
				.setNegativeButton(R.string.common_cancel, null)
				.show();
	}

	private void deleteShare(final Share share) {
		Util.confirmDialog(context, R.string.common_delete, share.getName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new LoadingTask<Void>(context, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						musicService.deleteShare(share.getId(), context, null);
						return null;
					}

					@Override
					protected void done(Void result) {
						adapter.removeItem(share);
						Util.toast(context, context.getResources().getString(R.string.share_deleted, share.getName()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.share_deleted_error, share.getName()) + " " + getErrorMessage(error);
						}

						Util.toast(context, msg, false);
					}
				}.execute();
			}
		});
	}
}
