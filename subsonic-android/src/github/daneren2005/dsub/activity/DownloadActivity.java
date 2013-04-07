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
package github.daneren2005.dsub.activity;

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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.domain.RepeatMode;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.HorizontalSlider;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.view.SongView;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.VisualizerView;

import static github.daneren2005.dsub.domain.PlayerState.*;
import github.daneren2005.dsub.util.*;
import github.daneren2005.dsub.view.AutoRepeatButton;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import com.mobeta.android.dslv.*;
import github.daneren2005.dsub.service.DownloadServiceImpl;

public class DownloadActivity extends SubsonicTabActivity implements OnGestureListener {
	private static final String TAG = DownloadActivity.class.getSimpleName();

    private static final int DIALOG_SAVE_PLAYLIST = 100;
    private static final int PERCENTAGE_OF_SCREEN_FOR_SWIPE = 10;
    private static final int COLOR_BUTTON_ENABLED = Color.rgb(51, 181, 229);
    private static final int COLOR_BUTTON_DISABLED = Color.rgb(206, 213, 211);
	private static final int INCREMENT_TIME = 5000;

    private ViewFlipper playlistFlipper;
    private TextView emptyTextView;
    private TextView songTitleTextView;
    private ImageView albumArtImageView;
    private DragSortListView playlistView;
    private TextView positionTextView;
    private TextView durationTextView;
    private TextView statusTextView;
    private HorizontalSlider progressBar;
    private AutoRepeatButton previousButton;
    private AutoRepeatButton nextButton;
    private View pauseButton;
    private View stopButton;
    private View startButton;
    private ImageButton repeatButton;
    private Button equalizerButton;
    private Button visualizerButton;
    private Button jukeboxButton;
    private View toggleListButton;
    private ImageButton starButton;
    private ScheduledExecutorService executorService;
    private DownloadFile currentPlaying;
    private long currentRevision;
    private EditText playlistNameView;
    private GestureDetector gestureScanner;
    private int swipeDistance;
    private int swipeVelocity;
    private VisualizerView visualizerView;
	private boolean nowPlaying = true;
	private ScheduledFuture<?> hideControlsFuture;
	private SongListAdapter songListAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setTitle(nowPlaying ? "Now Playing" : "Downloading");
        setContentView(R.layout.download);

        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        swipeDistance = (d.getWidth() + d.getHeight()) * PERCENTAGE_OF_SCREEN_FOR_SWIPE / 100;
        swipeVelocity = (d.getWidth() + d.getHeight()) * PERCENTAGE_OF_SCREEN_FOR_SWIPE / 100;
        gestureScanner = new GestureDetector(this);

        playlistFlipper = (ViewFlipper) findViewById(R.id.download_playlist_flipper);
        emptyTextView = (TextView) findViewById(R.id.download_empty);
        songTitleTextView = (TextView) findViewById(R.id.download_song_title);
        albumArtImageView = (ImageView) findViewById(R.id.download_album_art_image);
        positionTextView = (TextView) findViewById(R.id.download_position);
        durationTextView = (TextView) findViewById(R.id.download_duration);
        statusTextView = (TextView) findViewById(R.id.download_status);
        progressBar = (HorizontalSlider) findViewById(R.id.download_progress_bar);
        playlistView = (DragSortListView) findViewById(R.id.download_list);
        previousButton = (AutoRepeatButton)findViewById(R.id.download_previous);
        nextButton = (AutoRepeatButton)findViewById(R.id.download_next);
        pauseButton = findViewById(R.id.download_pause);
        stopButton = findViewById(R.id.download_stop);
        startButton = findViewById(R.id.download_start);
        repeatButton = (ImageButton) findViewById(R.id.download_repeat);
        equalizerButton = (Button) findViewById(R.id.download_equalizer);
        visualizerButton = (Button) findViewById(R.id.download_visualizer);
        jukeboxButton = (Button) findViewById(R.id.download_jukebox);
        LinearLayout visualizerViewLayout = (LinearLayout) findViewById(R.id.download_visualizer_view_layout);
        toggleListButton = findViewById(R.id.download_toggle_list);
        
        starButton = (ImageButton) findViewById(R.id.download_star);
        starButton.setVisibility(Util.isOffline(this) ? View.GONE : View.VISIBLE);
		starButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DownloadFile currentDownload = getDownloadService().getCurrentPlaying();
				if (currentDownload != null) {
					MusicDirectory.Entry currentSong = currentDownload.getSong();
					toggleStarred(currentSong);
					starButton.setImageResource(currentSong.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
				}
			}
		});

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return gestureScanner.onTouchEvent(me);
            }
        };
        pauseButton.setOnTouchListener(touchListener);
        stopButton.setOnTouchListener(touchListener);
        startButton.setOnTouchListener(touchListener);
        equalizerButton.setOnTouchListener(touchListener);
        visualizerButton.setOnTouchListener(touchListener);
        jukeboxButton.setOnTouchListener(touchListener);
        emptyTextView.setOnTouchListener(touchListener);
        albumArtImageView.setOnTouchListener(touchListener);

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().previous();
						return null;
					}

					@Override
					protected void done(Void result) {
						onCurrentChanged();
						onProgressChanged();
					}
				}.execute();
				setControlsVisible(true);
            }
        });
		previousButton.setOnRepeatListener(new Runnable() {
			public void run() {
				changeProgress(-INCREMENT_TIME);
			}
		});

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
				new SilentBackgroundTask<Boolean>(DownloadActivity.this) {
					@Override
					protected Boolean doInBackground() throws Throwable {
						if (getDownloadService().getCurrentPlayingIndex() < getDownloadService().size() - 1) {
							getDownloadService().next();
							return true;
						} else {
							return false;
						}
					}

					@Override
					protected void done(Boolean result) {
						if(result) {
							onCurrentChanged();
							onProgressChanged();
						}
					}
				}.execute();
				setControlsVisible(true);
            }
        });
		nextButton.setOnRepeatListener(new Runnable() {
			public void run() {
				changeProgress(INCREMENT_TIME);
			}
		});

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().pause();
						return null;
					}

					@Override
					protected void done(Void result) {
						onCurrentChanged();
						onProgressChanged();
					}
				}.execute();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().reset();
						return null;
					}

					@Override
					protected void done(Void result) {
						onCurrentChanged();
						onProgressChanged();
					}
				}.execute();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warnIfNetworkOrStorageUnavailable();
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						start();
						return null;
					}

					@Override
					protected void done(Void result) {
						onCurrentChanged();
						onProgressChanged();
					}
				}.execute();
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
				setControlsVisible(true);
            }
        });

        equalizerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				DownloadService downloadService = getDownloadService();
				if(downloadService != null && downloadService.getEqualizerController() != null
						&& downloadService.getEqualizerController().getEqualizer() != null) {
					startActivity(new Intent(DownloadActivity.this, EqualizerActivity.class));
					setControlsVisible(true);
				} else {
					Util.toast(DownloadActivity.this, "Failed to start equalizer.  Try restarting.");
				}
            }
        });

        visualizerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean active = !visualizerView.isActive();
                visualizerView.setActive(active);
				boolean isActive = visualizerView.isActive();
                getDownloadService().setShowVisualization(isActive);
                updateButtons();
				if(active == isActive) {
					Util.toast(DownloadActivity.this, active ? R.string.download_visualizer_on : R.string.download_visualizer_off);
				} else {
					Util.toast(DownloadActivity.this, "Failed to start visualizer.  Try restarting.");
				}
				setControlsVisible(true);
            }
        });

        jukeboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean jukeboxEnabled = !getDownloadService().isJukeboxEnabled();
                getDownloadService().setJukeboxEnabled(jukeboxEnabled);
                updateButtons();
                Util.toast(DownloadActivity.this, jukeboxEnabled ? R.string.download_jukebox_on : R.string.download_jukebox_off, false);
				setControlsVisible(true);
            }
        });

        toggleListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullscreenAlbumArt();
				setControlsVisible(true);
            }
        });

        progressBar.setOnSliderChangeListener(new HorizontalSlider.OnSliderChangeListener() {
            @Override
            public void onSliderChanged(View view, final int position, boolean inProgress) {
                Util.toast(DownloadActivity.this, Util.formatDuration(position / 1000), true);
                if (!inProgress) {
					new SilentBackgroundTask<Void>(DownloadActivity.this) {
						@Override
						protected Void doInBackground() throws Throwable {
							 getDownloadService().seekTo(position);
							return null;
						}

						@Override
						protected void done(Void result) {
							onProgressChanged();
						}
					}.execute();
                }
				setControlsVisible(true);
            }
        });
        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				if(nowPlaying) {
					warnIfNetworkOrStorageUnavailable();
					new SilentBackgroundTask<Void>(DownloadActivity.this) {
						@Override
						protected Void doInBackground() throws Throwable {
							getDownloadService().play(position);
							return null;
						}

						@Override
						protected void done(Void result) {
							onCurrentChanged();
							onProgressChanged();
						}
					}.execute();
				}
            }
        });
		playlistView.setDropListener(new DragSortListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				getDownloadService().swap(from, to);
				onDownloadListChanged();
			}
		});
		playlistView.setRemoveListener(new DragSortListView.RemoveListener() {
			@Override
			public void remove(int which) {
				getDownloadService().remove(which);
				onDownloadListChanged();
			}
		});

        registerForContextMenu(playlistView);

        DownloadService downloadService = getDownloadService();
        if (downloadService != null && getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false)) {
			getIntent().removeExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE);
            warnIfNetworkOrStorageUnavailable();
            downloadService.setShufflePlayEnabled(true);
        }

        boolean visualizerAvailable = downloadService != null && downloadService.getVisualizerAvailable();
        boolean equalizerAvailable = downloadService != null && downloadService.getEqualizerAvailable();

        if (!equalizerAvailable) {
            equalizerButton.setVisibility(View.GONE);
        }
        if (!visualizerAvailable) {
            visualizerButton.setVisibility(View.GONE);
        } else {
            visualizerView = new VisualizerView(this);
            visualizerViewLayout.addView(visualizerView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
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
		
		setControlsVisible(true);

        DownloadService downloadService = getDownloadService();
        if (downloadService == null || downloadService.getCurrentPlaying() == null) {
            playlistFlipper.setDisplayedChild(1);
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

        if (visualizerView != null && downloadService != null && downloadService.getShowVisualization()) {
            visualizerView.setActive(true);
        }

        updateButtons();
    }
	
	private void scheduleHideControls() {
        if (hideControlsFuture != null) {
            hideControlsFuture.cancel(false);
        }

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setControlsVisible(false);
                    }
                });
            }
        };
        hideControlsFuture = executorService.schedule(runnable, 3000L, TimeUnit.MILLISECONDS);
    }

    private void setControlsVisible(boolean visible) {
		try {
			long duration = 1700L;
			FadeOutAnimation.createAndStart(findViewById(R.id.download_overlay_buttons), !visible, duration);

			if (visible) {
				scheduleHideControls();
			}
		} catch(Exception e) {
			
		}
    }

    private void updateButtons() {
		SharedPreferences prefs = Util.getPreferences(DownloadActivity.this);
		boolean equalizerOn = prefs.getBoolean(Constants.PREFERENCES_EQUALIZER_ON, false);
		if(equalizerOn && getDownloadService() != null && getDownloadService().getEqualizerController() != null &&
                getDownloadService().getEqualizerController().isEnabled()) {
			equalizerButton.setTextColor(COLOR_BUTTON_ENABLED);
		} else {
			equalizerButton.setTextColor(COLOR_BUTTON_DISABLED);
		}
        
        if (visualizerView != null) {
            visualizerButton.setTextColor(visualizerView.isActive() ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
        }

        boolean jukeboxEnabled = getDownloadService() != null && getDownloadService().isJukeboxEnabled();
        jukeboxButton.setTextColor(jukeboxEnabled ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
    }

    // Scroll to current playing/downloading.
    private void scrollToCurrent() {
        if (getDownloadService() == null || songListAdapter == null) {
            return;
        }

        for (int i = 0; i < songListAdapter.getCount(); i++) {
            if (currentPlaying == playlistView.getItemAtPosition(i)) {
                playlistView.setSelectionFromTop(i, 40);
                return;
            }
        }
        DownloadFile currentDownloading = getDownloadService().getCurrentDownloading();
        for (int i = 0; i < songListAdapter.getCount(); i++) {
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
        if (visualizerView != null && visualizerView.isActive()) {
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
            String playlistName = (getDownloadService() != null) ? getDownloadService().getSuggestedPlaylistName() : null;
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
		MenuInflater inflater = getSupportMenuInflater();
		if(Util.isOffline(this)) {
			inflater.inflate(R.menu.nowplaying_offline, menu);
		} else {
			if(nowPlaying)
				inflater.inflate(R.menu.nowplaying, menu);
			else
				inflater.inflate(R.menu.nowplaying_downloading, menu);
			
			if(getDownloadService() != null && getDownloadService().getSleepTimer()) {
				menu.findItem(R.id.menu_toggle_timer).setTitle(R.string.download_stop_timer);
			}
		}
		if(getDownloadService() != null && getDownloadService().getKeepScreenOn()) {
			menu.findItem(R.id.menu_screen_on_off).setTitle(R.string.download_menu_screen_off);
		}
		return true;
	}

    @Override
    public void onCreateContextMenu(android.view.ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (view == playlistView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            DownloadFile downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);

            android.view.MenuInflater inflater = getMenuInflater();
			if(Util.isOffline(this)) {
				inflater.inflate(R.menu.nowplaying_context_offline, menu);
			} else {
				inflater.inflate(R.menu.nowplaying_context, menu);
				menu.findItem(R.id.menu_star).setTitle(downloadFile.getSong().isStarred() ? R.string.common_unstar : R.string.common_star);
			}

            if (downloadFile.getSong().getParent() == null) {
            	menu.findItem(R.id.menu_show_album).setVisible(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        DownloadFile downloadFile = (DownloadFile) playlistView.getItemAtPosition(info.position);
        return menuItemSelected(menuItem.getItemId(), downloadFile) || super.onContextItemSelected(menuItem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return menuItemSelected(menuItem.getItemId(), null) || super.onOptionsItemSelected(menuItem);
    }

    private boolean menuItemSelected(int menuItemId, final DownloadFile song) {
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
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().remove(song);
						return null;
					}

					@Override
					protected void done(Void result) {
						onDownloadListChanged();
					}
				}.execute();
                return true;
			case R.id.menu_delete:
				List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(1);
				songs.add(song.getSong());
				getDownloadService().delete(songs);
				return true;
            case R.id.menu_remove_all:
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().setShufflePlayEnabled(false);
						if(nowPlaying) {
							getDownloadService().clear();
						}
						else {
							getDownloadService().clearBackground();
						}
						return null;
					}

					@Override
					protected void done(Void result) {
						onDownloadListChanged();
					}
				}.execute();
                return true;
            case R.id.menu_screen_on_off:
                if (getDownloadService().getKeepScreenOn()) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            		getDownloadService().setKeepScreenOn(false);
            	} else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            		getDownloadService().setKeepScreenOn(true);
            	}
				invalidateOptionsMenu();
                return true;
            case R.id.menu_shuffle:
				new SilentBackgroundTask<Void>(DownloadActivity.this) {
					@Override
					protected Void doInBackground() throws Throwable {
						getDownloadService().shuffle();
						return null;
					}

					@Override
					protected void done(Void result) {
						Util.toast(DownloadActivity.this, R.string.download_menu_shuffle_notification);
					}
				}.execute();
                return true;
            case R.id.menu_save_playlist:
                showDialog(DIALOG_SAVE_PLAYLIST);
                return true;
			case R.id.menu_star:
				toggleStarred(song.getSong());
				return true;
			case R.id.menu_toggle_now_playing:
				toggleNowPlaying();
				invalidateOptionsMenu();
				return true;
			case R.id.menu_toggle_timer:
				if(getDownloadService().getSleepTimer()) {
					getDownloadService().stopSleepTimer();
					invalidateOptionsMenu();
				} else {
					startTimer();
				}
				return true;
			case R.id.menu_exit:
				intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
                Util.startActivityWithoutTransition(this, intent);
				return true;
			case R.id.menu_add_playlist:
				songs = new ArrayList<MusicDirectory.Entry>(1);
				songs.add(song.getSong());
				addToPlaylist(songs);
				return true;
			case R.id.menu_info:
				displaySongInfo(song.getSong());
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
                for (DownloadFile downloadFile : getDownloadService().getSongs()) {
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
	
	protected void startTimer() {
		View dialogView = getLayoutInflater().inflate(R.layout.start_timer, null);
		final EditText lengthBox = (EditText)dialogView.findViewById(R.id.timer_length);
		
		final SharedPreferences prefs = Util.getPreferences(DownloadActivity.this);
		lengthBox.setText(prefs.getString(Constants.PREFERENCES_KEY_SLEEP_TIMER_DURATION, ""));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(DownloadActivity.this);
		builder.setTitle("Set Timer")
			.setView(dialogView)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					String length = lengthBox.getText().toString();
					
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(Constants.PREFERENCES_KEY_SLEEP_TIMER_DURATION, length);
					editor.commit();
					
					getDownloadService().setSleepTimerDuration(Integer.parseInt(length));
					getDownloadService().startSleepTimer();
					invalidateOptionsMenu();
				}
			})
			.setNegativeButton("Cancel", null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

    private void toggleFullscreenAlbumArt() {
    	scrollToCurrent();
        if (playlistFlipper.getDisplayedChild() == 1) {
            playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_in));
            playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_down_out));
            playlistFlipper.setDisplayedChild(0);
        } else {
            playlistFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
            playlistFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
            playlistFlipper.setDisplayedChild(1);
        }
    }

    private void start() {
        DownloadService service = getDownloadService();
        PlayerState state = service.getPlayerState();
        if (state == PAUSED || state == COMPLETED || state == STOPPED) {
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
		onDownloadListChanged(false);
	}
    private void onDownloadListChanged(boolean refresh) {
        DownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }

		List<DownloadFile> list;
		if(nowPlaying) {
			list = downloadService.getSongs();
		}
		else {
			list = downloadService.getBackgroundDownloads();
		}
		
		if(downloadService.isShufflePlayEnabled()) {
			emptyTextView.setText(R.string.download_shuffle_loading);
		}
		else {
			emptyTextView.setText(R.string.download_empty);
		}

		if(songListAdapter == null || refresh) {
			playlistView.setAdapter(songListAdapter = new SongListAdapter(list));
		} else {
			songListAdapter.notifyDataSetChanged();
		}
        emptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        currentRevision = downloadService.getDownloadListUpdateRevision();

        switch (downloadService.getRepeatMode()) {
            case OFF:
        		if("light".equals(theme) | "light_fullscreen".equals(theme)) {
        			repeatButton.setImageResource(R.drawable.media_repeat_off_light);
        		} else {
        			repeatButton.setImageResource(R.drawable.media_repeat_off);
        		}
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
            getImageLoader().loadImage(albumArtImageView, song, true, true);
            starButton.setImageResource(song.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        } else {
            songTitleTextView.setText(null);
            getImageLoader().loadImage(albumArtImageView, null, true, false);
            starButton.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void onProgressChanged() {
        if (getDownloadService() == null) {
            return;
        }
		
		new SilentBackgroundTask<Void>(this) {
			DownloadService downloadService;
			boolean isJukeboxEnabled;
			int millisPlayed;
			Integer duration;
			PlayerState playerState;
			
			@Override
            protected Void doInBackground() throws Throwable {
                downloadService = getDownloadService();
				isJukeboxEnabled = downloadService.isJukeboxEnabled();
				millisPlayed = Math.max(0, downloadService.getPlayerPosition());
				duration = downloadService.getPlayerDuration();
				playerState = getDownloadService().getPlayerState();
                return null;
            }
            
            @Override
            protected void done(Void result) {
				if (currentPlaying != null) {
					int millisTotal = duration == null ? 0 : duration;

					positionTextView.setText(Util.formatDuration(millisPlayed / 1000));
					durationTextView.setText(Util.formatDuration(millisTotal / 1000));
					progressBar.setMax(millisTotal == 0 ? 100 : millisTotal); // Work-around for apparent bug.
					progressBar.setProgress(millisPlayed);
					progressBar.setSlidingEnabled(currentPlaying.isWorkDone() || isJukeboxEnabled);
				} else {
					positionTextView.setText("0:00");
					durationTextView.setText("-:--");
					progressBar.setProgress(0);
					progressBar.setSlidingEnabled(false);
				}

				switch (playerState) {
					case DOWNLOADING:
						long bytes = currentPlaying.getPartialFile().length();
						statusTextView.setText(getResources().getString(R.string.download_playerstate_downloading, Util.formatLocalizedBytes(bytes, DownloadActivity.this)));
						break;
					case PREPARING:
						statusTextView.setText(R.string.download_playerstate_buffering);
						break;
					case STARTED:
						statusTextView.setText((currentPlaying != null) ? (currentPlaying.getSong().getArtist() + " - " + currentPlaying.getSong().getAlbum()) : null);
						break;
					default:
						statusTextView.setText((currentPlaying != null) ? (currentPlaying.getSong().getArtist() + " - " + currentPlaying.getSong().getAlbum()) : null);
						break;
				}

				switch (playerState) {
					case STARTED:
						pauseButton.setVisibility(View.VISIBLE);
						stopButton.setVisibility(View.INVISIBLE);
						startButton.setVisibility(View.INVISIBLE);
						break;
					case DOWNLOADING:
					case PREPARING:
						pauseButton.setVisibility(View.INVISIBLE);
						stopButton.setVisibility(View.VISIBLE);
						startButton.setVisibility(View.INVISIBLE);
						break;
					default:
						pauseButton.setVisibility(View.INVISIBLE);
						stopButton.setVisibility(View.INVISIBLE);
						startButton.setVisibility(View.VISIBLE);
						break;
				}

				jukeboxButton.setTextColor(isJukeboxEnabled ? COLOR_BUTTON_ENABLED : COLOR_BUTTON_DISABLED);
            }
		}.execute();
    }
	
	private void changeProgress(final int ms) {
		final DownloadService downloadService = getDownloadService();
		if(downloadService == null) {
			return;
		}
		
		new SilentBackgroundTask<Void>(this) {
			boolean isJukeboxEnabled;
			int msPlayed;
			Integer duration;
			PlayerState playerState;
			int seekTo;
			
			@Override
            protected Void doInBackground() throws Throwable {
				msPlayed = Math.max(0, downloadService.getPlayerPosition());
				duration = downloadService.getPlayerDuration();
				playerState = getDownloadService().getPlayerState();
				int msTotal = duration == null ? 0 : duration;
				if(msPlayed + ms > msTotal) {
					seekTo = msTotal;
				} else {
					seekTo = msPlayed + ms;
				}
				downloadService.seekTo(seekTo);
                return null;
            }
            
            @Override
            protected void done(Void result) {
				progressBar.setProgress(seekTo);
			}
		}.execute();
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
		setControlsVisible(true);
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
		else if (e2.getX() - e1.getX() > swipeDistance && Math.abs(velocityX) > swipeVelocity) {
			warnIfNetworkOrStorageUnavailable();
			downloadService.previous();
			onCurrentChanged();
			onProgressChanged();
			return true;
		}

		// Top to Bottom swipe
		 else if (e2.getY() - e1.getY() > swipeDistance && Math.abs(velocityY) > swipeVelocity) {
			 warnIfNetworkOrStorageUnavailable();
			 downloadService.seekTo(downloadService.getPlayerPosition() + 30000); 
			 onProgressChanged();
			 return true;
		 }

		// Bottom to Top swipe
		else if (e1.getY() - e2.getY() > swipeDistance && Math.abs(velocityY) > swipeVelocity) {
			warnIfNetworkOrStorageUnavailable();
			downloadService.seekTo(downloadService.getPlayerPosition() - 8000);
			onProgressChanged();
			return true;
		}

		return false;
	}
	
	private void toggleNowPlaying() {
		nowPlaying = !nowPlaying;
		setTitle(nowPlaying ? "Now Playing" : "Downloading");
		onDownloadListChanged(true);
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
