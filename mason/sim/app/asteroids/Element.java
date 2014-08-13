/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;
import java.awt.geom.*;
import java.awt.*;
import sim.portrayal.*;

/** Element is the abstract superclass of objects which have shapes and which are displayed
    on-screen.  Shapes have orientations and locations (the locations are stored with them
    in the field rather than here), and also have both rotational and translational
    velocities.  Elements are steppable and the step() method moves them via their current
    velocity and rotates them via their current rotational velocity.
        
    <p>Elements can do true collision testing with other elements via collisionWithElement().
    It's not cheap.
        
    <p>By default the drawing procedure obtains a translated and rotated version
    of the shape using getLocatedArea(), then draws it using the color provided by getColor().
    All shapes can also break into shards.  This procedure constructs some N Shards, one per
    side or curve of the original shape, and adds them to the world (the schedule and the
    field).  Shards are given velocities and spins to make them appear like an explosion.
        
    <p>All Elements have Stoppables which are called when the Element has been asked to end
    itself and remove itself from the world.
*/

public abstract class Element extends sim.portrayal.SimplePortrayal2D implements Steppable
    {
    private static final long serialVersionUID = 1;

    public Shape shape;
    public double orientation;
    public MutableDouble2D velocity;
    public double rotationalVelocity;
    public Stoppable stopper;
    
    /** Rotates (using orientation), translates, and scales the shape as requested, then returns the
        modified shape as an Area. */
    public Area getLocatedArea(double translateX, double translateY, double scaleX, double scaleY)
        {
        AffineTransform transform = new AffineTransform();
        transform.translate(translateX, translateY);
        transform.rotate(orientation);
        if (scaleX != 1.0 && scaleY != 1.0) transform.scale(scaleX, scaleY);
        Area area = new Area(shape);
        area.transform(transform);
        return area;
        }

    /** Rotates (using orientation), and translates the shape as requested, then returns
        the modified shape as an Area.  Translation is done by centering the shape at the
        Element's location in the field. */
    public Area getLocatedArea(Asteroids asteroids)
        {
        Double2D loc = asteroids.field.getObjectLocation(this);
        return getLocatedArea(loc.x, loc.y, 1.0, 1.0);
        }
        
    /** Returns true if this element overlaps with the provided element, including possible
        toroidal overlaps.  As these shapes are arbitrary, this can be expensive. */
    public boolean collisionWithElement(Asteroids asteroids, Element element)
        {
        Double2D d = (asteroids.field.getObjectLocation(this));
        double width = asteroids.field.width;
        double height = asteroids.field.height;
        Area elementloc = element.getLocatedArea(asteroids);
                
        // the obvious one
        Area a = getLocatedArea(asteroids);
        a.add(elementloc);
        if (a.isSingular()) return true;
                
        // check wrap-around situations
        AffineTransform transform = new AffineTransform();
        transform.translate(
            d.x < width / 2 ? width : // I'm on the left, gotta check on the right
            0 - width, 0); // I'm on the right, gotta check on the left
        a.transform(transform);
        a.add(elementloc);
        if (a.isSingular()) return true;
                
        // get another area
        a = getLocatedArea(asteroids);
        AffineTransform transform2 = new AffineTransform();
        transform2.translate(0, 
            d.y < height / 2 ? height : // I'm on the top, gotta check on the bottom
            0 - height ); // I'm on the bottom, gotta check on the top
        a.transform(transform2);
        a.add(elementloc);
        if (a.isSingular()) return true;
                
        return false;
        }

    /** Updates the shape's orientation and location according to its velocity and rotational velocity. */
    public void step(SimState state)
        {
        Asteroids asteroids = (Asteroids)state;
        orientation += rotationalVelocity;
        Double2D location = asteroids.field.getObjectLocation(this);
        if (location == null) return;  // we've just been destroyed
        asteroids.field.setObjectLocation(this, new Double2D(asteroids.field.stx(location.x + velocity.x),
                asteroids.field.sty(location.y + velocity.y)));
        }

    /** Override this to provide a custom color for the shape when drawn.  By default it's blue. */
    public Color getColor() { return Color.blue; }
        
    /** Sets the color to getColor(), translates the shape to the desired location on-screen and scales
        it appropriately, then draws it. */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        graphics.setColor(getColor());
        graphics.draw(getLocatedArea(info.draw.x, info.draw.y, info.draw.width, info.draw.height));
        }
        
    /** Produces N shards, one per side or curve of the shape, and adds them to the schedule and
        the field.  Shards have an initial velocity and spin appropriate to the shape having
        "exploded". */
    public void breakIntoShards(Asteroids asteroids)
        {
        Double2D location = asteroids.field.getObjectLocation(this);
        PathIterator p = new Area(shape).getPathIterator(null);
        float[] vals = new float[6];
        float lastX = 0;
        float lastY = 0;
        float firstX = 0;
        float firstY = 0;
        float midX = 0;
        float midY = 0;
        Shard shard = null;
        Double2D vec = null;
        while(!p.isDone())
            {
            GeneralPath s = new GeneralPath();
            int type = p.currentSegment(vals);
            double force = asteroids.random.nextDouble() * Shard.MAXIMUM_EXPLOSION_FORCE;
            switch(type)
                {
                case PathIterator.SEG_MOVETO:
                    lastX = firstX = vals[0];                       // need to hold onto previous points
                    lastY = firstY = vals[1];
                    break;
                case PathIterator.SEG_LINETO: 
                    midX = (lastX + vals[0]) / 2;                   // we want to roughly shift this to be around (0,0)
                    midY = (lastY + vals[1]) / 2;
                    s.moveTo((float)(0 - midX), (float)(0 - midY));
                    s.lineTo(lastX - midX, lastY - midY);
                    s.lineTo(vals[0] - midX, vals[1] - midY);
                    s.closePath();

                    vec = new Double2D(midX, midY).normalize().multiply(force);
                    shard = new Shard(asteroids, s, orientation, new MutableDouble2D(vec), new Double2D(location.x + midX, location.y + midY), getColor());
                    lastX = vals[0];
                    lastY = vals[1];
                    break;
                case PathIterator.SEG_CLOSE:
                    midX = (lastX + vals[0]) / 2;                   // we want to roughly shift this to be around (0,0)
                    midY = (lastY + vals[1]) / 2;
                    s.moveTo((float)(0 - midX), (float)(0 - midY));
                    s.lineTo(lastX - midX, lastY - midY);
                    s.lineTo(firstX - midX, firstY - midY);
                    s.closePath();

                    vec = new Double2D(midX, midY).normalize().multiply(force);
                    shard = new Shard(asteroids, s, orientation, new MutableDouble2D(vec), new Double2D(location.x + midX, location.y + midY), getColor());
                    lastX = vals[0];
                    lastY = vals[1];
                    break;
                case PathIterator.SEG_QUADTO: 
                    midX = (lastX + vals[0] + vals[2]) / 3;                 // we want to roughly shift this to be around (0,0)
                    midY = (lastY + vals[1] + vals[3]) / 3;
                    s.moveTo((float)(0 - midX), (float)(0 - midY));
                    s.lineTo(lastX - midX, lastY - midY);
                    s.quadTo(vals[0] - midX, vals[1] - midY, vals[2] - midX, vals[3] - midY);
                    s.closePath();
                    vec = new Double2D(midX, midY).normalize().multiply(force);
                    shard = new Shard(asteroids, s, orientation, new MutableDouble2D(vec), new Double2D(location.x + midX, location.y + midY), getColor());
                    lastX = vals[2];
                    lastY = vals[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    midX = (lastX + vals[0] + vals[2] + vals[4]) / 4;                       // we want to roughly shift this to be around (0,0)
                    midY = (lastY + vals[1] + vals[3] + vals[5]) / 4;
                    s.moveTo((float)(0 - midX), (float)(0 - midY));
                    s.lineTo(lastX - midX, lastY - midY);
                    s.curveTo(vals[0] - midX, vals[1] - midY, vals[2] - midX, vals[3] - midY, vals[4] - midX, vals[5] - midY);
                    s.closePath();
                    vec = new Double2D(midX, midY).normalize().multiply(force);
                    shard = new Shard(asteroids, s, orientation, new MutableDouble2D(vec), new Double2D(location.x + midX, location.y + midY), getColor());
                    lastX = vals[4];
                    lastY = vals[5];
                    break;
                default:
                    throw new RuntimeException("default case should never occur");
                }
            p.next();
            }
        }
                
    /** Ends the Element.  Stops it and removes it from the field. */
    public void end(Asteroids asteroids)
        {
        if (stopper!=null) stopper.stop();
        asteroids.field.remove(this);
        }
    }
