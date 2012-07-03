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

import java.lang.reflect.Method;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import net.sourceforge.subsonic.androidapp.R;
import net.sourceforge.subsonic.androidapp.service.MusicServiceFactory;
import net.sourceforge.subsonic.androidapp.util.Constants;
import net.sourceforge.subsonic.androidapp.util.Util;

/**
 * Plays videos in a web page.
 *
 * @author Sindre Mehus
 */
public final class PlayVideoActivity extends Activity {

    private static final String TAG = PlayVideoActivity.class.getSimpleName();
    private WebView webView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.play_video);

        webView = (WebView) findViewById(R.id.play_video_contents);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginsEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);

        webView.setWebViewClient(new Client());
        if (bundle != null) {
            webView.restoreState(bundle);
        } else {
            webView.loadUrl(getVideoUrl());
        }

        // Show warning if Flash plugin is not installed.
        if (isFlashPluginInstalled()) {
            Util.toast(this, R.string.play_video_loading, false);
        } else {
            Util.toast(this, R.string.play_video_noplugin, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        callHiddenWebViewMethod("onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        callHiddenWebViewMethod("onResume");
    }

    private String getVideoUrl() {
        String id = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID);
        return MusicServiceFactory.getMusicService(this).getVideoUrl(this, id);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        webView.saveState(state);
    }

    private void callHiddenWebViewMethod(String name){
        if( webView != null ){
            try {
                Method method = WebView.class.getMethod(name);
                method.invoke(webView);
            } catch (Throwable x) {
                Log.e(TAG, "Failed to invoke " + name, x);
            }
        }
    }

    private boolean isFlashPluginInstalled() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("com.adobe.flashplayer", 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException x) {
            return false;
        }
    }

    private final class Client extends WebViewClient {

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Util.toast(PlayVideoActivity.this, description);
            Log.e(TAG, "Error: " + description);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            Log.d(TAG, "onLoadResource: " + url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "onPageStarted: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "onPageFinished: " + url);
        }
    }
}
