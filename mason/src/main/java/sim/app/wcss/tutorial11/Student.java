/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.wcss.tutorial11;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import sim.field.network.*;

public class Student implements Steppable
    {
    private static final long serialVersionUID = 1;

    public static final double MAX_FORCE = 3.0;
    
    double friendsClose = 0.0;  // initially very close to my friends
    double enemiesCloser = 10.0;  // WAY too close to my enemies
    public double getAgitation() { return friendsClose + enemiesCloser; }

    public String toString() { return "[" + System.identityHashCode(this) + "] agitation: " + getAgitation(); }
                
    public void step(SimState state)
        {
        Students students = (Students) state;
        Continuous2D yard = students.yard;

        Double2D me = students.yard.getObjectLocation(this);

        MutableDouble2D sumForces = new MutableDouble2D();
        
        friendsClose = enemiesCloser = 0.0;
        
        // Go through my buddies and determine how much I want to be near them
        MutableDouble2D forceVector = new MutableDouble2D();
        Bag out = students.buddies.getEdges(this, null);
        int len = out.size();
        for(int buddy = 0 ; buddy < len; buddy++)
            {
            Edge e = (Edge)(out.get(buddy));
            double buddiness = ((Double)(e.info)).doubleValue();
            
            // I could be in the to() end or the from() end.  getOtherNode is a cute function
            // which grabs the guy at the opposite end from me.
            Double2D him = students.yard.getObjectLocation(e.getOtherNode(this));
            
            if (buddiness >= 0)  // the further I am from him the more I want to go to him
                {
                forceVector.setTo((him.x - me.x) * buddiness, (him.y - me.y) * buddiness);
                if (forceVector.length() > MAX_FORCE)  // I'm far enough away
                    forceVector.resize(MAX_FORCE);
                friendsClose += forceVector.length();
                }
            else  // the nearer I am to him the more I want to get away from him, up to a limit
                {
                forceVector.setTo((him.x - me.x) * buddiness, (him.y - me.y) * buddiness);
                if (forceVector.length() > MAX_FORCE)  // I'm far enough away
                    forceVector.resize(0.0);
                else if (forceVector.length() > 0)
                    forceVector.resize(MAX_FORCE - forceVector.length());  // invert the distance
                enemiesCloser += forceVector.length();
                }
            sumForces.addIn(forceVector);
            }
        

        // add in a vector to the "teacher" -- the center of the yard, so we don't go too far away
        sumForces.addIn(new Double2D((yard.width * 0.5 - me.x) * students.forceToSchoolMultiplier, 
                (yard.height * 0.5 - me.y) * students.forceToSchoolMultiplier));
        
        // add a bit of randomness
        sumForces.addIn(new Double2D(students.randomMultiplier * (students.random.nextDouble() * 1.0 - 0.5), 
                students.randomMultiplier * (students.random.nextDouble() * 1.0 - 0.5)));

        sumForces.addIn(me);
        
        students.yard.setObjectLocation(this, new Double2D(sumForces));
        }
    } 
    
