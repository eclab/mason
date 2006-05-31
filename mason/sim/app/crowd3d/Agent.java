/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.crowd3d;
import sim.util.*;
import sim.engine.*;
import javax.vecmath.*;

public class Agent implements Steppable, Stoppable
    {
    public static double SIGHT = 5;
    public static double SPEED = .05;
    public static double WALL_AVERSION = 4.0;
    public static double CROWD_AVERSION = 1.0;//more is better//
    public static double MAX_FN_VAL;
    public static double FORCE_MIN_THRESHOLD = 0.75;
        
    Vector3d direction = new Vector3d();
    static      Vector3d tmpSumOfCrowdForces = new Vector3d();
    static      Vector3d tmpSumOfWallForces = new Vector3d();
    static      Vector3d tmpSumOfForces = new Vector3d();
    static      Vector3d tmpMyPosition = new Vector3d();
        
    public void step( final SimState state )
        {
        CrowdSim hb = (CrowdSim)state;
        Double3D myPositionD3D = hb.boidSpace.getObjectLocation(this);
        tmpMyPosition.x = myPositionD3D.x;
        tmpMyPosition.y = myPositionD3D.y;
        tmpMyPosition.z = myPositionD3D.z;
                
        Bag neighbors = hb.boidSpace.getObjectsWithinDistance(myPositionD3D, SIGHT);
        tmpSumOfCrowdForces.x = tmpSumOfCrowdForces.y = tmpSumOfCrowdForces.z = 0;
        tmpSumOfWallForces.x = tmpSumOfWallForces.y = tmpSumOfWallForces.z = 0;
            
        //I run from neighbors
        for(int i=0;i<neighbors.numObjs; ++i)
            {
            if(neighbors.objs[i] == this)
                continue;
            Double3D nPosition = hb.boidSpace.getObjectLocation(neighbors.objs[i]);
            tmpSumOfCrowdForces.x +=    fn(myPositionD3D.x-nPosition.x);
            tmpSumOfCrowdForces.y +=    fn(myPositionD3D.y-nPosition.y);
            tmpSumOfCrowdForces.z +=    fn(myPositionD3D.z-nPosition.z);
            }       
        tmpSumOfCrowdForces.scale(CROWD_AVERSION);
            
            
        if(myPositionD3D.x < SIGHT/*+0*/)
            tmpSumOfWallForces.x +=     fn(myPositionD3D.x/*-0*/);
        if(myPositionD3D.x> hb.spaceWidth-SIGHT)
            tmpSumOfWallForces.x -=     fn(hb.spaceWidth-myPositionD3D.x);
                
        if(myPositionD3D.y< SIGHT/*+0*/)
            tmpSumOfWallForces.y +=     fn(myPositionD3D.y/*-0*/);
        if(myPositionD3D.y> hb.spaceHeight-SIGHT)
            tmpSumOfWallForces.y -= fn(hb.spaceHeight-myPositionD3D.y);
                        
        if(myPositionD3D.z< SIGHT/*+0*/)
            tmpSumOfWallForces.z +=     fn(myPositionD3D.z/*-0*/);
        if(myPositionD3D.z> hb.spaceDepth-SIGHT)
            tmpSumOfWallForces.z -=     fn(hb.spaceDepth-myPositionD3D.z);
        tmpSumOfWallForces.scale(WALL_AVERSION);



        tmpSumOfForces.add(tmpSumOfCrowdForces, tmpSumOfWallForces);
                
        if(tmpSumOfForces.length()>FORCE_MIN_THRESHOLD)
            {
            tmpSumOfForces.normalize();
            tmpSumOfForces.scale(SPEED);
            tmpMyPosition.add(tmpSumOfForces);
                        
            clamp(tmpMyPosition, hb);
                        
                        
            Double3D newLocation = new Double3D(tmpMyPosition.x,tmpMyPosition.y,tmpMyPosition.z);
            hb.boidSpace.setObjectLocation(this,newLocation);
            }
        }
        
    private void clamp(Vector3d position, CrowdSim hb)
        {
        position.x = Math.min(Math.max(position.x, 0), hb.spaceWidth);
        position.y = Math.min(Math.max(position.y, 0), hb.spaceHeight);
        position.z = Math.min(Math.max(position.z, 0), hb.spaceDepth);
        }
        
    private double fn(double d)
        {
        return Math.min(MAX_FN_VAL, 1.0/d);
        }
        
    private Stoppable stopper = null;
    public void setStopper(Stoppable stopper)   {this.stopper = stopper;}
    public void stop(){stopper.stop();}
    }
