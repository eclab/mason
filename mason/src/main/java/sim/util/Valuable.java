/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** 
    Having a value.  This interface defines a single method, doubleValue(), which should return
    the "value" of the object, whatever that is.  doubleValue() is not defined as getDoubleValue()
    for two reasons.  First, we don't necessarily want this value to show up as a property.
    Second, it's consistent with Number.doubleValue().
*/

public interface Valuable
    {
    public double doubleValue();
    }
