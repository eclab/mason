/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   A Fixed2D object has control over how it is moved by a MovablePortrayal2D object.
   
   <p>Objects which implement this interface can force MovablePortrayal2D to NOT move them
   whenever they wish, or can interrupt the moving to move themselves as they like.
   
   <p>Objects which do not implement this interface have no say: MovablePortrayal2D will
   go directly to their fields and ask the fields to move them about.
*/

public interface Fixed2D
    {
    /** Returns true if the object permits you to change its location in the field.
        Else returns false.  This can be used in one of two common ways.  First,
        the object may wish to simply deny you the ability to move it at certain times.
        Second, the object may wish to move ITSELF rather than have the field move it.
        In the second case, the object could, during canSetLocation(...), move itself
        in the field, then return FALSE, denying you the ability to ask the field
        to move the object again. */
    public boolean maySetLocation(Object field, Object location);
    }
