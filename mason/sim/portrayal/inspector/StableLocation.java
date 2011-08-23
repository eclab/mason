/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;

/** StableLocations are simple classes which provides set/get methods to Int2D, Double2D, etc. from SparseFields
    of different sorts.  Here's the idea: in order to inspect the *location* of an object in a SparseField, we need
    to request the location of the object each inspector update time.  But the SparseFields continually provide
    different instances for their Int2D, Double2D, etc. locations.  This causes inspectors to stop updating
    themselves as they think the object they're inspecting (a location) has gone away and has been replaced
    with something else.  StableLocations provide a constant location that has inspectable get/set methods and are
    used by various LocationWrappers to prevent this from happening.
    
    StableLocations are constructed by providing a field and the object which has the location.  Then whenever you
    call the update() method, the location is reladed into the StableLocation and can be inspected.
*/

public interface StableLocation
    {
    public String toString();
    }
