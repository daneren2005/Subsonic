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
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.UpdateView;

import java.io.File;

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
	private DownloadFile downloadFile;

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
    }

    public void setSong(MusicDirectory.Entry song, boolean checkable) {
        this.song = song;
		
		if(Util.isOffline(context)) {
			DownloadFile downloadFile = new DownloadFile(context, song, false);
			File file = downloadFile.getCompleteFile();
			if(file.exists()) {
				try {
					MediaMetadataRetriever metadata = new MediaMetadataRetriever();
					metadata.setDataSource(file.getAbsolutePath());
          String discNumber = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER);
          if(discNumber == null) {
            discNumber = "1/1";
          }
          int slashIndex = discNumber.indexOf("/");
          if(slashIndex > 0) {
            discNumber = discNumber.substring(0, slashIndex);
          }
          song.setDiscNumber(Integer.parseInt(discNumber));
					String bitrate = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
					song.setBitRate(Integer.parseInt((bitrate != null) ? bitrate : "0") / 1000);
					String length = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
					song.setDuration(Integer.parseInt(length) / 1000);
				} catch(Exception e) {
					Log.i(TAG, "Device doesn't properly support MediaMetadataRetreiver");
				}
			}
		}
		
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
			artist.append(song.getArtist()).append(" (")
				.append(String.format(getContext().getString(R.string.song_details_all), bitRate == null ? "" : bitRate, fileFormat))
				.append(")");
		} else {
			artist.append(String.format(getContext().getString(R.string.song_details_all), bitRate == null ? "" : bitRate, fileFormat));
		}

        titleTextView.setText(song.getTitle());
        artistTextView.setText(artist);
        durationTextView.setText(Util.formatDuration(song.getDuration()));
        checkedTextView.setVisibility(checkable && !song.isVideo() ? View.VISIBLE : View.GONE);
		starButton.setVisibility((Util.isOffline(getContext()) || !song.isStarred()) ? View.GONE : View.VISIBLE);
		starButton.setFocusable(false);
		
		moreButton = (ImageView) findViewById(R.id.artist_more);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				v.showContextMenu();
			}
		});

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
		
		downloadFile = downloadService.forSong(song);
	}

	@Override
    protected void update() {
        if (downloadService == null) {
            return;
        }
		
		starButton.setVisibility((Util.isOffline(getContext()) || !song.isStarred()) ? View.GONE : View.VISIBLE);
        File partialFile = downloadFile.getPartialFile();

        int leftImage = 0;
        int rightImage = 0;

        if (downloadFile.isWorkDone()) {
            leftImage = downloadFile.isSaved() ? R.drawable.saved : R.drawable.downloaded;
			moreButton.setImageResource(R.drawable.list_item_more_shaded);
        } else {
			moreButton.setImageResource(R.drawable.list_item_more);
		}

        if (downloadFile.isDownloading() && !downloadFile.isDownloadCancelled() && partialFile.exists()) {
            statusTextView.setText(Util.formatLocalizedBytes(partialFile.length(), getContext()));
            rightImage = R.drawable.downloading;
        } else {
            statusTextView.setText(null);
        }
        statusTextView.setCompoundDrawablesWithIntrinsicBounds(leftImage, 0, rightImage, 0);

        boolean playing = downloadService.getCurrentPlaying() == downloadFile;
        if (playing) {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stat_notify_playing, 0, 0, 0);
        } else {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
