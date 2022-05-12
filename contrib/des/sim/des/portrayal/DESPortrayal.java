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
    Class baseImageClass;
    
	public boolean hideLabel() { return true; }
	/** Returns the label to visually describe the object.  By default returns the object's name: if there is no name, returns its classname. */
    public String getLabel() { if (getName() != null) return getName(); return getClass().getSimpleName(); }

	public boolean hideImagePath() { return true; }
	/** Sets the path to the image file, relative to the class file in setImageClass(). */
    public void setImagePath(String path) { baseImagePath = path; }
	/** Returns the path to the image file, relative to the class file in setImageClass(). */
    public String getImagePath() { return baseImagePath; }

	public boolean hideImageClass() { return true; }
	/** Sets the class file from which setImagePath(...) defines a path to the image, if any. */
	public void setImageClass(Class cls) { baseImageClass = cls; }
	/** Returns the class file from which setImagePath(...) defines a path to the image, if any. */
    public Class getImageClass() { return baseImageClass; }

	public boolean hideImage() { return true; }
	/** Sets the class file and the path relative to that file which leads to the image, if any. */
    public void setImage(Class cls, String path) { setImagePath(path); setImageClass(cls); }


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
	To do this, it first builds a "base portrayal" -- this is either an ImagePortrayal2D
	(if you have set the baseImagePath and baseImageClass), or a ShapePortrayal2D
	of some sort (by calling buildDefaultPortrayal(....)). It then passes this "base portrayal"
	to the DESPortrayalFactory, which wraps it with various of gizmos, producing a final
	portrayal for the object.  This final portrayal is then returned.
	*/
    public SimplePortrayal2D buildPortrayal(Object object)
    	{
		if (baseImagePath != null)
			{
			if (baseImageClass != null)
				{
				return DESPortrayalFactory.wrapPortrayal(new ImagePortrayal2D(new ImageIcon(baseImageClass.getResource(baseImagePath)), portrayalScale));
				}
			else
				{
				return DESPortrayalFactory.wrapPortrayal(new ImagePortrayal2D(new ImageIcon(object.getClass().getResource(baseImagePath)), portrayalScale));
				}
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
	