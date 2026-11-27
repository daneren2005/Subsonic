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

package github.paroj.dsub2000.fragments;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.adapter.SectionAdapter;
import github.paroj.dsub2000.domain.Share;
import github.paroj.dsub2000.service.MusicService;
import github.paroj.dsub2000.service.MusicServiceFactory;
import github.paroj.dsub2000.service.OfflineException;
import github.paroj.dsub2000.service.ServerTooOldException;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.LoadingTask;
import github.paroj.dsub2000.util.ProgressListener;
import github.paroj.dsub2000.util.Util;
import github.paroj.dsub2000.adapter.ShareAdapter;
import github.paroj.dsub2000.view.UpdateView;

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
		final TextView expireBox = (TextView)dialogView.findViewById(R.id.get_share_expire);
		final TextInputLayout expireLayout = (TextInputLayout)dialogView.findViewById(R.id.get_share_expire_layout);
		final MaterialSwitch noExpiresBox = (MaterialSwitch)dialogView.findViewById(R.id.get_share_no_expire);

		nameBox.setText(share.getDescription());
		// Local date the share expires on, today if it does not expire yet
		final Calendar expireDate = Calendar.getInstance();
		Date expires = share.getExpires();
		if(expires != null) {
			expireDate.setTime(expires);
		}
		final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
		expireBox.setText(dateFormat.format(expireDate.getTime()));

		View.OnClickListener pickDate = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// The picker works with midnight UTC of the selected day
				final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				utc.clear();
				utc.set(expireDate.get(Calendar.YEAR), expireDate.get(Calendar.MONTH), expireDate.get(Calendar.DAY_OF_MONTH));
				MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
						.setTitleText(expireLayout.getHint())
						.setSelection(utc.getTimeInMillis())
						.build();
				picker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
					@Override
					public void onPositiveButtonClick(Long selection) {
						utc.setTimeInMillis(selection);
						expireDate.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
						expireBox.setText(dateFormat.format(expireDate.getTime()));
					}
				});
				picker.show(context.getSupportFragmentManager(), "share_expire");
			}
		};
		expireBox.setOnClickListener(pickDate);
		expireLayout.setEndIconOnClickListener(pickDate);

		boolean noExpires = share.getExpires() == null;
		if(noExpires) {
			expireLayout.setEnabled(false);
			noExpiresBox.setChecked(true);
		}

		noExpiresBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				expireLayout.setEnabled(!isChecked);
			}
		});

		new MaterialAlertDialogBuilder(context)
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
									Date expires = new Date(expireDate.get(Calendar.YEAR) - 1900, expireDate.get(Calendar.MONTH), expireDate.get(Calendar.DAY_OF_MONTH));
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
