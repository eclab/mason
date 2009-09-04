/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.balls3d;
import sim.engine.*;
import sim.util.*;
import sim.field.network.*;
import sim.field.continuous.*;


public class Ball implements Steppable
    {
    // force on the Ball
    public double forcex;
    public double forcey;
    public double forcez;
    
    // Ball mass
    public double mass;
        
    // Old mass -- for Java3D to know the mass has changed
    public double oldMass;
    
    // Current Ball velocity
    public double velocityx;
    public double velocityy;
    public double velocityz; 
    
    // did the Ball collide?
    public boolean collision;
    
    // Old collision -- for Java3D to know the collision has changed
    public boolean oldCollision;
    
    // for drawing: always sqrt of mass
    public double diameter;
        
    public double getVelocityX() { return velocityx; }
    public void setVelocityX(double val) { velocityx = val; }
    public double getVelocityY() { return velocityy; }
    public void setVelocityY(double val) { velocityy = val; }
    public double getVelocityZ() { return velocityz; }
    public void setVelocityZ(double val) { velocityz = val; } 
    public double getMass() { return mass; }
    public void setMass(double val) { if (val > 0) { mass = val; diameter = Math.sqrt(val); } }
        
    public Ball(double vx, double vy, double vz, double m)
        {
        velocityx=vx;
        velocityy=vy;
        velocityz=vz; 
        mass = m;
        oldMass = m;
        diameter = Math.sqrt(m);
        }
        
    Bag myBag = new Bag();
    public void computeCollision(Balls3D tut)
        {
        Double3D me = tut.balls.getObjectLocation(this);
        Bag b = tut.balls.getObjectsExactlyWithinDistance(me,Balls3D.collisionDistance);
        collision = b.numObjs > 1;  // other than myself of course
        }

                
    public void addForce(Double3D otherBallLoc, Double3D myLoc, Band band)
        {
        // compute difference
        final double dx = otherBallLoc.x - myLoc.x;
        final double dy = otherBallLoc.y - myLoc.y;
        final double dz = otherBallLoc.z - myLoc.z; 
        final double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        final double l = band.laxDistance;
                        
        final double k = band.strength/512.0;  // cut-down
        final double forcemagnitude = (len - l) * k;
                        
        // add rubber band force
        if (len - l > 0) 
            {
            forcex += (dx * forcemagnitude) / len;
            forcey += (dy * forcemagnitude) / len;
            forcez += (dz * forcemagnitude) / len; 
            }
        }
                
    public void computeForce(SimState state)
        {
        Balls3D tut = (Balls3D) state;
        Network bands = tut.bands;
        Continuous3D balls = tut.balls;

        Double3D me = balls.getObjectLocation(this);
        
        forcex = 0; forcey = 0; forcez = 0;
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
                Double3D him = balls.getObjectLocation(other);
                addForce(him,me,b);
                }
        if (out!=null)
            for(int x=0;x<out.numObjs;x++)
                {
                Edge e = (Edge)(out.objs[x]);
                Band b = (Band) (e.info);
                Ball other = (Ball)(e.to());  // from me to him
                Double3D him = balls.getObjectLocation(other);
                addForce(him,me,b);
                }
        }
    
    public void step(SimState state)
        {
        Balls3D tut = (Balls3D) state;
        
        // acceleration = force / mass
        final double ax = forcex / mass;
        final double ay = forcey / mass;
        final double az = forcez / mass; 
        
        // velocity = velocity + acceleration
        velocityx += ax;
        velocityy += ay;
        velocityz += az; 
        
        // position = position + velocity
        Double3D pos = tut.balls.getObjectLocation(this);
        Double3D newpos = new Double3D(pos.x+velocityx, pos.y + velocityy, pos.z + velocityz);
        tut.balls.setObjectLocation(this,newpos);
        
        // compute collisions
        computeCollision(tut);
        }
    }
    
