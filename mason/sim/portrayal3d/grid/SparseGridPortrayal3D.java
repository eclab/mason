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
    /*
      public Transform3D getDefaultTransform()
      {
      // adjust so that the [0,0,0] object is centered on the origin
      return new Transform3D(new Quat4f(), new Vector3d(-0.5,-0.5,-0.5), 1);
      }
    */
    
    /**
     * @see sim.portrayal.SparseFieldPortrayal3D#getLocationOfObjectAsVector3d(Object)
     */
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
        dirtyField = true;
        if (field instanceof SparseGrid3D || field instanceof SparseGrid2D) this.field = field;
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
                /*
                  if(field instanceof SparseGrid3D)
                  return ((SparseGrid3D)field).getObjectLocation(object);
                  else
                  return ((SparseGrid2D)field).getObjectLocation(object);
                */
                loc.update();
                return loc;
                }
                
            public String getLocationName()
                {
                /*
                  if(field instanceof SparseGrid3D)
                  {
                  Int3D loc = ((SparseGrid3D)field).getObjectLocation(object);
                  if (loc!=null) return loc.toCoordinates();
                  }
                  else
                  {
                  Int2D loc = ((SparseGrid2D)field).getObjectLocation(object);
                  if (loc!=null) return loc.toCoordinates();
                  }
                  return null;
                */
                loc.update();
                return loc.toString();
                }
            };
        }

    }
