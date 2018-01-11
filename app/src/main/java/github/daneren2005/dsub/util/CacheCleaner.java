package github.daneren2005.dsub.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.os.StatFs;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MediaStoreService;

import java.util.*;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class CacheCleaner {

    private static final String TAG = CacheCleaner.class.getSimpleName();
	private static final long MIN_FREE_SPACE = 500 * 1024L * 1024L;
	private static final long MAX_COVER_ART_SPACE = 100 * 1024L * 1024L;

    private final Context context;
    private final DownloadService downloadService;
	private final MediaStoreService mediaStore;

    public CacheCleaner(Context context, DownloadService downloadService) {
        this.context = context;
        this.downloadService = downloadService;
		this.mediaStore = new MediaStoreService(context);
    }

    public void clean() {
		new BackgroundCleanup(context).execute();
    }
	public void cleanSpace() {
		new BackgroundSpaceCleanup(context).execute();
	}
	public void cleanPlaylists(List<Playlist> playlists) {
		new BackgroundPlaylistsCleanup(context, playlists).execute();
	}

    private void deleteEmptyDirs(List<File> dirs, Set<File> undeletable) {
        for (File dir : dirs) {
            if (undeletable.contains(dir)) {
                continue;
            }

            FileUtil.deleteEmptyDir(dir);
        }
    }
	
	private long getMinimumDelete(List<File> files, List<File> pinned) {
		if(files.size() == 0) {
			return 0L;
		}
		
		long cacheSizeBytes = Util.getCacheSizeMB(context) * 1024L * 1024L;
		
        long bytesUsedBySubsonic = 0L;
        for (File file : files) {
            bytesUsedBySubsonic += file.length();
        }
        for (File file : pinned) {
            bytesUsedBySubsonic += file.length();
        }
		
		// Ensure that file system is not more than 95% full.
        StatFs stat = new StatFs(files.get(0).getPath());
        long bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
        long bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        long bytesUsedFs = bytesTotalFs - bytesAvailableFs;
        long minFsAvailability = bytesTotalFs - MIN_FREE_SPACE;

        long bytesToDeleteCacheLimit = Math.max(bytesUsedBySubsonic - cacheSizeBytes, 0L);
        long bytesToDeleteFsLimit = Math.max(bytesUsedFs - minFsAvailability, 0L);
        long bytesToDelete = Math.max(bytesToDeleteCacheLimit, bytesToDeleteFsLimit);

        Log.i(TAG, "File system       : " + Util.formatBytes(bytesAvailableFs) + " of " + Util.formatBytes(bytesTotalFs) + " available");
        Log.i(TAG, "Cache limit       : " + Util.formatBytes(cacheSizeBytes));
        Log.i(TAG, "Cache size before : " + Util.formatBytes(bytesUsedBySubsonic));
        Log.i(TAG, "Minimum to delete : " + Util.formatBytes(bytesToDelete));
		
		return bytesToDelete;
	}

    private void deleteFiles(List<File> files, Set<File> undeletable, long bytesToDelete, boolean deletePartials) {
        if (files.isEmpty()) {
            return;
        }

        long bytesDeleted = 0L;
        for (File file : files) {
			if(!deletePartials && bytesDeleted > bytesToDelete) break;

            if (bytesToDelete > bytesDeleted || (deletePartials && (file.getName().endsWith(".partial") || file.getName().contains(".partial.")))) {
                if (!undeletable.contains(file) && !file.getName().equals(Constants.ALBUM_ART_FILE)) {
                    long size = file.length();
                    if (Util.delete(file)) {
                        bytesDeleted += size;
						mediaStore.deleteFromMediaStore(file);
                    }
                }
            }
        }

        Log.i(TAG, "Deleted           : " + Util.formatBytes(bytesDeleted));
    }

    private void findCandidatesForDeletion(File file, List<File> files, List<File> pinned, List<File> dirs) {
        if (file.isFile()) {
            String name = file.getName();
            boolean isCacheFile = name.endsWith(".partial") || name.contains(".partial.") || name.endsWith(".complete") || name.contains(".complete.");
            if (isCacheFile) {
                files.add(file);
            } else {
				pinned.add(file);
			}
        } else {
            // Depth-first
            for (File child : FileUtil.listFiles(file)) {
                findCandidatesForDeletion(child, files, pinned, dirs);
            }
            dirs.add(file);
        }
    }

    private void sortByAscendingModificationTime(List<File> files) {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.lastModified() < b.lastModified()) {
                    return -1;
                }
                if (a.lastModified() > b.lastModified()) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private Set<File> findUndeletableFiles() {
        Set<File> undeletable = new HashSet<File>(5);

        for (DownloadFile downloadFile : downloadService.getRecentDownloads()) {
            undeletable.add(downloadFile.getPartialFile());
            undeletable.add(downloadFile.getCompleteFile());
        }

        undeletable.add(FileUtil.getMusicDirectory(context));
        return undeletable;
    }
    
	private void cleanupCoverArt(Context context) {
		File dir = FileUtil.getAlbumArtDirectory(context);
		
		List<File> files = new ArrayList<File>();
		long bytesUsed = 0L;
		for(File file: dir.listFiles()) {
			if(file.isFile()) {
				files.add(file);
				bytesUsed += file.length();
			}
		}
		
		// Don't waste time sorting if under limit already
		if(bytesUsed < MAX_COVER_ART_SPACE) {
			return;
		}
		
		sortByAscendingModificationTime(files);
		long bytesDeleted = 0L;
		for(File file: files) {
			// End as soon as the space used is below the threshold
			if(bytesUsed < MAX_COVER_ART_SPACE) {
				break;
			}
			
			long bytes = file.length();
			if(file.delete()) {
				bytesUsed -= bytes;
				bytesDeleted += bytes;
			}
		}
		
		Log.i(TAG, "Deleted " + Util.formatBytes(bytesDeleted) + " worth of cover art");
	}
	
	private class BackgroundCleanup extends SilentBackgroundTask<Void> {
		public BackgroundCleanup(Context context) {
			super(context);
		}

		@Override
		protected Void doInBackground() {
			if (downloadService == null) {
				Log.e(TAG, "DownloadService not set. Aborting cache cleaning.");
				return null;
			}

			try {
				List<File> files = new ArrayList<File>();
				List<File> pinned = new ArrayList<File>();
				List<File> dirs = new ArrayList<File>();

				findCandidatesForDeletion(FileUtil.getMusicDirectory(context), files, pinned, dirs);
				sortByAscendingModificationTime(files);

				Set<File> undeletable = findUndeletableFiles();

				deleteFiles(files, undeletable, getMinimumDelete(files, pinned), true);
				deleteEmptyDirs(dirs, undeletable);
				
				// Make sure cover art directory does not grow too large
				cleanupCoverArt(context);
			} catch (RuntimeException x) {
				Log.e(TAG, "Error in cache cleaning.", x);
			}

			return null;
		}
	}

	private class BackgroundSpaceCleanup extends SilentBackgroundTask<Void> {
		public BackgroundSpaceCleanup(Context context) {
			super(context);
		}

		@Override
		protected Void doInBackground() {
			if (downloadService == null) {
				Log.e(TAG, "DownloadService not set. Aborting cache cleaning.");
				return null;
			}

			try {
				List<File> files = new ArrayList<File>();
				List<File> pinned = new ArrayList<File>();
				List<File> dirs = new ArrayList<File>();
				findCandidatesForDeletion(FileUtil.getMusicDirectory(context), files, pinned, dirs);

				long bytesToDelete = getMinimumDelete(files, pinned);
				if(bytesToDelete > 0L) {
					sortByAscendingModificationTime(files);
					Set<File> undeletable = findUndeletableFiles();
					deleteFiles(files, undeletable, bytesToDelete, false);
				}
			} catch (RuntimeException x) {
				Log.e(TAG, "Error in cache cleaning.", x);
			}

			return null;
		}
	}

	private class BackgroundPlaylistsCleanup extends SilentBackgroundTask<Void> {
		private final List<Playlist> playlists;

		public BackgroundPlaylistsCleanup(Context context, List<Playlist> playlists) {
			super(context);
			this.playlists = playlists;
		}

		@Override
		protected Void doInBackground() {
			try {
				String server = Util.getServerName(context);
				SortedSet<File> playlistFiles = FileUtil.listFiles(FileUtil.getPlaylistDirectory(context, server));
				for (Playlist playlist : playlists) {
					playlistFiles.remove(FileUtil.getPlaylistFile(context, server, playlist.getName()));
				}

				for(File playlist : playlistFiles) {
					playlist.delete();
				}
			} catch (RuntimeException x) {
				Log.e(TAG, "Error in playlist cache cleaning.", x);
			}

			return null;
		}
	}
}
