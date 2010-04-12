/*
 * Mover.java
 *
 * An agent that will be moving within a shapes world
 *
 * $Id: Mover.java,v 1.1 2010-04-12 20:32:40 mcoletti Exp $
 */

package sim.app.geo.touchingworld;

import sim.engine.*;
import sim.util.Bag;
import sim.util.geo.GeomWrapper;

/** Randomly selects currently highlighted shape
 *
 * Will randomly swap as selected a district adjacent to the currently
 * selected district.
 *
 * @author mcoletti
 */
public class Mover implements Steppable {


    public Mover()
    {
    }
	

    public void step(SimState state)
    {
        TouchingWorld world = (TouchingWorld)state;

        // Find all shapes touching the current one
        Bag adjacentShapes = world.shapes.getTouchingObjects(world.selectedShape.geometry);

        // We have a serious problem if there are no shapes adjacent to the
        // current one.
        if (adjacentShapes.isEmpty())
        {
            throw new RuntimeException("No adjacent shapes");
        }
        else
        {
            System.out.println(world.selectedShape);
            System.out.println("\t" + adjacentShapes.size() + " adjacent shapes");
            for (int i = 0; i < adjacentShapes.size(); i++)
            {
                System.out.println("\t\t" + adjacentShapes.objs[i]);
            }
        }

        // Pick one randomly
        GeomWrapper nextShape = null;

        if ( 1 == adjacentShapes.size() )
        {
            nextShape = (GeomWrapper) adjacentShapes.objs[0];
        }
        else
        {
            nextShape = (GeomWrapper) adjacentShapes.objs[state.random.nextInt(adjacentShapes.size())];
        }

        System.out.println("\tselected " + nextShape);

        // And then do the swap
        
        world.selectShape(nextShape);

    }
}
