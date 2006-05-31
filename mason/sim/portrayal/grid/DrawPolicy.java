/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.util.*;

/** Called by Sparse2DPortrayal and Object2DPortrayal to determine if all objects
    should be drawn or only one.  The Bags provided contain objects all sitting in
    the same cell location onscreen. */

public interface DrawPolicy
    {
    public static final boolean DRAW_ALL = false;
    public static final boolean DONE = true;
    /** Give the bag fromHere (which you should NOT MODIFY), which contains objects all
        occupying the same location, add to the bag addtoHere only those objects you wish to be drawn,
        then return DONE.  Alternatively, simply make no additions, then return DRAW_ALL if you wish the
        entire fromHere bag to be drawn.  If you don't want anything to be drawn, just return DONE.
        Do not replace the array in addToHere.  */
    public boolean objectToDraw(Bag fromHere, Bag addToHere);
    }
