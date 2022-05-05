/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.network;

/**
   A simple interface for objects which have names.
*/

public interface Displayable
    {
    public boolean getDrawState();
    public String getLabel();
    }
