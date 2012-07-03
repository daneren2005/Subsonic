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
package net.sourceforge.subsonic.androidapp.domain;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxStatus {

    private Integer positionSeconds;
    private Integer currentPlayingIndex;
    private Float gain;
    private boolean playing;

    public Integer getPositionSeconds() {
        return positionSeconds;
    }

    public void setPositionSeconds(Integer positionSeconds) {
        this.positionSeconds = positionSeconds;
    }

    public Integer getCurrentPlayingIndex() {
        return currentPlayingIndex;
    }

    public void setCurrentIndex(Integer currentPlayingIndex) {
        this.currentPlayingIndex = currentPlayingIndex;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public Float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }
}
