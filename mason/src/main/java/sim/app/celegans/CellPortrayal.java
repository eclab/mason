/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;
 
import javax.media.j3d.*;
import javax.vecmath.*;
import sim.portrayal3d.simple.*;
import java.awt.Color;
 
public class CellPortrayal extends SpherePortrayal3D
    {
    final static Color[] fateColors = new Color[] { null /* Doesn't matter */, Color.lightGray, Color.blue, Color.yellow, Color.red, Color.green, Color.orange };
    final static Color[] typeColors = new Color[] { Color.magenta , Color.pink, Color.cyan, Color.darkGray, new Color(255, 0, 255) };
    double multiply;
        
    public CellPortrayal( double diam )
        {
        multiply = diam;
        }
 
    public TransformGroup getModel(Object obj, TransformGroup j3dModel)
        {
        if (j3dModel==null)
            {
            Cell cell = (Cell)obj;
            Color color = null;
            if (cell.fate > 0) color = fateColors[cell.fate];
            else color = typeColors[cell.type];
            setAppearance(j3dModel, appearanceForColors(
                    color, // ambient color
                    null,     // emissive color (black)
                    color, // diffuse color
                    null,     // specular color (white)
                    1.0f,     // no shininess
                    1.0f));   // full opacity?

            setScale(j3dModel, (float)(multiply * ((Cell)obj).radius));
            }
        return super.getModel(obj, j3dModel);
        }
    }
