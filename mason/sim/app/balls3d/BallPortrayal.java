/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.balls3d;
 
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.portrayal3d.simple.*;
 
public class BallPortrayal extends SpherePortrayal3D
    {
	final static java.awt.Color obColor = java.awt.Color.green;
	final static java.awt.Color colColor = java.awt.Color.red;
    float multiply;
        
    public BallPortrayal( double diam )
        {
        multiply = (float) diam;
        }
 
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {               
		if (j3dModel==null || ((Ball)obj).oldCollision != ((Ball)obj).collision)  // either the first time or when it changes
			{
			((Ball)obj).oldCollision = ((Ball)obj).collision;  // reset it

			if (((Ball)obj).collision)
				setAppearance(j3dModel, appearanceForColors(
					colColor, colColor, null, colColor, null, 1.0f, 1.0f));
			else setAppearance(j3dModel, appearanceForColors(
					obColor, obColor, null, obColor, null, 1.0f, 1.0f));
			}

		if (j3dModel==null || ((Ball)obj).oldMass != ((Ball)obj).mass)  // likewise
			{
			((Ball)obj).oldMass = ((Ball)obj).mass;  // reset it
			
			setScale(j3dModel, multiply * (float)(((Ball)obj).diameter) / 2.0f);
			}
			
		return super.getModel(obj, j3dModel);
        }
    }
