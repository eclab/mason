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
    final static Color3f obstacleColor = new Color3f(0,255f/255,0); 
    float multiply;
        
    public BallPortrayal( double diam )
        {
        multiply = (float) diam;
        generateNormals = true;
        }
 
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {               
        if(j3dModel==null || ((Ball)obj).oldMass != ((Ball)obj).mass)
            {
            // change the scale to reflect the desired diameter
            scale = multiply * (float)(((Ball)obj).diameter) / 2;
                        
            // change the appearance
            appearance = new Appearance();
            appearance.setColoringAttributes(new ColoringAttributes(obstacleColor, ColoringAttributes.SHADE_GOURAUD));
            Material m= new Material();
            m.setAmbientColor(obstacleColor);
            m.setEmissiveColor(0f,0f,0f);
            m.setDiffuseColor(obstacleColor);
            m.setSpecularColor(1f,1f,1f);
            m.setShininess(128f);
            appearance.setMaterial(m);
                        
            // force a re-build
            return super.getModel(obj, null);
            }
        else return super.getModel(obj, j3dModel);
        }
    }
