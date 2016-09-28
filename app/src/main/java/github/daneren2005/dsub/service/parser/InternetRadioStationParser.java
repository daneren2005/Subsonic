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

	Copyright 2016 (C) Scott Jackson
*/
package github.daneren2005.dsub.service.parser;

import android.content.Context;
import github.daneren2005.dsub.domain.InternetRadioStation;
import github.daneren2005.dsub.util.ProgressListener;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class InternetRadioStationParser extends ErrorParser {
	public InternetRadioStationParser(Context context, int instance) {
		super(context, instance);
	}

	public List<InternetRadioStation> parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		List<InternetRadioStation> result = new ArrayList<>();
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("internetRadioStation".equals(name)) {
					InternetRadioStation station = new InternetRadioStation();

					station.setId(get("id"));
					station.setTitle(get("name"));
					station.setStreamUrl(get("streamUrl"));
					station.setHomePageUrl(get("homePageUrl"));

					result.add(station);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();
		return result;
	}

}