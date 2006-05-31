/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   A proxiable object is one which provides SimpleProperties with a proxy to
   stand in for it; that is, to have the proxy's properties inspected instead
   of the object itself. 
*/
    
public interface Proxiable
    {
    /** Returns the proxy object to query for Properties instead of me. */
    public Object propertiesProxy();
    }
