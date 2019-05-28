/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field;
import sim.util.*;

public interface SparseField2D
    {
    /** Returns the width and height of the sparse field as a Double2D */
    public Double2D getDimensions();
        
    /** Returns the location of an object in the sparse field as a Double2D */
    public Double2D getObjectLocationAsDouble2D(Object obect);
    }