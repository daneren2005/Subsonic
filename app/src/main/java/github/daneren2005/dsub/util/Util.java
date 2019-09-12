/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.adapter.DetailsAdapter;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RepeatMode;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.receiver.MediaButtonIntentReceiver;
import github.daneren2005.dsub.service.DownloadService;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

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
	private static SimpleDateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("MMM d h:mm a");
	private static SimpleDateFormat DATE_FORMAT_LONG = new SimpleDateFormat("MMM d, yyyy h:mm a");
	private static SimpleDateFormat DATE_FORMAT_NO_TIME = new SimpleDateFormat("MMM d, yyyy");
	private static int CURRENT_YEAR = new Date().getYear();

    public static final String EVENT_META_CHANGED = "github.daneren2005.dsub.EVENT_META_CHANGED";
    public static final String EVENT_PLAYSTATE_CHANGED = "github.daneren2005.dsub.EVENT_PLAYSTATE_CHANGED";
	
	public static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
	public static final String AVRCP_METADATA_CHANGED = "com.android.music.metachanged";

	private static OnAudioFocusChangeListener focusListener;
	private static AudioFocusRequest audioFocusRequest;
	private static boolean pauseFocus = false;
	private static boolean lowerFocus = false;

    // Used by hexEncode()
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static Toast toast;
	// private static Map<Integer, Pair<String, String>> tokens = new HashMap<>();
	private static SparseArray<Pair<String, String>> tokens = new SparseArray<>();
	private static Random random;

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
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCROBBLE, true) && (isOffline(context) || UserUtil.canScrobble());
    }

    public static void setActiveServer(Context context, int instance) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, instance);
        editor.commit();
    }

    public static int getActiveServer(Context context) {
        SharedPreferences prefs = getPreferences(context);
		// Don't allow the SERVER_INSTANCE to ever be 0
        return prefs.getBoolean(Constants.PREFERENCES_KEY_OFFLINE, false) ? 0 : Math.max(1, prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1));
    }
	public static int getMostRecentActiveServer(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return Math.max(1, prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1));
	}
	
	public static int getServerCount(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 1);
	}

	public static void removeInstanceName(Context context, int instance, int activeInstance) {
		SharedPreferences prefs = getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();

		int newInstance = instance + 1;

		// Get what the +1 server details are
		String server = prefs.getString(Constants.PREFERENCES_KEY_SERVER_KEY + newInstance, null);
		String serverName = prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
		String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
		String userName = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
		String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);
		if ((password != null) && (prefs.getBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + instance, false))) password = KeyStoreUtil.decrypt(password);
		String musicFolderId = prefs.getString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + newInstance, null);

		// Store the +1 server details in the to be deleted instance
		editor.putString(Constants.PREFERENCES_KEY_SERVER_KEY + instance, server);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, serverName);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + instance, serverUrl);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + instance, userName);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + instance, password);
		editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, musicFolderId);

		// Delete the +1 server instance
		// Calling method will loop up to fill this in if +2 server exists
		editor.putString(Constants.PREFERENCES_KEY_SERVER_KEY + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + newInstance, null);
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

    public static void setSelectedMusicFolderId(Context context, String musicFolderId) {
        int instance = getActiveServer(context);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, musicFolderId);
        editor.commit();
    }

    public static String getSelectedMusicFolderId(Context context) {
        return getSelectedMusicFolderId(context, getActiveServer(context));
    }
	public static String getSelectedMusicFolderId(Context context, int instance) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, null);
	}

	public static boolean getAlbumListsPerFolder(Context context) {
		return getAlbumListsPerFolder(context, getActiveServer(context));
	}
	public static boolean getAlbumListsPerFolder(Context context, int instance) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_ALBUMS_PER_FOLDER + instance, false);
	}
	public static void setAlbumListsPerFolder(Context context, boolean perFolder) {
		int instance = getActiveServer(context);
		SharedPreferences prefs = getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.PREFERENCES_KEY_ALBUMS_PER_FOLDER + instance, perFolder);
		editor.commit();
	}
	
	public static boolean getDisplayTrack(Context context) {
		SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_DISPLAY_TRACK, true);
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
	public static boolean isBatchMode(Context context) {
		return Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_BATCH_MODE, false);
	}
	public static void setBatchMode(Context context, boolean batchMode) {
		Util.getPreferences(context).edit().putBoolean(Constants.PREFERENCES_KEY_BATCH_MODE, batchMode).commit();
	}

    public static String getRestUrl(Context context, String method) {
        return getRestUrl(context, method, true);
    }
	public static String getRestUrl(Context context, String method, boolean allowAltAddress) {
		SharedPreferences prefs = getPreferences(context);
		int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
		return getRestUrl(context, method, prefs, instance, allowAltAddress);
	}
    public static String getRestUrl(Context context, String method, int instance) {
    	return getRestUrl(context, method, instance, true);
    }
	public static String getRestUrl(Context context, String method, int instance, boolean allowAltAddress) {
		SharedPreferences prefs = getPreferences(context);
		return getRestUrl(context, method, prefs, instance, allowAltAddress);
	}
    public static String getRestUrl(Context context, String method, SharedPreferences prefs, int instance) {
        return getRestUrl(context, method, prefs, instance, true);
    }
	public static String getRestUrl(Context context, String method, SharedPreferences prefs, int instance, boolean allowAltAddress) {
		StringBuilder builder = new StringBuilder();

		String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
		if(allowAltAddress && Util.isWifiConnected(context)) {
			String SSID = prefs.getString(Constants.PREFERENCES_KEY_SERVER_LOCAL_NETWORK_SSID + instance, "");
			if(!SSID.isEmpty()) {
				String currentSSID = Util.getSSID(context);

				String[] ssidParts = SSID.split(",");
				if ("".equals(SSID) || SSID.equals(currentSSID) || Arrays.asList(ssidParts).contains(currentSSID)) {
					String internalUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_INTERNAL_URL + instance, null);
					if (internalUrl != null && !"".equals(internalUrl) && !"http://".equals(internalUrl)) {
						serverUrl = internalUrl;
					}
				}
			}
		}

		String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
		String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
		if ((password != null) && (prefs.getBoolean(Constants.PREFERENCES_KEY_ENCRYPTED_PASSWORD + instance, false))) password = KeyStoreUtil.decrypt(password);

		builder.append(serverUrl);
		if (builder.charAt(builder.length() - 1) != '/') {
			builder.append("/");
		}
		if(method != null && ServerInfo.isMadsonic6(context, instance)) {
			builder.append("rest2/");
		} else {
			builder.append("rest/");
		}
		builder.append(method).append(".view");
		builder.append("?u=").append(username);
		if(method != null && ServerInfo.canUseToken(context, instance)) {
			int hash = (username + password).hashCode();
			Pair<String, String> values = tokens.get(hash);
			if(values == null) {
				String salt = new BigInteger(130, getRandom()).toString(32);
				String token = md5Hex(password + salt);
				values = new Pair<>(salt, token);
				tokens.put(hash, values);
			}

			builder.append("&s=").append(values.getFirst());
			builder.append("&t=").append(values.getSecond());
		} else {
			// Slightly obfuscate password
			password = "enc:" + Util.utf8HexEncode(password);

			builder.append("&p=").append(password);
		}

		if(method != null && ServerInfo.isMadsonic6(context, instance)) {
			builder.append("&v=").append(Constants.REST_PROTOCOL_VERSION_MADSONIC);
		} else {
			builder.append("&v=").append(Constants.REST_PROTOCOL_VERSION_SUBSONIC);
		}
		builder.append("&c=").append(Constants.REST_CLIENT_ID);

		return builder.toString();
	}
	public static int getRestUrlHash(Context context) {
		return getRestUrlHash(context, Util.getMostRecentActiveServer(context));
	}
	public static int getRestUrlHash(Context context, int instance) {
		StringBuilder builder = new StringBuilder();

		SharedPreferences prefs = Util.getPreferences(context);
		builder.append(prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null));
		builder.append(prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null));

		return builder.toString().hashCode();
	}

	public static String getBlockTokenUsePref(Context context, int instance) {
		return Constants.CACHE_BLOCK_TOKEN_USE + Util.getRestUrl(context, null, instance, false);
	}
	public static boolean getBlockTokenUse(Context context, int instance) {
		return getPreferences(context).getBoolean(getBlockTokenUsePref(context, instance), false);
	}
	public static void setBlockTokenUse(Context context, int instance, boolean block) {
		SharedPreferences.Editor editor = getPreferences(context).edit();
		editor.putBoolean(getBlockTokenUsePref(context, instance), block);
		editor.commit();
	}

	public static String replaceInternalUrl(Context context, String url) {
		// Only change to internal when using https
		if(url.indexOf("https") != -1) {
			SharedPreferences prefs = Util.getPreferences(context);
			int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
			String internalUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_INTERNAL_URL + instance, null);
			if(internalUrl != null && !"".equals(internalUrl)) {
				String externalUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
				url = url.replace(internalUrl, externalUrl);
			}
		}

		//  Use separate profile for Chromecast so users can do ogg on phone, mp3 for CC
		return url.replace("c=" + Constants.REST_CLIENT_ID, "c=" + Constants.CHROMECAST_CLIENT_ID);
	}

	public static boolean isTagBrowsing(Context context) {
		return isTagBrowsing(context, Util.getActiveServer(context));
	}
	public static boolean isTagBrowsing(Context context, int instance) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_BROWSE_TAGS + instance, false);
	}
	
	public static boolean isSyncEnabled(Context context, int instance) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_SERVER_SYNC + instance, true);
	}

	public static String getParentFromEntry(Context context, MusicDirectory.Entry entry) {
		if(Util.isTagBrowsing(context)) {
			if(!entry.isDirectory()) {
				return entry.getAlbumId();
			} else if(entry.isAlbum()) {
				return entry.getArtistId();
			} else {
				return null;
			}
		} else {
			return entry.getParent();
		}
	}

	public static String openToTab(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_OPEN_TO_TAB, null);
	}

	public static boolean disableExitPrompt(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_DISABLE_EXIT_PROMPT, false);
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

	public static String getCacheName(Context context, String name, String id) {
		return getCacheName(context, getActiveServer(context), name, id);
	}
	public static String getCacheName(Context context, int instance, String name, String id) {
		String s = getRestUrl(context, null, instance, false) + id;
		return name + "-" + s.hashCode() + ".ser";
	}
	public static String getCacheName(Context context, String name) {
		return getCacheName(context, getActiveServer(context), name);
	}
	public static String getCacheName(Context context, int instance, String name) {
		String s = getRestUrl(context, null, instance, false);
		return name + "-" + s.hashCode() + ".ser";
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
		// Try to get this info based off of tags first
		String name = parseOfflineIDSearch(id);
		if(name != null) {
			return name;
		}

		// Otherwise go nuts trying to parse from file structure
		name = id.replace(cacheLocation, "");
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

	public static String parseOfflineIDSearch(String id) {
		MusicDirectory.Entry entry = new MusicDirectory.Entry();
		File file = new File(id);

		if(file.exists()) {
			entry.loadMetadata(file);

			if(entry.getArtist() != null) {
				String title = file.getName();
				title = title.replace(".complete", "").replace(".partial", "");
				int index = title.lastIndexOf(".");
				title = index == -1 ? title : title.substring(0, index);
				title = title.substring(title.indexOf('-') + 1);

				String query = "artist:\"" + entry.getArtist() + "\"" +
					" AND title:\"" + title + "\"";

				return query;
			} else {
				return null;
			}
		} else {
			return null;
		}
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

	public static boolean isCastProxy(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_CAST_PROXY, false);
	}

	public static boolean isFirstLevelArtist(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_FIRST_LEVEL_ARTIST + getActiveServer(context), true);
	}
	public static void toggleFirstLevelArtist(Context context) {
		SharedPreferences prefs = Util.getPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();

		if(prefs.getBoolean(Constants.PREFERENCES_KEY_FIRST_LEVEL_ARTIST + getActiveServer(context), true)) {
			editor.putBoolean(Constants.PREFERENCES_KEY_FIRST_LEVEL_ARTIST + getActiveServer(context), false);
		} else {
			editor.putBoolean(Constants.PREFERENCES_KEY_FIRST_LEVEL_ARTIST + getActiveServer(context), true);
		}

		editor.commit();
	}

	public static boolean shouldCacheDuringCasting(Context context) {
		return Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_CAST_CACHE, false);
	}

	public static boolean shouldStartOnHeadphones(Context context) {
		SharedPreferences prefs = getPreferences(context);
		return prefs.getBoolean(Constants.PREFERENCES_KEY_START_ON_HEADPHONES, false);
	}

	public static String getSongPressAction(Context context) {
		return getPreferences(context).getString(Constants.PREFERENCES_KEY_SONG_PRESS_ACTION, "all");
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

	public static void renameFile(File from, File to) throws IOException {
		if(!from.renameTo(to)) {
			Log.i(TAG, "Failed to rename " + from + " to " + to);
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
	
	public static void confirmDialog(Context context, int action, int subject, DialogInterface.OnClickListener onClick) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), context.getResources().getString(subject), onClick, null);
	}
	public static void confirmDialog(Context context, int action, int subject, DialogInterface.OnClickListener onClick, DialogInterface.OnClickListener onCancel) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), context.getResources().getString(subject), onClick, onCancel);
	}
	public static void confirmDialog(Context context, int action, String subject, DialogInterface.OnClickListener onClick) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), subject, onClick, null);
	}
	public static void confirmDialog(Context context, int action, String subject, DialogInterface.OnClickListener onClick, DialogInterface.OnClickListener onCancel) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), subject, onClick, onCancel);
	}
	public static void confirmDialog(Context context, String action, String subject, DialogInterface.OnClickListener onClick, DialogInterface.OnClickListener onCancel) {
		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.common_confirm)
			.setMessage(context.getResources().getString(R.string.common_confirm_message, action, subject))
			.setPositiveButton(R.string.common_ok, onClick)
			.setNegativeButton(R.string.common_cancel, onCancel)
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

	public static String formatDate(Context context, String dateString) {
		return formatDate(context, dateString, true);
	}
	public static String formatDate(Context context, String dateString, boolean includeTime) {
		if(dateString == null) {
			return "";
		}

		try {
			dateString = dateString.replace(' ', 'T');
			boolean isDateNormalized = ServerInfo.checkServerVersion(context, "1.11");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
			if (isDateNormalized) {
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			}

			return formatDate(dateFormat.parse(dateString), includeTime);
		} catch(ParseException e) {
			Log.e(TAG, "Failed to parse date string", e);
			return dateString;
		}
	}
	public static String formatDate(Date date) {
		return formatDate(date, true);
	}
	public static String formatDate(Date date, boolean includeTime) {
		if(date == null) {
			return "Never";
		} else {
			if(includeTime) {
				if (date.getYear() != CURRENT_YEAR) {
					return DATE_FORMAT_LONG.format(date);
				} else {
					return DATE_FORMAT_SHORT.format(date);
				}
			} else {
				return DATE_FORMAT_NO_TIME.format(date);
			}
		}
	}
	public static String formatDate(long millis) {
		return formatDate(new Date(millis));
	}

	public static String formatBoolean(Context context, boolean value) {
		return context.getResources().getString(value ? R.string.common_true : R.string.common_false);
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

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static int getScaledHeight(double height, double width, int newWidth) {
		// Try to keep correct aspect ratio of the original image, do not force a square
		double aspectRatio = height / width;

		// Assume the size given refers to the width of the image, so calculate the new height using
		//	the previously determined aspect ratio
		return (int) Math.round(newWidth * aspectRatio);
	}

	public static int getScaledHeight(Bitmap bitmap, int width) {
		return Util.getScaledHeight((double) bitmap.getHeight(), (double) bitmap.getWidth(), width);
	}

	public static int getStringDistance(CharSequence s, CharSequence t) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}

		if(t.toString().toLowerCase().indexOf(s.toString().toLowerCase()) != -1) {
			return 1;
		}

        int n = s.length();
		int m = t.length();

		if (n == 0) {
			return m;
		} else if (m == 0) {
			return n;
		}

		if (n > m) {
			final CharSequence tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}

		int p[] = new int[n + 1];
		int d[] = new int[n + 1];
		int _d[];

		int i;
		int j;
		char t_j;
		int cost;

		for (i = 0; i <= n; i++) {
			p[i] = i;
		}

		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;

			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
			}

			_d = p;
			p = d;
			d = _d;
		}

		return p[n];
	}

	public static boolean isNetworkConnected(Context context) {
		return isNetworkConnected(context, false);
	}
    public static boolean isNetworkConnected(Context context, boolean streaming) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

		if(streaming) {
			boolean wifiConnected = connected && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
			boolean wifiRequired = isWifiRequiredForDownload(context);

			boolean isLocalNetwork = connected && !networkInfo.isRoaming();
			boolean localNetworkRequired = isLocalNetworkRequiredForDownload(context);

			return connected && (!wifiRequired || wifiConnected) && (!localNetworkRequired || isLocalNetwork);
		} else {
			return connected;
		}
    }
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		boolean connected = networkInfo != null && networkInfo.isConnected();
		return connected && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
	}
	public static String getSSID(Context context) {
		if (isWifiConnected(context)) {
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null) {
				return wifiManager.getConnectionInfo().getSSID().replace("\"", "");
			}
			return null;
		}
		return null;
	}

    public static boolean isExternalStoragePresent() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

	public static boolean isAllowedToDownload(Context context) {
		return isNetworkConnected(context, true) && !isOffline(context);
	}
    public static boolean isWifiRequiredForDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD, false);
    }

    public static boolean isLocalNetworkRequiredForDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_LOCAL_NETWORK_REQUIRED_FOR_DOWNLOAD, false);
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

	public static void showDialog(Context context, int icon, int titleId, int messageId) {
		showDialog(context, icon, titleId, messageId, true);
	}
	public static void showDialog(Context context, int icon, int titleId, String message) {
		showDialog(context, icon, titleId, message, true);
	}
	public static void showDialog(Context context, int icon, String title, String message) {
		showDialog(context, icon, title, message, true);
	}
	public static void showDialog(Context context, int icon, int titleId, int messageId, boolean linkify) {
		showDialog(context, icon, context.getResources().getString(titleId), context.getResources().getString(messageId), linkify);
	}
	public static void showDialog(Context context, int icon, int titleId, String message, boolean linkify) {
		showDialog(context, icon, context.getResources().getString(titleId), message, linkify);
	}
	public static void showDialog(Context context, int icon, String title, String message, boolean linkify) {
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
	public static void showHTMLDialog(Context context, int title, int message) {
		showHTMLDialog(context, title, context.getResources().getString(message));
	}
	public static void showHTMLDialog(Context context, int title, String message) {
		AlertDialog dialog = new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(title)
			.setMessage(Html.fromHtml(message))
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					dialog.dismiss();
				}
			})
			.show();

		((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}

	public static void showDetailsDialog(Context context, @StringRes int title, List<Integer> headers, List<String> details) {
		List<String> headerStrings = new ArrayList<>();
		for(@StringRes Integer res: headers) {
			headerStrings.add(context.getResources().getString(res));
		}
		showDetailsDialog(context, context.getResources().getString(title), headerStrings, details);
	}
	public static void showDetailsDialog(Context context, String title, List<String> headers, final List<String> details) {
		ListView listView = new ListView(context);
		listView.setAdapter(new DetailsAdapter(context, R.layout.details_item, headers, details));
		listView.setDivider(null);
		listView.setScrollbarFadingEnabled(false);

		// Let the user long-click on a row to copy its value to the clipboard
		final Context contextRef = context;
		listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				TextView nameView = (TextView) view.findViewById(R.id.detail_name);
				TextView detailsView = (TextView) view.findViewById(R.id.detail_value);
				if(nameView == null || detailsView == null) {
					return false;
				}

				CharSequence name = nameView.getText();
				CharSequence value = detailsView.getText();

				ClipboardManager clipboard = (ClipboardManager) contextRef.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText(name, value);
				clipboard.setPrimaryClip(clip);

				toast(contextRef, "Copied " + name + " to clipboard");

				return true;
			}
		});

		new AlertDialog.Builder(context)
				// .setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(title)
				.setView(listView)
				.setPositiveButton(R.string.common_close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {
						dialog.dismiss();
					}
				})
				.show();
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

        // Only do it if enabled in the settings and api < 21
        SharedPreferences prefs = getPreferences(context);
        boolean enabled = prefs.getBoolean(Constants.PREFERENCES_KEY_MEDIA_BUTTONS, true);

        if (enabled && Build.VERSION.SDK_INT < 21) {

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
	public static void requestAudioFocus(final Context context, final AudioManager audioManager) {
    	if(Build.VERSION.SDK_INT >= 26) {
    		if(audioFocusRequest == null) {
				AudioAttributes playbackAttributes = new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.build();

				audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
						.setAudioAttributes(playbackAttributes)
						.setOnAudioFocusChangeListener(getAudioFocusChangeListener(context, audioManager))
						.setWillPauseWhenDucked(true)
						.build();
				audioManager.requestAudioFocus(audioFocusRequest);
			}
		} else if (Build.VERSION.SDK_INT >= 8 && focusListener == null) {
    		audioManager.requestAudioFocus(focusListener = getAudioFocusChangeListener(context, audioManager), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    	}
    }

    private static OnAudioFocusChangeListener getAudioFocusChangeListener(final Context context, final AudioManager audioManager) {
		return new OnAudioFocusChangeListener() {
			public void onAudioFocusChange(int focusChange) {
				DownloadService downloadService = (DownloadService)context;
				if((focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) && !downloadService.isRemoteEnabled()) {
					if(downloadService.getPlayerState() == PlayerState.STARTED) {
						Log.i(TAG, "Temporary loss of focus");
						SharedPreferences prefs = getPreferences(context);
						int lossPref = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_TEMP_LOSS, "1"));
						if(lossPref == 2 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
							lowerFocus = true;
							downloadService.setVolume(0.1f);
						} else if(lossPref == 0 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)) {
							pauseFocus = true;
							downloadService.pause(true);
						}
					}
				} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
					if(pauseFocus) {
						pauseFocus = false;
						downloadService.start();
					}
					if(lowerFocus) {
						lowerFocus = false;
						downloadService.setVolume(1.0f);
					}
				} else if(focusChange == AudioManager.AUDIOFOCUS_LOSS && !downloadService.isRemoteEnabled()) {
					Log.i(TAG, "Permanently lost focus");
					focusListener = null;
					downloadService.pause();

					if(audioFocusRequest != null && Build.VERSION.SDK_INT >= 26) {
						audioManager.abandonAudioFocusRequest(audioFocusRequest);
						audioFocusRequest = null;
					} else {
						audioManager.abandonAudioFocus(this);
					}
				}
			}
		};
	}

	public static void abandonAudioFocus(Context context) {
		if(focusListener != null) {
			final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			audioManager.abandonAudioFocus(focusListener);
			focusListener = null;
		}
	}

    /**
     * <p>Broadcasts the given song info as the new song being played.</p>
     */
    public static void broadcastNewTrackInfo(Context context, MusicDirectory.Entry song) {
		try {
			Intent intent = new Intent(EVENT_META_CHANGED);
			Intent avrcpIntent = new Intent(AVRCP_METADATA_CHANGED);

			if (song != null) {
				intent.putExtra("title", song.getTitle());
				intent.putExtra("artist", song.getArtist());
				intent.putExtra("album", song.getAlbum());

				File albumArtFile = FileUtil.getAlbumArtFile(context, song);
				intent.putExtra("coverart", albumArtFile.getAbsolutePath());
				avrcpIntent.putExtra("playing", true);
			} else {
				intent.putExtra("title", "");
				intent.putExtra("artist", "");
				intent.putExtra("album", "");
				intent.putExtra("coverart", "");
				avrcpIntent.putExtra("playing", false);
			}
			addTrackInfo(context, song, avrcpIntent);

			context.sendBroadcast(intent);
			context.sendBroadcast(avrcpIntent);
		} catch(Exception e) {
			Log.e(TAG, "Failed to broadcastNewTrackInfo", e);
		}
    }

    /**
     * <p>Broadcasts the given player state as the one being set.</p>
     */
    public static void broadcastPlaybackStatusChange(Context context, MusicDirectory.Entry song, PlayerState state) {
		try {
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
				case PREPARED:
					// Only send quick pause event for samsung devices, causes issues for others
					if (Build.MANUFACTURER.toLowerCase().indexOf("samsung") != -1) {
						avrcpIntent.putExtra("playing", false);
					} else {
						return; // Don't broadcast anything
					}
					break;
				case COMPLETED:
					intent.putExtra("state", "complete");
					avrcpIntent.putExtra("playing", false);
					break;
				default:
					return; // No need to broadcast.
			}
			addTrackInfo(context, song, avrcpIntent);

			if (state != PlayerState.PREPARED) {
				context.sendBroadcast(intent);
			}
			context.sendBroadcast(avrcpIntent);
		} catch(Exception e) {
			Log.e(TAG, "Failed to broadcastPlaybackStatusChange", e);
		}
    }

	private static void addTrackInfo(Context context, MusicDirectory.Entry song, Intent intent) {
		if (song != null) {
			DownloadService downloadService = (DownloadService)context;
			File albumArtFile = FileUtil.getAlbumArtFile(context, song);

			intent.putExtra("track", song.getTitle());
			intent.putExtra("artist", song.getArtist());
			intent.putExtra("album", song.getAlbum());
			intent.putExtra("ListSize", (long) downloadService.getSongs().size());
			intent.putExtra("id", (long) downloadService.getCurrentPlayingIndex() + 1);
			intent.putExtra("duration", (long) downloadService.getPlayerDuration());
			intent.putExtra("position", (long) downloadService.getPlayerPosition());
			intent.putExtra("coverart", albumArtFile.getAbsolutePath());
			intent.putExtra("package","github.daneren2005.dsub");
		} else {
			intent.putExtra("track", "");
			intent.putExtra("artist", "");
			intent.putExtra("album", "");
			intent.putExtra("ListSize", (long) 0);
			intent.putExtra("id", (long) 0);
			intent.putExtra("duration", (long) 0);
			intent.putExtra("position", (long) 0);
			intent.putExtra("coverart", "");
			intent.putExtra("package","github.daneren2005.dsub");
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

	public static Random getRandom() {
		if(random == null) {
			random = new SecureRandom();
		}

		return random;
	}
}
