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
import android.widget.CheckBox;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.Constants;

public class EditPlayActionActivity extends SubsonicActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_play_action);
	}

	private void accept() {
		Intent intent = new Intent();
		intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", "Start DSub");

		CheckBox checkBox = (CheckBox) findViewById(R.id.edit_shuffle_checkbox);

		Bundle data = new Bundle();
		data.putBoolean(Constants.INTENT_EXTRA_NAME_SHUFFLE, checkBox.isChecked());
		intent.putExtra(Constants.TASKER_EXTRA_BUNDLE, data);

		setResult(Activity.RESULT_OK);
		finish();
	}
	private void cancel() {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
}
