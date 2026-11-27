package github.paroj.dsub2000.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import github.paroj.dsub2000.domain.SearchResult;

public class SearchViewModel extends ViewModel {

    private final MutableLiveData<SearchResult> searchResult = new MutableLiveData<>();

    public LiveData<SearchResult> getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult newData) {
        searchResult.setValue(newData);
    }

}