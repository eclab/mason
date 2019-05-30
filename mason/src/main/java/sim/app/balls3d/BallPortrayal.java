/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.balls3d;
 
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.portrayal3d.simple.*;
import java.awt.Color;
 
public class BallPortrayal extends SpherePortrayal3D
    {
    final static Color obColor = Color.green;
    final static Color colColor = Color.red;
    float multiply;
        
    public BallPortrayal( double diam )
        {
        multiply = (float) diam;
        }
 
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        // Below we'd like to change the appearance of our sphere whenever
        // it collides or stops colliding, or set it the very first time we present
        // the sphere to the user.  Likewise we'd like to change the size of
        // the sphere whenever its mass changes, or set it the very first time
        // as well.
                
        // If the j3DModel is null, then we're being asked to construct a brand new model
        // likely for the very first time.  At this point there is *no* model
        // on which we can change appearance or set scales.  So how do we set them
        // the very first time?  The answer is: don't worry about it.  The setAppearance
        // and setScale methods are constructed such that if you call them
        // _before_ the model is built, your desired changes are stored away temporarily
        // until super.getModel(...) is called, at which point they're applied when
        // the model is built.
                
        // alternatively, the arrangement below could have instead have been done as:
                 
        // TransformGroup ret = super.getModel(obj, j3dModel);
        // ... do the various collision and mass stuff ...
        // return ret;
                 
        // ... that is, we call super.getModel(...) *first*, then update things.  Here
        // the model has for sure already been built (because if it wasn't built already,
        // super.getModel(...) surely did so).  Thus the setAppearance and setScale methods
        // just directly modify the model.
                 
        // It works either way.
                 
        if (j3dModel==null || ((Ball)obj).oldCollision != ((Ball)obj).collision )  // either the first time or when it changes
            {
            ((Ball)obj).oldCollision = ((Ball)obj).collision;  // reset it

            if (((Ball)obj).collision)
                setAppearance(j3dModel, appearanceForColors(
                        colColor, // ambient color
                        null,     // emissive color (black)
                        colColor, // diffuse color
                        null,     // specular color (white)
                        1.0f,     // no shininess
                        1.0f));   // full opacity
            else setAppearance(j3dModel, appearanceForColors(
                    obColor,  // ambient color
                    null,     // emissive color (black)
                    obColor,  // diffuse color
                    null,     // specular color (white)
                    1.0f,     // no shininess
                    1.0f));   // full opacity
            }

        if (j3dModel==null || ((Ball)obj).oldMass != ((Ball)obj).mass)  // likewise
            {
            ((Ball)obj).oldMass = ((Ball)obj).mass;  // reset it
                        
            setScale(j3dModel, multiply * (float)(((Ball)obj).diameter) / 2.0f);
            }
                
        return super.getModel(obj, j3dModel);
        }
    }
