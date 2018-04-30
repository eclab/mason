/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package touchingworld;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/** Randomly selects currently highlighted shape
 *
 * Will randomly swap as selected a district adjacent to the currently
 * selected district.
 *
 */
public class Mover implements Steppable
{
    private static final long serialVersionUID = 5456255360842258779L;


	public Mover() {}

    
    public void step(SimState state)
    {
        TouchingWorld world = (TouchingWorld)state;

        // Find all shapes touching the current one

        // selectedShape will have only one geometry, so just grab the first one
        MasonGeometry selectedShape = (MasonGeometry) world.selectedShape.getGeometries().objs[0];

        // Find all the objects that touch the currently selected object.
        Bag adjacentShapes = world.shapes.getTouchingObjects(selectedShape);

        // We have a serious problem if there are no shapes adjacent to the
        // current one.
        if (adjacentShapes.isEmpty())
        {
            throw new RuntimeException("No adjacent shapes");
        }
        else
        {
            // TODO: Sean hates std out; so replace with inspectors, or something.
//            System.out.println(world.selectedShape);
//            System.out.println("\t" + adjacentShapes.size() + " adjacent shapes");
//            for (int i = 0; i < adjacentShapes.size(); i++)
//            {
//                System.out.println("\t\t" + adjacentShapes.objs[i]);
//            }
        }

        // Pick one randomly
        MasonGeometry nextShape = null;

        if ( 1 == adjacentShapes.size() )
        {
            nextShape = (MasonGeometry) adjacentShapes.objs[0];
        }
        else
        {
            nextShape = (MasonGeometry) adjacentShapes.objs[state.random.nextInt(adjacentShapes.size())];
        }

//        System.out.println("\tselected " + nextShape);

        // And then do the swap
        world.selectShape(nextShape);
    }
}
