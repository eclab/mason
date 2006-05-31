/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import sim.portrayal.*;
import javax.media.j3d.*;

/**
 * @author Gabriel Balan
 *
 * Wraps a non-pickable SharedGroup into a SimplePortrayal3D.
 * You are responsible for compiling the Shared Group if you so
 * desire.
 */
 
public class SharedGroupPortrayal3D extends SimplePortrayal3D 
    {
    SharedGroup group;
    
    public SharedGroupPortrayal3D(SharedGroup group)
        {
        this.group = group;
        group.setCapability(Group.ALLOW_CHILDREN_READ);
        group.clearCapabilityIsFrequent(Group.ALLOW_CHILDREN_READ);
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            j3dModel.setCapability(Group.ALLOW_CHILDREN_READ);
            
            // make a Link
            Link link = new Link(group);
            link.setCapability(Link.ALLOW_SHARED_GROUP_READ);
            link.clearCapabilityIsFrequent(Link.ALLOW_SHARED_GROUP_READ);

            // make it unpickable
            clearPickableFlags(link);
            
            // the next four lines are boilerplate, but in fact I think that
            // since this isn't pickable, it doesn't matter
            
            // build a LocationWrapper for the object
            LocationWrapper pickI = new LocationWrapper(obj, null, parentPortrayal);
            // Store the LocationWrapper in the user data
            link.setUserData(pickI);

            j3dModel.addChild(link);
            }
        return j3dModel;
        }
    }
