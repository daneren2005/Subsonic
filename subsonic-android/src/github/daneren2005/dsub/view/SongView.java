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
package github.daneren2005.dsub.view;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.util.Util;

import java.io.File;
import java.text.DateFormat;

/**
 * Used to display songs in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class SongView extends UpdateView implements Checkable {
    private static final String TAG = SongView.class.getSimpleName();
    
	private Context context;
    private MusicDirectory.Entry song;

    private CheckedTextView checkedTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private TextView durationTextView;
    private TextView statusTextView;
    private ImageButton starButton;
	private ImageView moreButton;
	
	private DownloadService downloadService;
	private long revision = -1;
	private DownloadFile downloadFile;

	private boolean playing = false;
	private int rightImage = 0;
	private int moreImage = 0;
	private boolean starred = false;
	private boolean isWorkDone = false;
	private boolean isSaved = false;
	private File partialFile;
	private boolean partialFileExists = false;

    public SongView(Context context) {
        super(context);
		this.context = context;
        LayoutInflater.from(context).inflate(R.layout.song_list_item, this, true);

        checkedTextView = (CheckedTextView) findViewById(R.id.song_check);
        titleTextView = (TextView) findViewById(R.id.song_title);
        artistTextView = (TextView) findViewById(R.id.song_artist);
        durationTextView = (TextView) findViewById(R.id.song_duration);
        statusTextView = (TextView) findViewById(R.id.song_status);
        starButton = (ImageButton) findViewById(R.id.song_star);
		starButton.setFocusable(false);
		moreButton = (ImageView) findViewById(R.id.artist_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});
    }

    public void setSong(MusicDirectory.Entry song, boolean checkable) {
        this.song = song;
		
        StringBuilder artist = new StringBuilder(40);

        String bitRate = null;
        if (song.getBitRate() != null) {
        	bitRate = String.format(getContext().getString(R.string.song_details_kbps), song.getBitRate());
        }
        
        String fileFormat = null;
        if (song.getTranscodedSuffix() != null && !song.getTranscodedSuffix().equals(song.getSuffix())) {
        	fileFormat = String.format("%s > %s", song.getSuffix(), song.getTranscodedSuffix());
    	} else {
            fileFormat = song.getSuffix();
        }

		if(!song.isVideo()) {
			if(song instanceof PodcastEpisode) {
				String date = ((PodcastEpisode)song).getDate();
				if(date != null) {
					int index = date.indexOf(" ");
					artist.append(date.substring(0, index != -1 ? index : date.length()));
				}
			}
			else if(song.getArtist() != null) {
				artist.append(song.getArtist());
			}
			
			String status = (song instanceof PodcastEpisode) ? ((PodcastEpisode)song).getStatus() : "";
			artist.append(" (");
			if("error".equals(status)) {
				artist.append(getContext().getString(R.string.song_details_error));
			} else if("skipped".equals(status)) {
				artist.append(getContext().getString(R.string.song_details_skipped));
			} else if("downloading".equals(status)) {
				artist.append(getContext().getString(R.string.song_details_downloading));
			} else {
				artist.append(String.format(getContext().getString(R.string.song_details_all), bitRate == null ? "" : bitRate, fileFormat));
			}
			artist.append(")");
		} else {
			artist.append(String.format(getContext().getString(R.string.song_details_all), bitRate == null ? "" : bitRate, fileFormat));
		}
		
		String title = song.getTitle();
		Integer track = song.getTrack();
		if(track != null && Util.getDisplayTrack(context)) {
			title = String.format("%02d", track) + " " + title;
		}

        titleTextView.setText(title);
		artistTextView.setText(artist);
        durationTextView.setText(Util.formatDuration(song.getDuration()));
        checkedTextView.setVisibility(checkable && !song.isVideo() ? View.VISIBLE : View.GONE);

		updateBackground();
        update();
    }
	
	@Override
	protected void updateBackground() {
        if (downloadService == null) {
			downloadService = DownloadServiceImpl.getInstance();
			if(downloadService == null) {
				return;
			}
        }

		long newRevision = downloadService.getDownloadListUpdateRevision();
		if(revision != newRevision) {
			downloadFile = downloadService.forSong(song);
			revision = newRevision;
		}

		isWorkDone = downloadFile.isWorkDone();
		isSaved = downloadFile.isSaved();
		partialFile = downloadFile.getPartialFile();
		partialFileExists = partialFile.exists();
	}

	@Override
    protected void update() {
        if (downloadService == null) {
            return;
        }

		if(song.isStarred()) {
			if(!starred) {
				starButton.setVisibility(View.VISIBLE);
				starred = true;
			}
		} else {
			if(starred) {
				starButton.setVisibility(View.GONE);
				starred = false;
			}
		}

        int rightImage = 0;
        if (isWorkDone) {
			int moreImage = isSaved ? R.drawable.list_item_more_saved : R.drawable.list_item_more_shaded;
			if(moreImage != this.moreImage) {
				moreButton.setImageResource(moreImage);
				this.moreImage = moreImage;
			}
        } else if(this.moreImage != R.drawable.list_item_more) {
			moreButton.setImageResource(R.drawable.list_item_more);
			this.moreImage = R.drawable.list_item_more;
		}

        if (downloadFile.isDownloading() && !downloadFile.isDownloadCancelled() && partialFileExists) {
			statusTextView.setText(Util.formatLocalizedBytes(partialFile.length(), getContext()));
			rightImage = R.drawable.downloading;
        } else if(this.rightImage != 0) {
            statusTextView.setText(null);
        }
		if(this.rightImage != rightImage) {
        	statusTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, rightImage, 0);
			this.rightImage = rightImage;
		}

        boolean playing = downloadService.getCurrentPlaying() == downloadFile;
        if (playing) {
			if(!this.playing) {
				this.playing = playing;
            	titleTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stat_notify_playing, 0, 0, 0);
			}
        } else {
			if(this.playing) {
				this.playing = playing;
            	titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}
    }

    @Override
    public void setChecked(boolean b) {
        checkedTextView.setChecked(b);
    }

    @Override
    public boolean isChecked() {
        return checkedTextView.isChecked();
    }

    @Override
    public void toggle() {
        checkedTextView.toggle();
    }
}
