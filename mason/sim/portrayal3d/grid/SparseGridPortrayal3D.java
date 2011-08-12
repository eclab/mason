/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid;

import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;
import sim.portrayal.*;
import sim.field.grid.*;
import sim.portrayal3d.*;
import sim.util.*;
import sim.portrayal3d.inspector.*;
import sim.portrayal.inspector.*;

/**
 * Portrays both SparseGrid2D and SparseGrid3D fields.  A (0,0) or (0,0,0) object is centered
 * on the origin.  2D fields are spread through the XY plane and are presumed to have Z=0.
 * Generally speaking, SparseGrid2D is better drawn using a different class: the SparseGrid2DPortrayal3D.
 * 
 * @author Gabriel Balan
 */
public class SparseGridPortrayal3D extends SparseFieldPortrayal3D
    {    
    public Vector3d getLocationOfObjectAsVector3d(Object obj, Vector3d putInHere)
        {
        if(field instanceof SparseGrid3D)
            {
            Int3D locationI3d = ((SparseGrid3D)field).getObjectLocation(obj);
            putInHere.x = locationI3d.x;
            putInHere.y = locationI3d.y;
            putInHere.z = locationI3d.z;
            }
        else
            {
            Int2D locationI2d = ((SparseGrid2D)field).getObjectLocation(obj);
            putInHere.x = locationI2d.x;
            putInHere.y = locationI2d.y;
            putInHere.z = 0;
            }
        return putInHere;
        }

    public void setField(Object field)
        {
        if (field instanceof SparseGrid3D || field instanceof SparseGrid2D) super.setField(field);
        else throw new RuntimeException("Invalid field for SparseGridPortrayal3D: " + field);
        }
        
    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)     
        {
        final Object field = getField();
        StableLocation d = null;
        if (field instanceof SparseGrid2D) { d = new StableInt2D((SparseGrid2D) field, w.getObject()); }
        else  { d = new StableInt3D((SparseGrid3D) field,  w.getObject()); }
        final StableLocation loc = d;
        return new LocationWrapper( w.getObject(), null , this)  // don't care about location
            {
            public Object getLocation()
                {
                return loc;
                }
                
            public String getLocationName()
                {
                return loc.toString();
                }
            };
        }

    }
