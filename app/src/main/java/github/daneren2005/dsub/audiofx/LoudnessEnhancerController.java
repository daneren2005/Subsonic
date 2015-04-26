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

	Copyright 2014 (C) Scott Jackson
*/
package github.daneren2005.dsub.audiofx;

import android.content.Context;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Log;

public class LoudnessEnhancerController {
	private static final String TAG = LoudnessEnhancerController.class.getSimpleName();

	private final Context context;
	private LoudnessEnhancer enhancer;
	private boolean released = false;
	private int audioSessionId = 0;

	public LoudnessEnhancerController(Context context, int audioSessionId) {
		this.context = context;
		try {
			this.audioSessionId = audioSessionId;
			enhancer = new LoudnessEnhancer(audioSessionId);
		} catch (Throwable x) {
			Log.w(TAG, "Failed to create enhancer", x);
		}
	}

	public boolean isAvailable() {
		return enhancer != null;
	}

	public boolean isEnabled() {
		try {
			return isAvailable() && enhancer.getEnabled();
		} catch(Exception e) {
			return false;
		}
	}

	public void enable() {
		enhancer.setEnabled(true);
	}
	public void disable() {
		enhancer.setEnabled(false);
	}

	public float getGain() {
		return enhancer.getTargetGain();
	}
	public void setGain(int gain) {
		enhancer.setTargetGain(gain);
	}

	public void release() {
		if (isAvailable()) {
			enhancer.release();
			released = true;
		}
	}

}

