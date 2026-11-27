package github.paroj.dsub2000.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import github.paroj.dsub2000.domain.MusicDirectory;

public class SelectPodcastViewModel extends ViewModel {

    private final MutableLiveData<MusicDirectory> newestEpisodes = new MutableLiveData<>();

    public LiveData<MusicDirectory> getNewestEpisodes() {
        return newestEpisodes;
    }

    public void setNewestEpisodes(MusicDirectory newData) {
        newestEpisodes.setValue(newData);
    }

}