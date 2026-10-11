package github.paroj.dsub2000.util;

import com.google.android.material.slider.Slider;

/**
 * Integer, SeekBar-style access to a Material {@link Slider}.
 * Unlike SeekBar, a Slider throws when its value lies outside the range, so values are clamped here.
 */
public final class Sliders {
	private Sliders() {
	}

	public static void setMax(Slider slider, int max) {
		slider.setValueFrom(0);
		slider.setValueTo(Math.max(1, max));
	}

	public static void setProgress(Slider slider, int progress) {
		float value = Math.max(slider.getValueFrom(), Math.min(slider.getValueTo(), progress));
		slider.setValue(value);
	}

	public static int getProgress(Slider slider) {
		return Math.round(slider.getValue());
	}
}
