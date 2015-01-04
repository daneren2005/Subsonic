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

import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.util.ProgressListener;

public class ArtistInfoParser extends AbstractParser {

	public ArtistInfoParser(Context context, int instance) {
		super(context, instance);
	}

	public ArtistInfo parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		ArtistInfo info = new ArtistInfo();
		List<Artist> artists = new ArrayList<Artist>();

		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("biography".equals(name)) {
					info.setBiography(getText());
				} else if ("musicBrainzId".equals(name)) {
					info.setMusicBrainzId(getText());
				} else if ("lastFmUrl".equals(name)) {
					info.setLastFMUrl(getText());
				} else if ("largeImageUrl".equals(name)) {
					info.setImageUrl(getText());
				} else if ("similarArtist".equals(name)) {
					Artist artist = new Artist();
					artist.setId(get("id"));
					artist.setName(get("name"));
					artists.add(artist);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		info.setSimilarArtists(artists);
		return info;
	}
}
