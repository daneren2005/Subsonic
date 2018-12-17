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
package github.vrih.xsub.service.parser;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import github.vrih.xsub.domain.PodcastChannel;

/**
 *
 * @author Scott
 */
public class PodcastChannelParser extends AbstractParser {
	public PodcastChannelParser(Context context, int instance) {
		super(context, instance);
	}

	public List<PodcastChannel> parse(Reader reader) throws Exception {
		init(reader);

		List<PodcastChannel> channels = new ArrayList<>();
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("channel".equals(name)) {
					PodcastChannel channel = new PodcastChannel();
					channel.setId(get("id"));
					channel.setUrl(get("url"));
					channel.setName(get("title"));
					channel.setDescription(get("description"));
					channel.setStatus(get("status"));
					channel.setErrorMessage(get("errorMessage"));
					channel.setCoverArt(get("coverArt"));
					channels.add(channel);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();
		return PodcastChannel.PodcastComparator.sort(channels, context);
	}
}
