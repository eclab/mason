/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    MutableNumberND is the top-level abstract class of MASON's 2D and 3D mutable ints and doubles,
    and is a subclass of NumberND.  All MutableNumberND classes are cloneable.
*/

public abstract class MutableNumberND extends NumberND implements Cloneable
    {
    private static final long serialVersionUID = 1;
    
    public abstract void setVal(int val, double to);
    public boolean isMutable() { return true; }
    
    }
