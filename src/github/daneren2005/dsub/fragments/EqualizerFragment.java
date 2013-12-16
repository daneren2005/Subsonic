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

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.fragments;

import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.audiofx.EqualizerController;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

/**
 * Created by Scott on 10/27/13.
 */
public class EqualizerFragment extends SubsonicFragment {
	private static final String TAG = EqualizerFragment.class.getSimpleName();

	private static final int MENU_GROUP_PRESET = 100;

	private final Map<Short, SeekBar> bars = new HashMap<Short, SeekBar>();
	private EqualizerController equalizerController;
	private Equalizer equalizer;
	private short masterLevel = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.equalizer, container, false);

		equalizerController = DownloadServiceImpl.getInstance().getEqualizerController();
		equalizer = equalizerController.getEqualizer();

		try {
			initEqualizer();
		} catch(Exception e) {
			Util.toast(context, "Failed to initialize EQ");
			context.onBackPressed();
		}

		final View presetButton = rootView.findViewById(R.id.equalizer_preset);
		registerForContextMenu(presetButton);
		presetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				presetButton.showContextMenu();
			}
		});

		CheckBox enabledCheckBox = (CheckBox) rootView.findViewById(R.id.equalizer_enabled);
		enabledCheckBox.setChecked(equalizer.getEnabled());
		enabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				setEqualizerEnabled(b);
			}
		});

		setTitle(R.string.equalizer_label);

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		equalizerController.saveSettings();

		if(!equalizer.getEnabled()) {
			equalizerController.release();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		equalizerController = DownloadServiceImpl.getInstance().getEqualizerController();
		equalizer = equalizerController.getEqualizer();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		if(!primaryFragment) {
			return;
		}

		short currentPreset;
		try {
			currentPreset = equalizer.getCurrentPreset();
		} catch (Exception x) {
			currentPreset = -1;
		}

		for (short preset = 0; preset < equalizer.getNumberOfPresets(); preset++) {
			MenuItem menuItem = menu.add(MENU_GROUP_PRESET, preset, preset, equalizer.getPresetName(preset));
			if (preset == currentPreset) {
				menuItem.setChecked(true);
			}
		}
		menu.setGroupCheckable(MENU_GROUP_PRESET, true, true);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		short preset = (short) menuItem.getItemId();
		equalizer.usePreset(preset);
		updateBars(false);
		return true;
	}

	private void setEqualizerEnabled(boolean enabled) {
		SharedPreferences prefs = Util.getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.PREFERENCES_EQUALIZER_ON, enabled);
		editor.commit();
		equalizer.setEnabled(enabled);
		updateBars(true);
	}

	private void updateBars(boolean changedEnabled) {
		boolean isEnabled = equalizer.getEnabled();
		short minEQLevel = equalizer.getBandLevelRange()[0];
		short maxEQLevel = equalizer.getBandLevelRange()[1];
		for (Map.Entry<Short, SeekBar> entry : bars.entrySet()) {
			short band = entry.getKey();
			SeekBar bar = entry.getValue();
			bar.setEnabled(isEnabled);
			if(band >= (short)0) {
				short setLevel;
				if(changedEnabled) {
					setLevel = (short)(equalizer.getBandLevel(band) - masterLevel);
					if(isEnabled) {
						bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);
					} else {
						bar.setProgress(-minEQLevel);
					}
				} else {
					bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);
					setLevel = (short)(equalizer.getBandLevel(band) + masterLevel);
				}
				if(setLevel < minEQLevel) {
					setLevel = minEQLevel;
				} else if(setLevel > maxEQLevel) {
					setLevel = maxEQLevel;
				}
				equalizer.setBandLevel(band, setLevel);
			} else if(!isEnabled) {
				bar.setProgress(-minEQLevel);
			}
		}

		if(!isEnabled) {
			masterLevel = 0;
			SharedPreferences prefs = Util.getPreferences(context);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(Constants.PREFERENCES_EQUALIZER_SETTINGS, masterLevel);
			editor.commit();
		}
	}

	private void initEqualizer() {
		LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.equalizer_layout);

		final short minEQLevel = equalizer.getBandLevelRange()[0];
		final short maxEQLevel = equalizer.getBandLevelRange()[1];

		// Setup Pregain
		SharedPreferences prefs = Util.getPreferences(context);
		masterLevel = (short)prefs.getInt(Constants.PREFERENCES_EQUALIZER_SETTINGS, 0);
		initPregain(layout, minEQLevel, maxEQLevel);

		for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
			final short band = i;

			View bandBar = LayoutInflater.from(context).inflate(R.layout.equalizer_bar, null);
			TextView freqTextView = (TextView) bandBar.findViewById(R.id.equalizer_frequency);
			final TextView levelTextView = (TextView) bandBar.findViewById(R.id.equalizer_level);
			SeekBar bar = (SeekBar) bandBar.findViewById(R.id.equalizer_bar);

			freqTextView.setText((equalizer.getCenterFreq(band) / 1000) + " Hz");

			bars.put(band, bar);
			bar.setMax(maxEQLevel - minEQLevel);
			short level = equalizer.getBandLevel(band);
			if(equalizer.getEnabled()) {
				level = (short) (level - masterLevel);
			}
			bar.setProgress(level - minEQLevel);
			bar.setEnabled(equalizer.getEnabled());
			updateLevelText(levelTextView, level);

			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					short level = (short) (progress + minEQLevel);
					if (fromUser) {
						equalizer.setBandLevel(band, (short)(level + masterLevel));
					}
					updateLevelText(levelTextView, level);
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			layout.addView(bandBar);
		}
	}

	private void initPregain(LinearLayout layout, final short minEQLevel, final short maxEQLevel) {
		View bandBar = LayoutInflater.from(context).inflate(R.layout.equalizer_bar, null);
		TextView freqTextView = (TextView) bandBar.findViewById(R.id.equalizer_frequency);
		final TextView levelTextView = (TextView) bandBar.findViewById(R.id.equalizer_level);
		SeekBar bar = (SeekBar) bandBar.findViewById(R.id.equalizer_bar);

		freqTextView.setText("Master");

		bars.put((short)-1, bar);
		bar.setMax(maxEQLevel - minEQLevel);
		bar.setProgress(masterLevel - minEQLevel);
		bar.setEnabled(equalizer.getEnabled());
		updateLevelText(levelTextView, masterLevel);

		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				masterLevel = (short) (progress + minEQLevel);
				if (fromUser) {
					SharedPreferences prefs = Util.getPreferences(context);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt(Constants.PREFERENCES_EQUALIZER_SETTINGS, masterLevel);
					editor.commit();
					for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
						short level = (short) ((bars.get(i).getProgress() + minEQLevel) + masterLevel);
						equalizer.setBandLevel(i, level);
					}
				}
				updateLevelText(levelTextView, masterLevel);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		layout.addView(bandBar);
	}

	private void updateLevelText(TextView levelTextView, short level) {
		levelTextView.setText((level > 0 ? "+" : "") + level / 100 + " dB");
	}
}
