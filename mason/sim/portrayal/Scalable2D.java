/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   A Scalable2D object provides a scale, and can have the same changed.
   
   <p>Objects which implement this interface can have their scales changed by 
   the AdjustablePortryal2D.  Note that scale is not necessarily the same thing as value
   (as in the Valuable interface), though you can make them the same thing if you like.
   Typically scale is some size notion, where value is some intrinsic value which might be
   reflected, for example, in the color of the object.
*/

public interface Scalable2D
    {
    public double getScale2D();
    public void setScale2D(double val);
    }
