package sim.app.geo.touchingworld;

import sim.engine.*;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/** Randomly selects currently highlighted shape
 *
 * Will randomly swap as selected a district adjacent to the currently
 * selected district.
 *
 * @author mcoletti
 */
public class Mover implements Steppable {


    private static final long serialVersionUID = 5456255360842258779L;


	public Mover() {}
   
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
        MasonGeometry nextShape = null;

        if ( 1 == adjacentShapes.size() )
        {
            nextShape = (MasonGeometry) adjacentShapes.objs[0];
        }
        else
        {
            nextShape = (MasonGeometry) adjacentShapes.objs[state.random.nextInt(adjacentShapes.size())];
        }

        System.out.println("\tselected " + nextShape);

        // And then do the swap
        
        world.selectShape(nextShape);

    }
}
