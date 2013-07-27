package github.daneren2005.dsub.domain;

import java.io.Serializable;

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
}
