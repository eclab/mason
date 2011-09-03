/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import javax.vecmath.*;
import javax.media.j3d.*;
import sim.portrayal3d.*;
import sim.util.*;


/**
 * Draws coordinate system axes 1 unit long each, centered at the origin, 
 * and labelled "O", "X", "Y", and "Z".
 *
 * @author Gabriel Catalin Balan
 */
 
public class AxesPortrayal3D extends SimplePortrayal3D
    {
    // thickness of the arrows
    double arrowRadius;
        
    // flag showing/hidding the letters
    boolean mLetters;
        
    public AxesPortrayal3D(double arrowRadius, boolean letters)
        {
        this.arrowRadius = arrowRadius;
        mLetters = letters;       
        }
                
    void createAxes(Group group, double arrowRadius, boolean letters)
        {
        float length = 1.1f;
        group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        group.addChild(new Arrow(arrowRadius, 
                new Double3D(0, 0, 0), 
                new Double3D(length,0,0),
                (letters? "O": null),
                (letters? "X": null),
                null));
        group.addChild(new Arrow(arrowRadius, 
                new Double3D(0, 0, 0), 
                new Double3D(0,length,0), 
                null, 
                (letters? "Y": null),
                null));
        group.addChild(new Arrow(arrowRadius, 
                new Double3D(0, 0, 0), 
                new Double3D(0,0,length), 
                null, 
                (letters? "Z": null),
                null));
        }

                
    public TransformGroup getModel(Object obj, TransformGroup prev)
        {
        if(prev != null)
            return prev;
        TransformGroup tg = new TransformGroup();
        createAxes(tg, arrowRadius, mLetters);
        clearPickableFlags(tg);
        return tg;
        }
    }
