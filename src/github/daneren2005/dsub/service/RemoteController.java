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
	
	Copyright 2009 (C) Sindre Mehus
*/

package github.daneren2005.dsub.service;

public class RemoteController {
	protected DownloadServiceImpl downloadService;
	private VolumeToast volumeToast;
	
	public abstract void start();
	public abstract void stop();
	
	public abstract public abstract void updatePlaylist();
	public abstract void changePosition(int seconds);
	public abstract void changeTrack(int index, DownloadFile song);
	public abstract void setVolume(float gain);
	
	public abstract int getRemotePosition();
	
	protected VolumeToast getVolumeToast() {
		if(volumeToast == null) {
			volumeToast == new VolumeToast(downloadService);
		}
		return volumeToast;
	}
	
	private static class VolumeToast extends Toast {
		private final ProgressBar progressBar;
		
		public VolumeToast(Context context) {
			super(context);
			setDuration(Toast.LENGTH_SHORT);
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.jukebox_volume, null);
			progressBar = (ProgressBar) view.findViewById(R.id.jukebox_volume_progress_bar);
			
			setView(view);
			setGravity(Gravity.TOP, 0, 0);
		}
		
		public void setVolume(float volume) {
			progressBar.setProgress(Math.round(100 * volume));
			show();
		}
	}
}
