/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.display.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.display3d.*;
import sim.util.*;

public class SwitchedPortrayal3D extends SimplePortrayal3D
    {
    SimplePortrayal3D child;
    
    public SwitchedPortrayal3D(SimplePortrayal3D child)
        {
        this.child = child;
        }

    public PolygonAttributes polygonAttributes()
        { 
        return child.polygonAttributes(); 
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return child.getInspector(wrapper,state);
        }
        
    public String getName(LocationWrapper wrapper)
        {
        return child.getName(wrapper);
        }
    
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

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return child.setSelected(wrapper,selected);
        }
        
    public SimplePortrayal3D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal3D))
                throw new RuntimeException("Object provided to SwitchedPortrayal3D is not a SimplePortrayal3D: " + object);
            return (SimplePortrayal3D) object;
            }
        }
    
    public boolean getShowsChild(Object obj)
        {
        if (obj == null) return true;  // no information to say otherwise
        else if (obj instanceof Number)
            {
            return ((Number)obj).doubleValue() != 0.0;
            }
        else if (obj instanceof Valuable)
            {
            return ((Valuable)obj).doubleValue() != 0.0;
            }
        else return true;
        }
        
    
    public TransformGroup getModel(Object obj, TransformGroup previousTransformGroup)
        {
        Switch internalSwitch;
        if (previousTransformGroup == null)
            {
            TransformGroup internalTransformGroup = getChild(obj).getModel(obj,null);
            internalTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            internalTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            
            internalSwitch = new Switch();
            internalSwitch.addChild(internalTransformGroup);
            internalSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
            internalSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
            internalSwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
            internalSwitch.clearCapabilityIsFrequent(Switch.ALLOW_CHILDREN_READ);

            previousTransformGroup = new TransformGroup();
            previousTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.clearCapabilityIsFrequent(TransformGroup.ALLOW_CHILDREN_READ);
            previousTransformGroup.addChild(internalSwitch);
            }
        else 
            {
            internalSwitch = (Switch)(previousTransformGroup.getChild(0));
            TransformGroup internalTransformGroup = (TransformGroup)(internalSwitch.getChild(0));
            getChild(obj).getModel(obj,internalTransformGroup);
            }

        internalSwitch.setWhichChild(getShowsChild(obj) ? Switch.CHILD_ALL : Switch.CHILD_NONE);
        return previousTransformGroup;
        }
    }
