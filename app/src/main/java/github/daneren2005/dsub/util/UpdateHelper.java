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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.view.UpdateView;

public final class UpdateHelper {
	private static final String TAG = UpdateHelper.class.getSimpleName();

	public static void toggleStarred(Context context, Entry entry) {
		toggleStarred(context, entry, null);
	}

	public static void toggleStarred(final Context context, final Entry entry, final OnStarChange onStarChange) {
		toggleStarred(context, Arrays.asList(entry), onStarChange);
	}

	public static void toggleStarred(Context context, List<Entry> entries) {
		toggleStarred(context, entries, null);
	}
	public static void toggleStarred(final Context context, final List<Entry> entries, final OnStarChange onStarChange) {
		if(entries.isEmpty()) {
			return;
		}

		final Entry firstEntry = entries.get(0);
		final boolean starred = !firstEntry.isStarred();
		for(Entry entry: entries) {
			entry.setStarred(starred);
		}
		if(onStarChange != null) {
			onStarChange.entries = entries;
			onStarChange.starChange(starred);
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				List<Entry> songs = new ArrayList<Entry>();
				List<Entry> artists = new ArrayList<Entry>();
				List<Entry> albums = new ArrayList<Entry>();
				for(Entry entry: entries) {
					if(entry.isDirectory() && Util.isTagBrowsing(context)) {
						if(entry.isAlbum()) {
							albums.add(entry);
						} else {
							artists.add(entry);
						}
					} else {
						songs.add(entry);
					}
				}
				musicService.setStarred(songs, artists, albums, starred, this, context);

				for(Entry entry: entries) {
					new UpdateHelper.EntryInstanceUpdater(entry) {
						@Override
						public void update(Entry found) {
							found.setStarred(starred);
						}
					}.execute();
				}

				return null;
			}

			@Override
			protected void done(Void result) {
				// UpdateView
				int starMsgId = starred ? R.string.starring_content_starred : R.string.starring_content_unstarred;
				String starMsgBody = (entries.size() > 1) ? Integer.toString(entries.size()) : firstEntry.getTitle();
				Util.toast(context, context.getResources().getString(starMsgId, starMsgBody));

				if(onStarChange != null) {
					onStarChange.starCommited(starred);
				}
			}

			@Override
			protected void error(Throwable error) {
				Log.w(TAG, "Failed to star", error);
				for(Entry entry: entries) {
					entry.setStarred(!starred);
				}
				if(onStarChange != null) {
					onStarChange.starChange(!starred);
				}

				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					String errorBody = (entries.size() > 1) ? Integer.toString(entries.size()) : firstEntry.getTitle();
					msg = context.getResources().getString(R.string.starring_content_error, errorBody) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	public static void toggleStarred(final Context context, final Artist entry) {
		final boolean starred = !entry.isStarred();
		entry.setStarred(starred);

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				if(Util.isTagBrowsing(context) && !Util.isOffline(context)) {
					musicService.setStarred(null, Arrays.asList(new Entry(entry)), null, starred, null, context);
				} else {
					musicService.setStarred(Arrays.asList(new Entry(entry)), null, null, starred, null, context);
				}
				return null;
			}

			@Override
			protected void done(Void result) {
				// UpdateView
				Util.toast(context, context.getResources().getString(starred ? R.string.starring_content_starred : R.string.starring_content_unstarred, entry.getName()));
			}

			@Override
			protected void error(Throwable error) {
				Log.w(TAG, "Failed to star", error);
				entry.setStarred(!starred);

				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(R.string.starring_content_error, entry.getName()) + " " + getErrorMessage(error);
				}

				Util.toast(context, msg, false);
			}
		}.execute();
	}

	public static void setRating(Activity context, Entry entry) {
		setRating(context, entry, null);
	}
	public static void setRating(final Activity context, final Entry entry, final OnRatingChange onRatingChange) {
		View layout = context.getLayoutInflater().inflate(R.layout.rating, null);
		final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.rating_bar);
		ratingBar.setRating((float) entry.getRating());

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getResources().getString(R.string.rating_title, entry.getTitle()))
				.setView(layout)
				.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						int rating = (int) ratingBar.getRating();
						setRating(context, entry, rating, onRatingChange);
					}
				})
				.setNegativeButton(R.string.common_cancel, null);

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public static void setRating(Context context, Entry entry, int rating) {
		setRating(context, entry, rating, null);
	}
	public static void setRating(final Context context, final Entry entry, final int rating, final OnRatingChange onRatingChange) {
		final int oldRating = entry.getRating();
		entry.setRating(rating);

		if(onRatingChange != null) {
			onRatingChange.ratingChange(rating);
		}

		new SilentBackgroundTask<Void>(context) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				musicService.setRating(entry, rating, context, null);

				new EntryInstanceUpdater(entry) {
					@Override
					public void update(Entry found) {
						found.setRating(rating);
					}
				}.execute();
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(context, context.getResources().getString(rating > 0 ? R.string.rating_set_rating : R.string.rating_remove_rating, entry.getTitle()));
			}

			@Override
			protected void error(Throwable error) {
				entry.setRating(oldRating);
				if(onRatingChange != null) {
					onRatingChange.ratingChange(oldRating);
				}

				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = context.getResources().getString(rating > 0 ? R.string.rating_set_rating_failed : R.string.rating_remove_rating_failed, entry.getTitle()) + " " + getErrorMessage(error);
				}

				Log.e(TAG, "Failed to setRating", error);
				Util.toast(context, msg, false);
			}
		}.execute();
	}

	public static abstract class EntryInstanceUpdater {
		private Entry entry;
		protected int metadataUpdate = DownloadService.METADATA_UPDATED_ALL;

		public EntryInstanceUpdater(Entry entry) {
			this.entry = entry;
		}
		public EntryInstanceUpdater(Entry entry, int metadataUpdate) {
			this.entry = entry;
			this.metadataUpdate = metadataUpdate;
		}

		public abstract void update(Entry found);

		public void execute() {
			DownloadService downloadService = DownloadService.getInstance();
			if(downloadService != null && !entry.isDirectory()) {
				boolean serializeChanges = false;
				List<DownloadFile> downloadFiles = downloadService.getDownloads();
				DownloadFile currentPlaying = downloadService.getCurrentPlaying();

				for(DownloadFile file: downloadFiles) {
					Entry check = file.getSong();
					if(entry.getId().equals(check.getId())) {
						update(check);
						serializeChanges = true;

						if(currentPlaying != null && currentPlaying.getSong() != null && currentPlaying.getSong().getId().equals(entry.getId())) {
							downloadService.onMetadataUpdate(metadataUpdate);
						}
					}
				}

				if(serializeChanges) {
					downloadService.serializeQueue();
				}
			}

			Entry find = UpdateView.findEntry(entry);
			if(find != null) {
				update(find);
			}
		}
	}

	public static abstract class OnStarChange {
		protected List<Entry> entries;

		public abstract void starChange(boolean starred);
		public abstract void starCommited(boolean starred);
	}
	public static abstract class OnRatingChange {
		public abstract void ratingChange(int rating);
	}
}
