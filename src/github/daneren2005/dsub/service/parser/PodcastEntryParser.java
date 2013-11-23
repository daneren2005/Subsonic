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
package github.daneren2005.dsub.service.parser;

import android.content.Context;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.domain.PodcastEpisode;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ProgressListener;
import java.io.Reader;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * @author Scott
 */
public class PodcastEntryParser extends AbstractParser {
	private static int bogusId = -1;
	
	public PodcastEntryParser(Context context) {
        super(context);
    }
	
	public MusicDirectory parse(String channel, Reader reader, ProgressListener progressListener) throws Exception {
		updateProgress(progressListener, R.string.parser_reading);
		init(reader);

		MusicDirectory episodes = new MusicDirectory();
		int eventType;
		boolean valid = false;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("channel".equals(name)) {
					String id = get("id");
					if(id.equals(channel)) {
						episodes.setId(id);
						episodes.setName(get("title"));
						valid = true;
					} else {
						valid = false;
					}
				}
				else if ("episode".equals(name) && valid) {
					PodcastEpisode episode = new PodcastEpisode();
					episode.setEpisodeId(get("id"));
					episode.setId(get("streamId"));
					episode.setTitle(get("title"));
					episode.setArtist(episodes.getName());
					episode.setAlbum(get("description"));
					episode.setDate(get("publishDate"));
					if(episode.getDate() == null) {
						episode.setDate(get("created"));
					}
					if(episode.getDate() != null && episode.getDate().indexOf("T") != -1) {
						episode.setDate(episode.getDate().replace("T", " "));
					}
					episode.setStatus(get("status"));
					episode.setCoverArt(get("coverArt"));
					episode.setSize(getLong("size"));
					episode.setContentType(get("contentType"));
					episode.setSuffix(get("suffix"));
					episode.setDuration(getInteger("duration"));
					episode.setBitRate(getInteger("bitRate"));
					episode.setVideo(getBoolean("isVideo"));
					episode.setPath(get("path"));
					if(episode.getPath() == null) {
						episode.setPath(FileUtil.getPodcastPath(context, episode));
					} else if(episode.getPath().indexOf("Podcasts/") == 0) {
						episode.setPath(episode.getPath().substring("Podcasts/".length()));
					}
					
					if(episode.getId() == null) {
						episode.setId(String.valueOf(bogusId));
						bogusId--;
					}
					episodes.addChild(episode);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();
		return episodes;
	}
}
