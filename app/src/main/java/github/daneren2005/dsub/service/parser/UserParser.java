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
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.domain.User;
import github.daneren2005.dsub.domain.User.MusicFolderSetting;
import github.daneren2005.dsub.domain.User.Setting;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.ProgressListener;

public class UserParser extends AbstractParser {
	private static final String TAG = UserParser.class.getSimpleName();

	public UserParser(Context context, int instance) {
		super(context, instance);
	}

	public List<User> parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);
		List<User> result = new ArrayList<User>();
		List<MusicFolder> musicFolders = null;
		User user = null;
		int eventType;

		String tagName = null;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				tagName = getElementName();
				if ("user".equals(tagName)) {
					user = new User();

					user.setUsername(get("username"));
					user.setEmail(get("email"));
					parseSetting(user, User.SCROBBLING);
					for(String role: User.ROLES) {
						parseSetting(user, role);
					}
					parseSetting(user, User.LASTFM);

					result.add(user);
				} else if ("error".equals(tagName)) {
					handleError();
				}
			} else if(eventType == XmlPullParser.TEXT) {
				if("folder".equals(tagName)) {
					String id = getText();
					if(musicFolders == null) {
						musicFolders = getMusicFolders();
					}

					if(user != null) {
						if(user.getMusicFolderSettings() == null) {
							for (MusicFolder musicFolder : musicFolders) {
								user.addMusicFolder(musicFolder);
							}
						}

						for(Setting musicFolder: user.getMusicFolderSettings()) {
							if(musicFolder.getName().equals(id)) {
								musicFolder.setValue(true);
								break;
							}
						}
					}
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();

		return result;
	}

	private List<MusicFolder> getMusicFolders() throws Exception{
		MusicService musicService = MusicServiceFactory.getMusicService(context);
		return musicService.getMusicFolders(false, context, null);
	}
	
	private void parseSetting(User user, String name) {
		String value = get(name);
		if(value != null) {
			user.addSetting(name, "true".equals(value));
		}
	}
}
