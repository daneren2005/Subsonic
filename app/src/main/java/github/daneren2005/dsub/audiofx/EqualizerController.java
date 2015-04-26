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
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Build;
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
	private BassBoost bass;
	private boolean loudnessAvailable = false;
	private LoudnessEnhancerController loudnessEnhancerController;
	private boolean released = false;
	private int audioSessionId = 0;

	public EqualizerController(Context context, int audioSessionId) {
		this.context = context;
		this.audioSessionId = audioSessionId;
		init();
	}

	private void init() {
		equalizer = new Equalizer(0, audioSessionId);
		bass = new BassBoost(0, audioSessionId);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			loudnessAvailable = true;
			loudnessEnhancerController = new LoudnessEnhancerController(context, audioSessionId);
		}
	}

	public void saveSettings() {
		try {
			if (isAvailable()) {
				FileUtil.serialize(context, new EqualizerSettings(equalizer, bass, loudnessEnhancerController), "equalizer.dat");
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
					settings.apply(equalizer, bass, loudnessEnhancerController);
				}
			}
		} catch (Throwable x) {
			Log.w(TAG, "Failed to load equalizer settings.", x);
		}
	}

	public boolean isAvailable() {
		return equalizer != null && bass != null;
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
			bass.release();
			if(loudnessEnhancerController != null && loudnessEnhancerController.isAvailable()) {
				loudnessEnhancerController.release();
			}
		}
	}

	public Equalizer getEqualizer() {
		if(released) {
			released = false;
			try {
				init();
			} catch (Throwable x) {
				equalizer = null;
				released = true;
				Log.w(TAG, "Failed to create equalizer.", x);
			}
		}
		return equalizer;
	}
	public BassBoost getBassBoost() {
		if(released) {
			released = false;
			try {
				init();
			} catch (Throwable x) {
				bass = null;
				Log.w(TAG, "Failed to create bass booster.", x);
			}
		}
		return bass;
	}
	public LoudnessEnhancerController getLoudnessEnhancerController() {
		if(loudnessAvailable && released) {
			released = false;
			try {
				init();
			} catch (Throwable x) {
				loudnessEnhancerController = null;
				Log.w(TAG, "Failed to create loudness enhancer.", x);
			}
		}
		return loudnessEnhancerController;
	}

	private static class EqualizerSettings implements Serializable {

		private short[] bandLevels;
		private short preset;
		private boolean enabled;
		private short bass;
		private int loudness;

		public EqualizerSettings() {

		}
		public EqualizerSettings(Equalizer equalizer, BassBoost boost, LoudnessEnhancerController loudnessEnhancerController) {
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
			try {
				bass = boost.getRoundedStrength();
			} catch(Exception e) {
				bass = 0;
			}

			try {
				loudness = (int) loudnessEnhancerController.getGain();
			} catch(Exception e) {
				loudness = 0;
			}
		}

		public void apply(Equalizer equalizer, BassBoost boost, LoudnessEnhancerController loudnessController) {
			for (short i = 0; i < bandLevels.length; i++) {
				equalizer.setBandLevel(i, bandLevels[i]);
			}
			equalizer.setEnabled(enabled);
			if(bass != 0) {
				boost.setEnabled(true);
				boost.setStrength(bass);
			}
			if(loudness != 0) {
				loudnessController.enable();
				loudnessController.setGain(loudness);
			}
		}
	}
}

