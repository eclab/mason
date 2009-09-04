/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.util.*;

/** Called by Sparse2DPortrayal to determine if all objects
    should be drawn or only one.  The Bags provided contain objects all sitting in
    the same cell location onscreen. */

public interface DrawPolicy
    {
    /** Specifies objects which should be drawn at a given location, and which objects should not.
        The <tt>fromHere</tt> Bag contains, for a given location, all the objects which can be found
        at the location.  This function places into the bag <tt>addToHere</tt> a subset of those objects
        which you wish to actually have drawn.  The order in which the objects appear in 
        <tt>addToHere</tt> Bag is the order in which they will be drawn, so the later objects in
        the Bag will be drawn on TOP of the earlier objects in the Bag.  Do <b>not modify</b> the 
        <tt>fromHere</tt> Bag.  The <tt>addToHere</tt> bag will be provided to this function in 
        an empty state; no need to clear() it.
                
        <p>This function should usually return <b>true</b>. However if you wish to use <b>all</b>
        the objects in the <tt>fromHere</tt> Bag, in exactly the order in which they appear, 
        you can be quite a bit more efficient by <i>not bothering</i> to add the objects into the
        <tt>addToHere</tt> bag and instead simply returning <b>false</b>, in which case the 
        <tt>fromHere</tt> bag will be used instead.
    */
    public boolean objectToDraw(Bag fromHere, Bag addToHere);
    }
