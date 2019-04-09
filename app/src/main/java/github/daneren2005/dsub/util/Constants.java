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

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public final class Constants {

    // Character encoding used throughout.
    public static final String UTF_8 = "UTF-8";

    // REST protocol version and client ID.
    // Note: Keep it as low as possible to maintain compatibility with older servers.
    public static final String REST_PROTOCOL_VERSION_SUBSONIC = "1.2.0";
	public static final String REST_PROTOCOL_VERSION_MADSONIC = "2.0.0";
    public static final String REST_CLIENT_ID = "DSub";
    public static final String CHROMECAST_CLIENT_ID = "DSubCC";
	public static final String LAST_VERSION = "subsonic.version";

    // Names for intent extras.
    public static final String INTENT_EXTRA_NAME_ID = "subsonic.id";
    public static final String INTENT_EXTRA_NAME_NAME = "subsonic.name";
	public static final String INTENT_EXTRA_NAME_DIRECTORY = "subsonic.directory";
	public static final String INTENT_EXTRA_NAME_CHILD_ID = "subsonic.child.id";
    public static final String INTENT_EXTRA_NAME_ARTIST = "subsonic.artist";
    public static final String INTENT_EXTRA_NAME_TITLE = "subsonic.title";
    public static final String INTENT_EXTRA_NAME_AUTOPLAY = "subsonic.playall";
    public static final String INTENT_EXTRA_NAME_QUERY = "subsonic.query";
    public static final String INTENT_EXTRA_NAME_PLAYLIST_ID = "subsonic.playlist.id";
    public static final String INTENT_EXTRA_NAME_PLAYLIST_NAME = "subsonic.playlist.name";
    public static final String INTENT_EXTRA_NAME_PLAYLIST_OWNER = "subsonic.playlist.isOwner";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_TYPE = "subsonic.albumlisttype";
	public static final String INTENT_EXTRA_NAME_ALBUM_LIST_EXTRA = "subsonic.albumlistextra";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_SIZE = "subsonic.albumlistsize";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET = "subsonic.albumlistoffset";
    public static final String INTENT_EXTRA_NAME_SHUFFLE = "subsonic.shuffle";
    public static final String INTENT_EXTRA_REQUEST_SEARCH = "subsonic.requestsearch";
    public static final String INTENT_EXTRA_NAME_EXIT = "subsonic.exit" ;
	public static final String INTENT_EXTRA_NAME_DOWNLOAD = "subsonic.download";
	public static final String INTENT_EXTRA_NAME_DOWNLOAD_VIEW = "subsonic.download_view";
	public static final String INTENT_EXTRA_VIEW_ALBUM = "subsonic.view_album";
	public static final String INTENT_EXTRA_NAME_PODCAST_ID = "subsonic.podcast.id";
	public static final String INTENT_EXTRA_NAME_PODCAST_NAME = "subsonic.podcast.name";
	public static final String INTENT_EXTRA_NAME_PODCAST_DESCRIPTION = "subsonic.podcast.description";
	public static final String INTENT_EXTRA_NAME_SHARE = "subsonic.share";
	public static final String INTENT_EXTRA_FRAGMENT_TYPE = "fragmentType";
	public static final String INTENT_EXTRA_REFRESH_LISTINGS = "refreshListings";
	public static final String INTENT_EXTRA_SEARCH_SONG = "searchSong";
	public static final String INTENT_EXTRA_TOP_TRACKS = "topTracks";
	public static final String INTENT_EXTRA_SHOW_ALL = "showAll";
	public static final String INTENT_EXTRA_PLAY_LAST = "playLast";
	public static final String INTENT_EXTRA_ENTRY = "passedEntry";
	public static final String INTENT_EXTRA_ENTRY_BYTES = "passedEntryBytes";

    // Preferences keys.
	public static final String PREFERENCES_KEY_SERVER_KEY = "server";
	public static final String PREFERENCES_KEY_SERVER_COUNT = "serverCount";
	public static final String PREFERENCES_KEY_SERVER_ADD = "serverAdd";
	public static final String PREFERENCES_KEY_SERVER_REMOVE = "serverRemove";
    public static final String PREFERENCES_KEY_SERVER_INSTANCE = "serverInstanceId";
    public static final String PREFERENCES_KEY_SERVER_NAME = "serverName";
    public static final String PREFERENCES_KEY_SERVER_URL = "serverUrl";
	public static final String PREFERENCES_KEY_SERVER_INTERNAL_URL = "serverInternalUrl";
	public static final String PREFERENCES_KEY_SERVER_LOCAL_NETWORK_SSID = "serverLocalNetworkSSID";
	public static final String PREFERENCES_KEY_TEST_CONNECTION = "serverTestConnection";
	public static final String PREFERENCES_KEY_OPEN_BROWSER = "openBrowser";
    public static final String PREFERENCES_KEY_MUSIC_FOLDER_ID = "musicFolderId";
    public static final String PREFERENCES_KEY_USERNAME = "username";
    public static final String PREFERENCES_KEY_PASSWORD = "password";
    public static final String PREFERENCES_KEY_ENCRYPTED_PASSWORD = "encryptedPassword";
    public static final String PREFERENCES_KEY_INSTALL_TIME = "installTime";
    public static final String PREFERENCES_KEY_THEME = "theme";
    public static final String PREFERENCES_KEY_FULL_SCREEN = "fullScreen";
	public static final String PREFERENCES_KEY_DISPLAY_TRACK = "displayTrack";
    public static final String PREFERENCES_KEY_MAX_BITRATE_WIFI = "maxBitrateWifi";
    public static final String PREFERENCES_KEY_MAX_BITRATE_MOBILE = "maxBitrateMobile";
	public static final String PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI = "maxVideoBitrateWifi";
    public static final String PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE = "maxVideoBitrateMobile";
	public static final String PREFERENCES_KEY_NETWORK_TIMEOUT = "networkTimeout";
    public static final String PREFERENCES_KEY_CACHE_SIZE = "cacheSize";
    public static final String PREFERENCES_KEY_CACHE_LOCATION = "cacheLocation";
    public static final String PREFERENCES_KEY_PRELOAD_COUNT_WIFI = "preloadCountWifi";
	public static final String PREFERENCES_KEY_PRELOAD_COUNT_MOBILE = "preloadCountMobile";
    public static final String PREFERENCES_KEY_HIDE_MEDIA = "hideMedia";
    public static final String PREFERENCES_KEY_MEDIA_BUTTONS = "mediaButtons";
    public static final String PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD = "screenLitOnDownload";
    public static final String PREFERENCES_KEY_SCROBBLE = "scrobble";
    public static final String PREFERENCES_KEY_REPEAT_MODE = "repeatMode";
    public static final String PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD = "wifiRequiredForDownload";
    public static final String PREFERENCES_KEY_LOCAL_NETWORK_REQUIRED_FOR_DOWNLOAD = "localNetworkRequiredForDownload";
	public static final String PREFERENCES_KEY_RANDOM_SIZE = "randomSize";
	public static final String PREFERENCES_KEY_SLEEP_TIMER_DURATION = "sleepTimerDuration";
	public static final String PREFERENCES_KEY_OFFLINE = "offline";
	public static final String PREFERENCES_KEY_TEMP_LOSS = "tempLoss";
	public static final String PREFERENCES_KEY_SHUFFLE_START_YEAR = "startYear";
	public static final String PREFERENCES_KEY_SHUFFLE_END_YEAR = "endYear";
	public static final String PREFERENCES_KEY_SHUFFLE_GENRE = "genre";
	public static final String PREFERENCES_KEY_KEEP_SCREEN_ON = "keepScreenOn";
	public static final String PREFERENCES_EQUALIZER_ON = "equalizerOn";
	public static final String PREFERENCES_EQUALIZER_SETTINGS = "equalizerSettings";
	public static final String PREFERENCES_KEY_PERSISTENT_NOTIFICATION = "persistentNotification";
	public static final String PREFERENCES_KEY_GAPLESS_PLAYBACK = "gaplessPlayback";
	public static final String PREFERENCES_KEY_REMOVE_PLAYED = "removePlayed";
	public static final String PREFERENCES_KEY_KEEP_PLAYED_CNT = "keepPlayedCount";
	public static final String PREFERENCES_KEY_SHUFFLE_MODE = "shuffleMode2";
	public static final String PREFERENCES_KEY_SHUFFLE_MODE_EXTRA = "shuffleModeExtra";
	public static final String PREFERENCES_KEY_CHAT_REFRESH = "chatRefreshRate";
	public static final String PREFERENCES_KEY_CHAT_ENABLED = "chatEnabled";
	public static final String PREFERENCES_KEY_VIDEO_PLAYER = "videoPlayer";
	public static final String PREFERENCES_KEY_CONTROL_MODE = "remoteControlMode";
	public static final String PREFERENCES_KEY_CONTROL_ID = "remoteControlId";
	public static final String PREFERENCES_KEY_SYNC_ENABLED = "syncEnabled";
	public static final String PREFERENCES_KEY_SYNC_INTERVAL = "syncInterval";
	public static final String PREFERENCES_KEY_SYNC_WIFI = "syncWifi";
	public static final String PREFERENCES_KEY_SYNC_NOTIFICATION = "syncNotification";
	public static final String PREFERENCES_KEY_SYNC_STARRED = "syncStarred";
	public static final String PREFERENCES_KEY_SYNC_MOST_RECENT = "syncMostRecent";
	public static final String PREFERENCES_KEY_PAUSE_DISCONNECT = "pauseOnDisconnect";
	public static final String PREFERENCES_KEY_HIDE_WIDGET = "hideWidget";
	public static final String PREFERENCES_KEY_PODCASTS_ENABLED = "podcastsEnabled";
	public static final String PREFERENCES_KEY_BOOKMARKS_ENABLED = "bookmarksEnabled";
	public static final String PREFERENCES_KEY_INTERNET_RADIO_ENABLED = "internetRadioEnabled";
	public static final String PREFERENCES_KEY_CUSTOM_SORT_ENABLED = "customSortEnabled";
	public static final String PREFERENCES_KEY_MENU_PLAY_NOW = "showPlayNow";
	public static final String PREFERENCES_KEY_MENU_PLAY_SHUFFLED = "showPlayShuffled";
	public static final String PREFERENCES_KEY_MENU_PLAY_NEXT = "showPlayNext";
	public static final String PREFERENCES_KEY_MENU_PLAY_LAST = "showPlayLast";
	public static final String PREFERENCES_KEY_MENU_DOWNLOAD = "showDownload";
	public static final String PREFERENCES_KEY_MENU_PIN = "showPin";
	public static final String PREFERENCES_KEY_MENU_DELETE = "showDelete";
	public static final String PREFERENCES_KEY_MENU_STAR = "showStar";
	public static final String PREFERENCES_KEY_MENU_SHARED = "showShared";
	public static final String PREFERENCES_KEY_SHARED_ENABLED = "sharedEnabled";
	public static final String PREFERENCES_KEY_BROWSE_TAGS = "browseTags";
	public static final String PREFERENCES_KEY_OPEN_TO_TAB = "openToTab";
	public static final String PREFERENCES_KEY_OVERRIDE_SYSTEM_LANGUAGE = "overrideSystemLanguage";
	// public static final String PREFERENCES_KEY_PLAY_NOW_AFTER = "playNowAfter";
	public static final String PREFERENCES_KEY_SONG_PRESS_ACTION = "songPressAction";
	public static final String PREFERENCES_KEY_LARGE_ALBUM_ART = "largeAlbumArt";
	public static final String PREFERENCES_KEY_ADMIN_ENABLED = "adminEnabled";
	public static final String PREFERENCES_KEY_PLAYLIST_NAME = "suggestedPlaylistName";
	public static final String PREFERENCES_KEY_PLAYLIST_ID = "suggestedPlaylistId";
	public static final String PREFERENCES_KEY_SERVER_SYNC = "serverSync";
	public static final String PREFERENCES_KEY_RECENT_COUNT = "mostRecentCount";
	public static final String PREFERENCES_KEY_MENU_RATING = "showRating";
	public static final String PREFERENCES_KEY_REPLAY_GAIN = "replayGain";
	public static final String PREFERENCES_KEY_REPLAY_GAIN_BUMP = "replayGainBump2";
	public static final String PREFERENCES_KEY_REPLAY_GAIN_UNTAGGED = "replayGainUntagged2";
	public static final String PREFERENCES_KEY_REPLAY_GAIN_TYPE= "replayGainType";
	public static final String PREFERENCES_KEY_ALBUMS_PER_FOLDER = "albumsPerFolder";
	public static final String PREFERENCES_KEY_CAST_PROXY = "castProxy";
	public static final String PREFERENCES_KEY_DISABLE_EXIT_PROMPT = "disableExitPrompt";
	public static final String PREFERENCES_KEY_RENAME_DUPLICATES = "renameDuplicates";
	public static final String PREFERENCES_KEY_FIRST_LEVEL_ARTIST = "firstLevelArtist";
	public static final String PREFERENCES_KEY_START_ON_HEADPHONES = "startOnHeadphones";
	public static final String PREFERENCES_KEY_COLOR_ACTION_BAR = "colorActionBar";
	public static final String PREFERENCES_KEY_SHUFFLE_BY_ALBUM = "shuffleByAlbum";
	public static final String PREFERENCES_KEY_RESUME_PLAY_QUEUE_NEVER = "neverResumePlayQueue";
	public static final String PREFERENCES_KEY_BATCH_MODE = "batchMode";
	public static final String PREFERENCES_KEY_CAST_GAPLESS_PLAYBACK = "castingGaplessPlayback";
	public static final String PREFERENCES_KEY_CAST_STREAM_ORIGINAL = "castStreamOriginal";
	public static final String PREFERENCES_KEY_HEADS_UP_NOTIFICATION = "headsUpNotification";
	public static final String PREFERENCES_KEY_CAST_CACHE = "castCache";
	public static final String PREFERENCES_KEY_PLAYBACK_SPEED = "playbackSpeed";
	public static final String PREFERENCES_KEY_SONG_PLAYBACK_SPEED = "songPlaybackSpeed";
	public static final String PREFERENCES_KEY_DLNA_CASTING_ENABLED = "dlnaCastingEnabled";
	public static final String PREFERENCES_KEY_REWIND_INTERVAL = "rewindInterval";
	public static final String PREFERENCES_KEY_FASTFORWARD_INTERVAL = "fastforwardInterval";

	public static final String OFFLINE_SCROBBLE_COUNT = "scrobbleCount";
	public static final String OFFLINE_SCROBBLE_ID = "scrobbleID";
	public static final String OFFLINE_SCROBBLE_SEARCH = "scrobbleTitle";
	public static final String OFFLINE_SCROBBLE_TIME = "scrobbleTime";
	public static final String OFFLINE_STAR_COUNT = "starCount";
	public static final String OFFLINE_STAR_ID = "starID";
	public static final String OFFLINE_STAR_SEARCH = "starTitle";
	public static final String OFFLINE_STAR_SETTING = "starSetting";
	
	public static final String CACHE_KEY_IGNORE = "ignoreArticles";
	public static final String CACHE_AUDIO_SESSION_ID = "audioSessionId";
	public static final String CACHE_AUDIO_SESSION_VERSION_CODE = "audioSessionVersionCode";
	public static final String CACHE_BLOCK_TOKEN_USE = "blockTokenUse";
	
	public static final String MAIN_BACK_STACK = "backStackIds";
	public static final String MAIN_BACK_STACK_SIZE = "backStackIdsSize";
	public static final String MAIN_NOW_PLAYING = "nowPlayingId";
	public static final String MAIN_NOW_PLAYING_SECONDARY = "nowPlayingSecondaryId";
	public static final String MAIN_SLIDE_PANEL_STATE = "slidePanelState";
	public static final String FRAGMENT_LIST = "fragmentList";
	public static final String FRAGMENT_LIST2 = "fragmentList2";
	public static final String FRAGMENT_EXTRA = "fragmentExtra";
	public static final String FRAGMENT_DOWNLOAD_FLIPPER = "fragmentDownloadFlipper";
	public static final String FRAGMENT_NAME = "fragmentName";
	public static final String FRAGMENT_POSITION = "fragmentPosition";

    // Name of the preferences file.
    public static final String PREFERENCES_FILE_NAME = "github.daneren2005.dsub_preferences";
	public static final String OFFLINE_SYNC_NAME = "github.daneren2005.dsub.offline";
	public static final String OFFLINE_SYNC_DEFAULT = "syncDefaults";

	// Account prefs
	public static final String SYNC_ACCOUNT_NAME = "Subsonic Account";
	public static final String SYNC_ACCOUNT_TYPE = "subsonic.org";
	public static final String SYNC_ACCOUNT_PLAYLIST_AUTHORITY = "github.daneren2005.dsub.playlists.provider";
	public static final String SYNC_ACCOUNT_PODCAST_AUTHORITY = "github.daneren2005.dsub.podcasts.provider";
	public static final String SYNC_ACCOUNT_STARRED_AUTHORITY = "github.daneren2005.dsub.starred.provider";
	public static final String SYNC_ACCOUNT_MOST_RECENT_AUTHORITY = "github.daneren2005.dsub.mostrecent.provider";

	public static final String TASKER_EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";

    // Number of free trial days for non-licensed servers.
    public static final int FREE_TRIAL_DAYS = 30;

    // URL for project donations.
    public static final String DONATION_URL = "http://subsonic.org/pages/android-donation.jsp";

    public static final String ALBUM_ART_FILE = "albumart.jpg";

    private Constants() {
    }
}
