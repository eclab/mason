/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial5;
import sim.engine.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;

public class Tutorial5 extends SimState
    {
    public Continuous2D balls;
    public Network bands;

    public int numBalls = 50;
    public int numBands = 60;
    
    public final static double maxMass = 10.0;
    public final static double minMass = 1.0;
    public final static double minLaxBandDistance = 10.0;
    public final static double maxLaxBandDistance = 50.0;
    public final static double minBandStrength = 5.0;
    public final static double maxBandStrength = 10.0;
    public final static double collisionDistance = 5.0;
    
    public int getNumBalls() { return numBalls; }
    public void setNumBalls(int val) { if (val >= 2 ) numBalls = val; }
    public int getNumBands() { return numBands; }
    public void setNumBands(int val) { if (val >= 0 ) numBands = val; }

    public Tutorial5(long seed)
        {
        super(seed);
        balls = new Continuous2D(collisionDistance,100,100);
        bands = new Network();
        }

    public void start()
        {
        super.start();

        balls = new Continuous2D(collisionDistance,100,100);
        bands = new Network();
        
        Steppable[] s = new Steppable[numBalls];
        
        // make the balls
        for(int i=0; i<numBalls;i++)
            {
            // must be final to be used in the anonymous class below
            final Ball ball = new Ball(0,0,random.nextDouble() * (maxMass-minMass) + minMass);
            balls.setObjectLocation(ball,
                new Double2D(random.nextDouble() * 100,
                    random.nextDouble() * 100));
            bands.addNode(ball);
            schedule.scheduleRepeating(ball);
            
            // schedule the balls to compute their force after everyone's moved
            s[i] = new Steppable()
                {
                public void step(SimState state) { ball.computeForce(state); }
                // see Tutorial 3 for why this is helpful
                static final long serialVersionUID = -4269174171145445918L;
                };
            }
            
        // add the sequence
        schedule.scheduleRepeating(Schedule.EPOCH,1,new Sequence(s),1);
        
        // make the bands
        Bag ballObjs = balls.getAllObjects();
        for(int i=0;i<numBands;i++)
            {
            Band band = new Band(random.nextDouble() * 
                (maxLaxBandDistance - minLaxBandDistance) + minLaxBandDistance,
                random.nextDouble() *
                (maxBandStrength - minBandStrength) + minBandStrength);
            Ball from;
            from = (Ball)(ballObjs.objs[random.nextInt(ballObjs.numObjs)]);

            Ball to = from;
            while(to == from)
                to = (Ball)(ballObjs.objs[random.nextInt(ballObjs.numObjs)]);
            
            bands.addEdge(from,to,band);
            }

        // To make the initial screenshot pretty, let's have all the balls do an initial collision check
        ballObjs = balls.getAllObjects();
        for(int i = 0; i < ballObjs.numObjs; i++)
            ((Ball)(ballObjs.objs[i])).computeCollision(this);
        }

    public static void main(String[] args)
        {
        doLoop(Tutorial5.class, args);
        System.exit(0);
        }    
        
    // see Tutorial 3 for why this is helpful
    static final long serialVersionUID = -7164072518609011190L;
    }
