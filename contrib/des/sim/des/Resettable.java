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
   Things that can be reset.
*/

public interface Resettable
    {
    public void reset(SimState state);
    }
