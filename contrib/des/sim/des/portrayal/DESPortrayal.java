/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

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

public abstract class DESPortrayal extends InternalPortrayal2D implements Displayable, Named
	{
    double portrayalScale = Double.NaN;
    String baseImagePath;
    boolean usesGlobalImageClass = false;
    
	public boolean hideLabel() { return true; }
	/** Returns the label to visually describe the object.  By default returns the object's name: if there is no name, returns its classname. */
    public String getLabel() { if (getName() != null) return getName(); return getClass().getSimpleName(); }

	public boolean hideImagePath() { return true; }
	/** Sets the path to the image file, relative to the class file in setImageClass(). */
    public void setImage(String path, boolean usesGlobalImageClass) { baseImagePath = path; this.usesGlobalImageClass = usesGlobalImageClass; }
	/** Returns the path to the image file, relative to the class file in setImageClass(). */
    public String getImagePath() { return baseImagePath; }
	/** Returns the class file from which setImagePath(...) defines a path to the image, if any. */
    public boolean getUsesGlobalImageClass() { return usesGlobalImageClass; }

	// wraps the base portrayal provided
    static SimplePortrayal2D wrapPortrayal(SimplePortrayal2D basePortrayal)
    	{
    	double portrayalScale = DESPortrayalParameters.getPortrayalScale();
    	BarPortrayal bar = new BarPortrayal(basePortrayal,
    							LabelledPortrayal2D.DEFAULT_OFFSET_X, LabelledPortrayal2D.DEFAULT_OFFSET_Y,
    							-portrayalScale / 2.0, portrayalScale / 2.0,
    							new Font("SansSerif",Font.PLAIN, 10), LabelledPortrayal2D.ALIGN_LEFT,
    							null, Color.black, false)
									{
									public String getLabel(Object object, DrawInfo2D info)
										{
										return ((Displayable)object).getLabel();
										}
									};
		bar.setLabelScaling(bar.SCALE_WHEN_SMALLER);
    	return new MovablePortrayal2D(
    				new CircledPortrayal2D(bar, 0, portrayalScale * DESPortrayalParameters.CIRCLE_RING_SCALE, Color.gray, false)
								{
								public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
									{
									setCircleShowing(((Displayable)object).getDrawState());
									super.draw(object, graphics, info);
									}
								}); 
    	}
    	
	/** 
	Called by InternalPortrayal2D to provide the portrayal to draw this object.
 	We override the standard method to update the scale and rebuild the portrayal
	if the scale is wrong.
	*/
    public SimplePortrayal2D providePortrayal(Object object)
    	{
    	// I am always the object
    	double scale = DESPortrayalFactory.getPortrayalScale();
    	
    	if (scale != portrayalScale)
    		{
    		// rebuild it!
    		portrayalScale = scale;
    		portrayal = null;
    		}
    	
    	return super.providePortrayal(object);
    	}
    	
	/** 
	Called by InternalPortrayal2D to build a new portrayal when called for.
	To do this, it first either builds an ImagePortrayal2D (if you have set the baseImagePath),
	or a ShapePortrayal2D of some sort (returned by buildDefaultPortrayal(....)). 
	It then attaches to this base portrayal a variety of labels, indicators, and the ability
	to move.  The final modified portrayal is then returned.
	*/
    public SimplePortrayal2D buildPortrayal(Object object)
    	{
		if (baseImagePath != null)
			{
			return DESPortrayalFactory.wrapPortrayal(new ImagePortrayal2D(new ImageIcon(
				(usesGlobalImageClass ? DESPortrayalParameters.getImageClass() : 
					this.getClass()).getResource(baseImagePath)), portrayalScale));
			}
		else
			{
			return DESPortrayalFactory.wrapPortrayal(buildDefaultPortrayal(portrayalScale));
			}
    	}
	
	/** 
	Builds the "base portrayal" for the object, if the image path and class haven't been set (and thus
	the portrayal isn't an ImagePortrayal2D).  The default sets to a simple gray and black quare.
	*/
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
    	{
    	return new ShapePortrayal2D(
    		ShapePortrayal2D.POLY_SQUARE,
    		Color.GRAY, Color.BLACK, 2.0, scale);
    	}
	}	
	