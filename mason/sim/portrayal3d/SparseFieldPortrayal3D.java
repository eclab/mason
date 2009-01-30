/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d;

import sim.util.*;
import sim.field.*;
import sim.portrayal.*;

import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * An abstract superclass for all FieldPortrayal3Ds which display SparseFields.
 * This class handles the createModel() and updateModel() methods for you; all you
 * need to implement are the setField(), completedWrapper(), and getLocationOfObjectAsVector3d()
 * methods. 
 * 
 * <p>SparseFieldPortrayal3D presently takes the TransformGroups of the models of its children and
 * wraps them into BranchGroups so that they can be removed and added dynamically.
 *  
 * @author Gabriel Balan
 */
 
public abstract class SparseFieldPortrayal3D extends FieldPortrayal3D
    {
    /** Converts a given location (perhaps a Double3D, Double2D, Int3D, or Int2D) into a Vector3d,
        placing it in the given Vector3d, and returning that Vector3d. Double2D and Int2D should
        convert to a Vector3d with a zero Z value. */
    public abstract Vector3d getLocationOfObjectAsVector3d(Object location, Vector3d putInHere);
    
    public TransformGroup createModel()
        {
        SparseField field = (SparseField)(this.field);
        Vector3d locationV3d = new Vector3d();
        TransformGroup globalTG = new TransformGroup(); 
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

        if (field==null) return globalTG;
        Bag objects = field.getAllObjects();
        Transform3D tmpLocalT = new Transform3D();
        
        for(int z = 0; z<objects.numObjs; z++)
            {
            getLocationOfObjectAsVector3d(objects.objs[z], locationV3d);
            tmpLocalT.setTranslation(locationV3d);
            globalTG.addChild(wrapModelForNewObject(objects.objs[z], tmpLocalT));                     
            }
        
        return globalTG;
        }
        
    /**
     * This function is called from createModel for each object in the 
     * field and from the updateModel part of getModel for the
     * new objects.
     * 
     * <p>In order to dynamically add/remove the subtrees associated with
     * children, this function wraps their TransformGroups into BranchGroups.
     **/
    protected BranchGroup wrapModelForNewObject(Object o, Transform3D localT)
        {
        Portrayal p = getPortrayalForObject(o);
        if(! (p instanceof SimplePortrayal3D))
            throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                o + " -- expecting a SimplePortrayal3D");
        SimplePortrayal3D p3d = (SimplePortrayal3D)p;

        p3d.setParentPortrayal(this);
        TransformGroup localTG = p3d.getModel(o, null);
        localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        localTG.setTransform(localT);
        
        BranchGroup localBG = new BranchGroup();
        localBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        localBG.setCapability(BranchGroup.ALLOW_DETACH);
        localBG.addChild(localTG);
        localBG.setUserData(o);
        return localBG;
        }
    
        
    public void updateModel(TransformGroup globalTG)
        {
        SparseField field = (SparseField)(this.field);
        if (field==null) return;
        Bag b = field.getAllObjects();
        HashMap hm = new HashMap();
        Transform3D tmpLocalT = new Transform3D();
        Vector3d locationV3d = new Vector3d();

        for(int i=0;i<b.numObjs;i++)
            hm.put(b.objs[i],b.objs[i]);

        // update children if they're still in the field,
        // else remove the children if they appear to have left.
        // We use a hashmap to efficiently mark out the children
        // as we delete them and update them
        
        for(int t= globalTG.numChildren()-1; t>=0; t--)
            {
            BranchGroup localBG = (BranchGroup)globalTG.getChild(t);
            Object fieldObj = localBG.getUserData();
            if(hm.remove(fieldObj) != null) // hm.containsKey(fieldObj))  // object still in the field
                {  // we can pull this off because sparse fields are not allowed to contain null -- Sean
                TransformGroup localTG = (TransformGroup)localBG.getChild(0);
                Portrayal p = getPortrayalForObject(fieldObj);
                if(! (p instanceof SimplePortrayal3D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                        fieldObj + " -- expecting a SimplePortrayal3D");
                SimplePortrayal3D p3d = (SimplePortrayal3D)p;

                TransformGroup localTG2 = p3d.getModel(fieldObj, localTG);
                getLocationOfObjectAsVector3d(fieldObj, locationV3d);
                tmpLocalT.setTranslation(locationV3d);
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
            Iterator newObjs = hm.values().iterator();  // yuck, inefficient
            while(newObjs.hasNext())
                {
                Object fieldObj = newObjs.next();
                locationV3d = getLocationOfObjectAsVector3d(fieldObj, locationV3d);
                tmpLocalT.setTranslation(locationV3d);
                
                BranchGroup localBG = wrapModelForNewObject(fieldObj, tmpLocalT);                     
                globalTG.addChild(localBG);
                }
            }
        }
    }
            
