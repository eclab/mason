/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import sim.portrayal.*;
import javax.media.j3d.*;
import sim.display3d.*;
 
public class SharedPortrayal3D extends SimplePortrayal3D 
    {
	SimplePortrayal3D child;
	SharedGroup group = null;
    
	/** Sets the current display both here and in the child. */
	public void setCurrentDisplay(Display3D display)
		{
		super.setCurrentDisplay(display);
		child.setCurrentDisplay(display);
		}
		
	/** Sets the current field portrayal both here and in the child. */
    public void setCurrentFieldPortrayal(FieldPortrayal3D p)
        {
		super.setCurrentFieldPortrayal(p);
        child.setCurrentFieldPortrayal(p);
        }

    public SharedPortrayal3D(SimplePortrayal3D child)
        {
		this.child = child;
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
			// load the child
			if (group == null)
				{
				group = new SharedGroup();
				group.addChild(child.getModel(obj, null));
				}
			
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
            LocationWrapper pickI = new LocationWrapper(obj, null, getCurrentFieldPortrayal());
            // Store the LocationWrapper in the user data
            link.setUserData(pickI);

            j3dModel.addChild(link);
            }
        return j3dModel;
        }
    }
