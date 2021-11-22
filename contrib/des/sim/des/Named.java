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
   A simple interface for objects which have names.
*/

public interface Named extends Steppable
    {
    /**
       Returns the object's name.
    */
    public String getName();

    /**
       Sets the object's name.
    */
    public void setName(String name);
    }
