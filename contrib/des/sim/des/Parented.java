/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   A simple interface for objects which have parents and names
*/

public interface Parented extends Named, Resettable, sim.engine.Steppable
    {
    /**
       Returns the object's parent.
    */
    public Object getParent();

    /**
       Sets the object's parent.
    */
    public void setParent(Object parent);
    }
