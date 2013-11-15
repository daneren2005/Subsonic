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

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RepeatMode;
import github.daneren2005.dsub.domain.Version;
import github.daneren2005.dsub.provider.DSubWidgetProvider;
import github.daneren2005.dsub.receiver.MediaButtonIntentReceiver;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import org.apache.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public final class Util {
    private static final String TAG = Util.class.getSimpleName();

    private static final DecimalFormat GIGA_BYTE_FORMAT = new DecimalFormat("0.00 GB");
    private static final DecimalFormat MEGA_BYTE_FORMAT = new DecimalFormat("0.00 MB");
    private static final DecimalFormat KILO_BYTE_FORMAT = new DecimalFormat("0 KB");

    private static DecimalFormat GIGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat MEGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat KILO_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat BYTE_LOCALIZED_FORMAT = null;

    public static final String EVENT_META_CHANGED = "github.daneren2005.dsub.EVENT_META_CHANGED";
    public static final String EVENT_PLAYSTATE_CHANGED = "github.daneren2005.dsub.EVENT_PLAYSTATE_CHANGED";
	
	public static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
	public static final String AVRCP_METADATA_CHANGED = "com.android.music.metachanged";
	
	private static boolean hasFocus = false;
	private static boolean pauseFocus = false;
	private static boolean lowerFocus = false;

    private static final Map<Integer, Version> SERVER_REST_VERSIONS = new ConcurrentHashMap<Integer, Version>();

    // Used by hexEncode()
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();
    private static Toast toast;

    private Util() {
    }

    public static boolean isOffline(Context context) {
        SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_OFFLINE, false);
    }
	
	public static void setOffline(Context context, boolean offline) {
		SharedPreferences prefs = getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.PREFERENCES_KEY_OFFLINE, offline);
		editor.commit();
	}

    public static boolean isScreenLitOnDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD, false);
    }

    public static RepeatMode getRepeatMode(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return RepeatMode.valueOf(prefs.getString(Constants.PREFERENCES_KEY_REPEAT_MODE, RepeatMode.OFF.name()));
    }

    public static void setRepeatMode(Context context, RepeatMode repeatMode) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_REPEAT_MODE, repeatMode.name());
        editor.commit();
    }

    public static boolean isScrobblingEnabled(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCROBBLE, false);
    }

    public static void setActiveServer(Context context, int instance) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, instance);
		editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, null);
        editor.commit();
    }

    public static int getActiveServer(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_OFFLINE, false) ? 0 : prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
    }
	
	public static boolean checkServerVersion(Context context, String requiredVersion) {
		Version version = Util.getServerRestVersion(context);
		Version required = new Version(requiredVersion);
		if(version != null && version.compareTo(required) >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public static int getServerCount(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 1);
	}

	public static void removeInstanceName(Context context, int instance, int activeInstance) {
		SharedPreferences prefs = getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();

		int newInstance = instance + 1;

		String server = prefs.getString(Constants.PREFERENCES_KEY_SERVER_KEY + newInstance, null);
		String serverName = prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
		String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
		String userName = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
		String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);

		editor.putString(Constants.PREFERENCES_KEY_SERVER_KEY + instance, server);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, serverName);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + instance, serverUrl);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + instance, userName);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + instance, password);

		editor.putString(Constants.PREFERENCES_KEY_SERVER_KEY + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);
		editor.commit();

		if (instance == activeInstance) {
			if(instance != 1) {
				Util.setActiveServer(context, 1);
			} else {
				Util.setOffline(context, true);
			}
		} else if (newInstance == activeInstance) {
			Util.setActiveServer(context, instance);
		}
	}

	public static String getServerName(Context context) {
		SharedPreferences prefs = getPreferences(context);
		int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        return prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
	}
    public static String getServerName(Context context, int instance) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
    }
	
	public static String getUserName(Context context, int instance) {
		if (instance == 0) {
			return context.getResources().getString(R.string.main_offline);
		}
		SharedPreferences prefs = getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
	}

    public static void setServerRestVersion(Context context, Version version) {
		int instance = getActiveServer(context);
		Version current = SERVER_REST_VERSIONS.get(instance);
		if(current != version) {
			SERVER_REST_VERSIONS.put(instance, version);
			SharedPreferences.Editor editor = getPreferences(context).edit();
			editor.putString(Constants.PREFERENCES_KEY_SERVER_VERSION + instance, version.getVersion());
			editor.commit();
		}
    }

    public static Version getServerRestVersion(Context context) {
		int instance = getActiveServer(context);
        Version version = SERVER_REST_VERSIONS.get(instance);
		if(version == null) {
			SharedPreferences prefs = getPreferences(context);
			String versionString = prefs.getString(Constants.PREFERENCES_KEY_SERVER_VERSION + instance, null);
			if(versionString != null && versionString != "") {
				version = new Version(versionString);
				SERVER_REST_VERSIONS.put(instance, version);
			}
		}
		return version;
    }

    public static void setSelectedMusicFolderId(Context context, String musicFolderId) {
        int instance = getActiveServer(context);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, musicFolderId);
        editor.commit();
    }

    public static String getSelectedMusicFolderId(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int instance = getActiveServer(context);
        return prefs.getString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, null);
    }

    public static String getTheme(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.PREFERENCES_KEY_THEME, null);
    }
	
	public static boolean getDisplayTrack(Context context) {
		SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_DISPLAY_TRACK, false);
	}

    public static int getMaxBitrate(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return 0;
        }

        boolean wifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(wifi ? Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI : Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE, "0"));
    }
	
	public static int getMaxVideoBitrate(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return 0;
        }

        boolean wifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(wifi ? Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI : Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE, "0"));
    }

    public static int getPreloadCount(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return 3;
        }
		
        SharedPreferences prefs = getPreferences(context);
		boolean wifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        int preloadCount = Integer.parseInt(prefs.getString(wifi ? Constants.PREFERENCES_KEY_PRELOAD_COUNT_WIFI : Constants.PREFERENCES_KEY_PRELOAD_COUNT_MOBILE, "-1"));
        return preloadCount == -1 ? Integer.MAX_VALUE : preloadCount;
    }

    public static int getCacheSizeMB(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int cacheSize = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_CACHE_SIZE, "-1"));
        return cacheSize == -1 ? Integer.MAX_VALUE : cacheSize;
    }

    public static String getRestUrl(Context context, String method) {
        StringBuilder builder = new StringBuilder();

        SharedPreferences prefs = getPreferences(context);

        int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
        String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
        String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);

        // Slightly obfuscate password
        password = "enc:" + Util.utf8HexEncode(password);

        builder.append(serverUrl);
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append("/");
        }
        builder.append("rest/").append(method).append(".view");
        builder.append("?u=").append(username);
        builder.append("&p=").append(password);
        builder.append("&v=").append(Constants.REST_PROTOCOL_VERSION);
        builder.append("&c=").append(Constants.REST_CLIENT_ID);

        return builder.toString();
    }
	
	public static String getVideoPlayerType(Context context) {
		SharedPreferences prefs = getPreferences(context); 
		return prefs.getString(Constants.PREFERENCES_KEY_VIDEO_PLAYER, "raw"); 
	}

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Constants.PREFERENCES_FILE_NAME, 0);
    }
	public static SharedPreferences getOfflineSync(Context context) {
		return context.getSharedPreferences(Constants.OFFLINE_SYNC_NAME, 0);
	}
	
	public static String getSyncDefault(Context context) {
		SharedPreferences prefs = Util.getOfflineSync(context);
		return prefs.getString(Constants.OFFLINE_SYNC_DEFAULT, null);
	}
	public static void setSyncDefault(Context context, String defaultValue) {
		SharedPreferences.Editor editor = Util.getOfflineSync(context).edit();
		editor.putString(Constants.OFFLINE_SYNC_DEFAULT, defaultValue);
		editor.commit();
	}
	
	public static int offlineScrobblesCount(Context context) {
		SharedPreferences offline = getOfflineSync(context);
		return offline.getInt(Constants.OFFLINE_SCROBBLE_COUNT, 0);
	}
	public static int offlineStarsCount(Context context) {
		SharedPreferences offline = getOfflineSync(context);
		return offline.getInt(Constants.OFFLINE_STAR_COUNT, 0);
	}
	
	public static String parseOfflineIDSearch(Context context, String id, String cacheLocation) {
		String name = id.replace(cacheLocation, "");
		if(name.startsWith("/")) {
			name = name.substring(1);
		}
		name = name.replace(".complete", "").replace(".partial", "");
		int index = name.lastIndexOf(".");
		name = index == -1 ? name : name.substring(0, index);
		String[] details = name.split("/");
		
		String title = details[details.length - 1];
		if(index == -1) {
			if(details.length > 1) {
				String artist = "artist:\"" + details[details.length - 2] + "\"";
				String simpleArtist = "artist:\"" + title + "\"";
				title = "album:\"" + title + "\"";
				if(details[details.length - 1].equals(details[details.length - 2])) {
					name = title;
				} else {
					name = "(" + artist + " AND " + title + ")" + " OR " + simpleArtist;
				}
			} else {
				name = "artist:\"" + title + "\" OR album:\"" + title + "\"";
			}
		} else {
			String artist;
			if(details.length > 2) {
				artist = "artist:\"" + details[details.length - 3] + "\"";
			} else {
				artist = "(artist:\"" + details[0] + "\" OR album:\"" + details[0] + "\")";
			}
			title = "title:\"" + title.substring(title.indexOf('-') + 1) + "\"";
			name = artist + " AND " + title;
		}
		
		return name;
	}

    public static String getContentType(HttpEntity entity) {
        if (entity == null || entity.getContentType() == null) {
            return null;
        }
        return entity.getContentType().getValue();
    }

    public static int getRemainingTrialDays(Context context) {
        SharedPreferences prefs = getPreferences(context);
        long installTime = prefs.getLong(Constants.PREFERENCES_KEY_INSTALL_TIME, 0L);

        if (installTime == 0L) {
            installTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(Constants.PREFERENCES_KEY_INSTALL_TIME, installTime);
            editor.commit();
        }

        long now = System.currentTimeMillis();
        long millisPerDay = 24L * 60L * 60L * 1000L;
        int daysSinceInstall = (int) ((now - installTime) / millisPerDay);
        return Math.max(0, Constants.FREE_TRIAL_DAYS - daysSinceInstall);
    }

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void atomicCopy(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        File tmp = null;
        try {
            tmp = new File(to.getPath() + ".tmp");
            in = new FileInputStream(from);
            out = new FileOutputStream(tmp);
            in.getChannel().transferTo(0, from.length(), out.getChannel());
            out.close();
            if (!tmp.renameTo(to)) {
                throw new IOException("Failed to rename " + tmp + " to " + to);
            }
            Log.i(TAG, "Copied " + from + " to " + to);
        } catch (IOException x) {
            close(out);
            delete(to);
            throw x;
        } finally {
            close(in);
            close(out);
            delete(tmp);
        }
    }
	public static void renameFile(File from, File to) throws IOException {
		if(from.renameTo(to)) {
			Log.i(TAG, "Renamed " + from + " to " + to);
		} else {
			atomicCopy(from, to);
		}
	}

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static boolean delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete file " + file);
                return false;
            }
            Log.i(TAG, "Deleted file " + file);
        }
        return true;
    }
	public static boolean recursiveDelete(File dir) {
		if (dir != null && dir.exists()) {
			for(File file: dir.listFiles()) {
				if(file.isDirectory()) {
					if(!recursiveDelete(file)) {
						return false;
					}
				} else if(file.exists()) {
					if(!file.delete()) {
						return false;
					}
				}
			}
			return dir.delete();
		}
		return false;
	}

    public static void toast(Context context, int messageId) {
        toast(context, messageId, true);
    }

    public static void toast(Context context, int messageId, boolean shortDuration) {
        toast(context, context.getString(messageId), shortDuration);
    }

    public static void toast(Context context, String message) {
        toast(context, message, true);
    }

    public static void toast(Context context, String message, boolean shortDuration) {
        if (toast == null) {
            toast = Toast.makeText(context, message, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        } else {
            toast.setText(message);
            toast.setDuration(shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        }
        toast.show();
    }
	
	public static void confirmDialog(Context context, int action, String subject, DialogInterface.OnClickListener onClick) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), subject, onClick);
	}
	public static void confirmDialog(Context context, String action, String subject, DialogInterface.OnClickListener onClick) {
		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.common_confirm)
			.setMessage(context.getResources().getString(R.string.common_confirm_message, action, subject))
			.setPositiveButton(R.string.common_ok, onClick)
			.setNegativeButton(R.string.common_cancel, null)
			.show();
	}

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * To get a localized string, please use formatLocalizedBytes instead.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatBytes(long byteCount) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            NumberFormat gigaByteFormat = GIGA_BYTE_FORMAT;
            return gigaByteFormat.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            NumberFormat megaByteFormat = MEGA_BYTE_FORMAT;
            return megaByteFormat.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            NumberFormat kiloByteFormat = KILO_BYTE_FORMAT;
            return kiloByteFormat.format((double) byteCount / 1024);
        }

        return byteCount + " B";
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * This version of the method returns a localized string.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatLocalizedBytes(long byteCount, Context context) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            if (GIGA_BYTE_LOCALIZED_FORMAT == null) {
                GIGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_gigabyte));
            }

            return GIGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            if (MEGA_BYTE_LOCALIZED_FORMAT == null) {
                MEGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_megabyte));
            }

            return MEGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            if (KILO_BYTE_LOCALIZED_FORMAT == null) {
                KILO_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_kilobyte));
            }

            return KILO_BYTE_LOCALIZED_FORMAT.format((double) byteCount / 1024);
        }

        if (BYTE_LOCALIZED_FORMAT == null) {
            BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_byte));
        }

        return BYTE_LOCALIZED_FORMAT.format((double) byteCount);
    }

    public static String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }

		int hours = seconds / 3600;
        int minutes = (seconds / 60) % 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(7);
		if(hours > 0) {
			builder.append(hours).append(":");
			if(minutes < 10) {
				builder.append("0");
			}
		}
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
    }

    public static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        return object1.equals(object2);

    }

    /**
     * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to encode.
     * @return The encoded string.
     */
    public static String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        try {
            utf8 = s.getBytes(Constants.UTF_8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
        return hexEncode(utf8);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data Bytes to convert to hexadecimal characters.
     * @return A string containing hexadecimal characters.
     */
    public static String hexEncode(byte[] data) {
        int length = data.length;
        char[] out = new char[length << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < length; i++) {
            out[j++] = HEX_DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = HEX_DIGITS[0x0F & data[i]];
        }
        return new String(out);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    public static String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return hexEncode(md5.digest(s.getBytes(Constants.UTF_8)));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }
	
	public static boolean isNullOrWhiteSpace(String string) {
		return string == null || "".equals(string) || "".equals(string.trim());
	}

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        boolean wifiConnected = connected && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        boolean wifiRequired = isWifiRequiredForDownload(context);

        return connected && (!wifiRequired || wifiConnected);
    }

    public static boolean isExternalStoragePresent() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private static boolean isWifiRequiredForDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD, false);
    }

    public static void info(Context context, int titleId, int messageId) {
    	info(context, titleId, messageId, true);
    }
	public static void info(Context context, int titleId, String message) {
		info(context, titleId, message, true);
	}
	public static void info(Context context, String title, String message) {
		info(context, title, message, true);
	}
	public static void info(Context context, int titleId, int messageId, boolean linkify) {
        showDialog(context, android.R.drawable.ic_dialog_info, titleId, messageId, linkify);
    }
	public static void info(Context context, int titleId, String message, boolean linkify) {
		showDialog(context, android.R.drawable.ic_dialog_info, titleId, message, linkify);
	}
	public static void info(Context context, String title, String message, boolean linkify) {
		showDialog(context, android.R.drawable.ic_dialog_info, title, message, linkify);
	}

	private static void showDialog(Context context, int icon, int titleId, int messageId) {
		showDialog(context, icon, titleId, messageId, true);
	}
	private static void showDialog(Context context, int icon, int titleId, String message) {
		showDialog(context, icon, titleId, message, true);
	}
	private static void showDialog(Context context, int icon, String title, String message) {
		showDialog(context, icon, title, message, true);
	}
	private static void showDialog(Context context, int icon, int titleId, int messageId, boolean linkify) {
		showDialog(context, icon, context.getResources().getString(titleId), context.getResources().getString(messageId), linkify);
	}
	private static void showDialog(Context context, int icon, int titleId, String message, boolean linkify) {
		showDialog(context, icon, context.getResources().getString(titleId), message, linkify);
	}
	private static void showDialog(Context context, int icon, String title, String message, boolean linkify) {
		SpannableString ss = new SpannableString(message);
		if(linkify) {
			Linkify.addLinks(ss, Linkify.ALL);
		}
		
		AlertDialog dialog = new AlertDialog.Builder(context)
			.setIcon(icon)
			.setTitle(title)
			.setMessage(ss)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					dialog.dismiss();
				}
			})
			.show();
		
		((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

	public static void showPlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler, MusicDirectory.Entry song) {
        // Set the icon, scrolling text and timestamp
        final Notification notification = new Notification(R.drawable.stat_notify_playing, song.getTitle(), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		boolean playing = downloadService.getPlayerState() == PlayerState.STARTED;
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN){
			RemoteViews expandedContentView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
            setupViews(expandedContentView,context,song, playing);
            notification.bigContentView = expandedContentView;
        }
        
        RemoteViews smallContentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        setupViews(smallContentView, context, song, playing);
        notification.contentView = smallContentView;
        
        Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
		handler.post(new Runnable() {
			@Override
			public void run() {
				downloadService.startForeground(Constants.NOTIFICATION_ID_PLAYING, notification);
			}
		});

        // Update widget
        DSubWidgetProvider.notifyInstances(context, downloadService, playing);
    }
    
    private static void setupViews(RemoteViews rv, Context context, MusicDirectory.Entry song, boolean playing){
    
     // Use the same text for the ticker and the expanded notification
        String title = song.getTitle();
        String arist = song.getArtist();
        String album = song.getAlbum();
    
     // Set the album art.
        try {
            int size = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
            Bitmap bitmap = FileUtil.getAlbumArtBitmap(context, song, size);
            if (bitmap == null) {
             // set default album art
             rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
            } else {
             rv.setImageViewBitmap(R.id.notification_image, bitmap);
            }
        } catch (Exception x) {
            Log.w(TAG, "Failed to get notification cover art", x);
            rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
        }
        
        // set the text for the notifications
        rv.setTextViewText(R.id.notification_title, title);
        rv.setTextViewText(R.id.notification_artist, arist);
        rv.setTextViewText(R.id.notification_album, album);
        
        Pair<Integer, Integer> colors = getNotificationTextColors(context);
        if (colors.getFirst() != null) {
            rv.setTextColor(R.id.notification_title, colors.getFirst());
        }
        if (colors.getSecond() != null) {
            rv.setTextColor(R.id.notification_artist, colors.getSecond());
        }

		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_PERSISTENT_NOTIFICATION, false)) {
			rv.setImageViewResource(R.id.control_previous, playing ? R.drawable.notification_pause : R.drawable.notification_play);
			rv.setImageViewResource(R.id.control_pause, R.drawable.notification_next);
			rv.setImageViewResource(R.id.control_next, R.drawable.notification_close);
		}
        
        // Create actions for media buttons
        PendingIntent pendingIntent;
		int previous = 0, pause = 0, next = 0, close = 0;
		if(Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_PERSISTENT_NOTIFICATION, false)) {
			pause = R.id.control_previous;
			next = R.id.control_pause;
			close = R.id.control_next;
		} else {
			previous = R.id.control_previous;
			pause = R.id.control_pause;
			next = R.id.control_next;
		}

		if(previous > 0) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_PREVIOUS");
			prevIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(previous, pendingIntent);
		}
		if(pause > 0) {
			if(playing) {
				Intent pauseIntent = new Intent("KEYCODE_MEDIA_PLAY_PAUSE");
				pauseIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
				pauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
				pendingIntent = PendingIntent.getService(context, 0, pauseIntent, 0);
				rv.setOnClickPendingIntent(pause, pendingIntent);
			} else {
				Intent prevIntent = new Intent("KEYCODE_MEDIA_START");
				prevIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
				prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
				pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
				rv.setOnClickPendingIntent(pause, pendingIntent);
			}
		}
		if(next > 0) {
			Intent nextIntent = new Intent("KEYCODE_MEDIA_NEXT");
			nextIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
			nextIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
			pendingIntent = PendingIntent.getService(context, 0, nextIntent, 0);
			rv.setOnClickPendingIntent(next, pendingIntent);
		}
		if(close > 0) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_STOP");
			prevIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(close, pendingIntent);
		}
    }

    public static void hidePlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler) {
		// Remove notification and remove the service from the foreground
		handler.post(new Runnable() {
			@Override
			public void run() {
				downloadService.stopForeground(true);
			}
		}); 

        // Update widget
        DSubWidgetProvider.notifyInstances(context, downloadService, false);
    }
    
    public static void showDownloadingNotification(final Context context, DownloadFile file, int size) {
    	NotificationCompat.Builder builder;
    	builder = new NotificationCompat.Builder(context)
    		.setSmallIcon(R.drawable.stat_notify_download)
    		.setContentTitle("Downloading " + size + " songs")
    		.setContentText("Current: " + (file != null ? file.getSong().getTitle() : "none"))
    		.setProgress(10, 5, true)
			.setOngoing(true);
    	
		Intent notificationIntent = new Intent(context, SubsonicFragmentActivity.class);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
		notificationIntent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD_VIEW, true);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		builder.setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_ID_DOWNLOADING, builder.build());

    }
    public static void hideDownloadingNotification(final Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOADING);
    }

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            Log.w(TAG, "Interrupted from sleep.", x);
        }
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Class<? extends Activity> newActivitiy) {
        startActivityWithoutTransition(currentActivity, new Intent(currentActivity, newActivitiy));
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Intent intent) {
        currentActivity.startActivity(intent);
        disablePendingTransition(currentActivity);
    }

    public static void disablePendingTransition(Activity activity) {

        // Activity.overridePendingTransition() was introduced in Android 2.0.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Method method = Activity.class.getMethod("overridePendingTransition", int.class, int.class);
            method.invoke(activity, 0, 0);
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static Drawable createDrawableFromBitmap(Context context, Bitmap bitmap) {
        // BitmapDrawable(Resources, Bitmap) was introduced in Android 1.6.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Constructor<BitmapDrawable> constructor = BitmapDrawable.class.getConstructor(Resources.class, Bitmap.class);
            return constructor.newInstance(context.getResources(), bitmap);
        } catch (Throwable x) {
            return new BitmapDrawable(bitmap);
        }
    }

    public static void registerMediaButtonEventReceiver(Context context) {

        // Only do it if enabled in the settings.
        SharedPreferences prefs = getPreferences(context);
        boolean enabled = prefs.getBoolean(Constants.PREFERENCES_KEY_MEDIA_BUTTONS, true);

        if (enabled) {

            // AudioManager.registerMediaButtonEventReceiver() was introduced in Android 2.2.
            // Use reflection to maintain compatibility with 1.5.
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
                Method method = AudioManager.class.getMethod("registerMediaButtonEventReceiver", ComponentName.class);
                method.invoke(audioManager, componentName);
            } catch (Throwable x) {
                // Ignored.
            }
        }
    }

    public static void unregisterMediaButtonEventReceiver(Context context) {
        // AudioManager.unregisterMediaButtonEventReceiver() was introduced in Android 2.2.
        // Use reflection to maintain compatibility with 1.5.
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
            Method method = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver", ComponentName.class);
            method.invoke(audioManager, componentName);
        } catch (Throwable x) {
            // Ignored.
        }
    }
    
    @TargetApi(8)
	public static void requestAudioFocus(final Context context) {
    	if (Build.VERSION.SDK_INT >= 8 && !hasFocus) {
    		final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			hasFocus = true;
    		audioManager.requestAudioFocus(new OnAudioFocusChangeListener() {
				public void onAudioFocusChange(int focusChange) {
					DownloadServiceImpl downloadService = (DownloadServiceImpl)context;
					if((focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) && !downloadService.isRemoteEnabled()) {
						if(downloadService.getPlayerState() == PlayerState.STARTED) {							
							SharedPreferences prefs = getPreferences(context);
							int lossPref = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_TEMP_LOSS, "1"));
							if(lossPref == 2 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
								lowerFocus = true;
								downloadService.setVolume(0.1f);
							} else if(lossPref == 0 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)) {
								pauseFocus = true;
								downloadService.pause();
							}
						}
					} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
						if(pauseFocus) {
							pauseFocus = false;
							downloadService.start();
						} else if(lowerFocus) {
							lowerFocus = false;
							downloadService.setVolume(1.0f);
						}
					} else if(focusChange == AudioManager.AUDIOFOCUS_LOSS && !downloadService.isRemoteEnabled()) {
						hasFocus = false;
						downloadService.pause();
						audioManager.abandonAudioFocus(this);
					}
				}
			}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    	}
    }

    /**
     * <p>Broadcasts the given song info as the new song being played.</p>
     */
    public static void broadcastNewTrackInfo(Context context, MusicDirectory.Entry song) {
		DownloadService downloadService = (DownloadServiceImpl)context;
        Intent intent = new Intent(EVENT_META_CHANGED);
		Intent avrcpIntent = new Intent(AVRCP_METADATA_CHANGED);

        if (song != null) {
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("album", song.getAlbum());

            File albumArtFile = FileUtil.getAlbumArtFile(context, song);
            intent.putExtra("coverart", albumArtFile.getAbsolutePath());
			
			avrcpIntent.putExtra("playing", true);
			avrcpIntent.putExtra("track", song.getTitle());
			avrcpIntent.putExtra("artist", song.getArtist());
			avrcpIntent.putExtra("album", song.getAlbum());
			avrcpIntent.putExtra("ListSize",(long) downloadService.getSongs().size());
			avrcpIntent.putExtra("id", (long) downloadService.getCurrentPlayingIndex()+1);
			avrcpIntent.putExtra("duration", (long) downloadService.getPlayerDuration());
			avrcpIntent.putExtra("position", (long) downloadService.getPlayerPosition());
			avrcpIntent.putExtra("coverart", albumArtFile.getAbsolutePath());
        } else {
            intent.putExtra("title", "");
            intent.putExtra("artist", "");
            intent.putExtra("album", "");
            intent.putExtra("coverart", "");
			
			avrcpIntent.putExtra("playing", false);
			avrcpIntent.putExtra("track", "");
			avrcpIntent.putExtra("artist", "");
			avrcpIntent.putExtra("album", "");
			avrcpIntent.putExtra("ListSize",(long)0);
			avrcpIntent.putExtra("id", (long) 0);
			avrcpIntent.putExtra("duration", (long )0);
			avrcpIntent.putExtra("position", (long) 0);
			avrcpIntent.putExtra("coverart", "");
        }

        context.sendBroadcast(intent);
		context.sendBroadcast(avrcpIntent);
    }

    /**
     * <p>Broadcasts the given player state as the one being set.</p>
     */
    public static void broadcastPlaybackStatusChange(Context context, PlayerState state) {
        Intent intent = new Intent(EVENT_PLAYSTATE_CHANGED);
		Intent avrcpIntent = new Intent(AVRCP_PLAYSTATE_CHANGED);

        switch (state) {
            case STARTED:
                intent.putExtra("state", "play");
				avrcpIntent.putExtra("playing", true);
                break;
            case STOPPED:
                intent.putExtra("state", "stop");
				avrcpIntent.putExtra("playing", false);
                break;
            case PAUSED:
                intent.putExtra("state", "pause");
				avrcpIntent.putExtra("playing", false);
                break;
            case COMPLETED:
                intent.putExtra("state", "complete");
				avrcpIntent.putExtra("playing", false);
                break;
            default:
                return; // No need to broadcast.
        }

        context.sendBroadcast(intent);
		context.sendBroadcast(avrcpIntent);
    }

    /**
     * Resolves the default text color for notifications.
     *
     * Based on http://stackoverflow.com/questions/4867338/custom-notification-layouts-and-text-colors/7320604#7320604
     */
    private static Pair<Integer, Integer> getNotificationTextColors(Context context) {
        if (NOTIFICATION_TEXT_COLORS.getFirst() == null && NOTIFICATION_TEXT_COLORS.getSecond() == null) {
            try {
                Notification notification = new Notification();
                String title = "title";
                String content = "content";
                notification.setLatestEventInfo(context, title, content, null);
                LinearLayout group = new LinearLayout(context);
                ViewGroup event = (ViewGroup) notification.contentView.apply(context, group);
                findNotificationTextColors(event, title, content);
                group.removeAllViews();
            } catch (Exception x) {
                Log.w(TAG, "Failed to resolve notification text colors.", x);
            }
        }
        return NOTIFICATION_TEXT_COLORS;
    }

    private static void findNotificationTextColors(ViewGroup group, String title, String content) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof TextView) {
                TextView textView = (TextView) group.getChildAt(i);
                String text = textView.getText().toString();
                if (title.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setFirst(textView.getTextColors().getDefaultColor());
                }
                else if (content.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setSecond(textView.getTextColors().getDefaultColor());
                }
            }
            else if (group.getChildAt(i) instanceof ViewGroup)
                findNotificationTextColors((ViewGroup) group.getChildAt(i), title, content);
        }
    }
	
	public static WifiManager.WifiLock createWifiLock(Context context, String tag) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int lockType = WifiManager.WIFI_MODE_FULL;
		if (Build.VERSION.SDK_INT >= 12) {
			lockType = 3;
		}
		return wm.createWifiLock(lockType, tag);
	}
}
