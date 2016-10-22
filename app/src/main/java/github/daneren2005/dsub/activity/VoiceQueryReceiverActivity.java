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

package github.daneren2005.dsub.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.util.Log;

import github.daneren2005.dsub.fragments.SubsonicFragment;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.provider.DSubSearchProvider;

/**
 * Receives voice search queries and forwards to the SearchFragment.
 *
 * http://android-developers.blogspot.com/2010/09/supporting-new-music-voice-action.html
 *
 * @author Sindre Mehus
 */
public class VoiceQueryReceiverActivity extends Activity {
	private static final String TAG = VoiceQueryReceiverActivity.class.getSimpleName();
	private static final String GMS_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String query = getIntent().getStringExtra(SearchManager.QUERY);

        if (query != null) {
            Intent intent = new Intent(VoiceQueryReceiverActivity.this, SubsonicFragmentActivity.class);
            intent.putExtra(Constants.INTENT_EXTRA_NAME_QUERY, query);
			if(!GMS_SEARCH_ACTION.equals(getIntent().getAction())) {
				intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
			}

			String artist = getIntent().getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST);
			if(artist != null) {
				intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
			}

			String album = getIntent().getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM);
			if(album != null) {
				intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
			}

			String title = getIntent().getStringExtra(MediaStore.EXTRA_MEDIA_TITLE);
			if(title != null) {
				intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, title);
			}

            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, getIntent().getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS));
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Util.startActivityWithoutTransition(VoiceQueryReceiverActivity.this, intent);
        }
        finish();
        Util.disablePendingTransition(this);
    }
}