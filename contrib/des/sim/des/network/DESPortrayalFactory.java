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

public class DESPortrayalFactory
	{
    private static final long serialVersionUID = 1;
    
    public static final double DEFAULT_PORTRAYAL_SCALE = 10.0;
    public static final double CIRCLE_RING_SCALE = 1.5;
    static double portrayalScale = DEFAULT_PORTRAYAL_SCALE;
    public static double getPortrayalScale() { return portrayalScale; }
    public static void setPortrayalScale(double scale) { portrayalScale = scale; }

	public static SimplePortrayal2D getPortrayal(Object obj, double scale, String imagePath)
		{
    	return getPortrayal(new ImagePortrayal2D(new ImageIcon(obj.getClass().getResource(imagePath)), scale));
		}
    
    public static SimplePortrayal2D getPortrayal(SimplePortrayal2D basePortrayal)
    	{
    	return new MovablePortrayal2D(
    				new CircledPortrayal2D(
    						new LabelledPortrayal2D(basePortrayal,
    							LabelledPortrayal2D.DEFAULT_OFFSET_X, LabelledPortrayal2D.DEFAULT_OFFSET_Y,
    							-portrayalScale / 2.0, portrayalScale / 2.0,
    							new Font("SansSerif",Font.PLAIN, 10), LabelledPortrayal2D.ALIGN_LEFT,
    							null, Color.black, false)
									{
									public String getLabel(Object object, DrawInfo2D info)
										{
										return ((Displayable)object).getLabel();
										}
									}, 
							0, portrayalScale * CIRCLE_RING_SCALE, Color.gray, false)
								{
								public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
									{
									setCircleShowing(((Displayable)object).getDrawState());
									super.draw(object, graphics, info);
									}
								}); 
    	}
	}	
	