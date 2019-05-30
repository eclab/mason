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
    Object field;
    Object field2;
    Network network;

    public SpatialNetwork3D(SparseField3D field, Network network)
        {
        this.field = field;
        if (field == null)
            throw new RuntimeException("Null SparseField3D.");
        this.network = network;
        if (network == null)
            throw new RuntimeException("Null Network.");
        }
    
    public SpatialNetwork3D(SparseField2D grid, Network network)
        {
        this.field = grid;
        if (field == null)
            throw new RuntimeException("Null SparseField2D.");
        this.network = network;
        if (network == null)
            throw new RuntimeException("Null Network.");
        }
    
    public void setAuxiliaryField(SparseField3D f)
        {
        field2 = f;
        }

    public void setAuxiliaryField(SparseField2D f)
        {
        field2 = f;
        }

    /** @deprecated Use setAuxiliaryField */
    public void setAuxillaryField(Continuous3D f)
        {
        setAuxiliaryField(f);
        }

    /** @deprecated Use setAuxiliaryField */
    public void setAuxillaryField(SparseGrid3D f)
        {
        setAuxiliaryField(f);
        }

    public Double3D getObjectLocation(Object node)
        {
        Double3D loc;
        if (field instanceof SparseField3D)
            loc = ((SparseField3D)field).getObjectLocationAsDouble3D(node);
        else
            loc = new Double3D(((SparseField2D)field).getObjectLocationAsDouble2D(node));

        if (loc == null && field2 != null)
            {
            if (field2 instanceof SparseField3D)
                loc = ((SparseField3D)field2).getObjectLocationAsDouble3D(node);
            else
                loc = new Double3D(((SparseField2D)field2).getObjectLocationAsDouble2D(node));
            }
        return loc;
        }

    /** @deprecated */
    public Double3D getDimensions()
        {
        if (field instanceof SparseField3D)
            return ((SparseField3D)field).getDimensions();
        else return new Double3D(((SparseField2D)field).getDimensions());
        }
    }
