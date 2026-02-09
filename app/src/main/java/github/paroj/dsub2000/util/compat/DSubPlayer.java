/*
  This file is part of Subsonic.
	Subsonic is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2014 (C) Scott Jackson
*/

package github.paroj.dsub2000.util.compat;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.util.UnstableApi;
import github.paroj.dsub2000.service.DownloadService;
import github.paroj.dsub2000.service.DownloadFile;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class DSubPlayer extends SimpleBasePlayer {
    private final DownloadService downloadService;
    private State state;

    public DSubPlayer(Looper looper, DownloadService downloadService) {
        super(looper);
        this.downloadService = downloadService;
        state = new State.Builder()
                .setAvailableCommands(new Player.Commands.Builder().addAllCommands().build())
                .setPlaylist(Collections.emptyList())
                .build();
    }

    @Override
    protected State getState() {
        return state;
    }

    public void setInternalState(State newState) {
        if (Looper.myLooper() == getApplicationLooper()) {
            state = newState;
            invalidateState();
        } else {
             new Handler(getApplicationLooper()).post(() -> setInternalState(newState));
        }
    }

    public interface UpdateAction {
        void update(State.Builder builder);
    }

    public void updateState(UpdateAction action) {
        if (Looper.myLooper() == getApplicationLooper()) {
            State.Builder builder = state.buildUpon();
            action.update(builder);
            setInternalState(builder.build());
        } else {
            new Handler(getApplicationLooper()).post(() -> updateState(action));
        }
    }

    public State.Builder getStateBuilder() {
        return state.buildUpon();
    }

    @NonNull
    @Override
    protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
        if (playWhenReady) {
            downloadService.start();
        } else {
            downloadService.pause();
        }
        return Futures.immediateVoidFuture();
    }

    @NonNull
    @Override
    protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int seekCommand) {
        if (mediaItemIndex != state.currentMediaItemIndex) {
             List<DownloadFile> queue = downloadService.getSongs();
             if (queue != null && mediaItemIndex >= 0 && mediaItemIndex < queue.size()) {
                 downloadService.play(queue.get(mediaItemIndex));
             }
        } else {
            downloadService.seekTo((int) positionMs);
        }
        return Futures.immediateVoidFuture();
    }
}
