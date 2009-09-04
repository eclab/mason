/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid;

import sim.util.*;
import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.field.grid.*;
import java.util.*;
import javax.vecmath.*;
import javax.media.j3d.*;

/**
 * Displays objects in a SparseGrid2D along the XY grid in a special way.  When multiple objects are 
 * at the same location, SparseGrid2DPortrayal3D will stack them up in a column towards the positive Z axis.
 * The centers of stacked objects are separated by a value <b>zScale</b>, which by default is 1.0.
 * 
 * @author Gabriel Balan
 * 
 */
public class SparseGrid2DPortrayal3D extends SparseGridPortrayal3D
    {
    public double zScale;
    
    /** Creates a SparseGrid2DPortrayal3D with the provided scale */
    public SparseGrid2DPortrayal3D(double zScale)
        {
        super();
        this.zScale = zScale;
        }
    
    /** Creates a SparseGrid2DPortrayal3D with scale = 1.0 */
    public SparseGrid2DPortrayal3D()
        {
        this(1.0);
        }
    
    public void setField(Object field)
        {
        if (field instanceof SparseGrid2D) this.field = (SparseGrid2D) field;
        else throw new RuntimeException("Invalid field for StackedSparse2DPortrayal3D: " + field);
        }

    public TransformGroup createModel()
        {
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

        if (field == null) return globalTG;

        Vector3d tmpV3D = new Vector3d();
        Transform3D tmpLocalT = new Transform3D();
        HashMap map = new HashMap();
        SparseGrid2D grid = (SparseGrid2D)field;
        Bag allobjs = grid.getAllObjects();
        
        for(int i = 0; i < allobjs.numObjs; i++)
            {
            if (!map.containsKey(allobjs.objs[i]))  // not done yet
                {
                Int2D location = grid.getObjectLocation(allobjs.objs[i]);
                Bag b = grid.getObjectsAtLocation(location);
                tmpV3D.x = location.x;
                tmpV3D.y = location.y;
                if(b != null)
                    for(int z = 0; z<b.numObjs; z++)
                        {
                        map.put(b.objs[z],b.objs[z]);  // mark as already completed
                        tmpV3D.z = z * zScale;
                        tmpLocalT.setTranslation(tmpV3D);

                        //wrap the TG in a BG so it can be removed dynamically
                        BranchGroup localBG = wrapModelForNewObject(b.objs[z], tmpLocalT);                    
                        globalTG.addChild(localBG);
                        }
                }
            }
                
        return globalTG;
        }
    
    public void updateModel(TransformGroup globalTG)
        {
        SparseGrid2D grid = (SparseGrid2D)field;
        if (grid==null) return;
        Vector3d tmpV3D = new Vector3d();
        Bag b = grid.getAllObjects();
        Transform3D tmpLocalT = new Transform3D();
        HashMap hm = new HashMap();
        
        HashMap stackCountByLocation = new HashMap();

        for(int i=0;i<b.numObjs;i++)
            hm.put(b.objs[i],b.objs[i]);

        for(int t= globalTG.numChildren()-1; t>=0; t--)
            {
            BranchGroup localBG = (BranchGroup)globalTG.getChild(t);
            Object fieldObj = localBG.getUserData();
            if( hm.remove(fieldObj) != null)
                {
                TransformGroup localTG = (TransformGroup)localBG.getChild(0);
                Portrayal p = getPortrayalForObject(fieldObj);
                if(! (p instanceof SimplePortrayal3D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                        fieldObj + " -- expecting a SimplePortrayal3D");
                SimplePortrayal3D p3d = (SimplePortrayal3D)p;
                TransformGroup localTG2 = p3d.getModel(fieldObj, localTG);
        
                // Vector3d locationV3d = getLocationOfObjectAsVector3d(fieldObj);
                Int2D location = grid.getObjectLocation(fieldObj);
                tmpV3D.x = location.x;
                tmpV3D.y = location.y;
                MutableDouble d = (MutableDouble)(stackCountByLocation.get(location));
                if (d==null)
                    {
                    d = new MutableDouble(0);
                    stackCountByLocation.put(location,d);
                    }
                else d.val++;
                // set Z
                tmpV3D.z = d.val * zScale;

                tmpLocalT.setTranslation(tmpV3D);
                localTG2.setTransform(tmpLocalT);
                    
                if(localTG != localTG2)
                    {
                    localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                    localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                                                
                    BranchGroup newlocalBG = new BranchGroup();
                    newlocalBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    newlocalBG.setCapability(BranchGroup.ALLOW_DETACH);
                    newlocalBG.setUserData(fieldObj);
                    newlocalBG.addChild(localTG2);
                                                
                    globalTG.setChild(newlocalBG, t);
                    }
                }
            else  // object is no longer in the field -- remove it from the scenegraph
                globalTG.removeChild(t);
            }
        
        // The remaining objects in hm must be new.  We add them to the scenegraph.
        // But first, we should check to see if hm is empty.
        
        if (!hm.isEmpty())
            {
            Iterator newObjs = hm.values().iterator();  
            while(newObjs.hasNext())
                {
                Object fieldObj = newObjs.next();
                //Vector3d locationV3d = getLocationOfObjectAsVector3d(fieldObj);
                Int2D location = grid.getObjectLocation(fieldObj);
                tmpV3D.x = location.x;
                tmpV3D.y = location.y;
                MutableDouble d = (MutableDouble)(stackCountByLocation.get(location));
                if (d==null)
                    {
                    d = new MutableDouble(0);
                    stackCountByLocation.put(location,d);
                    }
                else d.val++;
                // set Z
                tmpV3D.z = d.val * zScale;
                tmpLocalT.setTranslation(tmpV3D);
                //wrap the TG in a BG so it can be added 
                //and later on removed dynamically
                BranchGroup localBG = wrapModelForNewObject(fieldObj, tmpLocalT);                     
                globalTG.addChild(localBG);
                }
            hm.clear();
            }
        }    
    }
    
