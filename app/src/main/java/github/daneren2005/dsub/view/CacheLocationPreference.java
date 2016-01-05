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
package github.daneren2005.dsub.view;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.util.FileUtil;

public class CacheLocationPreference extends EditTextPreference {
	private static final String TAG = CacheLocationPreference.class.getSimpleName();
	private Context context;

	public CacheLocationPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	public CacheLocationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	public CacheLocationPreference(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			view.setLayoutParams(new ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

			final EditText editText = (EditText) view.findViewById(android.R.id.edit);
			ViewGroup vg = (ViewGroup) editText.getParent();

			LinearLayout cacheButtonsWrapper = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.cache_location_buttons, vg, true);
			Button internalLocation = (Button) cacheButtonsWrapper.findViewById(R.id.location_internal);
			Button externalLocation = (Button) cacheButtonsWrapper.findViewById(R.id.location_external);

			File[] dirs;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				dirs = context.getExternalMediaDirs();
			} else {
				dirs = ContextCompat.getExternalFilesDirs(context, null);
			}

			// Past 5.0 we can query directly for SD Card
			File internalDir = null, externalDir = null;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				for(int i = 0; i < dirs.length; i++) {
					try {
						if (dirs[i] != null) {
							if(Environment.isExternalStorageRemovable(dirs[i])) {
								if(externalDir != null) {
									externalDir = dirs[i];
								}
							} else {
								internalDir = dirs[i];
							}

							if(internalDir != null && externalDir != null) {
								break;
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "Failed to check if is external", e);
					}
				}
			}

			// Before 5.0, we have to guess.  Most of the time the SD card is last
			if(externalDir == null) {
				for (int i = dirs.length - 1; i >= 0; i--) {
					if (dirs[i] != null) {
						externalDir = dirs[i];
						break;
					}
				}
			}
			if(internalDir == null) {
				for (int i = 0; i < dirs.length; i++) {
					if (dirs[i] != null) {
						internalDir = dirs[i];
						break;
					}
				}
			}
			final File finalInternalDir = new File(internalDir, "music");
			final File finalExternalDir = new File(externalDir, "music");

			final EditText editTextBox = (EditText)view.findViewById(android.R.id.edit);
			if(finalInternalDir != null && (finalInternalDir.exists() || finalInternalDir.mkdirs())) {
				internalLocation.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String path = finalInternalDir.getPath();
						editTextBox.setText(path);
					}
				});
			} else {
				internalLocation.setEnabled(false);
			}

			if(finalExternalDir != null && !finalInternalDir.equals(finalExternalDir) && (finalExternalDir.exists() || finalExternalDir.mkdirs())) {
				externalLocation.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String path = finalExternalDir.getPath();
						editTextBox.setText(path);
					}
				});
			} else {
				externalLocation.setEnabled(false);
			}
		}
	}
}
