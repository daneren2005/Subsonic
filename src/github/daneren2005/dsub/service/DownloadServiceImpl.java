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
package github.daneren2005.dsub.service;

import static github.daneren2005.dsub.domain.PlayerState.COMPLETED;
import static github.daneren2005.dsub.domain.PlayerState.DOWNLOADING;
import static github.daneren2005.dsub.domain.PlayerState.IDLE;
import static github.daneren2005.dsub.domain.PlayerState.PAUSED;
import static github.daneren2005.dsub.domain.PlayerState.PREPARED;
import static github.daneren2005.dsub.domain.PlayerState.PREPARING;
import static github.daneren2005.dsub.domain.PlayerState.STARTED;
import static github.daneren2005.dsub.domain.PlayerState.STOPPED;
import github.daneren2005.dsub.audiofx.EqualizerController;
import github.daneren2005.dsub.audiofx.VisualizerController;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.domain.RepeatMode;
import github.daneren2005.dsub.receiver.MediaButtonIntentReceiver;
import github.daneren2005.dsub.util.CancellableTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ShufflePlayBuffer;
import github.daneren2005.dsub.util.SimpleServiceBinder;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.util.compat.RemoteControlClientHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.support.v4.util.LruCache;
import java.net.URLEncoder;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class DownloadServiceImpl extends Service implements DownloadService {

    private static final String TAG = DownloadServiceImpl.class.getSimpleName();

    public static final String CMD_PLAY = "github.daneren2005.dsub.CMD_PLAY";
    public static final String CMD_TOGGLEPAUSE = "github.daneren2005.dsub.CMD_TOGGLEPAUSE";
    public static final String CMD_PAUSE = "github.daneren2005.dsub.CMD_PAUSE";
    public static final String CMD_STOP = "github.daneren2005.dsub.CMD_STOP";
    public static final String CMD_PREVIOUS = "github.daneren2005.dsub.CMD_PREVIOUS";
    public static final String CMD_NEXT = "github.daneren2005.dsub.CMD_NEXT";

    
    private RemoteControlClientHelper mRemoteControl;
    
    private final IBinder binder = new SimpleServiceBinder<DownloadService>(this);
	private Looper mediaPlayerLooper;
    private MediaPlayer mediaPlayer;
	private MediaPlayer nextMediaPlayer;
	private boolean nextSetup = false;
	private boolean isPartial = true;
    private final List<DownloadFile> downloadList = new ArrayList<DownloadFile>();
	private final List<DownloadFile> backgroundDownloadList = new ArrayList<DownloadFile>();
	private final Handler handler = new Handler();
	private Handler mediaPlayerHandler;
    private final DownloadServiceLifecycleSupport lifecycleSupport = new DownloadServiceLifecycleSupport(this);
    private final ShufflePlayBuffer shufflePlayBuffer = new ShufflePlayBuffer(this);

    private final LruCache<MusicDirectory.Entry, DownloadFile> downloadFileCache = new LruCache<MusicDirectory.Entry, DownloadFile>(100);
    private final List<DownloadFile> cleanupCandidates = new ArrayList<DownloadFile>();
    private final Scrobbler scrobbler = new Scrobbler();
	private RemoteController remoteController;
    private DownloadFile currentPlaying;
	private DownloadFile nextPlaying;
    private DownloadFile currentDownloading;
    private CancellableTask bufferTask;
	private CancellableTask nextPlayingTask;
    private PlayerState playerState = IDLE;
	private PlayerState nextPlayerState = IDLE;
    private boolean shufflePlay;
    private long revision;
    private static DownloadService instance;
    private String suggestedPlaylistName;
	private String suggestedPlaylistId;
    private PowerManager.WakeLock wakeLock;
    private boolean keepScreenOn;
	private int cachedPosition = 0;
	private long downloadRevision;
	private boolean downloadOngoing = false;

    private static boolean equalizerAvailable;
    private static boolean visualizerAvailable;
    private EqualizerController equalizerController;
    private VisualizerController visualizerController;
    private boolean showVisualization;
    private RemoteControlState remoteState = RemoteControlState.LOCAL;
	private PositionCache positionCache;
	private StreamProxy proxy;
	
	private Timer sleepTimer;
	private int timerDuration;
	private boolean autoPlayStart = false;

    static {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			equalizerAvailable = true;
			visualizerAvailable = true;
		}
    }

	@Override
    public void onCreate() {
        super.onCreate();

		new Thread(new Runnable() {
			public void run() {
				Looper.prepare();
				
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setWakeMode(DownloadServiceImpl.this, PowerManager.PARTIAL_WAKE_LOCK);

				mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mediaPlayer, int what, int more) {
						handleError(new Exception("MediaPlayer error: " + what + " (" + more + ")"));
						return false;
					}
				});
				
				try {
					Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
					i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.getAudioSessionId());
					i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
					sendBroadcast(i);
				} catch(Throwable e) {
					// Froyo or lower
				}
				
				mediaPlayerLooper = Looper.myLooper();
				mediaPlayerHandler = new Handler(mediaPlayerLooper);
				Looper.loop();
			}
		}).start();

        Util.registerMediaButtonEventReceiver(this);

        if (mRemoteControl == null) {
        	// Use the remote control APIs (if available) to set the playback state
        	mRemoteControl = RemoteControlClientHelper.createInstance();
        	ComponentName mediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        	mRemoteControl.register(this, mediaButtonReceiverComponent);
        }

		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
		wakeLock.setReferenceCounted(false);
		
		SharedPreferences prefs = Util.getPreferences(this);
		try {
			timerDuration = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_SLEEP_TIMER_DURATION, "5"));
		} catch(Throwable e) {
			timerDuration = 5;
		}
		sleepTimer = null;
		
		keepScreenOn = prefs.getBoolean(Constants.PREFERENCES_KEY_KEEP_SCREEN_ON, false);

		instance = this;
		lifecycleSupport.onCreate();

		if(prefs.getBoolean(Constants.PREFERENCES_EQUALIZER_ON, false)) {
			getEqualizerController();
		}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        lifecycleSupport.onStart(intent);
        return START_NOT_STICKY;
    }

	@Override
    public void onDestroy() {
        super.onDestroy();
		instance = null;
		
		if(currentPlaying != null) currentPlaying.setPlaying(false);
		if(sleepTimer != null){
			sleepTimer.cancel();
			sleepTimer.purge();
		}
        lifecycleSupport.onDestroy();

		try {
			Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
			i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.getAudioSessionId());
			i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
			sendBroadcast(i);
		} catch(Throwable e) {
			// Froyo or lower
		}
        
        mediaPlayer.release();
        if(nextMediaPlayer != null) {
			nextMediaPlayer.release();
        }
		mediaPlayerLooper.quit();
        shufflePlayBuffer.shutdown();
        if (equalizerController != null) {
            equalizerController.release();
        }
        if (visualizerController != null) {
            visualizerController.release();
        }
        if (mRemoteControl != null) {
        	mRemoteControl.unregister(this);
        	mRemoteControl = null;
        }
		
		if(bufferTask != null) {
			bufferTask.cancel();
		}
		if(nextPlayingTask != null) {
			nextPlayingTask.cancel();
		}
		if(remoteController != null) {
			remoteController.stop();
			remoteController.shutdown();
		}
		Util.hidePlayingNotification(this, this, handler);
    }

    public static DownloadService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

	@Override
	public synchronized void download(Bookmark bookmark) {
		clear();
		DownloadFile downloadFile = new DownloadFile(this, bookmark.getEntry(), false);
		downloadList.add(downloadFile);
		revision++;
		updateJukeboxPlaylist();
		play(0, true, bookmark.getPosition());
		lifecycleSupport.serializeDownloadQueue();
	}

    @Override
    public synchronized void download(List<MusicDirectory.Entry> songs, boolean save, boolean autoplay, boolean playNext, boolean shuffle) {
        setShufflePlayEnabled(false);
        int offset = 1;

        if (songs.isEmpty()) {
            return;
        }
        if (playNext) {
            if (autoplay && getCurrentPlayingIndex() >= 0) {
                offset = 0;
            }
            for (MusicDirectory.Entry song : songs) {
                DownloadFile downloadFile = new DownloadFile(this, song, save);
                downloadList.add(getCurrentPlayingIndex() + offset, downloadFile);
                offset++;
            }
            revision++;
        } else {
			int size = size();
			int index = getCurrentPlayingIndex();
            for (MusicDirectory.Entry song : songs) {
                DownloadFile downloadFile = new DownloadFile(this, song, save);
                downloadList.add(downloadFile);
            }
			if(!autoplay && (size - 1) == index) {
				setNextPlaying();
			}
            revision++;
        }
        updateJukeboxPlaylist();
		
		if(shuffle) {
			shuffle();
		}

        if (autoplay) {
            play(0);
        } else {
            if (currentPlaying == null) {
                currentPlaying = downloadList.get(0);
				currentPlaying.setPlaying(true);
            }
            checkDownloads();
        }
        lifecycleSupport.serializeDownloadQueue();
    }
	public synchronized void downloadBackground(List<MusicDirectory.Entry> songs, boolean save) {
		for (MusicDirectory.Entry song : songs) {
			DownloadFile downloadFile = new DownloadFile(this, song, save);
			if(!downloadFile.isWorkDone() || (downloadFile.shouldSave() && !downloadFile.isSaved())) {
				// Only add to list if there is work to be done
				backgroundDownloadList.add(downloadFile);
			}
		}
		revision++;
		
		checkDownloads();
		lifecycleSupport.serializeDownloadQueue();
	}

    private void updateJukeboxPlaylist() {
        if (remoteState != RemoteControlState.LOCAL) {
            remoteController.updatePlaylist();
        }
    }

    public void restore(List<MusicDirectory.Entry> songs, int currentPlayingIndex, int currentPlayingPosition) {
		SharedPreferences prefs = Util.getPreferences(this);
		remoteState = RemoteControlState.values()[prefs.getInt(Constants.PREFERENCES_KEY_CONTROL_MODE, 0)];
		if(remoteState != RemoteControlState.LOCAL) {
			setRemoteState(remoteState);
		}
		boolean startShufflePlay = prefs.getBoolean(Constants.PREFERENCES_KEY_SHUFFLE_MODE, false);
        download(songs, false, false, false, false);
		if(startShufflePlay) {
			shufflePlay = true;
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREFERENCES_KEY_SHUFFLE_MODE, true);
			editor.commit();
		}
        if (currentPlayingIndex != -1) {
        	while(mediaPlayer == null) {
        		Util.sleepQuietly(50L);
        	}
        	
            play(currentPlayingIndex, false);
            if (currentPlaying != null && currentPlaying.isCompleteFileAvailable() && remoteState == RemoteControlState.LOCAL) {
                doPlay(currentPlaying, currentPlayingPosition, autoPlayStart);
            }
			autoPlayStart = false;
        }
    }

    @Override
    public synchronized void setShufflePlayEnabled(boolean enabled) {
        shufflePlay = enabled;
        if (shufflePlay) {
            clear();
            checkDownloads();
        }
		SharedPreferences.Editor editor = Util.getPreferences(this).edit();
		editor.putBoolean(Constants.PREFERENCES_KEY_SHUFFLE_MODE, enabled);
		editor.commit();
    }

    @Override
    public boolean isShufflePlayEnabled() {
        return shufflePlay;
    }

    @Override
    public synchronized void shuffle() {
        Collections.shuffle(downloadList);
        if (currentPlaying != null) {
            downloadList.remove(getCurrentPlayingIndex());
            downloadList.add(0, currentPlaying);
        }
        revision++;
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
		setNextPlaying();
    }

    @Override
    public RepeatMode getRepeatMode() {
        return Util.getRepeatMode(this);
    }

    @Override
    public void setRepeatMode(RepeatMode repeatMode) {
        Util.setRepeatMode(this, repeatMode);
		setNextPlaying();
    }

    @Override
    public boolean getKeepScreenOn() {
    	return keepScreenOn;
    }

    @Override
    public void setKeepScreenOn(boolean keepScreenOn) {
    	this.keepScreenOn = keepScreenOn;
		
		SharedPreferences prefs = Util.getPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.PREFERENCES_KEY_KEEP_SCREEN_ON, keepScreenOn);
		editor.commit();
    }

    @Override
    public boolean getShowVisualization() {
        return showVisualization;
    }

    @Override
    public void setShowVisualization(boolean showVisualization) {
        this.showVisualization = showVisualization;
    }

    @Override
    public synchronized DownloadFile forSong(MusicDirectory.Entry song) {
    	DownloadFile returnFile = null;
        for (DownloadFile downloadFile : downloadList) {
            if (downloadFile.getSong().equals(song)) {
            	if(((downloadFile.isDownloading() && !downloadFile.isDownloadCancelled() && downloadFile.getPartialFile().exists()) || downloadFile.isWorkDone())) {
            		// If downloading, return immediately
                	return downloadFile;
				} else {
					// Otherwise, check to make sure there isn't a background download going on first
					returnFile = downloadFile;
				}
            }
        }
		for (DownloadFile downloadFile : backgroundDownloadList) {
            if (downloadFile.getSong().equals(song)) {
                return downloadFile;
            }
        }
        
        if(returnFile != null) {
        	return returnFile;
        }

        DownloadFile downloadFile = downloadFileCache.get(song);
        if (downloadFile == null) {
            downloadFile = new DownloadFile(this, song, false);
            downloadFileCache.put(song, downloadFile);
        }
        return downloadFile;
    }

    @Override
    public synchronized void clear() {
        clear(true);
    }
	
	@Override
	public synchronized void clearBackground() {
		if(currentDownloading != null && backgroundDownloadList.contains(currentDownloading)) {
			currentDownloading.cancelDownload();
			currentDownloading = null;
		}
		backgroundDownloadList.clear();
	}

    @Override
    public synchronized void clearIncomplete() {
        reset();
        Iterator<DownloadFile> iterator = downloadList.iterator();
        while (iterator.hasNext()) {
            DownloadFile downloadFile = iterator.next();
            if (!downloadFile.isCompleteFileAvailable()) {
                iterator.remove();
            }
        }
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
    }

    @Override
    public synchronized int size() {
        return downloadList.size();
    }

    public synchronized void clear(boolean serialize) {
        reset();
        downloadList.clear();
        revision++;
        if (currentDownloading != null) {
            currentDownloading.cancelDownload();
            currentDownloading = null;
        }
        setCurrentPlaying(null, false);

        if (serialize) {
            lifecycleSupport.serializeDownloadQueue();
        }
        updateJukeboxPlaylist();
		setNextPlaying();
    }
	
	@Override
    public synchronized void remove(int which) {
		downloadList.remove(which);
	}

    @Override
    public synchronized void remove(DownloadFile downloadFile) {
        if (downloadFile == currentDownloading) {
            currentDownloading.cancelDownload();
            currentDownloading = null;
        }
        if (downloadFile == currentPlaying) {
            reset();
            setCurrentPlaying(null, false);
        }
        downloadList.remove(downloadFile);
		backgroundDownloadList.remove(downloadFile);
        revision++;
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
		if(downloadFile == nextPlaying) {
			setNextPlaying();
		}
    }

    @Override
    public synchronized void delete(List<MusicDirectory.Entry> songs) {
        for (MusicDirectory.Entry song : songs) {
            forSong(song).delete();
        }
    }

    @Override
    public synchronized void unpin(List<MusicDirectory.Entry> songs) {
        for (MusicDirectory.Entry song : songs) {
            forSong(song).unpin();
        }
    }

    synchronized void setCurrentPlaying(int currentPlayingIndex, boolean showNotification) {
        try {
            setCurrentPlaying(downloadList.get(currentPlayingIndex), showNotification);
        } catch (IndexOutOfBoundsException x) {
            // Ignored
        }
    }

	synchronized void setCurrentPlaying(DownloadFile currentPlaying, boolean showNotification) {
		if(this.currentPlaying != null) {
			this.currentPlaying.setPlaying(false);
		}
        this.currentPlaying = currentPlaying;

        if (currentPlaying != null) {
        	Util.broadcastNewTrackInfo(this, currentPlaying.getSong());
			mRemoteControl.updateMetadata(this, currentPlaying.getSong());
        } else {
            Util.broadcastNewTrackInfo(this, null);
			Util.hidePlayingNotification(this, this, handler);
        }
    }
	
	synchronized void setNextPlaying() {
		SharedPreferences prefs = Util.getPreferences(DownloadServiceImpl.this);
		boolean gaplessPlayback = prefs.getBoolean(Constants.PREFERENCES_KEY_GAPLESS_PLAYBACK, true);
		if(!gaplessPlayback) {
			nextPlaying = null;
			nextPlayerState = IDLE;
			return;
		}
		
		int index = getNextPlayingIndex();
		
		nextSetup = false;
		if(nextPlayingTask != null) {
			nextPlayingTask.cancel();
			nextPlayingTask = null;
		}
		if(index < size() && index != -1) {
			nextPlaying = downloadList.get(index);
			nextPlayingTask = new CheckCompletionTask(nextPlaying);
			nextPlayingTask.start();
		} else {
			nextPlaying = null;
			setNextPlayerState(IDLE);
		}
	}

    @Override
    public synchronized int getCurrentPlayingIndex() {
        return downloadList.indexOf(currentPlaying);
    }
    private int getNextPlayingIndex() {
    	int index = getCurrentPlayingIndex();
		if (index != -1) {
            switch (getRepeatMode()) {
                case OFF:
                    index = index + 1;
                    break;
                case ALL:
					index = (index + 1) % size();
                    break;
                case SINGLE:
                    break;
                default:
                    break;
            }
        }
        return index;
    }

    @Override
    public DownloadFile getCurrentPlaying() {
        return currentPlaying;
    }

    @Override
    public DownloadFile getCurrentDownloading() {
        return currentDownloading;
    }
	
	@Override
	public List<DownloadFile> getSongs() {
		return downloadList;
	}

    @Override
    public synchronized List<DownloadFile> getDownloads() {
		List<DownloadFile> temp = new ArrayList<DownloadFile>();
		temp.addAll(downloadList);
		temp.addAll(backgroundDownloadList);
        return temp;
    }
	
	@Override
	public List<DownloadFile> getBackgroundDownloads() {
		return backgroundDownloadList;
	}

    /** Plays either the current song (resume) or the first/next one in queue. */
    public synchronized void play()
    {
        int current = getCurrentPlayingIndex();
        if (current == -1) {
            play(0);
        } else {
            play(current);
        }
    }

    @Override
    public synchronized void play(int index) {
        play(index, true);
    }
	private synchronized void play(int index, boolean start) {
		play(index, start, 0);
	}
    private synchronized void play(int index, boolean start, int position) {
        if (index < 0 || index >= size()) {
            reset();
            setCurrentPlaying(null, false);
			lifecycleSupport.serializeDownloadQueue();
        } else {
			if(nextPlayingTask != null) {
				nextPlayingTask.cancel();
				nextPlayingTask = null;
			}
            setCurrentPlaying(index, start);
            if (start) {
                if (remoteState != RemoteControlState.LOCAL) {
					remoteController.changeTrack(index, downloadList.get(index));
                    setPlayerState(STARTED);
                } else {
                    bufferAndPlay(position);
                }
            }
			if (remoteState == RemoteControlState.LOCAL) {
				checkDownloads();
				setNextPlaying();
			}
        }
    }
	private synchronized void playNext() {
		if(nextPlaying != null && nextPlayerState == PlayerState.PREPARED) {
			if(!nextSetup) {
				playNext(true);
			} else {
				nextSetup = false;
				playNext(false);
			}
		} else {
			onSongCompleted();
		}
	}
	private synchronized void playNext(boolean start) {
		// Swap the media players since nextMediaPlayer is ready to play
		if(start) {
			nextMediaPlayer.start();
		} else {
			Log.i(TAG, "nextMediaPlayer already playing");
		}
		MediaPlayer tmp = mediaPlayer;
		mediaPlayer = nextMediaPlayer;
		nextMediaPlayer = tmp;
		setCurrentPlaying(nextPlaying, true);
		setPlayerState(PlayerState.STARTED);
		setupHandlers(currentPlaying, false);
		setNextPlaying();
		
		// Proxy should not be being used here since the next player was already setup to play
		if(proxy != null) {
			proxy.stop();
			proxy = null;
		}
	}

    /** Plays or resumes the playback, depending on the current player state. */
    public synchronized void togglePlayPause() {
        if (playerState == PAUSED || playerState == COMPLETED || playerState == STOPPED) {
            start();
        } else if (playerState == STOPPED || playerState == IDLE) {
			autoPlayStart = true;
        	play();
        } else if (playerState == STARTED) {
            pause();
        }
    }

    @Override
    public synchronized void seekTo(int position) {
        try {
            if (remoteState != RemoteControlState.LOCAL) {
				remoteController.changePosition(position / 1000);
            } else {
                mediaPlayer.seekTo(position);
				cachedPosition = position;
            }
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized void previous() {
        int index = getCurrentPlayingIndex();
        if (index == -1) {
            return;
        }

        // Restart song if played more than five seconds.
        if (getPlayerPosition() > 5000 || index == 0) {
            play(index);
        } else {
            play(index - 1);
        }
    }

    @Override
    public synchronized void next() {
        int index = getCurrentPlayingIndex();
		int nextPlayingIndex = getNextPlayingIndex();
		// Make sure to actually go to next when repeat song is on
		if(index == nextPlayingIndex) {
			nextPlayingIndex++;
		}
        if (index != -1 && nextPlayingIndex < size()) {
            play(nextPlayingIndex);
        }
    }

    private void onSongCompleted() {
    	play(getNextPlayingIndex());
    }

    @Override
    public synchronized void pause() {
        try {
            if (playerState == STARTED) {
                if (remoteState != RemoteControlState.LOCAL) {
					remoteController.stop();
                } else {
                    mediaPlayer.pause();
                }
                setPlayerState(PAUSED);
            }
        } catch (Exception x) {
            handleError(x);
        }
    }
	
	@Override
	public synchronized void stop() {
		try {
			if (playerState == STARTED) {
                if (remoteState != RemoteControlState.LOCAL) {
					remoteController.stop();
                } else {
                    mediaPlayer.pause();
                }
                setPlayerState(STOPPED);
            } else if(playerState == PAUSED) {
				setPlayerState(STOPPED);
			}
		} catch(Exception x) {
			handleError(x);
		}
	}

    @Override
    public synchronized void start() {
        try {
            if (remoteState != RemoteControlState.LOCAL) {
				remoteController.start();
            } else {
                mediaPlayer.start();
			}
            setPlayerState(STARTED);
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized void reset() {
        if (bufferTask != null) {
            bufferTask.cancel();
        }
        try {
			setPlayerState(IDLE);
			mediaPlayer.setOnErrorListener(null);
			mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.reset();
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public int getPlayerPosition() {
        try {
            if (playerState == IDLE || playerState == DOWNLOADING || playerState == PREPARING) {
                return 0;
            }
            if (remoteState != RemoteControlState.LOCAL) {
				return remoteController.getRemotePosition() * 1000;
            } else {
                return cachedPosition;
            }
        } catch (Exception x) {
            handleError(x);
            return 0;
        }
    }

    @Override
    public synchronized int getPlayerDuration() {
        if (currentPlaying != null) {
            Integer duration = currentPlaying.getSong().getDuration();
            if (duration != null) {
                return duration * 1000;
            }
        }
        if (playerState != IDLE && playerState != DOWNLOADING && playerState != PlayerState.PREPARING) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception x) {
                handleError(x);
            }
        }
        return 0;
    }

    @Override
    public PlayerState getPlayerState() {
        return playerState;
    }

	public synchronized void setPlayerState(final PlayerState playerState) {
        Log.i(TAG, this.playerState.name() + " -> " + playerState.name() + " (" + currentPlaying + ")");

        if (playerState == PAUSED) {
            lifecycleSupport.serializeDownloadQueue();
        }

        boolean show = playerState == PlayerState.STARTED;
        boolean pause = playerState == PlayerState.PAUSED;
		boolean hide = playerState == PlayerState.STOPPED;
        Util.broadcastPlaybackStatusChange(this, playerState);

        this.playerState = playerState;
		
		if(playerState == STARTED) {
			Util.requestAudioFocus(this);
		}
        
        if (show) {
            Util.showPlayingNotification(this, this, handler, currentPlaying.getSong());
        } else if (pause) {
			SharedPreferences prefs = Util.getPreferences(this);
			if(prefs.getBoolean(Constants.PREFERENCES_KEY_PERSISTENT_NOTIFICATION, false)) {
				Util.showPlayingNotification(this, this, handler, currentPlaying.getSong());
			} else {
				Util.hidePlayingNotification(this, this, handler);
			}
        } else if(hide) {
			Util.hidePlayingNotification(this, this, handler);
		}
		mRemoteControl.setPlaybackState(playerState.getRemoteControlClientPlayState());

        if (playerState == STARTED) {
            scrobbler.scrobble(this, currentPlaying, false);
        } else if (playerState == COMPLETED) {
            scrobbler.scrobble(this, currentPlaying, true);
        }
		
		if(playerState == STARTED && positionCache == null) {
			positionCache = new PositionCache();
			Thread thread = new Thread(positionCache);
			thread.start();
		} else if(playerState != STARTED && positionCache != null) {
			positionCache.stop();
			positionCache = null;
		}
    }
	
	private class PositionCache implements Runnable {
		boolean isRunning = true;

		public void stop() {
			isRunning = false;
		}

		@Override
		public void run() {
			// Stop checking position before the song reaches completion
			while(isRunning) {
				try {
					if(mediaPlayer != null && playerState == STARTED) {
						cachedPosition = mediaPlayer.getCurrentPosition();
					}
					Thread.sleep(200L);
				}
				catch(Exception e) {
					Log.w(TAG, "Crashed getting current position", e);
					isRunning = false;
					positionCache = null;
				}
			}
		}
	}
	
	private void setPlayerStateCompleted() {
		Log.i(TAG, this.playerState.name() + " -> " + PlayerState.COMPLETED + " (" + currentPlaying + ")");
		this.playerState = PlayerState.COMPLETED;
		if(positionCache != null) {
			positionCache.stop();
			positionCache = null;
		}
		scrobbler.scrobble(this, currentPlaying, true);
	}
	
	private synchronized void setNextPlayerState(PlayerState playerState) {
        Log.i(TAG, "Next: " + this.nextPlayerState.name() + " -> " + playerState.name() + " (" + nextPlaying + ")");
        this.nextPlayerState = playerState;
    }

    @Override
    public void setSuggestedPlaylistName(String name, String id) {
        this.suggestedPlaylistName = name;
		this.suggestedPlaylistId = id;
    }

    @Override
    public String getSuggestedPlaylistName() {
        return suggestedPlaylistName;
    }
	
	@Override
	public String getSuggestedPlaylistId() {
		return suggestedPlaylistId;
	}
	
	@Override
    public boolean getEqualizerAvailable() {
        return equalizerAvailable;
    }

    @Override
    public boolean getVisualizerAvailable() {
        return visualizerAvailable;
    }

    @Override
    public EqualizerController getEqualizerController() {
		if (equalizerAvailable && equalizerController == null) {
			equalizerController = new EqualizerController(this, mediaPlayer);
			if (!equalizerController.isAvailable()) {
				equalizerController = null;
			} else {
				equalizerController.loadSettings();
			}
		}
        return equalizerController;
    }

    @Override
    public VisualizerController getVisualizerController() {
		if (visualizerAvailable && visualizerController == null) {
			visualizerController = new VisualizerController(this, mediaPlayer);
			if (!visualizerController.isAvailable()) {
				visualizerController = null;
			}
		}
        return visualizerController;
    }

    @Override
    public boolean isRemoteEnabled() {
        return remoteState != RemoteControlState.LOCAL;
    }

    @Override
    public void setRemoteEnabled(RemoteControlState newState) {
		setRemoteState(newState);
        
    	SharedPreferences.Editor editor = Util.getPreferences(this).edit();
		editor.putInt(Constants.PREFERENCES_KEY_CONTROL_MODE, newState.getValue());
		editor.commit();
    }
	private void setRemoteState(RemoteControlState newState) {
		if(remoteController != null) {
			remoteController.stop();
			setPlayerState(PlayerState.IDLE);
			remoteController.shutdown();
			remoteController = null;
		}

		remoteState = newState;
		switch(newState) {
			case JUKEBOX_SERVER:
				remoteController = new JukeboxController(this, handler);
				break;
			case LOCAL: default:
				break;
		}

		if (remoteState != RemoteControlState.LOCAL) {
			reset();

			// Cancel current download, if necessary.
			if (currentDownloading != null) {
				currentDownloading.cancelDownload();
			}
		}
	}

    @Override
    public void setRemoteVolume(boolean up) {
		remoteController.setVolume(up);
    }

	private synchronized void bufferAndPlay() {
		bufferAndPlay(0);
	}
    private synchronized void bufferAndPlay(int position) {
		if(playerState != PREPARED) {
			reset();

			bufferTask = new BufferTask(currentPlaying, position);
			bufferTask.start();
		} else {
			doPlay(currentPlaying, position, true);
		}
    }

    private synchronized void doPlay(final DownloadFile downloadFile, final int position, final boolean start) {
        try {
			downloadFile.setPlaying(true);
            final File file = downloadFile.isCompleteFileAvailable() ? downloadFile.getCompleteFile() : downloadFile.getPartialFile();
			isPartial = file.equals(downloadFile.getPartialFile());
            downloadFile.updateModificationDate();
			
			mediaPlayer.setOnCompletionListener(null);
			mediaPlayer.reset();
			setPlayerState(IDLE);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			String dataSource = file.getPath();
			if(isPartial) {
				if (proxy == null) {
					proxy = new StreamProxy(this);
					proxy.start();
				}
				dataSource = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), URLEncoder.encode(dataSource, Constants.UTF_8));
				Log.i(TAG, "Data Source: " + dataSource);
			} else if(proxy != null) {
				proxy.stop();
				proxy = null;
			}
			mediaPlayer.setDataSource(dataSource);
			setPlayerState(PREPARING);

			mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					Log.i(TAG, "Buffered " + percent + "%");
					if(percent == 100) {
						mediaPlayer.setOnBufferingUpdateListener(null);
					}
				}
			});

			mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mediaPlayer) {
					try {
						setPlayerState(PREPARED);

						synchronized (DownloadServiceImpl.this) {
							if (position != 0) {
								Log.i(TAG, "Restarting player from position " + position);
								mediaPlayer.seekTo(position);
							}
							cachedPosition = position;

							if (start) {
								mediaPlayer.start();
								setPlayerState(STARTED);
							} else {
								setPlayerState(PAUSED);
							}
						}

						// Only call when starting, setPlayerState(PAUSED) already calls this
						if(start) {
							lifecycleSupport.serializeDownloadQueue();
						}
					} catch (Exception x) {
						handleError(x);
					}
				}
			});

			setupHandlers(downloadFile, isPartial);
			
			mediaPlayer.prepareAsync();
        } catch (Exception x) {
            handleError(x);
        }
    }
	
	private synchronized void setupNext(final DownloadFile downloadFile) {
		try {
            final File file = downloadFile.isCompleteFileAvailable() ? downloadFile.getCompleteFile() : downloadFile.getPartialFile();
            if(nextMediaPlayer != null) {
            	nextMediaPlayer.setOnCompletionListener(null);
            	nextMediaPlayer.setOnErrorListener(null);
            	nextMediaPlayer.reset();
            	nextMediaPlayer.release();
            	nextMediaPlayer = null;
            }
            nextMediaPlayer = new MediaPlayer();
			nextMediaPlayer.setWakeMode(DownloadServiceImpl.this, PowerManager.PARTIAL_WAKE_LOCK);
			try {
            	nextMediaPlayer.setAudioSessionId(mediaPlayer.getAudioSessionId());
			} catch(Throwable e) {
				nextMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			}
            nextMediaPlayer.setDataSource(file.getPath());
            setNextPlayerState(PREPARING);
			
			nextMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					try {
						setNextPlayerState(PREPARED);
						
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && (playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED)) {
							mediaPlayer.setNextMediaPlayer(nextMediaPlayer);
							nextSetup = true;
						}
					} catch (Exception x) {
						handleErrorNext(x);
					}
				}
			});
			
			nextMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
					Log.w(TAG, "Error on playing next " + "(" + what + ", " + extra + "): " + downloadFile);
					return true;
				}
			});
			
			nextMediaPlayer.prepareAsync();
        } catch (Exception x) {
            handleErrorNext(x);
        }
	}
	
	private void setupHandlers(final DownloadFile downloadFile, final boolean isPartial) {
		final int duration = downloadFile.getSong().getDuration() == null ? 0 : downloadFile.getSong().getDuration() * 1000;
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
				Log.w(TAG, "Error on playing file " + "(" + what + ", " + extra + "): " + downloadFile);
				int pos = cachedPosition;
				reset();
				if (!isPartial || (downloadFile.isWorkDone() && (Math.abs(duration - pos) < 10000))) {
					playNext();
				} else {
					downloadFile.setPlaying(false);
					doPlay(downloadFile, pos, true);
					downloadFile.setPlaying(true);
				}
				return true;
			}
		});
		
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mediaPlayer) {
				// Acquire a temporary wakelock, since when we return from
				// this callback the MediaPlayer will release its wakelock
				// and allow the device to go to sleep.
				wakeLock.acquire(60000);
				
				setPlayerStateCompleted();

				int pos = cachedPosition;
				Log.i(TAG, "Ending position " + pos + " of " + duration);
				if (!isPartial || (downloadFile.isWorkDone() && (Math.abs(duration - pos) < 10000))) {
					playNext();
					return;
				}

				// If file is not completely downloaded, restart the playback from the current position.
				synchronized (DownloadServiceImpl.this) {
					if(downloadFile.isWorkDone()) {
						// Complete was called early even though file is fully buffered
						Log.i(TAG, "Requesting restart from " + pos + " of " + duration);
						reset();
						downloadFile.setPlaying(false);
						doPlay(downloadFile, pos, true);
						downloadFile.setPlaying(true);
					} else {
						Log.i(TAG, "Requesting restart from " + pos + " of " + duration);
						reset();
						bufferTask = new BufferTask(downloadFile, pos);
						bufferTask.start();
					} 
				}
			}
		});
	}
	
	@Override
	public void setSleepTimerDuration(int duration){
		timerDuration = duration;
	}
	
	@Override
	public void startSleepTimer(){
		if(sleepTimer != null){
			sleepTimer.cancel();
			sleepTimer.purge();
		}

		sleepTimer = new Timer();		

		sleepTimer.schedule(new TimerTask() {			
			@Override
			public void run() {
				pause();
				sleepTimer.cancel();
				sleepTimer.purge();
				sleepTimer = null;
			}

		}, timerDuration * 60 * 1000);
	}
	
	@Override
	public void stopSleepTimer() {
		if(sleepTimer != null){
			sleepTimer.cancel();
			sleepTimer.purge();
		}
		sleepTimer = null;
	}
	
	@Override
	public boolean getSleepTimer() {
		return sleepTimer != null;
	}
	
	@Override
	public void setVolume(float volume) {
		if(mediaPlayer != null) {
			mediaPlayer.setVolume(volume, volume);
		}
	}
	
	@Override
	public synchronized void swap(boolean mainList, int from, int to) {
		List<DownloadFile> list = mainList ? downloadList : backgroundDownloadList;
		int max = list.size();
		if(to >= max) {
			to = max - 1;
		}
		else if(to < 0) {
			to = 0;
		}
		
		int currentPlayingIndex = getCurrentPlayingIndex();
		DownloadFile movedSong = list.remove(from);
		list.add(to, movedSong);
		if(remoteState != RemoteControlState.LOCAL && mainList) {
			updateJukeboxPlaylist();
		} else if(mainList && (movedSong == nextPlaying || (currentPlayingIndex + 1) == to)) {
			// Moving next playing or moving a song to be next playing
			setNextPlaying();
		}
	}

    private void handleError(Exception x) {
        Log.w(TAG, "Media player error: " + x, x);
        if(mediaPlayer != null) {
        	mediaPlayer.reset();
        }
        setPlayerState(IDLE);
    }
	private void handleErrorNext(Exception x) {
		Log.w(TAG, "Next Media player error: " + x, x);
        nextMediaPlayer.reset();
		setNextPlayerState(IDLE);
	}

    protected synchronized void checkDownloads() {
        if (!Util.isExternalStoragePresent() || !lifecycleSupport.isExternalStorageAvailable()) {
            return;
        }

        if (shufflePlay) {
            checkShufflePlay();
        }

        if (remoteState != RemoteControlState.LOCAL || !Util.isNetworkConnected(this) || Util.isOffline(this)) {
            return;
        }

        if (downloadList.isEmpty() && backgroundDownloadList.isEmpty()) {
            return;
        }
		
        // Need to download current playing?
        if (currentPlaying != null && currentPlaying != currentDownloading && !currentPlaying.isWorkDone()) {
            // Cancel current download, if necessary.
            if (currentDownloading != null) {
                currentDownloading.cancelDownload();
            }

            currentDownloading = currentPlaying;
            currentDownloading.download();
            cleanupCandidates.add(currentDownloading);
        }

        // Find a suitable target for download.
        else if (currentDownloading == null || currentDownloading.isWorkDone() || currentDownloading.isFailed() && (!downloadList.isEmpty() || !backgroundDownloadList.isEmpty())) {
			currentDownloading = null;
            int n = size();

            int preloaded = 0;

			if(n != 0) {
				int start = currentPlaying == null ? 0 : getCurrentPlayingIndex();
				if(start == -1) {
					start = 0;
				}
				int i = start;
				do {
					DownloadFile downloadFile = downloadList.get(i);
					if (!downloadFile.isWorkDone() && !downloadFile.isFailedMax()) {
						if (downloadFile.shouldSave() || preloaded < Util.getPreloadCount(this)) {
							currentDownloading = downloadFile;
							currentDownloading.download();
							cleanupCandidates.add(currentDownloading);
							if(i == (start + 1)) {
								setNextPlayerState(DOWNLOADING);
							}
							break;
						}
					} else if (currentPlaying != downloadFile) {
						preloaded++;
					}

					i = (i + 1) % n;
				} while (i != start);
			}
			
			if((preloaded + 1 == n || preloaded >= Util.getPreloadCount(this) || downloadList.isEmpty()) && !backgroundDownloadList.isEmpty()) {
				for(int i = 0; i < backgroundDownloadList.size(); i++) {
					DownloadFile downloadFile = backgroundDownloadList.get(i);
					if(downloadFile.isWorkDone() && (!downloadFile.shouldSave() || downloadFile.isSaved())) {
						// Don't need to keep list like active song list
						backgroundDownloadList.remove(i);
						revision++;
						i--;
					} else {
						if(!downloadFile.isFailedMax()) {
							currentDownloading = downloadFile;
							currentDownloading.download();
							cleanupCandidates.add(currentDownloading);
							break;
						}
					}
				}
			}
        }

		if(!backgroundDownloadList.isEmpty() && downloadRevision != revision) {
			Util.showDownloadingNotification(this, currentDownloading, backgroundDownloadList.size());
			downloadRevision = revision;
			downloadOngoing = true;
		} else if(backgroundDownloadList.isEmpty() && downloadOngoing) {
			Util.hideDownloadingNotification(this);
			downloadOngoing = false;
		}

        // Delete obsolete .partial and .complete files.
        cleanup();
    }

    private synchronized void checkShufflePlay() {

       // Get users desired random playlist size
		SharedPreferences prefs = Util.getPreferences(this);
		int listSize = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_RANDOM_SIZE, "20"));
        boolean wasEmpty = downloadList.isEmpty();

        long revisionBefore = revision;

        // First, ensure that list is at least 20 songs long.
        int size = size();
        if (size < listSize) {
            for (MusicDirectory.Entry song : shufflePlayBuffer.get(listSize - size)) {
                DownloadFile downloadFile = new DownloadFile(this, song, false);
                downloadList.add(downloadFile);
                revision++;
            }
        }

        int currIndex = currentPlaying == null ? 0 : getCurrentPlayingIndex();

        // Only shift playlist if playing song #5 or later.
        if (currIndex > 4) {
            int songsToShift = currIndex - 2;
            for (MusicDirectory.Entry song : shufflePlayBuffer.get(songsToShift)) {
                downloadList.add(new DownloadFile(this, song, false));
                downloadList.get(0).cancelDownload();
                downloadList.remove(0);
                revision++;
            }
        }

        if (revisionBefore != revision) {
            updateJukeboxPlaylist();
        }

        if (wasEmpty && !downloadList.isEmpty()) {
            play(0);
        }
    }

    public long getDownloadListUpdateRevision() {
        return revision;
    }

    private synchronized void cleanup() {
        Iterator<DownloadFile> iterator = cleanupCandidates.iterator();
        while (iterator.hasNext()) {
            DownloadFile downloadFile = iterator.next();
            if (downloadFile != currentPlaying && downloadFile != currentDownloading) {
                if (downloadFile.cleanup()) {
                    iterator.remove();
                }
            }
        }
    }

    private class BufferTask extends CancellableTask {
        private final DownloadFile downloadFile;
        private final int position;
        private final long expectedFileSize;
        private final File partialFile;

        public BufferTask(DownloadFile downloadFile, int position) {
            this.downloadFile = downloadFile;
            this.position = position;
            partialFile = downloadFile.getPartialFile();

			SharedPreferences prefs = Util.getPreferences(DownloadServiceImpl.this);
			long bufferLength = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_BUFFER_LENGTH, "5"));
			if(bufferLength == 0) {
				// Set to seconds in a day, basically infinity
				bufferLength = 86400L;
			}

            // Calculate roughly how many bytes BUFFER_LENGTH_SECONDS corresponds to.
            int bitRate = downloadFile.getBitRate();
            long byteCount = Math.max(100000, bitRate * 1024L / 8L * bufferLength);

            // Find out how large the file should grow before resuming playback.
			Log.i(TAG, "Buffering from position " + position + " and bitrate " + bitRate);
            expectedFileSize = (position * bitRate / 8) + byteCount;
        }

        @Override
        public void execute() {			
            setPlayerState(DOWNLOADING);

            while (!bufferComplete()) {
                Util.sleepQuietly(1000L);
                if (isCancelled()) {
                    return;
                }
            }
            doPlay(downloadFile, position, true);
        }

        private boolean bufferComplete() {
            boolean completeFileAvailable = downloadFile.isWorkDone();
            long size = partialFile.length();

            Log.i(TAG, "Buffering " + partialFile + " (" + size + "/" + expectedFileSize + ", " + completeFileAvailable + ")");
            return completeFileAvailable || size >= expectedFileSize;
        }

        @Override
        public String toString() {
            return "BufferTask (" + downloadFile + ")";
        }
    }
	
	private class CheckCompletionTask extends CancellableTask {
        private final DownloadFile downloadFile;
        private final File partialFile;

        public CheckCompletionTask(DownloadFile downloadFile) {
			setNextPlayerState(PlayerState.IDLE);
            this.downloadFile = downloadFile;
			if(downloadFile != null) {
				partialFile = downloadFile.getPartialFile();
			} else {
				partialFile = null;
			}
        }

        @Override
        public void execute() {
			if(downloadFile == null) {
				return;
			}
			
			// Do an initial sleep so this prepare can't compete with main prepare
			Util.sleepQuietly(5000L);
            while (!bufferComplete()) {
                Util.sleepQuietly(5000L);
                if (isCancelled()) {
                    return;
                }
            }
            
			// Start the setup of the next media player
			mediaPlayerHandler.post(new Runnable() {
				public void run() {
					setupNext(downloadFile);
				}
			});
        }

        private boolean bufferComplete() {
            boolean completeFileAvailable = downloadFile.isWorkDone();
            Log.i(TAG, "Buffering next " + partialFile + " (" + partialFile.length() + ")");
            return completeFileAvailable && (playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED);
        }

        @Override
        public String toString() {
            return "CheckCompletionTask (" + downloadFile + ")";
        }
    }
}
