/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;

import sim.field.grid.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;

/** A wrapper used by NetworkPortrayal2D to hold a Network and EITHER a Continuous2D OR a SparseGrid2D.
    The Continuous2D/SparseGrid2D specifies the spatial location of the nodes; the Network specifies the
    edges connecting those nodes. */

public class SpatialNetwork2D
    {
    public Continuous2D field;
    public SparseGrid2D grid;
    public Network network;

    public SpatialNetwork2D( final Continuous2D field, final Network network )
        {
        this.field = field;
        this.network = network;
        }
    
    public SpatialNetwork2D( final SparseGrid2D grid, final Network network )
        {
        this.grid = grid;
        this.network = network;
        }

    public Double2D getObjectLocation(Object node)
        {
        if (field!=null) return field.getObjectLocation(node);
        else return new Double2D(grid.getObjectLocation(node));
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
    }
