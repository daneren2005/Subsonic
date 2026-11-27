package github.paroj.dsub2000.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SelectRecyclerViewModel<T> extends ViewModel {

    private final MutableLiveData<List<T>> objects = new MutableLiveData<>();

    public LiveData<List<T>> getObjects() {
        return objects;
    }

    public void setObjects(List<T> newData) {
        objects.setValue(newData);
    }

}