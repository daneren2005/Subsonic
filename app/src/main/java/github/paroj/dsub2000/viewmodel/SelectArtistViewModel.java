package github.paroj.dsub2000.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import github.paroj.dsub2000.domain.MusicFolder;

public class SelectArtistViewModel extends ViewModel {

    private final MutableLiveData<List<MusicFolder>> musicFolders = new MutableLiveData<>();

    public LiveData<List<MusicFolder>> getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(List<MusicFolder> newData) {
        musicFolders.setValue(newData);
    }

}