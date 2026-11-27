package github.paroj.dsub2000.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import github.paroj.dsub2000.domain.ArtistInfo;
import github.paroj.dsub2000.domain.MusicDirectory.Entry;

public class SelectDirectoryViewModel extends ViewModel {

    private final MutableLiveData<List<Entry>> entries = new MutableLiveData<>();
    private final MutableLiveData<List<Entry>> albums = new MutableLiveData<>();
    private final MutableLiveData<ArtistInfo> artistInfo = new MutableLiveData<>();

    public LiveData<List<Entry>> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> list) {
        entries.setValue(list);
    }

    public LiveData<List<Entry>> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Entry> list) {
        albums.setValue(list);
    }

    public LiveData<ArtistInfo> getArtistInfo() {
        return artistInfo;
    }

    public void setArtistInfo(ArtistInfo newArtistInfo) {
        artistInfo.setValue(newArtistInfo);
    }

}
