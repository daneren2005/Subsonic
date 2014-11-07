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

package github.daneren2005.dsub.service;

import github.daneren2005.dsub.domain.DLNADevice;

public class DLNAController extends RemoteController {
	DLNADevice device;

	public DLNAController(DLNADevice device) {
		this.device = device;
	}

	@Override
	public void create(boolean playing, int seconds) {

	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public void updatePlaylist() {

	}

	@Override
	public void changePosition(int seconds) {

	}

	@Override
	public void changeTrack(int index, DownloadFile song) {

	}

	@Override
	public void setVolume(int volume) {

	}

	@Override
	public void updateVolume(boolean up) {

	}

	@Override
	public double getVolume() {
		return 0;
	}

	@Override
	public int getRemotePosition() {
		return 0;
	}
}
