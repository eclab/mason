/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   A Propertied object is one which provides its own Properties rather than
   letting SimpleProperties scan the object statically.  This is generally
   rare and mostly used for dynamic objects or certain abstract classes. 
*/
    
public interface Propertied
    {
    /** Returns my own Properties. */
    public Properties properties();
    }
