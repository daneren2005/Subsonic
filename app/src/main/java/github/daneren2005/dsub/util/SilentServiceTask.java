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

	Copyright 2015 (C) Scott Jackson
*/

package github.daneren2005.dsub.util;

import android.content.Context;

import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;

public abstract class SilentServiceTask<T> extends SilentBackgroundTask<T> {
	protected MusicService musicService;

	public SilentServiceTask(Context context) {
		super(context);
	}

	@Override
	protected T doInBackground() throws Throwable {
		musicService = MusicServiceFactory.getMusicService(getContext());
		return doInBackground(musicService);
	}

	protected abstract T doInBackground(MusicService musicService) throws Throwable;
}
