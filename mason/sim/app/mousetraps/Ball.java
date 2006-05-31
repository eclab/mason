/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mousetraps;
import sim.util.*;
import sim.engine.*;

public class Ball implements Steppable
    {
    public double posX, posY, posZ;
    public double velocityX, velocityY, velocityZ;
        
    public Ball( double x, double y, double z, double vx, double vy, double vz) 
        {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        }
            
    public void step( final SimState state )
        {
        MouseTraps sim = (MouseTraps)state;
        if(posZ <=0 && velocityZ <=0)
            {
            sim.schedule.scheduleOnce(sim.schedule.time()+1,new MouseTrap(sim.discretizeX(posX),sim.discretizeY(posY)));
            sim.ballSpace.remove(this);
            return;
            }
        double timeStepDuration = MouseTraps.TIME_STEP_DURATION;
        posX +=velocityX * timeStepDuration;
        posY +=velocityY * timeStepDuration;
        posZ +=velocityZ * timeStepDuration;
        velocityZ -= MouseTraps.GRAVITY_ACC * timeStepDuration;
        
        if(sim.toroidalWorld)
            {//wrap around
            posX = (posX + sim.spaceWidth)% sim.spaceWidth;
            posY = (posY + sim.spaceHeight)%sim.spaceHeight;
            //you don;t want wrap-around the Z axis; and anyway
            //dont forget that the ceiling is computed so that no 
            //ball can jumb higher than that; 
            }
        else
            {//bounce off the walls
            if(posX >sim.spaceWidth)
                {
                posX = sim.spaceWidth;  velocityX = -velocityX;
                }
            if(posX < 0)
                {
                posX = 0;                               velocityX = -velocityX;
                }
            if(posY >=sim.spaceHeight)
                {
                posY = sim.spaceHeight; velocityY = -velocityY;
                }
            if(posY < 0)
                {
                posY = 0;                               velocityY = -velocityY;
                }
            }
        sim.ballSpace.setObjectLocation(this,new Double3D(posX,posY,posZ));
        sim.schedule.scheduleOnce(this);
        }
        
    }
