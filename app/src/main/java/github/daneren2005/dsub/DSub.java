package github.daneren2005.dsub;

import android.app.Application;

import java.io.IOException;

import cz.fhucho.android.util.SimpleDiskCache;

public class DSub extends Application {

    private SimpleDiskCache cache;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            cache = SimpleDiskCache.open(getExternalCacheDir(), 1, 64 * 1024 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleDiskCache getCache() {
        return cache;
    }
}
