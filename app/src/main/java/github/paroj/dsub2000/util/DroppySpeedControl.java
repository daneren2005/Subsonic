package github.paroj.dsub2000.util;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.slider.Slider;
import com.shehabic.droppy.DroppyClickCallbackInterface;
import com.shehabic.droppy.DroppyMenuCustomItem;

/**
 * Created by marcus on 2/14/2017.
 */
public class DroppySpeedControl extends DroppyMenuCustomItem {

    private Context context;
    private Slider seekBar;
    private DroppyClickCallbackInterface updateBarCallback;
    public DroppySpeedControl(int customResourceId) {
        super(customResourceId);

    }

    @Override
    public View render(Context context) {
        return super.render(context);


    }

    public DroppySpeedControl setOnClicks(Context context, final DroppyClickCallbackInterface callback, int ... elementsByID){
        render(context);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.call(v, v.getId());
            }
        };
        for (Integer element : elementsByID) {
            renderedView.findViewById(element).setOnClickListener(listener);
        }
        return this;
    }


    public void updateSeekBar(float playbackSpeed){
        TextView tv = (TextView)seekBar.getTag();
        tv.setText(Float.toString(playbackSpeed));
        Sliders.setProgress(seekBar, Math.round(playbackSpeed*10)-5);
    }

    public DroppySpeedControl setOnSeekBarChangeListener(Context context, final DroppyClickCallbackInterface callback, int seekBarByID, int textViewByID, float playbackSpeed) {
        updateBarCallback = callback;
        render(context);
        final TextView textBox = (TextView) renderedView.findViewById(textViewByID);
        textBox.setText(Float.toString(playbackSpeed));
        Slider seekBar = ((Slider) renderedView.findViewById(seekBarByID));
        this.seekBar = seekBar;
        seekBar.setTag(textBox);
        Sliders.setProgress(seekBar, Math.round(playbackSpeed*10)-5);
        seekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                textBox.setText(Float.toString((Math.round(value) + 5) / 10f));
                callback.call(slider, slider.getId());
            }
        });
        return this;
    }
}
