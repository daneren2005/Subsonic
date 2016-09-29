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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import github.daneren2005.dsub.domain.InternetRadioStation;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.SilentBackgroundTask;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.util.CacheCleaner;
import github.daneren2005.serverproxy.BufferFile;

public class DownloadFile implements BufferFile {
    private static final String TAG = DownloadFile.class.getSimpleName();
    private static final int MAX_FAILURES = 5;
    private final Context context;
    private final MusicDirectory.Entry song;
    private final File partialFile;
    private final File completeFile;
    private final File saveFile;

    private final MediaStoreService mediaStoreService;
    private DownloadTask downloadTask;
    private boolean save;
	private boolean failedDownload = false;
    private int failed = 0;
    private int bitRate;
	private boolean isPlaying = false;
	private boolean saveWhenDone = false;
	private boolean completeWhenDone = false;
	private Long contentLength = null;
	private long currentSpeed = 0;
	private boolean rateLimit = false;

    public DownloadFile(Context context, MusicDirectory.Entry song, boolean save) {
        this.context = context;
        this.song = song;
        this.save = save;
        saveFile = FileUtil.getSongFile(context, song);
        bitRate = getActualBitrate();
        partialFile = new File(saveFile.getParent(), FileUtil.getBaseName(saveFile.getName()) +
                ".partial." + FileUtil.getExtension(saveFile.getName()));
        completeFile = new File(saveFile.getParent(), FileUtil.getBaseName(saveFile.getName()) +
                ".complete." + FileUtil.getExtension(saveFile.getName()));
        mediaStoreService = new MediaStoreService(context);
    }

    public MusicDirectory.Entry getSong() {
        return song;
    }
	public boolean isSong() {
		return song.isSong();
	}

	public Context getContext() {
		return context;
	}

    /**
     * Returns the effective bit rate.
     */
    public int getBitRate() {
		if(!partialFile.exists()) {
			bitRate = getActualBitrate();
		}
        if (bitRate > 0) {
            return bitRate;
        }
        return song.getBitRate() == null ? 160 : song.getBitRate();
    }
	private int getActualBitrate() {
		int br = song.isVideo() ? Util.getMaxVideoBitrate(context) : Util.getMaxBitrate(context);
		if(br == 0 && song.getTranscodedSuffix() != null && "mp3".equals(song.getTranscodedSuffix().toLowerCase())) {
			if(song.getBitRate() != null) {
				br = Math.min(320, song.getBitRate());
			} else {
				br = 320;
			}
		} else if(song.getSuffix() != null && (song.getTranscodedSuffix() == null || song.getSuffix().equals(song.getTranscodedSuffix()))) {
			// If just downsampling, don't try to upsample (ie: 128 kpbs -> 192 kpbs)
			if(song.getBitRate() != null && (br == 0 || br > song.getBitRate())) {
				br = song.getBitRate();
			}
		}

		return br;
	}
	
	public Long getContentLength() {
		return contentLength;
	}

	public long getCurrentSize() {
		if(partialFile.exists()) {
			return partialFile.length();
		} else {
			File file = getCompleteFile();
			if(file.exists()) {
				return file.length();
			} else {
				return 0L;
			}
		}
	}

	@Override
	public long getEstimatedSize() {
		if(contentLength != null) {
			return contentLength;
		}

		File file = getCompleteFile();
		if(file.exists()) {
			return file.length();
		} else if(song.getDuration() == null) {
			return 0;
		} else {
			int br = (getBitRate() * 1000) / 8;
			int duration = song.getDuration();
			return br * duration;
		}
	}

	public long getBytesPerSecond() {
		return currentSpeed;
	}

    public synchronized void download() {
    	rateLimit = false;
        preDownload();
        downloadTask.execute();
    }
    public synchronized void downloadNow(MusicService musicService) {
    	rateLimit = true;
    	preDownload();
		downloadTask.setMusicService(musicService);
		try {
			downloadTask.doInBackground();
		} catch(InterruptedException e) {
			// This should never be reached
		}
    }
    private void preDownload() {
    	FileUtil.createDirectoryForParent(saveFile);
        failedDownload = false;
		if(!partialFile.exists()) {
			bitRate = getActualBitrate();
		}
		downloadTask = new DownloadTask(context);
    }

    public synchronized void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

	@Override
	public File getFile() {
		if (saveFile.exists()) {
			return saveFile;
		} else if (completeFile.exists()) {
			return completeFile;
		} else {
			return partialFile;
		}
	}

    public File getCompleteFile() {
        if (saveFile.exists()) {
            return saveFile;
        }

        if (completeFile.exists()) {
            return completeFile;
        }

        return saveFile;
    }
	public File getSaveFile() {
		return saveFile;
	}

    public File getPartialFile() {
        return partialFile;
    }

    public boolean isSaved() {
        return saveFile.exists();
    }

    public synchronized boolean isCompleteFileAvailable() {
        return saveFile.exists() || completeFile.exists();
    }

	@Override
    public synchronized boolean isWorkDone() {
        return saveFile.exists() || (completeFile.exists() && !save) || saveWhenDone || completeWhenDone;
    }

	@Override
	public void onStart() {
		setPlaying(true);
	}

	@Override
	public void onStop() {
		setPlaying(false);
	}

	@Override
	public synchronized void onResume() {
		if(!isWorkDone() && !isFailedMax() && !isDownloading() && !isDownloadCancelled()) {
			download();
		}
	}

	public synchronized boolean isDownloading() {
        return downloadTask != null && downloadTask.isRunning();
    }

    public synchronized boolean isDownloadCancelled() {
        return downloadTask != null && downloadTask.isCancelled();
    }

    public boolean shouldSave() {
        return save;
    }

    public boolean isFailed() {
        return failedDownload;
    }
    public boolean isFailedMax() {
    	return failed > MAX_FAILURES;
    }

    public void delete() {
        cancelDownload();
        
        // Remove from mediaStore BEFORE deleting file since it calls getCompleteFile
		deleteFromStore();
		
		// Delete all possible versions of the file
		File parent = partialFile.getParentFile();
        Util.delete(partialFile);
        Util.delete(completeFile);
        Util.delete(saveFile);
		FileUtil.deleteEmptyDir(parent);
    }

    public void unpin() {
        if (saveFile.exists()) {
        	// Delete old store entry before renaming to pinned file
            saveFile.renameTo(completeFile);
			renameInStore(saveFile, completeFile);
        }
    }

    public boolean cleanup() {
        boolean ok = true;
        if (completeFile.exists() || saveFile.exists()) {
            ok = Util.delete(partialFile);
        }
        if (saveFile.exists()) {
            ok &= Util.delete(completeFile);
        }
        return ok;
    }

    // In support of LRU caching.
    public void updateModificationDate() {
        updateModificationDate(saveFile);
        updateModificationDate(partialFile);
        updateModificationDate(completeFile);
    }

    private void updateModificationDate(File file) {
        if (file.exists()) {
            boolean ok = file.setLastModified(System.currentTimeMillis());
            if (!ok) {
                Log.w(TAG, "Failed to set last-modified date on " + file);
            }
        }
    }
	
	public void setPlaying(boolean isPlaying) {
		try {
			if(saveWhenDone && !isPlaying) {
				Util.renameFile(completeFile, saveFile);
				renameInStore(completeFile, saveFile);
				saveWhenDone = false;
			} else if(completeWhenDone && !isPlaying) {
				if(save) {
					Util.renameFile(partialFile, saveFile);
                    saveToStore();
				} else {
					Util.renameFile(partialFile, completeFile);
					saveToStore();
				}
				completeWhenDone = false;
			}
		} catch(IOException ex) {
			Log.w(TAG, "Failed to rename file " + completeFile + " to " + saveFile, ex);
		}
		
		this.isPlaying = isPlaying;
	}
	public void renamePartial() {
		try {
			Util.renameFile(partialFile, completeFile);
			saveToStore();
		} catch(IOException ex) {
			Log.w(TAG, "Failed to rename file " + partialFile + " to " + completeFile, ex);
		}
	}
	public boolean getPlaying() {
		return isPlaying;
	}
	
	private void deleteFromStore() {
		try {
			mediaStoreService.deleteFromMediaStore(this);
		} catch(Exception e) {
			Log.w(TAG, "Failed to remove from store", e);
		}
	}
	private void saveToStore() {
		if(!Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_HIDE_MEDIA, false)) {
			try {
				mediaStoreService.saveInMediaStore(this);
			} catch(Exception e) {
				Log.w(TAG, "Failed to save in media store", e);
			}
		}
	}
	private void renameInStore(File start, File end) {
		try {
			mediaStoreService.renameInMediaStore(start, end);
		} catch(Exception e) {
			Log.w(TAG, "Failed to rename in store", e);
		}
	}

	public boolean isStream() {
		return song != null && song instanceof InternetRadioStation;
	}
	public String getStream() {
		if(song != null && song instanceof InternetRadioStation) {
			InternetRadioStation station = (InternetRadioStation) song;
			return station.getStreamUrl();
		} else {
			return null;
		}
	}

    @Override
    public String toString() {
        return "DownloadFile (" + song + ")";
    }

	// Don't do this.  Causes infinite loop if two instances of same song
	/*@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DownloadFile downloadFile = (DownloadFile) o;
		return Util.equals(this.getSong(), downloadFile.getSong());
	}*/

    private class DownloadTask extends SilentBackgroundTask<Void> {
		private MusicService musicService;

		public DownloadTask(Context context) {
			super(context);
		}

        @Override
        public Void doInBackground() throws InterruptedException {
            InputStream in = null;
            FileOutputStream out = null;
            PowerManager.WakeLock wakeLock = null;
			WifiManager.WifiLock wifiLock = null;
            try {

                if (Util.isScreenLitOnDownload(context)) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, toString());
                    wakeLock.acquire();
                }
				
				wifiLock = Util.createWifiLock(context, toString());
				wifiLock.acquire();

                if (saveFile.exists()) {
                    Log.i(TAG, saveFile + " already exists. Skipping.");
                    checkDownloads();
                    return null;
                }
                if (completeFile.exists()) {
                    if (save) {
						if(isPlaying) {
							saveWhenDone = true;
						} else {
							Util.renameFile(completeFile, saveFile);
							renameInStore(completeFile, saveFile);
						}
                    } else {
                        Log.i(TAG, completeFile + " already exists. Skipping.");
                    }
                    checkDownloads();
                    return null;
                }

				if(musicService == null) {
                	musicService = MusicServiceFactory.getMusicService(context);
				}

				// Some devices seem to throw error on partial file which doesn't exist
				boolean compare;
				try {
					compare = (bitRate == 0) || (song.getDuration() == 0) || (partialFile.length() == 0) || (bitRate * song.getDuration() * 1000 / 8) > partialFile.length();
				} catch(Exception e) {
					compare = true;
				}
				if(compare) {
					// Attempt partial HTTP GET, appending to the file if it exists.
					HttpURLConnection connection = musicService.getDownloadInputStream(context, song, partialFile.length(), bitRate, DownloadTask.this);
					long contentLength = connection.getContentLength();
					if(contentLength > 0) {
						Log.i(TAG, "Content Length: " + contentLength);
						DownloadFile.this.contentLength = contentLength;
					}

					in = connection.getInputStream();
					boolean partial = connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL;
					if (partial) {
						Log.i(TAG, "Executed partial HTTP GET, skipping " + partialFile.length() + " bytes");
					}

					out = new FileOutputStream(partialFile, partial);
					long n = copy(in, out);
					Log.i(TAG, "Downloaded " + n + " bytes to " + partialFile);
					out.flush();
					out.close();

					if (isCancelled()) {
						throw new Exception("Download of '" + song + "' was cancelled");
					} else if(partialFile.length() == 0) {
						throw new Exception("Download of '" + song + "' failed.  File is 0 bytes long.");
					}

					downloadAndSaveCoverArt(musicService);
				}

				if(isPlaying) {
					completeWhenDone = true;
				} else {
					if(save) {
						Util.renameFile(partialFile, saveFile);
					} else {
						Util.renameFile(partialFile, completeFile);
					}
					DownloadFile.this.saveToStore();
				}

            } catch(InterruptedException x) {
				throw x;
			} catch(FileNotFoundException x) {
				Util.delete(completeFile);
				Util.delete(saveFile);
				if(!isCancelled()) {
					failed = MAX_FAILURES + 1;
					failedDownload = true;
					Log.w(TAG, "Failed to download '" + song + "'.", x);
				}
			} catch(IOException x) {
				Util.delete(completeFile);
				Util.delete(saveFile);
				if(!isCancelled()) {
					failedDownload = true;
					Log.w(TAG, "Failed to download '" + song + "'.", x);
				}
			} catch (Exception x) {
                Util.delete(completeFile);
                Util.delete(saveFile);
                if (!isCancelled()) {
                	failed++;
                    failedDownload = true;
                    Log.w(TAG, "Failed to download '" + song + "'.", x);
                }
            } finally {
                Util.close(in);
                Util.close(out);
                if (wakeLock != null) {
                    wakeLock.release();
                    Log.i(TAG, "Released wake lock " + wakeLock);
                }
				if (wifiLock != null) {
					wifiLock.release();
				}
			}
			
			// Only run these if not interrupted, ie: cancelled
			DownloadService downloadService = DownloadService.getInstance();
			if(downloadService != null && !isCancelled()) {
				new CacheCleaner(context, downloadService).cleanSpace();
				checkDownloads();
			}

			return null;
        }
        
        private void checkDownloads() {
        	DownloadService downloadService = DownloadService.getInstance();
        	if(downloadService != null) {
        		downloadService.checkDownloads();
        	}
        }

        @Override
        public String toString() {
            return "DownloadTask (" + song + ")";
        }

		public void setMusicService(MusicService musicService) {
			this.musicService = musicService;
		}

        private void downloadAndSaveCoverArt(MusicService musicService) throws Exception {
            try {
                if (song.getCoverArt() != null) {
					// Check if album art already exists, don't want to needlessly load into memory
					File albumArtFile = FileUtil.getAlbumArtFile(context, song);
					if(!albumArtFile.exists()) {
						musicService.getCoverArt(context, song, 0, null, null);
					}
                }
            } catch (Exception x) {
                Log.e(TAG, "Failed to get cover art.", x);
            }
        }

        private long copy(final InputStream in, OutputStream out) throws IOException, InterruptedException {

            // Start a thread that will close the input stream if the task is
            // cancelled, thus causing the copy() method to return.
            new Thread("DownloadFile_copy") {
                @Override
                public void run() {
                    while (true) {
                        Util.sleepQuietly(3000L);
                        if (isCancelled()) {
                            Util.close(in);
                            return;
                        }
                        if (!isRunning()) {
                            return;
                        }
                    }
                }
            }.start();

            byte[] buffer = new byte[1024 * 16];
            long count = 0;
            int n;
            long lastLog = System.currentTimeMillis();
			long lastCount = 0;

			boolean activeLimit = rateLimit;
            while (!isCancelled() && (n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                count += n;
				lastCount += n;

                long now = System.currentTimeMillis();
                if (now - lastLog > 3000L) {  // Only every so often.
                    Log.i(TAG, "Downloaded " + Util.formatBytes(count) + " of " + song);
					currentSpeed = lastCount / ((now - lastLog) / 1000L);
                    lastLog = now;
					lastCount = 0;
					
					// Re-establish every few seconds whether screen is on or not
					if(rateLimit) {
						PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
						if(pm.isScreenOn()) {
							activeLimit = true;
						} else {
							activeLimit = false;
						}
					}
                }
                
                // If screen is on and rateLimit is true, stop downloading from exhausting bandwidth
                if(activeLimit) {
                	Thread.sleep(10L);
                }
            }
            return count;
        }
    }
}
