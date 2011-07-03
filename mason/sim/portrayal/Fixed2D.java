/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   <p>A Fixed2D object has control over how it is moved by a MovablePortrayal2D object.
   Objects may wish to implement this interface to:
   
   <p>
   <ul>
   <li>Prevent MovablePortrayal2D from moving them
   <li>Restrict how or where the MovablePortrayal2D can move them.
   <li>Stay informed about being moved
   <li>Update multiple fields as a result of being moved.
   </ul>
        
   <p>Objects which do not implement this interface have no say: MovablePortrayal2D will
   go directly to their fields and ask the fields to move them about.
*/

public interface Fixed2D
    {
    /** Returns true if the object permits you to change its location in the field.
        Else returns false.  This can be used for various purposes:
                
        <p>
        <ul>
        <li>If you don't want to be moved by a MovablePortrayal2D, simply return false.
        <li>If you're fine being moved, simply return true.
        <li>If want to be informed of being moved -- for example, to update internal
        belief about your location -- when this method is called just make the
        internal updates, then return true.
        <li>If you want to control where you're being moved, for example to guarantee
        that you're moved in a straight line, or constrained to be within a certain
        region, based on the provided location, move the Object itself in the field
        to a revised location of your choosing, then return false.
        <li>If you are stored in multiple fields and need to make certain that all of 
        them are updated properly when the user moves you in one field, when this
        method is called just make all the appropriate updates in the various fields,
        then return true.
        </ul>
    */
    public boolean maySetLocation(Object field, Object newObjectLocation);
    }
