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

package net.sourceforge.subsonic.androidapp.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import net.sourceforge.subsonic.androidapp.util.Constants;
import net.sourceforge.subsonic.androidapp.util.Util;
import net.sourceforge.subsonic.androidapp.provider.SearchSuggestionProvider;

/**
 * Receives search queries and forwards to the SelectAlbumActivity.
 *
 * @author Sindre Mehus
 */
public class QueryReceiverActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String query = getIntent().getStringExtra(SearchManager.QUERY);

        if (query != null) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionProvider.AUTHORITY,
                                                                              SearchSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);

            Intent intent = new Intent(QueryReceiverActivity.this, SearchActivity.class);
            intent.putExtra(Constants.INTENT_EXTRA_NAME_QUERY, query);
            Util.startActivityWithoutTransition(QueryReceiverActivity.this, intent);
        }
        finish();
        Util.disablePendingTransition(this);
    }
}