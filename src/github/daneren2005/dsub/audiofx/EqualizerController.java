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
package github.daneren2005.dsub.audiofx;

import java.io.Serializable;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.util.Log;
import github.daneren2005.dsub.util.FileUtil;

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
	private boolean released = false;
	private int audioSessionId = 0;

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
			audioSessionId = mediaPlayer.getAudioSessionId();
            equalizer = new Equalizer(0, audioSessionId);
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
                EqualizerSettings settings = FileUtil.deserialize(context, "equalizer.dat", EqualizerSettings.class);
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
    	try {
        	return isAvailable() && equalizer.getEnabled();
    	} catch(Exception e) {
    		return false;
    	}
    }

    public void release() {
        if (isAvailable()) {
			released = true;
            equalizer.release();
        }
    }

    public Equalizer getEqualizer() {
		if(released) {
			released = false;
			try {
				equalizer = new Equalizer(0, audioSessionId);
			} catch (Throwable x) {
				equalizer = null;
				Log.w(TAG, "Failed to create equalizer.", x);
			}
		}
        return equalizer;
    }

    private static class EqualizerSettings implements Serializable {

        private short[] bandLevels;
        private short preset;
        private boolean enabled;

		public EqualizerSettings() {

		}
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

