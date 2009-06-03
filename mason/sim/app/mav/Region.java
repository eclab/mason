/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mav;

import sim.portrayal.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;
import java.awt.font.*;

public /*strictfp*/ class Region extends SimplePortrayal2D
    {
    // we hard-code the available shapes here.  The reason for this is simple: shapes
    // and areas aren't serializable.  ARGH.  So we can't save out a shape/area and
    // load it back in again.  Instead we have to save out a shape "number", and then
    // load that number back in again.  Suboptimal.
    
    public static final Shape[] shapes = new Shape[]
    {
    new Ellipse2D.Double(0,0,100,100),
    AffineTransform.getRotateInstance(35*/*Strict*/Math.PI/180).createTransformedShape(
        new RoundRectangle2D.Double(0,0,100,100,15,15)),
    new Font("Serif", 0, 128).createGlyphVector(new FontRenderContext(
            new AffineTransform(),false,true),"MAV").getOutline()
    };
    
    // the location of the object's origin.
    public double originx;
    public double originy;
    int shapeNum;


    public static final Color[] surfacecolors = new Color[] {Color.white, Color.blue, Color.green, Color.red};
    public Shape shape;
    public Area area;
    public int surface;
    public Region (int num, int s,
        double x,
        double y) { shapeNum = num; 
        shape = shapes[shapeNum]; surface = s;
        area = new Area(shape); originx = x; originy = y; }
    
    
    // rule 1: don't fool around with graphics' own transforms because they effect its clip, ARGH.
    // so we have to create our own transformed shape.  To be more efficient, we only transform
    // it if it's moved around.
    Shape oldShape;
    Rectangle2D.Double oldDraw = null;
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (oldDraw == null ||
            oldDraw.x != info.draw.x ||
            oldDraw.y != info.draw.y ||
            oldDraw.width != info.draw.width ||
            oldDraw.height != info.draw.height) // new location or scale, must create
            {
            oldDraw = info.draw;
            AffineTransform transform = new AffineTransform();
            transform.translate(oldDraw.x, oldDraw.y);
            transform.scale(oldDraw.width, oldDraw.height);
            oldShape = transform.createTransformedShape(shape);
            }
        
        // okay, now draw the shape, it's properly transformed
        graphics.setColor(surfacecolors[surface]);
        graphics.fill(oldShape);
        }


    /** If drawing area intersects selected area, add to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        AffineTransform transform = new AffineTransform();
        transform.translate(range.draw.x, range.draw.y);
        transform.scale(range.draw.width, range.draw.height);
        Shape s = transform.createTransformedShape(shape);
                
        return (s.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height));
        }
        
    // because we're using Areas, and for some bizarre reason Area isn't serializable,
    // if WE want to be serializable or externalizable we need to handle our own read
    // and write methods.
    
    private void writeObject(java.io.ObjectOutputStream p)
        throws IOException
        {
        p.writeDouble(originx);
        p.writeDouble(originy);
        p.writeInt(shapeNum);
        p.writeInt(surface);
        }
        
    private void readObject(java.io.ObjectInputStream p)
        throws IOException, ClassNotFoundException
        {
        originx = p.readDouble();
        originy = p.readDouble();
        shapeNum = p.readInt();
        surface = p.readInt();
        // reload shapes and areas, which aren't serializable (ugh)
        shape = (Shape)(shapes[shapeNum]);
        area = new Area(shape);
        }    

    }
