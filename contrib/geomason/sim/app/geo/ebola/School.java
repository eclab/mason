package ebola;


import sim.util.Int2D;

/**
 * Created by rohansuri on 7/20/15.
 */
public class School extends WorkLocation {
    private int size;

    public School(Int2D location) {
        super(location, Constants.EDUCATION);
    }

    public int getSize() {
        return members.size();
    }

    public void setSize(int size) {
        this.size = size;
    }
}
