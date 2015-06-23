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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.activity;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Genre;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.LoadingTask;
import github.daneren2005.dsub.util.Util;

public class EditPlayActionActivity extends SubsonicActivity {
	private CheckBox shuffleCheckbox;
	private CheckBox startYearCheckbox;
	private EditText startYearBox;
	private CheckBox endYearCheckbox;
	private EditText endYearBox;
	private Button genreButton;
	private Spinner offlineSpinner;
	
	private String doNothing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.tasker_start_playing_title);
		setContentView(R.layout.edit_play_action);
		final Activity context = this;
		doNothing = context.getResources().getString(R.string.tasker_edit_do_nothing);

		shuffleCheckbox = (CheckBox) findViewById(R.id.edit_shuffle_checkbox);
		shuffleCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				startYearCheckbox.setEnabled(isChecked);
				endYearCheckbox.setEnabled(isChecked);
				genreButton.setEnabled(isChecked);
			}
		});

		startYearCheckbox = (CheckBox) findViewById(R.id.edit_start_year_checkbox);
		startYearBox = (EditText) findViewById(R.id.edit_start_year);
		// Disable/enable number box if checked
		startYearCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				startYearBox.setEnabled(isChecked);
			}
		});
		
		endYearCheckbox = (CheckBox) findViewById(R.id.edit_end_year_checkbox);
		endYearBox = (EditText) findViewById(R.id.edit_end_year);
		endYearCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				endYearBox.setEnabled(isChecked);
			}
		});

		genreButton = (Button) findViewById(R.id.edit_genre_spinner);
		genreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new LoadingTask<List<Genre>>(context, true) {
					@Override
					protected List<Genre> doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(context);
						return musicService.getGenres(false, context, this);
					}

					@Override
					protected void done(final List<Genre> genres) {
						List<String> names = new ArrayList<String>();
						String blank = context.getResources().getString(R.string.select_genre_blank);
						names.add(doNothing);
						names.add(blank);
						for(Genre genre: genres) {
							names.add(genre.getName());
						}
						final List<String> finalNames = names;

						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle(R.string.shuffle_pick_genre)
								.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										if(which == 1) {
											genreButton.setText("");
										} else {
											genreButton.setText(finalNames.get(which));
										}
									}
								});
						AlertDialog dialog = builder.create();
						dialog.show();
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = context.getResources().getString(R.string.playlist_error) + " " + getErrorMessage(error);
						}

						Util.toast(context, msg, false);
					}
				}.execute();
			}
		});
		genreButton.setText(doNothing);

		offlineSpinner = (Spinner) findViewById(R.id.edit_offline_spinner);
		ArrayAdapter<CharSequence> offlineAdapter = ArrayAdapter.createFromResource(this, R.array.editServerOptions, android.R.layout.simple_spinner_item);
		offlineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		offlineSpinner.setAdapter(offlineAdapter);
		
		// Setup default for everything
		Bundle extras = getIntent().getBundleExtra(Constants.TASKER_EXTRA_BUNDLE);
		if(extras != null) {
			if(extras.getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE)) {
				shuffleCheckbox.setChecked(true);
			}
			
			String startYear = extras.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, null);
			if(startYear != null) {
				startYearCheckbox.setEnabled(true);
				startYearBox.setText(startYear);
			}
			String endYear = extras.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, null);
			if(endYear != null) {
				endYearCheckbox.setEnabled(true);
				endYearBox.setText(endYear);
			}
			
			String genre = extras.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, doNothing);
			if(genre != null) {
				genreButton.setText(genre);
			}
			
			int offline = extras.getInt(Constants.PREFERENCES_KEY_OFFLINE, 0);
			if(offline != 0) {
				offlineSpinner.setSelection(offline);
			}
		}

		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.tasker_configuration, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			cancel();
			return true;
		} else if(item.getItemId() == R.id.menu_accept) {
			accept();
			return true;
		} else if(item.getItemId() == R.id.menu_cancel) {
			cancel();
			return true;
		}

		return false;
	}

	private void accept() {
		Intent intent = new Intent();

		String blurb = getResources().getString(shuffleCheckbox.isChecked() ? R.string.tasker_start_playing_shuffled : R.string.tasker_start_playing);
		intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", blurb);

		// Get settings user specified
		Bundle data = new Bundle();
		boolean shuffle = shuffleCheckbox.isChecked();
		data.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, shuffle);
		if(shuffle) {
			if(startYearCheckbox.isChecked()) {
				data.putString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, startYearBox.getText().toString());
			}
			if(endYearCheckbox.isChecked()) {
				data.putString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, endYearBox.getText().toString());
			}
			String genre = genreButton.getText().toString();
			if(!genre.equals(doNothing)) {
				data.putString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, genre);
			}
		}
		
		int offline = offlineSpinner.getSelectedItemPosition();
		if(offline != 0) {
			data.putInt(Constants.PREFERENCES_KEY_OFFLINE, offline);
		}
		
		intent.putExtra(Constants.TASKER_EXTRA_BUNDLE, data);

		setResult(Activity.RESULT_OK, intent);
		finish();
	}
	private void cancel() {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
}
