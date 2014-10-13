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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.Constants;

public class EditPlayActionActivity extends SubsonicActivity {
	private CheckBox shuffleCheckbox;
	private Spinner offlineSpinner;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.tasker_start_playing_title);
		setContentView(R.layout.edit_play_action);

		shuffleCheckbox = (CheckBox) findViewById(R.id.edit_shuffle_checkbox);
		if(getIntent().getBundleExtra(Constants.TASKER_EXTRA_BUNDLE) != null && getIntent().getBundleExtra(Constants.TASKER_EXTRA_BUNDLE).getBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE)) {
			shuffleCheckbox.setChecked(true);
		}

		offlineSpinner = (Spinner) findViewById(R.id.edit_offline_spinner);
		ArrayAdapter<CharSequence> offlineAdapter = ArrayAdapter.createFromResource(this, R.array.editServerOptions, android.R.layout.simple_spinner_item);
		offlineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		offlineSpinner.setAdapter(offlineAdapter);

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

		Bundle data = new Bundle();
		data.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, shuffleCheckbox.isChecked());
		intent.putExtra(Constants.TASKER_EXTRA_BUNDLE, data);

		setResult(Activity.RESULT_OK, intent);
		finish();
	}
	private void cancel() {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
}
