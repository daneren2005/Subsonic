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

import java.util.List;

import github.daneren2005.dsub.audiofx.EqualizerController;
import github.daneren2005.dsub.audiofx.VisualizerController;
import github.daneren2005.dsub.domain.Bookmark;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RemoteControlState;
import github.daneren2005.dsub.domain.RepeatMode;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public interface DownloadService {

	void download(Bookmark bookmark);
    void download(List<MusicDirectory.Entry> songs, boolean save, boolean autoplay, boolean playNext, boolean shuffle);
	void downloadBackground(List<MusicDirectory.Entry> songs, boolean save);

    void setShufflePlayEnabled(boolean enabled);

    boolean isShufflePlayEnabled();

    void shuffle();

    RepeatMode getRepeatMode();

    void setRepeatMode(RepeatMode repeatMode);

    boolean getKeepScreenOn();

    void setKeepScreenOn(boolean screenOn);

    boolean getShowVisualization();

    void setShowVisualization(boolean showVisualization);

    void clear();
	
	void clearBackground();

    void clearIncomplete();

    int size();
	
	void remove(int which);

    void remove(DownloadFile downloadFile);

	List<DownloadFile> getSongs();
	
    List<DownloadFile> getDownloads();
	
	List<DownloadFile> getBackgroundDownloads();

    int getCurrentPlayingIndex();

    DownloadFile getCurrentPlaying();

    DownloadFile getCurrentDownloading();

    void play(int index);

    void seekTo(int position);

    void previous();

    void next();

    void pause();
	
	void stop();

    void start();

    void reset();

    PlayerState getPlayerState();

    int getPlayerPosition();

    int getPlayerDuration();

    void delete(List<MusicDirectory.Entry> songs);

    void unpin(List<MusicDirectory.Entry> songs);

    DownloadFile forSong(MusicDirectory.Entry song);

    long getDownloadListUpdateRevision();

    void setSuggestedPlaylistName(String name, String id);

    String getSuggestedPlaylistName();
	
	String getSuggestedPlaylistId();
	
	boolean getEqualizerAvailable();

    boolean getVisualizerAvailable();

    EqualizerController getEqualizerController();

    VisualizerController getVisualizerController();

    boolean isRemoteEnabled();

    void setRemoteEnabled(RemoteControlState newState);

    void setRemoteVolume(boolean up);
	
	void setSleepTimerDuration(int duration);
	
	void startSleepTimer();
	
	void stopSleepTimer();
	
	boolean getSleepTimer();
	
	void setVolume(float volume);
	
	void swap(boolean mainList, int from, int to);
}
