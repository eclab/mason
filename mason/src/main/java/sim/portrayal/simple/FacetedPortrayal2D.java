/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import sim.display.*;
import sim.util.*;

/**
   A wrapper for multiple Portrayal2Ds which calls on one or the other one according to the
   underlying state of the object, which must be Valuable or a Number of some sort.  The
   particular sub-portrayal (called a "facet") chosen is based on the integer value returned
   (or converted to an integer if it's a double).  Optionally FacetedPortrayal2D will call on
   *all* its underlying portrayals at one time.  If any array element is null, FacetedPortrayal2D
   will assume it represents the object itself (assuming the object itself is also a SimplePortrayal2D).
*/

public class FacetedPortrayal2D extends SimplePortrayal2D
    {
    public SimplePortrayal2D[] children;
    boolean portrayAllChildren = false;
    boolean errorThrown;
    
    /** If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public FacetedPortrayal2D(SimplePortrayal2D[] children, boolean portrayAllChildren)
        {
        this.children = children;
        this.portrayAllChildren = portrayAllChildren;
        }
    
    /** If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public FacetedPortrayal2D(SimplePortrayal2D[] children)
        {
        this(children, false);
        }
    
    /** Returns the child index to use for the given object.
        The value must be >= 0 and < numIndices.   The default implementation
        returns the value of the object if it's a Number or is sim.util.Valuable
        and if the value is within the given range (and is an integer).
        Otherwise 0 is returned.
    */
    public int getChildIndex(Object object, int numIndices)
        {
        int element = 0;
        if( object instanceof Number )
            element = (int)(((Number)object).doubleValue());
        else if (object instanceof Valuable)
            element = (int)(((Valuable)object).doubleValue());
        if (element < 0 || element >= children.length)
            {
            if (!errorThrown)
                {
                errorThrown = true;
                System.err.println("WARNING: FacetedPortrayal was given a value that doesn't correspond to any array element.");
                }
            element = 0;
            }
        return element;
        }
        
    SimplePortrayal2D getChild(Object object)
        {
        int element = getChildIndex(object, children.length);
        if (children[element] == null)
            if (object instanceof SimplePortrayal2D)
                return (SimplePortrayal2D)object;
            else throw new RuntimeException("FacetedPortrayal had a null child but the object is not itself a SimplePortrayal2D");
        else return children[element];
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (portrayAllChildren)
            for(int i = 0; i < children.length;i++)
                children[i].draw(object, graphics, info);
        else
            getChild(object).draw(object, graphics, info);
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        if (portrayAllChildren)
            {
            for(int i = 0; i < children.length;i++)
                if (children[i].hitObject(object, range))
                    return true;
            return false;
            }
        else
            return getChild(object).hitObject(object,range);
        }

    /** If portrayAllChildren, Returns true if any ONE of the children returns true. */
    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        if (portrayAllChildren)
            {
            for(int i = 0; i < children.length;i++)
                if (children[i].setSelected(wrapper, selected))
                    return true;
            return false;
            }
        else
            return getChild(wrapper.getObject()).setSelected(wrapper, selected);
        }

    /** If portrayAllChildren, Calls on the first child to return the inspector. */
    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (portrayAllChildren)
            return children[0].getInspector(wrapper,state);
        else
            return getChild(wrapper.getObject()).getInspector(wrapper,state);
        }
    
    /** If portrayAllChildren, Calls on the first child to return the name. */
    public String getName(LocationWrapper wrapper)
        {
        if (portrayAllChildren)
            return children[0].getName(wrapper);
        else
            return getChild(wrapper.getObject()).getName(wrapper);
        }

    }
    
    
    
