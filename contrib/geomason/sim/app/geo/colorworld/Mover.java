/*
 * Mover.java
 *
 * An agent that will be moving within a county political currentDistrict
 *
 * $Id: Mover.java,v 1.1 2010-04-05 17:21:54 mcoletti Exp $
 */

package sim.app.geo.colorworld;

import sim.app.geo.adjacentworld.*;
import com.vividsolutions.jts.geom.*;
import sim.engine.*;
import sim.field.geo.*;
import sim.util.Bag;

/** Randomly selects currently highlighted district
 *
 * Will randomly swap as selected a district adjacent to the currently
 * selected district.
 *
 * @author mcoletti
 */
public class Mover implements Steppable {


    // agent's political currentDistrict
    private Polygon currentDistrict = null;

    public
    Polygon getCurrentDistrict()
    {
        return currentDistrict;
    }

    public
    void setCurrentDistrict(Polygon currentDistrict)
    {
        this.currentDistrict = currentDistrict;
    }

    public Mover()
    {
        currentDistrict = null;
    }
	

    public void step(SimState state)
    {
        GeomField world = ((AdjacentWorld)state).county;
        GeomField selectedDistrict = ((AdjacentWorld)state).selectedDistrict;

        // Find all districts touching the current one
        Bag adjacentDistricts = world.getTouchingObjects(currentDistrict);

        // We have a serious problem if there are no districts adjacent to the
        // current one.
        if (adjacentDistricts.isEmpty())
        {
            throw new RuntimeException("No adjacent districts");
        }

        // Pick one randomly

        // And then do the swap
    }
}
