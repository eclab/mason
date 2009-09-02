/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial5;
import sim.engine.*;
import sim.portrayal.*;
import sim.util.*;
import sim.field.network.*;
import sim.field.continuous.*;
import java.awt.*;
import java.awt.geom.*;


public class Ball extends SimplePortrayal2D implements Steppable
    {
    // force on the Ball
    public double forcex;
    public double forcey;
    
    // Ball mass
    public double mass;
    
    // Current Ball velocity
    public double velocityx;
    public double velocityy;
    
    // did the Ball collide?
    public boolean collision;
    
    // for drawing: always sqrt of mass
    public double diameter;
        
    public double getVelocityX() { return velocityx; }
    public void setVelocityX(double val) { velocityx = val; }
    public double getVelocityY() { return velocityy; }
    public void setVelocityY(double val) { velocityy = val; }
    public double getMass() { return mass; }
    public void setMass(double val) { if (val > 0) { mass = val; diameter = Math.sqrt(val); } }
    
    public Ball(double vx, double vy, double m)
        {
        velocityx=vx;
        velocityy=vy;
        mass = m;
        diameter = Math.sqrt(m);
        }

    public void computeCollision(Tutorial5 tut)
        {
        /*
        // old previous code
        
        collision = false;
        Double2D me = tut.balls.getObjectLocation(this);
        Bag b = tut.balls.getObjectsWithinDistance(me,Tutorial5.collisionDistance);
        for(int x=0;x<b.numObjs;x++)
        if( this != b.objs[x] )
        {
        Double2D loc = tut.balls.getObjectLocation(b.objs[x]);
        if ((loc.x-me.x)*(loc.x-me.x) + (loc.y-me.y)*(loc.y-me.y) 
        <= Tutorial5.collisionDistance * Tutorial5.collisionDistance)
        {
        collision = true;
        ((Ball)(b.objs[x])).collision = true;
        }
        }
        */
                
        Double2D me = tut.balls.getObjectLocation(this);
        Bag b = tut.balls.getObjectsExactlyWithinDistance(me,Tutorial5.collisionDistance);
        collision = b.numObjs > 1;  // other than myself of course
        }
    
    public void addForce(Double2D otherBallLoc, Double2D myLoc, Band band)
        {
        // compute difference
        final double dx = otherBallLoc.x - myLoc.x;
        final double dy = otherBallLoc.y - myLoc.y;
        final double len = Math.sqrt(dx*dx + dy*dy);
        final double l = band.laxDistance;

        final double k = band.strength/512.0;  // cut-down
        final double forcemagnitude = (len - l) * k;
        
        // add rubber band force
        if (len - l > 0) 
            {
            forcex += (dx * forcemagnitude) / len;
            forcey += (dy * forcemagnitude) / len;
            }
        }
    
    public void computeForce(SimState state)
        {
        Tutorial5 tut = (Tutorial5) state;
        Network bands = tut.bands;
        Continuous2D balls = tut.balls;

        Double2D me = balls.getObjectLocation(this);
        
        forcex = 0; forcey = 0;
        // rubber bands exert a force both ways --
        // so our graph is undirected.  We need to get edges
        // both in and out, as they could be located either place
        Bag in = bands.getEdgesIn(this);
        Bag out = bands.getEdgesOut(this);
        if (in!=null)
            for(int x=0;x<in.numObjs;x++)
                {
                Edge e = (Edge)(in.objs[x]);
                Band b = (Band) (e.info);
                Ball other = (Ball)(e.from());  // from him to me
                Double2D him = balls.getObjectLocation(other);
                addForce(him,me,b);
                }
        if (out!=null)
            for(int x=0;x<out.numObjs;x++)
                {
                Edge e = (Edge)(out.objs[x]);
                Band b = (Band) (e.info);
                Ball other = (Ball)(e.to());  // from me to him
                Double2D him = balls.getObjectLocation(other);
                addForce(him,me,b);
                }
        }
    
    public void step(SimState state)
        {
        Tutorial5 tut = (Tutorial5) state;
        
        // acceleration = force / mass
        final double ax = forcex / mass;
        final double ay = forcey / mass;
        
        // velocity = velocity + acceleration
        velocityx += ax;
        velocityy += ay;
        
        // position = position + velocity
        Double2D pos = tut.balls.getObjectLocation(this);
        Double2D newpos = new Double2D(pos.x+velocityx, pos.y + velocityy);
        tut.balls.setObjectLocation(this,newpos);
        
        // compute collisions
        computeCollision(tut);
        }
    
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double width = info.draw.width * diameter;
        final double height = info.draw.height * diameter;

        if (collision) graphics.setColor(Color.red);
        else graphics.setColor(Color.blue);

        final int x = (int)(info.draw.x - width / 2.0);
        final int y = (int)(info.draw.y - height / 2.0);
        final int w = (int)(width);
        final int h = (int)(height);

        // draw centered on the origin
        graphics.fillOval(x,y,w,h);
        }
    
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double SLOP = 1.0;  // need a little extra diameter to hit circles
        final double width = range.draw.width * diameter;
        final double height = range.draw.height * diameter;
        
        Ellipse2D.Double ellipse = new Ellipse2D.Double( 
            range.draw.x-width/2-SLOP, 
            range.draw.y-height/2-SLOP, 
            width+SLOP*2,
            height+SLOP*2 );
        return ( ellipse.intersects( range.clip.x, range.clip.y, range.clip.width, range.clip.height ) );
        }
    }
    
