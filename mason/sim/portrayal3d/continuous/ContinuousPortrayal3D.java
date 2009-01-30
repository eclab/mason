/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.continuous;

import javax.vecmath.*;
import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.portrayal3d.inspector.*;
import sim.portrayal.inspector.*;

import com.sun.j3d.utils.picking.*;

/**
 * Portrays both Continuous2D and Continuous3D fields. 
 * 2D fields are spread through the XY plane and are presumed to have Z=0.
 *
 * @author Gabriel Balan
 */
public class ContinuousPortrayal3D extends SparseFieldPortrayal3D
    {
    /**
     * @see sim.portrayal.SparseFieldPortrayal3D#getLocationOfObjectAsVector3d(Object)
     */
    public Vector3d getLocationOfObjectAsVector3d(Object obj, Vector3d putInHere)
        {
        if (field instanceof Continuous2D)
            {
            Double2D locationD2d = ((Continuous2D)field).getObjectLocation(obj);
            putInHere.x = locationD2d.x;
            putInHere.y = locationD2d.y;
            putInHere.z = 0;
            }
        else
            {
            Double3D locationD3d = ((Continuous3D)field).getObjectLocation(obj);
            putInHere.x = locationD3d.x;
            putInHere.y = locationD3d.y;
            putInHere.z = locationD3d.z;
            }
        return putInHere;
        }

    public void setField(Object field)
        {
        dirtyField = true;
        if (field instanceof Continuous3D || field instanceof Continuous2D) this.field = field;
        else throw new RuntimeException("Invalid field for ContinuousPortrayal3D3D: " + field);
        }
            
    public LocationWrapper completedWrapper(LocationWrapper w, PickIntersection pi, PickResult pr)
        {
        final Object field = getField();
        StableLocation d = null;
        if (field instanceof Continuous2D) { d = new StableDouble2D((Continuous2D) field, w.getObject()); }
        else  { d = new StableDouble3D((Continuous3D) field,  w.getObject()); }
        final StableLocation loc = d;
        return new LocationWrapper( w.getObject(), null, this)  // don't care about location
            {
            public Object getLocation()
                {
                /*
                  if(field instanceof Continuous3D)
                  return ((Continuous3D)field).getObjectLocation(object);
                  else
                  return ((Continuous2D)field).getObjectLocation(object);
                */
                loc.update();
                return loc;
                }
                
            public String getLocationName()
                {
                /*
                  if(field instanceof Continuous3D)
                  {
                  Double3D loc = ((Continuous3D)field).getObjectLocation(object);
                  if (loc!=null) return loc.toCoordinates();
                  }
                  else
                  {
                  Double2D loc = ((Continuous2D)field).getObjectLocation(object);
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
