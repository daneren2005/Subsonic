package github.daneren2005.dsub.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.shehabic.droppy.DroppyClickCallbackInterface;
import com.shehabic.droppy.DroppyMenuCustomItem;

/**
 * Created by marcus on 2/14/2017.
 */
public class DroppySpeedControl extends DroppyMenuCustomItem {

    private Context context;
    private SeekBar seekBar;
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
        seekBar.setProgress((int)(playbackSpeed*10)-5);
    }

    public DroppySpeedControl setOnSeekBarChangeListener(Context context, final DroppyClickCallbackInterface callback, int seekBarByID, int textViewByID, float playbackSpeed) {
        updateBarCallback = callback;
        render(context);
        final TextView textBox = (TextView) renderedView.findViewById(textViewByID);
        textBox.setText(Float.toString(playbackSpeed));
        SeekBar seekBar = ((SeekBar) renderedView.findViewById(seekBarByID));
        this.seekBar = seekBar;
        seekBar.setTag(textBox);
        seekBar.setProgress((int)(playbackSpeed*10)-5);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textBox.setText(new Float((progress + 5) / 10.0).toString());
                    seekBar.setProgress(progress);
                    callback.call(seekBar,seekBar.getId());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setProgress((int)((playbackSpeed/10.0) - 5));
        return this;
    }
}
