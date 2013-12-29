package github.daneren2005.dsub.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Share;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ShareAdapter;

/**
 * Created by Scott on 12/28/13.
 */
public class SelectShareFragment extends SelectListFragment<Share> {
	private static final String TAG = SelectShareFragment.class.getSimpleName();

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		android.view.MenuInflater inflater = context.getMenuInflater();
		inflater.inflate(R.menu.select_share_context, menu);
		recreateContextMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if(menuItem.getGroupId() != getSupportTag()) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Share share = (Share) listView.getItemAtPosition(info.position);

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
	public ArrayAdapter getAdapter(List<Share> objs) {
		return new ShareAdapter(context, objs);
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Share share = (Share) parent.getItemAtPosition(position);

		SubsonicFragment fragment = new SelectDirectoryFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.INTENT_EXTRA_NAME_SHARE, share);
		fragment.setArguments(args);

		replaceFragment(fragment, R.id.fragment_list_layout);
	}

	private void displayShareInfo(final Share share) {
		String message = context.getResources().getString(R.string.share_info,
			share.getUsername(), (share.getDescription() != null) ? share.getDescription() : "", share.getUrl(),
			share.getCreated(), share.getLastVisited(), share.getExpires(), share.getVisitCount());
		Util.info(context, share.getName(), message);
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
						adapter.remove(share);
						adapter.notifyDataSetChanged();
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
