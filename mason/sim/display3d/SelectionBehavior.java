/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information

  Portions of this software are copyrighted by Sun Microsystems Incorporated
  and fall under the license listed at the end of this file.
*/

package sim.display3d;

import javax.media.j3d.*;
import com.sun.j3d.utils.picking.*;
import com.sun.j3d.utils.picking.behaviors.*;

import javax.swing.SwingUtilities;
import javax.vecmath.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import sim.util.*;
import sim.display.*;
import sim.portrayal.*;
import sim.portrayal3d.*;

/**
 * A behavior added to Display3Ds which enables Portrayal3Ds to be selected (via Java3D picking).
 * 
 * <p>Portions of this software is based on the file ColorCube.java, available
 * as part of the Java3D Developer Kit examples, and falls under the license
 * that came with that example.  The license is listed at the end of this file.
 * The remainder of the file falls under the standard license for this library.
 *
 * @author Gabriel Catalin Balan
 */
public class SelectionBehavior extends PickMouseBehavior
    {
    GUIState guiState = null;

    /**
     * Constructor for SelectionBehavior.
     * @param canvas
     * @param root
     * @param bounds
     */
    public SelectionBehavior(Canvas3D canvas, BranchGroup root, Bounds bounds, GUIState guiState)
        {
        super(canvas, root, bounds);
        this.setSchedulingBounds(bounds);
        root.addChild(this);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        this.guiState = guiState;
        }

    /** 
     * Disregard all stimuli other than Dbl-Click or greater
     */
    public void processStimulus (Enumeration criteria) 
        {
        WakeupCriterion wakeup;
        AWTEvent[] evt = null;
        
        while(criteria.hasMoreElements())
            {
            wakeup = (WakeupCriterion)criteria.nextElement();
            if (wakeup instanceof WakeupOnAWTEvent)
                evt = ((WakeupOnAWTEvent)wakeup).getAWTEvent();
            }

        if(evt[0] instanceof MouseEvent)
            {
            mevent = (MouseEvent) evt[0];
            if (mevent.getClickCount()>=2)
                updateScene(mevent.getPoint().x, mevent.getPoint().y);
            }
        wakeupOn (wakeupCondition);
        }

    /**
     * @see com.sun.j3d.utils.picking.behaviors.PickMouseBehavior#updateScene(int, int)
     */
    public void updateScene(int xpos, int ypos)
        {
        PickResult pickResult = null;
        PickResult pickResults[] = null;
        Shape3D shape = null;
        pickCanvas.setShapeLocation(xpos, ypos);
        
        Point3d eyePos = pickCanvas.getStartPosition ();

        try
	    {
	    pickResults = pickCanvas.pickAll();
	    }
	catch (javax.media.j3d.CapabilityNotSetException e)
	    {
	    // we need to trap this but we're not sure why it's happening right now.
	    // it pops up occasionally when we click on 3d edge portrayals.  But they
	    // all appear to have their geometries set.
	    e.printStackTrace();
	    }
	    
        if(pickResults == null)
            return;
                
        // keep all picks to remove duplicates
        LocationWrapper[] picks = new LocationWrapper[pickResults.length];
        
        final Bag inspectors = new Bag(); 
        Bag inspectorPortrayals = new Bag();
        final Bag names = new Bag();
        
        int distinctObjectCount = 0;
                
        for(int i=0; i<pickResults.length; i++)
            {
            pickResult = pickResults[i];
            shape = (Shape3D) pickResult.getNode(PickResult.SHAPE3D);
            // The user data should now be a wrapper containing
            // the field portrayal, the object (quad arrays included), and a null location
            LocationWrapper w = (LocationWrapper)shape.getUserData();
            if(w==null)
                continue;
            boolean duplicate = false;

            for(int j=0;j<distinctObjectCount; j++)
                if(w == picks[j])
                    {
                    duplicate = true;
                    break;
                    }
            if(duplicate)
                continue;
            picks[distinctObjectCount++] = w;
            if(pickResult.numGeometryArrays() > 0)
                {
                PickIntersection pi = pickResult.getClosestIntersection(eyePos);

                // ... has intersections  -- for reasons we cannot explain, 
                // the pick intersection may be null, but the shape may NOT be null!
                // mystifying....
                if (pi!=null) 
                    {
                    // completedWrapper should fill out the location of the object
                    // stored in the wrapper (returning a COPY of the wrapper -- not
                    // changing the original at all).  ValueGrid stuff should return
                    // a new wrapper with a MutableDouble (the double value) as the new 'object' (replacing
                    // the quad array) AND the location of the MutableDouble
                                    
                    FieldPortrayal3D fPortrayal = (FieldPortrayal3D)w.getFieldPortrayal();
                    
                    // could be null for some reason
                    if (fPortrayal == null)
                        {
                        System.err.println("Warning: The value of a LocationWrapper.getFieldPortrayal() is null.\nLikely the wrapper was created from a SimplePortrayal3D whose parentPortrayal\nwas not set before getModel(...) was called."); 
                        }
                    else
                        {
                        LocationWrapper filledLW = fPortrayal.completedWrapper(w,pi,pickResult);
                        inspectors.add(fPortrayal.getInspector(filledLW, guiState));
                        //
                        // FieldPortrayal[3D] should declare abstract method getWrapper(obj).
                        // here i can retrieve it from pinfo.portrayal.
                        //
                        inspectorPortrayals.add(fPortrayal);
                        names.add(fPortrayal.getName(filledLW));
                        }
                    }
                }
            }
            
        final GUIState g = guiState;  // in the wild off-chance this is changed
        if(distinctObjectCount!=0)
            SwingUtilities.invokeLater(new Runnable()
                {
                public void run()
                    {
                    g.controller.setInspectors(inspectors,names);
                    }
                });
        }
    }

/* Portions of this software is based on the file ColorCube.java, available
 * as part of the Java3D Developer Kit examples.  The license for ColorCube.java
 * is listed below.
 * 
 *      @(#)IntersectInfoBehavior.java 1.10 02/04/01 15:03:49
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
 * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
 * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed,licensed or intended
 * for use in the design, construction, operation or maintenance of
 * any nuclear facility.
 */
