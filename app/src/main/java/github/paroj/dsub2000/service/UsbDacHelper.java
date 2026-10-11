/*
 This file is part of DSub2000.
 Released under GPLv3, see project LICENSE.txt.
*/
package github.paroj.dsub2000.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import github.paroj.dsub2000.util.Constants;
import github.paroj.dsub2000.util.Util;

/**
 * Helpers for the optional "Route to USB DAC" preference (see issue #141).
 * Locates a connected USB audio output device so callers can hand it to
 * {@link LocalPlayback#setPreferredDevice(AudioDeviceInfo)} before {@code prepare},
 * and reads source sample rates for diagnostics so users can confirm what the
 * DAC is actually receiving.
 */
public final class UsbDacHelper {

    private static final String TAG = UsbDacHelper.class.getSimpleName();

    private UsbDacHelper() {}

    /**
     * @return the first connected USB audio output device, or {@code null} if the
     *         preference is disabled, the OS is older than Android 9, or no USB
     *         DAC is plugged in.
     */
    public static AudioDeviceInfo findUsbAudioDevice(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        SharedPreferences prefs = Util.getPreferences(context);
        if (!prefs.getBoolean(Constants.PREFERENCES_KEY_USB_DAC_EXCLUSIVE_MODE, false)) {
            return null;
        }
        return findUsbAudioDeviceInternal(context);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static AudioDeviceInfo findUsbAudioDeviceInternal(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            return null;
        }
        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo info : devices) {
            int type = info.getType();
            if (type == AudioDeviceInfo.TYPE_USB_DEVICE
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_ACCESSORY) {
                Log.i(TAG, "Routing playback to USB audio device type=" + type
                        + " name=" + info.getProductName());
                return info;
            }
        }
        return null;
    }

    /**
     * Read the source sample rate of a local media file. Useful for confirming
     * via {@code adb logcat} that a high-res FLAC actually reaches the player
     * at its native rate (Android may still resample internally).
     *
     * @return sample rate in Hz, or {@code null} on failure / non-local source.
     */
    public static Integer readSampleRate(String localPath) {
        if (localPath == null) {
            return null;
        }
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(localPath);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")
                        && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not read sample rate from " + localPath, t);
        } finally {
            extractor.release();
        }
        return null;
    }
}
