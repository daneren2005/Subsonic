package github.daneren2005.dsub.domain;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

public class Genre implements Serializable {
	private String name;
    private String index;
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
    
    @Override
    public String toString() {
        return name;
    }

	public static class GenreComparator implements Comparator<Genre> {
		@Override
		public int compare(Genre genre1, Genre genre2) {
			return genre1.getName().compareToIgnoreCase(genre2.getName());
		}

		public static List<Genre> sort(List<Genre> genres) {
			Collections.sort(genres, new GenreComparator());
			return genres;
		}

	}
}
