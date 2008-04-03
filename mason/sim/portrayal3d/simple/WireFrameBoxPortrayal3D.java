/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information


  Portions of this software are copyrighted by Sun Microsystems Incorporated
  and fall under the license listed at the end of this file.
*/

package sim.portrayal3d.simple;

import sim.portrayal3d.*;
import javax.media.j3d.*;

/**
 * A SimplePortrayal3D which draws an arbitrary wireframe box.  Unlike most other
 * SimplePortrayal3Ds, where you specify a scale, in WireFrameBoxPortrayal3D you
 * specify actual coordinates for opposite corners of the box.  This is because
 * most uses of this portrayal is to draw a big box around your field's extent, and you need
 * this capability to do so conveniently.  Objects portrayed by a WireFrameBoxPortrayal3D
 * are selectable.
 * 
 * <p>Portions of this software is based on the file ColorCube.java, available
 * as part of the Java3D Developer Kit examples, and falls under the license
 * that came with that example.  The license is listed at the end of this file.
 * The remainder of the file falls under the standard license for this library.
 * 
 **/
public class WireFrameBoxPortrayal3D extends SimplePortrayal3D
    {
    //LineStripArray box;
    public Appearance appearance;
    
    private static final float[] verts = 
        {
        1f, 0f,  1f,     1f,  1f,  1f,  0f,  1f,  1f,   0f, 0f,  1f,     1f, 0f,  1f,/* front face*/    
        0f, 0f, 0f,     0f,  1f, 0f,     1f,  1f, 0f,    1f, 0f, 0f,    0f, 0f, 0f,/* back face*/       
        1f, 0f, 0f,      1f,  1f, 0f,    1f,  1f,  1f,   1f, 0f,  1f,    1f, 0f, 0f,/* right face*/     
        0f, 0f,  1f,    0f,  1f,  1f,   0f,  1f, 0f,    0f, 0f, 0f,     0f, 0f,  1f,/* left face*/      
        1f,  1f,  1f,    1f,  1f, 0f,   0f,  1f, 0f,    0f,  1f,  1f,    1f,  1f,  1f,/* top face*/     
        0f, 0f,  1f,    0f, 0f, 0f,      1f, 0f, 0f,     1f, 0f,  1f,   0f, 0f,  1f /* bottom face*/
        };
        
    float[] scaledVerts = new float[verts.length];
    
    /** Draws a white wireframe box from (-0.5,-0.5,-0.5) to (0.5,0.5,0.5) */
    public WireFrameBoxPortrayal3D() { this(-0.5,-0.5,-0.5,0.5,0.5,0.5); }

    //    /** Draws a white wireframe box from (-dx/2,-dy/2,-dz/2) to (dx/2,dy/2,dz/2) */
    //    public WireFrameBoxPortrayal3D(double dx, double dy, double dz)
    //      {
    //        this(-dx/2,-dy/2,-dz/2, dx/2,dy/2,dz/2);
    //      }

    /** Draws a white wireframe box from (x,y,z) to (x2,y2,z2) */
    public WireFrameBoxPortrayal3D(double x, double y, double z, double x2, double y2, double z2)
        {
        this(x,y,z,x2,y2,z2,java.awt.Color.white);
        }

    /** Draws a wireframe box from (x,y,z) to (x2,y2,z2) in the specified color. */
    public WireFrameBoxPortrayal3D(double x, double y, double z, double x2, double y2, double z2, java.awt.Color color)
        {
        this(x,y,z,x2,y2,z2,appearanceForColor(color));
        }

    /** Draws a wireframe box from (x,y,z) to (x2,y2,z2) in the specified appearance. */
    public WireFrameBoxPortrayal3D(double x, double y, double z, double x2, double y2, double z2, Appearance appearance)
        {
        this.appearance = appearance;
        //float scaledVerts[] = new float[verts.length];
        for (int i = 0; i < verts.length/3; i++)
            {
            scaledVerts[3*i  ] = verts[3*i  ] * (float)(x2-x) + (float)x;
            scaledVerts[3*i+1] = verts[3*i+1] * (float)(y2-y) + (float)y;
            scaledVerts[3*i+2] = verts[3*i+2] * (float)(z2-z) + (float)z;
            }
        }

    public TransformGroup getModel(Object obj, TransformGroup tg)
        {
        if(tg==null)
            {
            TransformGroup modelTG = new TransformGroup();

            LineStripArray box = new LineStripArray(30, QuadArray.COORDINATES, new int[]{5, 5, 5, 5, 5, 5});
            box.setCoordinates(0, scaledVerts);

            Shape3D s = new Shape3D(box,appearance);
            modelTG.addChild(s);
            setPickableFlags(s);  // make it pickable
            return modelTG;
            }
        else return tg;
        }
    }

/* Portions of this software is based on the file ColorCube.java, available
 * as part of the Java3D Developer Kit examples.  The license for ColorCube.java
 * is listed below.
 *
 *
 *      @(#)ColorCube.java 1.3 98/09/28 13:12:26 
 *
 * Copyright (c) 1996-1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
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
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
