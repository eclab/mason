/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;

import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;

/** A wrapper used by NetworkPortrayal2D to hold a Network and EITHER a Continuous2D OR a SparseGrid2D (or some other SparseField2D).
    The Continuous2D/SparseGrid2D specifies the spatial location of the nodes; the Network specifies the
    edges connecting those nodes. 
    
    <p>SpatialNetwork2D can also hold an additional location (another Continuous2D/SparseGrid2D) which might
    hold the nodes if the first location does not.  This allows you to (for example) have the FROM nodes
    in one field and the TO nodes in another field.  You can set this location with setAuxillaryField.
    Note that this will only work properly if the fields have exactly the same dimensions with respect to
    how their field portrayals draw them onscreen.  As a sanity check: you shouldn't have one field be a Continuous2D
    and the other be a SparseGrid2D.  */

public class SpatialNetwork2D
    {
    SparseField2D field;
    SparseField2D field2;
    Network network;

    public SpatialNetwork2D( final SparseField2D field, final Network network )
        {
        this.field = field;
        if (field == null)
            throw new RuntimeException("Null SparseField2D.");
        this.network = network;
        if (network == null)
            throw new RuntimeException("Null Network.");
        }
    
    public void setAuxiliaryField( final SparseField2D f)
        {
        field2 = f;
        }
                
    /**
       @deprecated, misspelled name!  Use setAuxiliaryField instead.
    */
    public void setAuxillaryField( final SparseField2D f)
        {
        setAuxiliaryField(f);
        }

    /**
       NOTE this  used to be deprecated, but it has now been de-deprecated so as to be
       consistent with SpatialNetwork3D.  This method returns the location of the object
       as a Double2D regardless of which field it is located in and regardless of whether
       the field is a Double2D or SparseGrid2D.  We may re-deprecate it eventually.
    */
    public Double2D getObjectLocation(Object node)
        {
        Double2D loc= field.getObjectLocationAsDouble2D(node);
        if (loc == null && field2 != null)
            loc = field2.getObjectLocationAsDouble2D(node);
        return loc;
        }

    /** 
        @deprecated
    */
    public Double2D getDimensions() { return field.getDimensions(); }
    }

