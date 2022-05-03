/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.network;

import sim.field.network.*;
import sim.util.*;
import sim.engine.*;
import java.util.*;
import sim.des.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.geom.*;
import java.awt.event.*;
import sim.display.*;

public abstract class DESPortrayal extends SimplePortrayal2D implements Displayable
	{
	transient SimplePortrayal2D portrayal;
    double portrayalScale = Double.NaN;
    String baseImagePath;

    public SimplePortrayal2D getDefaultPortrayal()
    	{
    	double scale = DESPortrayalFactory.getPortrayalScale();
    	
    	if (portrayal == null || scale != portrayalScale)
    		{
    		// rebuild it!
    		portrayalScale = scale;
    		if (baseImagePath != null)
    			{
	    		portrayal = DESPortrayalFactory.getPortrayal(this, portrayalScale, baseImagePath);
    			}
    		else
    			{
	    		portrayal = DESPortrayalFactory.getPortrayal(buildDefaultPortrayal(portrayalScale));
	    		}
    		}
    	return portrayal;
    	}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) 
		{ getDefaultPortrayal().draw(object, graphics, info); }
	public boolean hitObject(Object object, DrawInfo2D range) 
		{ return getDefaultPortrayal().hitObject(object, range); }
	public boolean setSelected(LocationWrapper wrapper, boolean selected) 
		{ return getDefaultPortrayal().setSelected(wrapper, selected); }
	public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type) 
		{ return getDefaultPortrayal().handleMouseEvent(guistate, manipulating, wrapper, event, fieldPortrayalDrawInfo, type); }
	public Inspector getInspector(LocationWrapper wrapper, GUIState state) 
		{ return getDefaultPortrayal().getInspector(wrapper, state); }
	public String getName(LocationWrapper wrapper) 
		{ return getDefaultPortrayal().getName(wrapper); }

    public void setImage(String path) { baseImagePath = path; }
    public String getImage() { return baseImagePath; }

    public SimplePortrayal2D buildDefaultPortrayal(double scale)
    	{
    	return new RectanglePortrayal2D(Color.black, scale, false);
    	}
	}	
	