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

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ServerInfo;
import github.daneren2005.dsub.util.ProgressListener;

public class ScanStatusParser extends AbstractParser {

	public ScanStatusParser(Context context, int instance) {
		super(context, instance);
	}

	public boolean parse(Reader reader, ProgressListener progressListener) throws Exception {
		init(reader);

		String scanName, scanningName;
		if(ServerInfo.isMadsonic(context, instance)) {
			scanName = "status";
			scanningName = "started";
		} else {
			scanName = "scanStatus";
			scanningName = "scanning";
		}

		Boolean scanning = null;
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if(scanName.equals(name)) {
					scanning = getBoolean(scanningName);

					String msg = context.getResources().getString(R.string.parser_scan_count, getInteger("count"));
					progressListener.updateProgress(msg);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();

		return scanning != null && scanning;
	}
}