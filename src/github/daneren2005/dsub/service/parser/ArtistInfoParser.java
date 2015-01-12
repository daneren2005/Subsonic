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

import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.ArtistInfo;
import github.daneren2005.dsub.util.ProgressListener;

public class ArtistInfoParser extends AbstractParser {
	private static final String TAG = ArtistInfo.class.getSimpleName();

	public ArtistInfoParser(Context context, int instance) {
		super(context, instance);
	}

	public ArtistInfo parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		ArtistInfo info = new ArtistInfo();
		List<Artist> artists = new ArrayList<Artist>();
		List<String> missingArtists = new ArrayList<String>();

		String tagName = null;
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				tagName = getElementName();
				if ("similarArtist".equals(tagName)) {
					String id = get("id");
					if(id.equals("-1")) {
						missingArtists.add(get("name"));
					} else {
						Artist artist = new Artist();
						artist.setId(id);
						artist.setName(get("name"));
						artist.setStarred(get("starred") != null);
						artists.add(artist);
					}
				} else if ("error".equals(tagName)) {
					handleError();
				}
			} else if(eventType == XmlPullParser.TEXT) {
				if ("biography".equals(tagName) && info.getBiography() == null) {
					info.setBiography(getText());
				} else if ("musicBrainzId".equals(tagName) && info.getMusicBrainzId() == null) {
					info.setMusicBrainzId(getText());
				} else if ("lastFmUrl".equals(tagName) && info.getLastFMUrl() == null) {
					info.setLastFMUrl(getText());
				} else if ("largeImageUrl".equals(tagName) && info.getImageUrl() == null) {
					info.setImageUrl(getText());
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		info.setSimilarArtists(artists);
		info.setMissingArtists(missingArtists);
		return info;
	}
}
