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
package net.sourceforge.subsonic.androidapp.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.domain.PlayerState;
import net.sourceforge.subsonic.androidapp.domain.RepeatMode;
import net.sourceforge.subsonic.androidapp.service.DownloadFile;
import net.sourceforge.subsonic.androidapp.service.DownloadService;
import net.sourceforge.subsonic.androidapp.service.MusicService;
import net.sourceforge.subsonic.androidapp.service.MusicServiceFactory;
import net.sourceforge.subsonic.androidapp.util.Constants;
import net.sourceforge.subsonic.androidapp.util.HorizontalSlider;
import net.sourceforge.subsonic.androidapp.util.SilentBackgroundTask;
import net.sourceforge.subsonic.androidapp.util.SongView;
import net.sourceforge.subsonic.androidapp.util.Util;
import net.sourceforge.subsonic.androidapp.view.VisualizerView;

import static net.sourceforge.subsonic.androidapp.domain.PlayerState.*;

public class DownloadActivity extends SubsonicTabActivity implements OnGestureListener {

    private static final int DIALOG_SAVE_PLAYLIST = 100;
    private static final int PERCENTAGE_OF_SCREEN_FOR_SWIPE = 5;
    private static final int COLOR_BUTTON_ENABLED = Color.rgb(129, 201, 54);
    private static final int COLOR_BUTTON_DISABLED = Color.rgb(164, 166, 158);

    private ViewFlipper playlistFlipper;
    private ViewFlipper buttonBarFlipper;
    private TextView emptyTextView;
    private TextView songTitleTextView;
    private TextView albumTextView;
    private TextView artistTextView;
    private ImageView albumArtImageView;
    private ListView playlistView;
    private TextView positionTextView;
    private TextView durationTextView;
    private TextView statusTextView;
    private HorizontalSlider progressBar;
    private View previousButton;
    private View nextButton;
    private View pauseButton;
    private View stopButton;
    private View startButton;
    private View shuffleButton;
    private ImageButton repeatButton;
    private Button equalizerButton;
    private Button visualizerButton;
    private Button jukeboxButton;
    private View toggleListButton;
    private ScheduledExecutorService executorService;
    private DownloadFile currentPlaying;
    private long currentRevision;
    private EditText playlistNameView;
    private GestureDetector gestureScanner;
    private int swipeDistance;
    private int swipeVelocity;
    private VisualizerView visualizerView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);

        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        swipeDistance = (d.getWidth() + d.getHeight()) * PERCENTAGE_OF_SCREEN_FOR_SWIPE / 100;
        swipeVelocity = (d.getWidth() + d.getHeight()) * PERCENTAGE_OF_SCREEN_FOR_SWIPE / 100;
        gestureScanner = new GestureDetector(this);

        playlistFlipper = (ViewFlipper) findViewById(R.id.download_playlist_flipper);
        buttonBarFlipper = (ViewFlipper) findViewById(R.id.download_button_bar_flipper);
        emptyTextView = (TextView) findViewById(R.id.download_empty);
        songTitleTextView = (TextView) findViewById(R.id.download_song_title);
        albumTextView = (TextView) findViewById(R.id.download_album);
        artistTextView = (TextView) findViewById(R.id.download_artist);
        albumArtImageView = (ImageView) findViewById(R.id.download_album_art_image);
        positionTextView = (TextView) findViewById(R.id.download_position);
        durationTextView = (TextView) findViewById(R.id.download_duration);
        statusTextView = (TextView) findViewById(R.id.download_status);
        progressBar = (HorizontalSlider) findViewById(R.id.download_progress_bar);
        playlistView = (ListView) findViewById(R.id.download_list);
        previousButton = findViewById(R.id.download_previous);
        nextButton = findViewById(R.id.download_next);
        pauseButton = findViewById(R.id.download_pause);
        stopButton = findViewById(R.id.download_stop);
        startButton = findViewById(R.id.download_start);
        shuffleButton = findViewById(R.id.download_shuffle);
        repeatButton = (ImageButton) findViewById(R.id.download_repeat);
        equalizerButton = (Button) findViewById(R.id.download_equalizer);
        visualizerButton = (Button) findViewById(R.id.download_visualizer);
        jukeboxButton = (Button) findViewById(R.id.download_jukebox);
        LinearLayout visualizerViewLayout = (LinearLayout) findViewById(R.id.download_visualizer_view_layout);

        toggleListButton = findViewById(R.id.download_toggle_list);

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return gestureScanner.onTouchEvent(me);
            }
        };
        previousButton.setOnTouchListener(touchListener);
        nextButton.setOnTouchListener(touchListener);
        pauseButton.setOnTouchListener(touchListener);
        stopButton.setOnTouchListener(touchListener);
        startButton.setOnTouchListener(touchListener);
        equalizerButton.setOnTouchListener(touchListener);
        visualizerButton.setOnTouchListener(touchListener);
        jukeboxButton.setOnTouchListener(touchListener);
        buttonBarFlipper.setOnTouchListener(touchListener);
        emptyTextView.setOnTouchListener(touchListener);
        albumArtImageView.setOnTouchListener(touchListener);

        albumArtImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullscreenAlbumArt();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
                getDownloadService().previous();
                onCurrentChanged();
                onProgressChanged();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
                if (getDownloadService().getCurrentPlayingIndex() < getDownloadService().size() - 1) {
                    getDownloadService().next();
                    onCurrentChanged();
                    onProgressChanged();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDownloadService().pause();
                onCurrentChanged();
                onProgressChanged();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDownloadService().reset();
                onCurrentChanged();
                onProgressChanged();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
                start();
                onCurrentChanged();
                onProgressChanged();
            }
        });

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDownloadService().shuffle();
                Util.toast(DownloadActivity.this, R.string.download_menu_shuffle_notification);
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RepeatMode repeatMode = getDownloadService().getRepeatMode().next();
                getDownloadService().setRepeatMode(repeatMode);
                onDownloadListChanged();
                switch (repeatMode) {
                    case OFF:
                        Util.toast(DownloadActivity.this, R.string.download_repeat_off);
                        break;
                    case ALL:
                        Util.toast(DownloadActivity.this, R.string.download_repeat_all);
                        break;
                    case SINGLE:
                        Util.toast(DownloadActivity.this, R.string.download_repeat_single);
                        break;
                    default:
                        break;
                }
            }
        });

        equalizerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DownloadActivity.this, EqualizerActivity.class));
            }
        });

        visualizerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean active = !visualizerView.isActive();
                visualizerView.setActive(active);
                getDownloadService().setShowVisualization(visualizerView.isActive());
                updateButtons();
                Util.toast(DownloadActivity.this, active ? R.string.download_visualizer_on : R.string.download_visualizer_off);
            }
        });

        jukeboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean jukeboxEnabled = !getDownloadService().isJukeboxEnabled();
                getDownloadService().setJukeboxEnabled(jukeboxEnabled);
                updateButtons();
                Util.toast(DownloadActivity.this, jukeboxEnabled ? R.string.download_jukebox_on : R.string.download_jukebox_off, false);
            }
        });

        toggleListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullscreenAlbumArt();
            }
        });

        progressBar.setOnSliderChangeListener(new HorizontalSlider.OnSliderChangeListener() {
            @Override
            public void onSliderChanged(View view, int position, boolean inProgress) {
                Util.toast(DownloadActivity.this, Util.formatDuration(position / 1000), true);
                if (!inProgress) {
                    getDownloadService().seekTo(position);
                    onProgressChanged();
                }
            }
        });
        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                warnIfNetworkOrStorageUnavailable();
                getDownloadService().play(position);
                onCurrentChanged();
                onProgressChanged();
            }
        });

        registerForContextMenu(playlistView);

        DownloadService downloadService = getDownloadService();
        if (downloadService != null && getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false)) {
            warnIfNetworkOrStorageUnavailable();
            downloadService.setShufflePlayEnabled(true);
        }

        boolean visualizerAvailable = downloadService != null && downloadService.getVisualizerController() != null;
        boolean equalizerAvailable = downloadService != null && downloadService.getEqualizerController() != null;

        if (!equalizerAvailable) {
            equalizerButton.setVisibility(View.GONE);
        }
        if (!visualizerAvailable) {
            visualizerButton.setVisibility(View.GONE);
        } else {
            visualizerView = new VisualizerView(this);
            visualizerViewLayout.addView(visualizerView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));

            visualizerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    visualizerView.setActive(!visualizerView.isActive());
                    getDownloadService().setShowVisualization(visualizerView.isActive());
                    updateButtons();
                    return true;
                }
            });
        }

        // TODO: Extract to utility method and cache.
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Storopia.ttf");
        equalizerButton.setTypeface(typeface);
        visualizerButton.setTypeface(typeface);
        jukeboxButton.setTypeface(typeface);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        };

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);

        DownloadService downloadService = getDownloadService();
        if (downloadService == null || downloadService.getCurrentPlaying() == null) {
            playlistFlipper.setDisplayedChild(1);
            buttonBarFlipper.setDisplayedChild(1);
        }

        onDownloadListChanged();
        onCurrentChanged();
        onProgressChanged();
        scrollToCurrent();
        if (downloadService != null && downloadService.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (visualizerView != null) {
            visualizerView.setActive(downloadService != null && downloadService.getShowVisualization());
        }

        updateButtons();
    }

    private void updateButtons() {
        boolean eqEnabled = getDownloadService() != null && getDownloadService().getEqualizerController() != null &&
                getDownloadService().getEqualizerController().isEnabled();
        equalizerButton.setTextColor(eqEnabled ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);

        if (visualizerView != null) {
            visualizerButton.setTextColor(visualizerView.isActive() ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
        }

        boolean jukeboxEnabled = getDownloadService() != null && getDownloadService().isJukeboxEnabled();
        jukeboxButton.setTextColor(jukeboxEnabled ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
    }

    // Scroll to current playing/downloading.
    private void scrollToCurrent() {
        if (getDownloadService() == null) {
            return;
        }

        for (int i = 0; i < playlistView.getAdapter().getCount(); i++) {
            if (currentPlaying == playlistView.getItemAtPosition(i)) {
                playlistView.setSelectionFromTop(i, 40);
                return;
            }
        }
        DownloadFile currentDownloading = getDownloadService().getCurrentDownloading();
        for (int i = 0; i < playlistView.getAdapter().getCount(); i++) {
            if (currentDownloading == playlistView.getItemAtPosition(i)) {
                playlistView.setSelectionFromTop(i, 40);
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        executorService.shutdown();
        if (visualizerView != null) {
            visualizerView.setActive(false);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_SAVE_PLAYLIST) {
            AlertDialog.Builder builder;

            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.save_playlist, (ViewGroup) findViewById(R.id.save_playlist_root));
            playlistNameView = (EditText) layout.findViewById(R.id.save_playlist_name);

            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.download_playlist_title);
            builder.setMessage(R.string.download_playlist_name);
            builder.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    savePlaylistInBackground(String.valueOf(playlistNameView.getText()));
                }
            });
            builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.setView(layout);
            builder.setCancelable(true);

            return builder.create();
        } else {
            return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_SAVE_PLAYLIST) {
            String playlistName = getDownloadService().getSuggestedPlaylistName();
            if (playlistName != null) {
                playlistNameView.setText(playlistName);
            } else {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                playlistNameView.setText(dateFormat.format(new Date()));
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.nowplaying, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem savePlaylist = menu.findItem(R.id.menu_save_playlist);
        boolean savePlaylistEnabled = !Util.isOffline(this);
        savePlaylist.setEnabled(savePlaylistEnabled);
        savePlaylist.setVisible(savePlaylistEnabled);
        MenuItem screenOption = menu.findItem(R.id.menu_screen_on_off);
        if (getDownloadService().getKeepScreenOn()) {
        	screenOption.setTitle(R.string.download_menu_screen_off);
        } else {
        	screenOption.setTitle(R.string.download_menu_screen_on);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (view == playlistView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            DownloadFile downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);

            MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.nowplaying_context, menu);

            if (downloadFile.getSong().getParent() == null) {
            	menu.findItem(R.id.menu_show_album).setVisible(false);
            }
            if (Util.isOffline(this)) {
                menu.findItem(R.id.menu_lyrics).setVisible(false);
                menu.findItem(R.id.menu_save_playlist).setVisible(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        DownloadFile downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);
        return menuItemSelected(menuItem.getItemId(), downloadFile) || super.onContextItemSelected(menuItem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return menuItemSelected(menuItem.getItemId(), null) || super.onOptionsItemSelected(menuItem);
    }

    private boolean menuItemSelected(int menuItemId, DownloadFile song) {
        switch (menuItemId) {
            case R.id.menu_show_album:
                Intent intent = new Intent(this, SelectAlbumActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, song.getSong().getParent());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, song.getSong().getAlbum());
                Util.startActivityWithoutTransition(this, intent);
                return true;
            case R.id.menu_lyrics:
                intent = new Intent(this, LyricsActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_ARTIST, song.getSong().getArtist());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_TITLE, song.getSong().getTitle());
                Util.startActivityWithoutTransition(this, intent);
                return true;
            case R.id.menu_remove:
                getDownloadService().remove(song);
                onDownloadListChanged();
                return true;
            case R.id.menu_remove_all:
                getDownloadService().setShufflePlayEnabled(false);
                getDownloadService().clear();
                onDownloadListChanged();
                return true;
            case R.id.menu_screen_on_off:
                if (getDownloadService().getKeepScreenOn()) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            		getDownloadService().setKeepScreenOn(false);
            	} else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            		getDownloadService().setKeepScreenOn(true);
            	}
                return true;
            case R.id.menu_shuffle:
                getDownloadService().shuffle();
                Util.toast(this, R.string.download_menu_shuffle_notification);
                return true;
            case R.id.menu_save_playlist:
                showDialog(DIALOG_SAVE_PLAYLIST);
                return true;
            default:
                return false;
        }
    }

    private void update() {
        if (getDownloadService() == null) {
            return;
        }

        if (currentRevision != getDownloadService().getDownloadListUpdateRevision()) {
            onDownloadListChanged();
        }

        if (currentPlaying != getDownloadService().getCurrentPlaying()) {
            onCurrentChanged();
        }

        onProgressChanged();
    }

    private void savePlaylistInBackground(final String playlistName) {
        Util.toast(DownloadActivity.this, getResources().getString(R.string.download_playlist_saving, playlistName));
        getDownloadService().setSuggestedPlaylistName(playlistName);
        new SilentBackgroundTask<Void>(this) {
            @Override
            protected Void doInBackground() throws Throwable {
                List<MusicDirectory.Entry> entries = new LinkedList<MusicDirectory.Entry>();
                for (DownloadFile downloadFile : getDownloadService().getDownloads()) {
                    entries.add(downloadFile.getSong());
                }
                MusicService musicService = MusicServiceFactory.getMusicService(DownloadActivity.this);
                musicService.createPlaylist(null, playlistName, entries, DownloadActivity.this, null);
                return null;
            }

            @Override
            protected void done(Void result) {
                Util.toast(DownloadActivity.this, R.string.download_playlist_done);
            }

            @Override
            protected void error(Throwable error) {
                String msg = getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
                Util.toast(DownloadActivity.this, msg);
            }
        }.execute();
    }

    private void toggleFullscreenAlbumArt() {
    	scrollToCurrent();
        if (playlistFlipper.getDisplayedChild() == 1) {
            playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
            playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
            playlistFlipper.setDisplayedChild(0);
            buttonBarFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
            buttonBarFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
            buttonBarFlipper.setDisplayedChild(0);


        } else {
            playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
            playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
            playlistFlipper.setDisplayedChild(1);
            buttonBarFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
            buttonBarFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
            buttonBarFlipper.setDisplayedChild(1);
        }
    }

    private void start() {
        DownloadService service = getDownloadService();
        PlayerState state = service.getPlayerState();
        if (state == PAUSED || state == COMPLETED) {
            service.start();
        } else if (state == STOPPED || state == IDLE) {
            warnIfNetworkOrStorageUnavailable();
            int current = service.getCurrentPlayingIndex();
            // TODO: Use play() method.
            if (current == -1) {
                service.play(0);
            } else {
                service.play(current);
            }
        }
    }

    private void onDownloadListChanged() {
        DownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }

        List<DownloadFile> list = downloadService.getDownloads();

        playlistView.setAdapter(new SongListAdapter(list));
        emptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        currentRevision = downloadService.getDownloadListUpdateRevision();

        switch (downloadService.getRepeatMode()) {
            case OFF:
                repeatButton.setImageResource(R.drawable.media_repeat_off);
                break;
            case ALL:
                repeatButton.setImageResource(R.drawable.media_repeat_all);
                break;
            case SINGLE:
                repeatButton.setImageResource(R.drawable.media_repeat_single);
                break;
            default:
                break;
        }
    }

    private void onCurrentChanged() {
        if (getDownloadService() == null) {
            return;
        }

        currentPlaying = getDownloadService().getCurrentPlaying();
        if (currentPlaying != null) {
            MusicDirectory.Entry song = currentPlaying.getSong();
            songTitleTextView.setText(song.getTitle());
            albumTextView.setText(song.getAlbum());
            artistTextView.setText(song.getArtist());
            getImageLoader().loadImage(albumArtImageView, song, true, true);
        } else {
            songTitleTextView.setText(null);
            albumTextView.setText(null);
            artistTextView.setText(null);
            getImageLoader().loadImage(albumArtImageView, null, true, false);
        }
    }

    private void onProgressChanged() {
        if (getDownloadService() == null) {
            return;
        }

        if (currentPlaying != null) {

            int millisPlayed = Math.max(0, getDownloadService().getPlayerPosition());
            Integer duration = getDownloadService().getPlayerDuration();
            int millisTotal = duration == null ? 0 : duration;

            positionTextView.setText(Util.formatDuration(millisPlayed / 1000));
            durationTextView.setText(Util.formatDuration(millisTotal / 1000));
            progressBar.setMax(millisTotal == 0 ? 100 : millisTotal); // Work-around for apparent bug.
            progressBar.setProgress(millisPlayed);
            progressBar.setSlidingEnabled(currentPlaying.isCompleteFileAvailable() || getDownloadService().isJukeboxEnabled());
        } else {
            positionTextView.setText("0:00");
            durationTextView.setText("-:--");
            progressBar.setProgress(0);
            progressBar.setSlidingEnabled(false);
        }

        PlayerState playerState = getDownloadService().getPlayerState();

        switch (playerState) {
            case DOWNLOADING:
                long bytes = currentPlaying.getPartialFile().length();
                statusTextView.setText(getResources().getString(R.string.download_playerstate_downloading, Util.formatLocalizedBytes(bytes, this)));
                break;
            case PREPARING:
                statusTextView.setText(R.string.download_playerstate_buffering);
                break;
            case STARTED:
                if (getDownloadService().isShufflePlayEnabled()) {
                    statusTextView.setText(R.string.download_playerstate_playing_shuffle);
                } else {
                    statusTextView.setText(null);
                }
                break;
            default:
                statusTextView.setText(null);
                break;
        }

        switch (playerState) {
            case STARTED:
                pauseButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                break;
            case DOWNLOADING:
            case PREPARING:
                pauseButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.GONE);
                break;
            default:
                pauseButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
                break;
        }

        jukeboxButton.setTextColor(getDownloadService().isJukeboxEnabled() ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
    }

    private class SongListAdapter extends ArrayAdapter<DownloadFile> {
        public SongListAdapter(List<DownloadFile> entries) {
            super(DownloadActivity.this, android.R.layout.simple_list_item_1, entries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SongView view;
            if (convertView != null && convertView instanceof SongView) {
                view = (SongView) convertView;
            } else {
                view = new SongView(DownloadActivity.this);
            }
            DownloadFile downloadFile = getItem(position);
            view.setSong(downloadFile.getSong(), false);
            return view;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return gestureScanner.onTouchEvent(me);
    }

	@Override
	public boolean onDown(MotionEvent me) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        DownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return false;
        }

		// Right to Left swipe
		if (e1.getX() - e2.getX() > swipeDistance && Math.abs(velocityX) > swipeVelocity) {
            warnIfNetworkOrStorageUnavailable();
            if (downloadService.getCurrentPlayingIndex() < downloadService.size() - 1) {
                downloadService.next();
                onCurrentChanged();
                onProgressChanged();
            }
			return true;
		}

		// Left to Right swipe
        if (e2.getX() - e1.getX() > swipeDistance && Math.abs(velocityX) > swipeVelocity) {
            warnIfNetworkOrStorageUnavailable();
            downloadService.previous();
            onCurrentChanged();
            onProgressChanged();
			return true;
		}

        // Top to Bottom swipe
         if (e2.getY() - e1.getY() > swipeDistance && Math.abs(velocityY) > swipeVelocity) {
             warnIfNetworkOrStorageUnavailable();
             downloadService.seekTo(downloadService.getPlayerPosition() + 30000);
             onProgressChanged();
             return true;
         }

        // Bottom to Top swipe
        if (e1.getY() - e2.getY() > swipeDistance && Math.abs(velocityY) > swipeVelocity) {
            warnIfNetworkOrStorageUnavailable();
            downloadService.seekTo(downloadService.getPlayerPosition() - 8000);
            onProgressChanged();
            return true;
        }

        return false;
    }

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
}
