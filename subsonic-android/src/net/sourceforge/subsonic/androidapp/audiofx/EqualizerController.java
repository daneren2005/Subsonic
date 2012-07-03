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
package net.sourceforge.subsonic.androidapp.audiofx;

import java.io.Serializable;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.util.Log;
import net.sourceforge.subsonic.androidapp.util.FileUtil;

/**
 * Backward-compatible wrapper for {@link Equalizer}, which is API Level 9.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class EqualizerController {

    private static final String TAG = EqualizerController.class.getSimpleName();

    private final Context context;
    private Equalizer equalizer;

    // Class initialization fails when this throws an exception.
    static {
        try {
            Class.forName("android.media.audiofx.Equalizer");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Throws an exception if the {@link Equalizer} class is not available.
     */
    public static void checkAvailable() throws Throwable {
        // Calling here forces class initialization.
    }

    public EqualizerController(Context context, MediaPlayer mediaPlayer) {
        this.context = context;
        try {
            equalizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
        } catch (Throwable x) {
            Log.w(TAG, "Failed to create equalizer.", x);
        }
    }

    public void saveSettings() {
        try {
            if (isAvailable()) {
                FileUtil.serialize(context, new EqualizerSettings(equalizer), "equalizer.dat");
            }
        } catch (Throwable x) {
            Log.w(TAG, "Failed to save equalizer settings.", x);
        }
    }

    public void loadSettings() {
        try {
            if (isAvailable()) {
                EqualizerSettings settings = FileUtil.deserialize(context, "equalizer.dat");
                if (settings != null) {
                    settings.apply(equalizer);
                }
            }
        } catch (Throwable x) {
            Log.w(TAG, "Failed to load equalizer settings.", x);
        }
    }

    public boolean isAvailable() {
        return equalizer != null;
    }

    public boolean isEnabled() {
        return isAvailable() && equalizer.getEnabled();
    }

    public void release() {
        if (isAvailable()) {
            equalizer.release();
        }
    }

    public Equalizer getEqualizer() {
        return equalizer;
    }

    private static class EqualizerSettings implements Serializable {

        private final short[] bandLevels;
        private short preset;
        private final boolean enabled;

        public EqualizerSettings(Equalizer equalizer) {
            enabled = equalizer.getEnabled();
            bandLevels = new short[equalizer.getNumberOfBands()];
            for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                bandLevels[i] = equalizer.getBandLevel(i);
            }
            try {
                preset = equalizer.getCurrentPreset();
            } catch (Exception x) {
                preset = -1;
            }
        }

        public void apply(Equalizer equalizer) {
            for (short i = 0; i < bandLevels.length; i++) {
                equalizer.setBandLevel(i, bandLevels[i]);
            }
            if (preset >= 0 && preset < equalizer.getNumberOfPresets()) {
                equalizer.usePreset(preset);
            }
            equalizer.setEnabled(enabled);
        }
    }
}

