package github.paroj.dsub2000.domain.repository;

import android.content.Context;

import java.util.List;

import github.paroj.dsub2000.domain.MusicDirectory;
import github.paroj.dsub2000.service.MusicService;
import github.paroj.dsub2000.util.ProgressListener;

public final class StarredRepository {

    private final MusicService musicService;
    private final Context context;

    public StarredRepository(MusicService musicService, Context context) {
        this.musicService = musicService;
        this.context = context;
    }

    public MusicDirectory getStarredAlbums(ProgressListener progressListener) throws Exception {
        return getFilteredStarredList(progressListener, true, false);
    }

    public MusicDirectory getStarredSongs(ProgressListener progressListener) throws Exception {
        return getFilteredStarredList(progressListener, false, true);
    }

    private MusicDirectory getFilteredStarredList(ProgressListener progressListener,
                                                  boolean includeDirs,
                                                  boolean includeFiles) throws Exception {

        MusicDirectory result = musicService.getStarredList(context, progressListener);
        List<MusicDirectory.Entry> children = result.getChildren(includeDirs, includeFiles);
        return new MusicDirectory(children);
    }
}
