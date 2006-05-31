/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.network;

import sim.field.grid.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;

/** A wrapper used by NetworkPortrayal3D to hold a Network and EITHER a Continuous3D OR a SparseGrid3D.
    The Continuous3D/SparseGrid3D specifies the spatial location of the nodes; the Network specifies the
    edges connecting those nodes. */

public class SpatialNetwork3D
    {
    public Continuous3D field;
    public SparseGrid3D grid;
    public Network network;

    public SpatialNetwork3D( final Continuous3D field, final Network network )
        {
        this.field = field;
        this.network = network;
        }
    
    public SpatialNetwork3D( final SparseGrid3D grid, final Network network )
        {
        this.grid = grid;
        this.network = network;
        }

    public Double3D getObjectLocation(Object node)
        {
        if (field!=null) return field.getObjectLocation(node);
        else return new Double3D(grid.getObjectLocation(node));
        }

    public double getWidth()
        {
        if (field!=null) return field.getWidth();
        else return grid.getWidth();
        }
        
    public double getHeight()
        {
        if (field!=null) return field.getHeight();
        else return grid.getHeight();
        }

    public double getLength() 
        {
        if (field!=null) return field.getLength(); 
        else return grid.getLength();
        }
    }
