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
	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.util.ProgressListener;

public class UserParser extends AbstractParser {

	public UserParser(Context context) {
		super(context);
	}

	public List<User> parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);
		List<User> result = new ArrayList<User>();
		int eventType;

		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("user".equals(name)) {
					User user = new User();

					user.setUsername(get("username"));
					user.setEmail(get("email"));
					user.setScrobblingEnabled(getBoolean("scrobblingEnabled"));
					user.setAdminRole(getBoolean("adminRole"));
					user.setSettingsRole(getBoolean("settingsRole"));
					user.setDownloadRole(getBoolean("downloadRole"));
					user.setUploadRole(getBoolean("uploadRole"));
					user.setPlaylistRole(getBoolean("playlistRole"));
					user.setCoverArtRole(getBoolean("coverArtRole"));
					user.setCommentRole(getBoolean("commentRole"));
					user.setPodcastRole(getBoolean("podcastRole"));
					user.setStreamRole(getBoolean("streamRole"));
					user.setJukeboxRole(getBoolean("jukeboxRole"));
					user.setShareRole(getBoolean("shareRole"));

					result.add(user);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();

		return result;
	}
}