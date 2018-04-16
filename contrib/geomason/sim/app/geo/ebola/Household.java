package ebola;

import ebola.Structure;
import sim.util.Bag;
import sim.util.Int2D;

/**
 * Created by rohansuri on 7/8/15.
 */
public class Household extends Structure
{
    private int country;

    public Household(Int2D location)
    {
        super(location);
    }

    public int getCountry() {
        return country;
    }

    public void setCountry(int country) {
        this.country = country;
    }
}
