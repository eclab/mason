/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d;

import sim.portrayal.*;
import javax.media.j3d.*;

/**
 * The top-level definition of Portrayals which portray underlying models using, er,
 * Java3D models.  There's a semantic overload here: "models" in the simulation
 * versus Java3D scenegraph "models".   We'll assume the second whenever we use
 * the term.
 *
 * <p>A Portrayal3D has two basic functions.  getModel(object) provides a Java3D model
 * which draws the representation of an object and can be selected if appropriate.
 * This is the Java3D equivalent to the draw(...) and hitObjects(...) methods in the
 * 2D portrayal code.  polygonAttributes() returns the polygon attributes of the Portrayal,
 * enabling certain portrayals to have their vertex forms manipulated by the user via a GUI.
 * This is primarily used for ValueGridPortrayal2D3Ds.
 *
 * <p>Portrayal3Ds provide their models in the following way: the top level of the object
 * will be a TransformGroup whose Transform3D the Portrayal3D will not fool around with.
 * Instead this TransformGroup is owned by the parent portrayal (say, a Field Portrayal)
 * enabling it to move, rotate, and scale Portrayal3D with relative ease.
 *
 */
 
public interface Portrayal3D extends Portrayal
    {
    /**
     * Provides a TransformGroup which defines the node(s) to place in
     * the scenegraph.  This is the Portrayal3D equivalent of Portrayal2D's
     * draw(object, graphics, drawinfo) method.  
     *
     * <p>You should hang your model off of the TransformGroup provided.
     * You should <i>not</i> transform that TransformGroup in any way --
     * it is used elsewhere.  Instead if you wish to transform your model
     * (rotate it etc.) you should add your own additional TransformGroup
     * as necessary.
     *
     * <p>The provided TransformGroup can be null; in this case you need to
     * create and return the outer TransformGroup for the object.  If the provided
     * TransformGroup is non-null, you should modify it and return the same.
     *
     * <p>SimplePortrayals should assume the following contract: at the point
     * that getModel(...) is called, the parentPortrayal will have already been
     * set if it exists, else it will be null.
     */
    public TransformGroup getModel(Object object, TransformGroup prev);
    
    /**
     * Provides a PolygonAttributes which can be modified to change
     * the underlying model's attributes (culling, vertex versus point versus fill).
     * This is an optional function: you are free to return null
     */
    // not getPolygonAttributes because that shows up in the inspector as a property!
    public PolygonAttributes polygonAttributes();
    }
    
