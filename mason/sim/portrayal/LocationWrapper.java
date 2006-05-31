/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   A LocationWrapper is used to embody the objects stored in a FieldPortrayal; for
   example, those returned by a hitObjects test on a FieldPortrayal2D.  The wrapper
   contains the FieldPortrayal, the original object, and the original location.  We
   say "original object" and "original location", because some Fields move objects
   about.  To get the <i>current</i> object and the <i>current</i> location, you 
   need to call the getObject() and getLocation() methods.  LocationWrappers are
   most commonly used to provide inspectors.
    
   <p>FieldPortrayals should subclass this class according to their needs.  For
   example, ValueGridPortrayal2D and ObjectGridPortrayal2D lock inspectors to point
   at certain <i>locations</i>, rather than follow <i>objects</i> around.  In this
   case, these portrayals will override getObject() to return the object <i>currently</i>
   at the given location.  On the other hand, SparseGridPortrayal2D and ContinuousPortrayal2D
   lock inspectors to point at certain <i>objects</i> regardless of where the object is
   located.  In this case, these portrayals will override getLocation() instead to return
   the object's <i>current</i> location.

   <p>LocationWrapper is used for nearly identical functions in FieldPortrayal3Ds as well.
*/

public class LocationWrapper
    {
    /** The ORIGINAL object */
    protected Object object;
    /** The ORIGINAL location of the object */
    protected Object location;
    /** The field portrayal depicting this object */
    public FieldPortrayal fieldPortrayal;
    
    public LocationWrapper(Object object, Object location, FieldPortrayal fieldPortrayal)
        { this.object = object; this.location = location; this.fieldPortrayal = fieldPortrayal; }
        
    public FieldPortrayal getFieldPortrayal() { return fieldPortrayal; }
    /** Override this to provide the current object */
    public Object getObject() { return object; }
    /** Override this to provide the current location */
    public Object getLocation() { return location; }
    /** Override this to provide the current location's name */
    public String getLocationName() { return "" + location; }
    }
