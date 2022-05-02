/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.portrayal.simple.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import sim.portrayal.*;
import sim.display.*;
import javax.swing.*;

/**
   A Sink accepts all incoming offers of resources matching a given type, then throws them away.
*/

public class Sink extends SimplePortrayal2D implements Receiver, StatReceiver
    {
    public static double getPortrayalScale() { return Provider.getPortrayalScale(); }
    public static void setPortrayalScale(double val) { Provider.setPortrayalScale(val); }
    protected SimplePortrayal2D portrayal = null;
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) { getPortrayal().draw(object, graphics, info); }
    public boolean hitObject(Object object, DrawInfo2D range) { return getPortrayal().hitObject(object, range); }
    public boolean setSelected(LocationWrapper wrapper, boolean selected) { return getPortrayal().setSelected(wrapper, selected); }
    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type) { return getPortrayal().handleMouseEvent(guistate, manipulating, wrapper, event, fieldPortrayalDrawInfo, type); }
    public Inspector getInspector(LocationWrapper wrapper, GUIState state) { return getPortrayal().getInspector(wrapper, state); }
    public String getName(LocationWrapper wrapper) { return getPortrayal().getName(wrapper); }
    
    SimplePortrayal2D getPortrayal()
    	{
    	if (portrayal == null) 
    		{
    		portrayal = new MovablePortrayal2D(new LabelledPortrayal2D(
    			image == null? buildPortrayal() : new ImagePortrayal2D(image, getPortrayalScale()), 
    				LabelledPortrayal2D.DEFAULT_OFFSET_X, LabelledPortrayal2D.DEFAULT_OFFSET_Y,
    				-getPortrayalScale() / 2.0, getPortrayalScale() / 2.0,
    				new Font("SansSerif",Font.PLAIN, 10), LabelledPortrayal2D.ALIGN_LEFT,
    				null, Color.black, false)
    			{
				public String getLabel(Object object, DrawInfo2D info)
					{
					return Sink.this.getLabel();
					}
				}); 
    		} 
    	return portrayal;
    	}

    protected SimplePortrayal2D buildPortrayal()
    	{
    	return new ShapePortrayal2D(ShapePortrayal2D.X_POINTS_OCTAGON, ShapePortrayal2D.Y_POINTS_OCTAGON, Color.black, getPortrayalScale(), false);
    	}

    protected String getLabel() { return (getName() == null ? "Sink" : getName()); }
    
	ImageIcon image = null;
	
	/** Be sure to set the portrayal scale FIRST */
	public void setImage(String imagePath)
		{
		image = new ImageIcon(getClass().getResource(imagePath));
		}

    private static final long serialVersionUID = 1;

    protected SimState state;
    Resource typical;
        
    double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }

	@Deprecated
    public Resource getTypical() { return getTypicalReceived(); }
        
    public Resource getTypicalReceived() { return typical; }
	public boolean hideTypicalReceived() { return true; }

    void throwUnequalTypeException(Resource resource)
        {
        throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
            " but got resource type " + resource.getName() + "(" + resource.getType() + ")" );
        }

    void throwInvalidAtLeastAtMost(double atLeast, double atMost)
        {
        throw new RuntimeException("Requested resource amounts are between " + atLeast + " and " + atMost + ", which is out of bounds.");
        }

    public Sink(SimState state, Resource typical)
        {
        this.state = state;
        this.typical = typical;
        }

    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost)
        {
    	if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(resource)) throwUnequalTypeException(resource);
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        if (resource instanceof CountableResource) 
            {
			totalReceivedResource += atMost;
            ((CountableResource) resource).reduce(atMost);
            return true;
            }
        else
            {
			totalReceivedResource += 1.0;
            return true;
            }
        }

    public String toString()
        {
        return "Sink@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }               

    public void step(SimState state)
        {
        // do nothing
        }

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
   	public boolean hideName() { return true; }

    public void reset(SimState state) { totalReceivedResource = 0; }
        
    boolean refusesOffers = false;
	public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
 	}
