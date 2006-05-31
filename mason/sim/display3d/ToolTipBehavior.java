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

import javax.swing.*;
import javax.vecmath.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import sim.display.*;
import sim.portrayal.*;
import sim.portrayal3d.*;
import sim.util.gui.*;

/**
 * A behavior similar to SelectionBehavior, except you don't have to
 * double-click and the resulting info is presented in a tool-tip, not the
 * console
 * 
 * I'm using swings default (ToolTipManager) initialDelay constant, but I did
 * not implement DismissDelay, ReshowDelay
 * 
 * 
 * @author Gabriel Catalin Balan
 */
public class ToolTipBehavior extends PickMouseBehavior 
    {
    GUIState guiState = null;
    Canvas3D canvas;

    public ToolTipBehavior(Canvas3D canvas, BranchGroup root, Bounds bounds, GUIState guiState) 
        {
        super(canvas, root, bounds);
        this.canvas = canvas;
        this.setSchedulingBounds(bounds);
        root.addChild(this);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        this.guiState = guiState;

        timer = new javax.swing.Timer(ToolTipManager.sharedInstance().getInitialDelay(), timeUpNoMovement);
        timer.setRepeats(false);
        }

    ActionListener timeUpNoMovement = new ActionListener() 
        {
        public void actionPerformed(ActionEvent value0) 
            {
                        
            showing = true;
            updateScene(lastPoint.x, lastPoint.y);
            // I don't think I need to use an
            // invokeLater, the timer runs on
            // swing's thread (i think: "timer
            // uses the same thread used to make
            // cursors blink, tool tips appear,
            // and so on.")
            //
            // Yeap, it's "AWT-EventQueue-0".
            }
        };

    WakeupCondition myWakeupCondition;

    javax.swing.Timer timer;

    Point lastPoint = null;

    // long lastTimeStamp = -1;

    /**
     * @see http://archives.java.sun.com/cgi-bin/wa?A2=ind0201&L=java3d-interest&F=&S=&P=4228
     */
    public void initialize() 
        {
        myWakeupCondition = new WakeupOr(new WakeupCriterion[] 
            {
            new WakeupOnAWTEvent(MouseEvent.MOUSE_MOVED),
            new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED),
            new WakeupOnAWTEvent(MouseEvent.MOUSE_ENTERED),
            new WakeupOnAWTEvent(MouseEvent.MOUSE_EXITED)
            });
        //the first two are standard (that's what wakeupCondition uses)
        //I also need mouse_exitsx [and maybe entered], so here we go.
        wakeupOn(myWakeupCondition);
        }

    boolean showing = false;
    boolean canShow = false;
        
    public void setCanShowToolTips(boolean val)
        {
        canShow = val;
        if (!canShow)
            {
            DialogToolTip.hideToolTip();
            timer.stop();
            showing = false;
            }
        }
                
    public boolean getCanShowToolTips()
        {
        return canShow;
        }
        
    /**
     * Disregard all stimuli other than ...
     */
    public void processStimulus(Enumeration criteria) 
        {
        WakeupCriterion wakeup;
        AWTEvent[] evt = null;

        while (criteria.hasMoreElements()) 
            {
            wakeup = (WakeupCriterion) criteria.nextElement();
            if (wakeup instanceof WakeupOnAWTEvent)
                evt = ((WakeupOnAWTEvent) wakeup).getAWTEvent();
                        

            // movement MouseEvets can be grouped together, I just want the last
            // one.
            AWTEvent levt = evt[evt.length - 1];
        
            if (levt instanceof MouseEvent) 
                {
                MouseEvent mlevt = (MouseEvent) levt;
                int mlevtId = mlevt.getID();
                if(mlevtId==MouseEvent.MOUSE_EXITED)
                    {
                    DialogToolTip.hideToolTip();
                    timer.stop();
                    showing = false;
                    }
                else if (canShow)
                    {
                    lastPoint = mlevt.getPoint();
                    if (showing)
                        updateScene(lastPoint.x, lastPoint.y);
                    //TODO maybe I should schedule this instead of calling it directly.
                    //This is the J3D thread, not the AWT/SWING thread. Got sync? 
                    else
                        timer.restart();// reset timer
                    }
                }
            }
        wakeupOn(myWakeupCondition);
        }
        
    static final int CURSOR_SKIP = 20;

    /**
     * @see com.sun.j3d.utils.picking.behaviors.PickMouseBehavior#updateScene(int,int)
     */
    public void updateScene(int xpos, int ypos) 
        {
        PickResult pickResult = null;
        PickResult pickResults[] = null;
        Shape3D shape = null;
        pickCanvas.setShapeLocation(xpos, ypos);

        Point3d eyePos = pickCanvas.getStartPosition();

        if (!pickCanvas.getBranchGroup().isLive())
            return;
        // it's too soon for tooltips, the model is not ready yet.

        pickResults = pickCanvas.pickAll();
        if (pickResults == null)
            return;

        // keep all picks to remove duplicates
        LocationWrapper[] picks = new LocationWrapper[pickResults.length];

        int distinctObjectCount = 0;

        String htmlText = null;

        for (int i = 0; i < pickResults.length; i++) 
            {
            pickResult = pickResults[i];
            shape = (Shape3D) pickResult.getNode(PickResult.SHAPE3D);
            // The user data should now be a wrapper containing
            // the field portrayal, the object (quad arrays included), and a
            // null location
            LocationWrapper w = (LocationWrapper) shape.getUserData();
            if (w == null) continue;
            boolean duplicate = false;

            for (int j = 0; j < distinctObjectCount; j++)
                if (w == picks[j]) 
                    {
                    duplicate = true;
                    break;
                    }
            if (duplicate) continue;
            picks[distinctObjectCount++] = w;
            if (pickResult.numGeometryArrays() > 0) 
                {
                PickIntersection pi = pickResult.getClosestIntersection(eyePos);

                // ... has intersections -- for reasons we cannot explain,
                // the pick intersection may be null, but the shape may NOT be
                // null!
                // mystifying....
                if (pi != null) 
                    {
                    // completedWrapper should fill out the location of the
                    // object
                    // stored in the wrapper (returning a COPY of the wrapper --
                    // not
                    // changing the original at all). ValueGrid stuff should
                    // return
                    // a new wrapper with a MutableDouble (the double value) as
                    // the new 'object' (replacing
                    // the quad array) AND the location of the MutableDouble

                    FieldPortrayal3D fPortrayal = (FieldPortrayal3D) w.getFieldPortrayal();
                    if (fPortrayal!=null)
                        {
                        LocationWrapper filledLW = fPortrayal.completedWrapper(w, pi, pickResult);
                        if (htmlText == null) htmlText = fPortrayal.getName(filledLW);
                        else htmlText = htmlText + "<br>" + fPortrayal.getName(filledLW);
                        }
                    }
                }
            }

        Point s = canvas.getLocationOnScreen();
        s.x += lastPoint.x; 
        s.y += lastPoint.y + CURSOR_SKIP;
                
        if (htmlText != null)
            htmlText = "<html><font size=\"-1\" face=\"" +
                sim.util.WordWrap.toHTML(canvas.getFont().getFamily()) +
                "\">" + htmlText + "</font></html>";
        DialogToolTip.showToolTip(s, htmlText);
        }
    }
/*
 * Portions of this software is based on the file ColorCube.java, available as
 * part of the Java3D Developer Kit examples. The license for ColorCube.java is
 * listed below.
 * 
 * @(#)IntersectInfoBehavior.java 1.10 02/04/01 15:03:49
 * 
 * Copyright (c) 1996-2002 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: -
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistribution in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed,licensed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */
