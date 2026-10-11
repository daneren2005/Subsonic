package github.paroj.dsub2000.view;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.core.graphics.ColorUtils;

import github.paroj.dsub2000.R;
import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.DrawableTint;
import github.paroj.dsub2000.util.ThemeUtil;
import github.paroj.dsub2000.util.Util;

public class ColorPickerPreference extends DialogPreference {
    private static final String TAG = ColorPickerPreference.class.getSimpleName();
    private SeekBar seekHue;
    private SeekBar seekSat;
    private SeekBar seekLight;
    private View preview;
    private float hue, sat, light; // 0..359 / 0..1 / 0..1
    private int mValue;
    private boolean isColorReset;
    private boolean isUntouched;
    private Context mContext;
    private final int initialThemeRes;
    private final SharedPreferences prefs;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setDialogLayoutResource(R.layout.dialog_color_picker);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogTitle(R.string.color_picker_title);
        initialThemeRes = ThemeUtil.getThemeRes(mContext);
        prefs = Util.getPreferences(mContext);
        isColorReset = false;
        isUntouched = true;
    }

    @Override
    protected View onCreateDialogView()
    {
        View view = super.onCreateDialogView();

        // Ensure the ColorPicker uses the initial theme,
        // since reopening it after a theme change can apply the new theme unexpectedly.
        String storedTheme = prefs.getString(Constants.PREFERENCES_KEY_THEME, null);
        int storedThemRes = ThemeUtil.getThemeRes(mContext, storedTheme);
        if (storedThemRes != initialThemeRes) {
            mContext = new ContextThemeWrapper(mContext, initialThemeRes);
        }

        int persistedColor = getPersistedInt(-1);
        if (persistedColor == -1) {
            mValue = getThemeDefaultColor();
        } else {
            mValue = persistedColor;
        }

        updateHSLFromInt(mValue);
        seekHue = view.findViewById(R.id.hueSlider);
        seekSat = view.findViewById(R.id.satSlider);
        seekLight = view.findViewById(R.id.lightSlider);
        preview = view.findViewById(R.id.colorPreview);
        Button buttonReset = view.findViewById(R.id.button_reset);

        seekHue.setProgress((int) hue);
        seekSat.setProgress((int) (sat * 100));
        seekLight.setProgress((int) (light * 100));

        final Runnable updatePreview = new Runnable() {
            @Override
            public void run() {
                isColorReset = false;
                isUntouched = false;
                setColor();
            }
        };

        seekHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        buttonReset.setOnClickListener(v -> {
            mValue = getThemeDefaultColor();
            updateHSLFromInt(mValue);
            seekHue.setProgress((int) hue);
            seekSat.setProgress((int) (sat * 100));
            seekLight.setProgress((int) (light * 100));
            isColorReset = true;
        });

        setColor();

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        if(positiveResult) {
            if (isColorReset || isUntouched) {
                persistInt(-1);
            } else {
                persistInt(mValue);
            }
            notifyChanged();
        }
    }

    private void setColor() {
        float hue = seekHue.getProgress();
        float sat = seekSat.getProgress() / 100f;
        float light = seekLight.getProgress() / 100f;
        float[] hsl = new float[]{hue, sat, light};
        mValue = ColorUtils.HSLToColor(hsl);
        GradientDrawable bg = (GradientDrawable) preview.getBackground();
        bg.setColor(mValue);
    }

    private void updateHSLFromInt(int value) {
        float[] hsl = new float[3];
        ColorUtils.RGBToHSL(Color.red(value), Color.green(value), Color.blue(value), hsl);
        hue = hsl[0];
        sat = hsl[1];
        light = hsl[2];
    }

    private int getThemeDefaultColor() {
        String key = getKey();
        String theme = prefs.getString(Constants.PREFERENCES_KEY_THEME, null);
        int themeResId = ThemeUtil.getThemeRes(mContext, theme);
        if ("actionBarColorNowPlaying".equals(key)) {
            return DrawableTint.getColorRes(mContext, R.attr.actionbarNowPlayingBackgroundColor, themeResId);
        } else {
            return DrawableTint.getColorRes(mContext, R.attr.actionbarBackgroundColor, themeResId);
        }
    }

}
