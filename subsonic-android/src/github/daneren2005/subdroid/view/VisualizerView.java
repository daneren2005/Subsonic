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
package github.daneren2005.subdroid.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;
import github.daneren2005.subdroid.audiofx.VisualizerController;
import github.daneren2005.subdroid.domain.PlayerState;
import github.daneren2005.subdroid.service.DownloadService;
import github.daneren2005.subdroid.service.DownloadServiceImpl;

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture}
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class VisualizerView extends View {

    private static final int PREFERRED_CAPTURE_RATE_MILLIHERTZ = 20000;

    private final Paint paint = new Paint();

    private byte[] data;
    private float[] points;
    private boolean active;

    public VisualizerView(Context context) {
        super(context);

        paint.setStrokeWidth(2f);
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(129, 201, 54));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        Visualizer visualizer = getVizualiser();
        if (visualizer == null) {
            return;
        }

        int captureRate = Math.min(PREFERRED_CAPTURE_RATE_MILLIHERTZ, Visualizer.getMaxCaptureRate());
        if (active) {
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    updateVisualizer(waveform);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                }
            }, captureRate, true, false);
        } else {
            visualizer.setDataCaptureListener(null, captureRate, false, false);
        }

        visualizer.setEnabled(active);
        invalidate();
    }

    private Visualizer getVizualiser() {
        DownloadService downloadService = DownloadServiceImpl.getInstance();
        VisualizerController visualizerController = downloadService == null ? null : downloadService.getVisualizerController();
        return visualizerController == null ? null : visualizerController.getVisualizer();
    }

    private void updateVisualizer(byte[] waveform) {
        this.data = waveform;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!active) {
            return;
        }
        DownloadService downloadService = DownloadServiceImpl.getInstance();
        if (downloadService != null && downloadService.getPlayerState() != PlayerState.STARTED) {
            return;
        }

        if (data == null) {
            return;
        }

        if (points == null || points.length < data.length * 4) {
            points = new float[data.length * 4];
        }

        int w = getWidth();
        int h = getHeight();

        for (int i = 0; i < data.length - 1; i++) {
            points[i * 4] = w * i / (data.length - 1);
            points[i * 4 + 1] = h / 2 + ((byte) (data[i] + 128)) * (h / 2) / 128;
            points[i * 4 + 2] = w * (i + 1) / (data.length - 1);
            points[i * 4 + 3] = h / 2 + ((byte) (data[i + 1] + 128)) * (h / 2) / 128;
        }

        canvas.drawLines(points, paint);
    }
}
