/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims3d;

import sim.portrayal3d.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.geometry.Sphere;

public class Obstacle3D extends SimplePortrayal3D
    {
    double diameter;
    protected Color3f obstacleColor = new Color3f(192f/255,255f/255,192f/255);

    public Obstacle3D( double diam )
        {
        this.diameter = diam;
        }

    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if(j3dModel==null)
            {
            j3dModel = new TransformGroup();
            Sphere s = new Sphere((float)diameter/2);
            Appearance appearance = new Appearance();
            appearance.setColoringAttributes(new ColoringAttributes(obstacleColor, ColoringAttributes.SHADE_GOURAUD));          
            Material m= new Material();
            m.setAmbientColor(obstacleColor);
            m.setEmissiveColor(0f,0f,0f);
            m.setDiffuseColor(obstacleColor);
            m.setSpecularColor(1f,1f,1f);
            m.setShininess(128f);
            appearance.setMaterial(m);

            s.setAppearance(appearance);
            j3dModel.addChild(s);
            clearPickableFlags(j3dModel);
            }
        return j3dModel;
        }
    }
