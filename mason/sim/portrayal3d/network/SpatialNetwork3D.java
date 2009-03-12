/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.network;

import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;

/** A wrapper used by NetworkPortrayal3D to hold a Network and EITHER a Continuous3D OR a SparseGrid3D.
    The Continuous3D/SparseGrid3D specifies the spatial location of the nodes; the Network specifies the
    edges connecting those nodes. */

public class SpatialNetwork3D
    {
    public SparseField field;
    public SparseField field2;
    public Network network;

    public SpatialNetwork3D( final Continuous3D field, final Network network )
        {
        this.field = field;
        if (field == null)
            throw new RuntimeException("Null Continuous3D.");
        this.network = network;
        if (network == null)
            throw new RuntimeException("Null Network.");
        }
    
    public SpatialNetwork3D( final SparseGrid3D grid, final Network network )
        {
        this.field = grid;
        if (field == null)
            throw new RuntimeException("Null SparseGrid3D.");
        this.network = network;
        if (network == null)
            throw new RuntimeException("Null Network.");
        }
    
    public void setAuxillaryField( final Continuous3D f)
        {
        field2 = f;
        if (field2 != null && field instanceof SparseGrid3D)
            throw new RuntimeException("The auxillary field of a SpatialNetwork3D should be the same type as the primary field.");
        }

    public void setAuxillaryField( final SparseGrid3D f)
        {
        field2 = f;
        if (field2 != null && field instanceof Continuous3D)
            throw new RuntimeException("The auxillary field of a SpatialNetwork3D should be the same type as the primary field.");
        }

    public Double3D getObjectLocation(Object node)
        {
        Double3D loc = null;
        if (field instanceof Continuous3D) loc = ((Continuous3D)field).getObjectLocation(node);
        else loc = ((SparseGrid3D)field).getObjectLocationAsDouble3D(node);
        if (loc == null && field2 != null)
            {
            if (field2 instanceof Continuous3D) loc = ((Continuous3D)field2).getObjectLocation(node);
            else loc = ((SparseGrid3D)field2).getObjectLocationAsDouble3D(node);
            }
        return loc;
        }

    public double getWidth()
        {
        if (field instanceof Continuous3D) return ((Continuous3D)field).getWidth();
        else return ((SparseGrid3D)field).getWidth();
        }
        
    public double getHeight()
        {
        if (field instanceof Continuous3D) return ((Continuous3D)field).getHeight();
        else return ((SparseGrid3D)field).getHeight();
        }
        
    public double getLength()
        {
        if (field instanceof Continuous3D) return ((Continuous3D)field).getLength();
        else return ((SparseGrid3D)field).getLength();
        }
    }
