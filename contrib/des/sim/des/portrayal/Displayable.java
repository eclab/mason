/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

/**
   A simple interface for objects which can be queried for state
   information in order to draw it in a cute fashion.
*/

public interface Displayable
    {
    /** Returns the object's "draw state", however it determines this.
    	This is is signalled by drawing a circle around the object.
    */
    public boolean getDrawState();

    /** Returns the object's text label. */
    public String getLabel();
    }
