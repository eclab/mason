/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.simple;

import javax.vecmath.*;
import javax.media.j3d.*;
import sim.portrayal3d.*;


/**
 * Draws coordinate system axes 1 unit long each, centered at the origin, 
 * and labelled "O", "X", "Y", and "Z".
 *
 * @author Gabriel Catalin Balan
 */
public class Axes extends SimplePortrayal3D
    {
    /** thickness of the arrows*/
    float mArrowDia; 
    /** flag showing/hidding the letters */
    boolean mLetters;
    public Axes(float ArrowDia, boolean letters)
        {
        mArrowDia = ArrowDia; mLetters = letters;       
        }
                
        
    public static void createAxes(final Group group, final float ArrowDia, final boolean letters)
        {
        float length = 1.1f;
        group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        group.addChild(Arrow.createArrow(ArrowDia, 
                new Vector3f(0, 0, 0), 
                new Vector3f(length,0,0),
                (letters? "O": null),
                (letters? "X": null)));
        group.addChild(Arrow.createArrow(ArrowDia, 
                new Vector3f(0, 0, 0), 
                new Vector3f(0,length,0), 
                null, 
                (letters? "Y": null)));
        group.addChild(Arrow.createArrow(ArrowDia, 
                new Vector3f(0, 0, 0), 
                new Vector3f(0,0,length), 
                null, 
                (letters? "Z": null)));
        }

                
    public TransformGroup getModel(Object obj, TransformGroup prev)
        {
        if(prev != null)
            return prev;
        TransformGroup tg = new TransformGroup();
        createAxes(tg, mArrowDia, mLetters);
        clearPickableFlags(tg);
        return tg;
        }
    }
