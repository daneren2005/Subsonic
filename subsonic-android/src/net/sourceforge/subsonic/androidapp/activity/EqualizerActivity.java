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

 Copyright 2011 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.androidapp.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.audiofx.EqualizerController;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceImpl;

/**
 * Equalizer controls.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class EqualizerActivity extends Activity {

    private static final int MENU_GROUP_PRESET = 100;

    private final Map<Short, SeekBar> bars = new HashMap<Short, SeekBar>();
    private EqualizerController equalizerController;
    private Equalizer equalizer;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.equalizer);
        equalizerController = DownloadServiceImpl.getInstance().getEqualizerController();
        equalizer = equalizerController.getEqualizer();

        initEqualizer();

        final View presetButton = findViewById(R.id.equalizer_preset);
        registerForContextMenu(presetButton);
        presetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presetButton.showContextMenu();
            }
        });

        CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.equalizer_enabled);
        enabledCheckBox.setChecked(equalizer.getEnabled());
        enabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setEqualizerEnabled(b);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        equalizerController.saveSettings();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

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
        updateBars();
        return true;
    }

    private void setEqualizerEnabled(boolean enabled) {
        equalizer.setEnabled(enabled);
        updateBars();
    }

    private void updateBars() {

        for (Map.Entry<Short, SeekBar> entry : bars.entrySet()) {
            short band = entry.getKey();
            SeekBar bar = entry.getValue();
            bar.setEnabled(equalizer.getEnabled());
            short minEQLevel = equalizer.getBandLevelRange()[0];
            bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);
        }
    }

    private void initEqualizer() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.equalizer_layout);

        final short minEQLevel = equalizer.getBandLevelRange()[0];
        final short maxEQLevel = equalizer.getBandLevelRange()[1];

        for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
            final short band = i;

            View bandBar = LayoutInflater.from(this).inflate(R.layout.equalizer_bar, null);
            TextView freqTextView = (TextView) bandBar.findViewById(R.id.equalizer_frequency);
            final TextView levelTextView = (TextView) bandBar.findViewById(R.id.equalizer_level);
            SeekBar bar = (SeekBar) bandBar.findViewById(R.id.equalizer_bar);

            freqTextView.setText((equalizer.getCenterFreq(band) / 1000) + " Hz");

            bars.put(band, bar);
            bar.setMax(maxEQLevel - minEQLevel);
            short level = equalizer.getBandLevel(band);
            bar.setProgress(level - minEQLevel);
            bar.setEnabled(equalizer.getEnabled());
            updateLevelText(levelTextView, level);

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short level = (short) (progress + minEQLevel);
                    if (fromUser) {
                        equalizer.setBandLevel(band, level);
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

    private void updateLevelText(TextView levelTextView, short level) {
        levelTextView.setText((level > 0 ? "+" : "") + level / 100 + " dB");
    }

}
