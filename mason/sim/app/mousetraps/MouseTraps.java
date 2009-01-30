/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mousetraps;
import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous3D;
import sim.field.grid.*;
import ec.util.*;


public class MouseTraps extends SimState
    {
    /** the number of balls a trap will throw in the air when triggered */
    public static final int BALLS_PER_TRAP = 2;
    /** the initial velocity of a ball when thrown by a trap
     * I was going to model trap's dispensed energy
     * E = 0.5 * BALLS_PER_TRAP * ball's_weight * INITIAL_VELOCITY^2 
     * But since all are contants, i skip the math and
     * set INITIAL_VELOCITY and save some math,
     * without loss in generality */
    public final double initialVelocity;
        
    public static final double GRAVITY_ACC = 9.8;
    public static final double TWO_OVER_G = 2.0/GRAVITY_ACC;
        
    public static final double TIME_STEP_DURATION = 1.0/64; 
    public static final double TIME_STEP_FREQUENCY = 1.0/TIME_STEP_DURATION; 
        
    public static final double TWO_PI = Math.PI * 2.0;
    public static final double HALF_PI = Math.PI * 0.5;
        
    public final boolean toroidalWorld;
    public final boolean modelBalls;

    /** @todo handle realocation of grids when these two are changed */
    public final int trapGridHeight;
    public final int trapGridWidth;
    public final double spaceWidth, spaceHeight, spaceLength;
    public final double oneOverSpaceWidth, oneOverSpaceHeight, oneOverSpaceLength;

    public final double trapSizeX, trapSizeY;

    public IntGrid2D trapStateGrid;
    public Continuous3D ballSpace;
    public static final int ARMED_TRAP = 0;
    public static final int OFF_TRAP = 1;
        
        
    /** Creates a HeatBugs simulation with the given random number seed. */
    public MouseTraps(long seed)
        {
        this(seed, 0.7, 100, 100, true);
        }
            
    public MouseTraps(long seed, double initialVelocity, int width, int height, boolean toroidal)
        {
        super(seed);
        this.initialVelocity = initialVelocity;
        toroidalWorld = toroidal;
        modelBalls = false;
        trapGridWidth = width;
        trapGridHeight = height;
        spaceWidth  = 1;
        spaceHeight = 1;
        spaceLength = 1;
        createGrids();
        trapSizeX = spaceWidth/trapGridWidth;
        trapSizeY = spaceHeight/trapGridHeight;
        oneOverSpaceHeight = 1.0/spaceHeight;
        oneOverSpaceWidth  = 1.0/spaceWidth;
        oneOverSpaceLength = 1.0/spaceLength;
        }
    public MouseTraps(  long seed, 
        double initialVelocity, 
        int trapsX, 
        int trapsY,
        double width, 
        double height, 
        boolean toroidal)
        {
        super(new MersenneTwisterFast(seed));
        this.initialVelocity = initialVelocity;
        toroidalWorld = toroidal;
        trapGridWidth = trapsX;
        trapGridHeight = trapsY;
        modelBalls  = true;
        spaceWidth  = width;
        spaceHeight = height;
        spaceLength = computeFishTankCeiling();
        createGrids();
        trapSizeX = spaceWidth/trapGridWidth;
        trapSizeY = spaceHeight/trapGridHeight;
        oneOverSpaceHeight = 1.0/spaceHeight;
        oneOverSpaceWidth  = 1.0/spaceWidth;
        oneOverSpaceLength = 1.0/spaceLength;
        }
        
    /** computes how high should be the ceiling of the
     * fishtank so the balls don't hit it., even if they
     * are shot straight up.
     * 
     * Y = V0T-.5gT^2   //y0=0
     * V = V0-gT
     * => Ttop = V0/g
     * => Ytop = .5V0^2/g;
     * 
     * or in one line using energy conservation 
     * 
     */
    public double computeFishTankCeiling()
        {
        return 0.5 * initialVelocity * initialVelocity / GRAVITY_ACC;
        }
        
    void createGrids()
        {
        trapStateGrid = new IntGrid2D(trapGridWidth, trapGridHeight,ARMED_TRAP);        
        if(modelBalls)
            ballSpace = new Continuous3D(Math.max(trapGridHeight, trapGridWidth)*2, spaceWidth, spaceHeight, spaceLength);
        }
        
    /**
     * determines on what trap a certain location belongs to
     * @param position = distance in continuous space.
     * @return  [0..width-1] trap index
     * 
     * note: boundary between traps belong to the one in the right,
     * 
     * if space is toroidal, wrap "distance" around the space using %
     * if is not, the ball would bounce off the wall => 
     *  - it is like it did not bounce, but it entered a mirrored grid 
     *    (this explains trapGridWidth-i)
     *  - after two bounces everything is back, so I wrap the distance around 2 grids 
     *    (this explains i %= (2*trapGridWidth))
     */
    public int discretizeX(double position)
        {
        int i =(int)(position* oneOverSpaceWidth* trapGridWidth);
        if(toroidalWorld)
            return (i+trapGridWidth) % trapGridWidth;
        i+=2*trapGridWidth;
        i %= (2*trapGridWidth);
        if( i<trapGridWidth)
            return i;
        return trapGridWidth-i;
        }
        
    /**
     * determines on what trap a certain location belongs to
     * @param position = distance in continuous space.
     * @return  [0..height-1] trap index
     * 
     * @see scretizeX
     */
    public int discretizeY(double position)
        {
        int i =(int)(position* oneOverSpaceHeight* trapGridHeight);
        if(toroidalWorld)
            return (i+trapGridHeight) % trapGridHeight;
        i+=2*trapGridHeight;
        i %= (2*trapGridHeight); 
        if( i<trapGridHeight)
            return i;
        return trapGridHeight-i;
        }
        
    public int discretizeX(double offset, int location)
        {
        return discretizeX(offset+(0.5+location)*trapSizeX);
        //offset is measured from the center of the trap
        }

    public int discretizeY(double offset, int location)
        {
        return discretizeY(offset+(0.5+location)*trapSizeY);
        //offset is measured from the center of the trap
        }
        
    public double trapPosX(int x)
        {
        return (0.5+x)*trapSizeX;
        }

    public double trapPosY(int y)
        {
        return (0.5+y)*trapSizeY;
        }
        
    public void triggerTrap(int posx, int posy)
        {
        if(trapStateGrid.get(posx, posy)== OFF_TRAP)
            return;
        trapStateGrid.set(posx, posy, OFF_TRAP);
        double spacePosX = (0.5+posx)*trapSizeX;
        double spacePosY = (0.5+posy)*trapSizeY;
                
                
        for(int i=0; i< MouseTraps.BALLS_PER_TRAP; i++)
            {   //decide the componnents of initial speed. 
            /**
             * http://www.phy6.org/stargaze/Scelcoor.htm
             * 
             * The angle f is measured in a horizontal plane, is known as azimuth and is measured from the 
             * north direction. A rotating table allows the telescope to be pointed in any azimuth.
             * 
             * The angle l is called elevation and is the angle by which the telescope is lifted above the 
             * horizontal (if it looks down, l is negative). The two angles together can in principle specify 
             * any direction: f ranges from 0 to 360, and l from -90 (straight down or "nadir") to +90 
             * (straight up or "zenith").
             * 
             * Again, one needs to decide from what direction is the azimuth measured--that is, where is azimuth
             * zero? The rotation of the heavens (and the fact most humanity lives north of the equator) suggests
             * (for surveyor-type measurements) the northward direction, and this is indeed the usual zero point.
             * The azimuth angle (viewed from the north) is measured counterclockwise.
             */ 
            double azimuth, elevation;
            azimuth = random.nextDouble()*TWO_PI;
            elevation = random.nextDouble()*HALF_PI;//balls are thrown UP
                        
            double cos_elevation = Math.cos(elevation);
            double sin_elevation = Math.sqrt(1 - cos_elevation*cos_elevation);
            //[sin(elevation) is positive, anyway]
            double cos_azimuth = Math.cos(azimuth);
            double sin_azimuth = Math.sin(azimuth);
            //[sin(azimiuth] is not always positive, so the sin(elevation) trick does not hold
                        
            double vz = initialVelocity * sin_elevation;
            double vxy = initialVelocity * cos_elevation;
            double vx = vxy * cos_azimuth;
            double vy = vxy * sin_azimuth;
                    
            if(! modelBalls)
                {
                double landing_time = vz * TWO_OVER_G;
                double landing_dx = vx* landing_time;
                double landing_dy = vy* landing_time;

                schedule.scheduleOnce(  schedule.time()+landing_time ,
                    new MouseTrap(discretizeX(landing_dx, posx),discretizeY(landing_dy, posy)));
                }
            else
                {
                Ball b = new Ball(spacePosX,spacePosY,0.0, vx, vy, vz);
                ballSpace.setObjectLocation(b,new Double3D(     spacePosX,spacePosY,0));
                schedule.scheduleOnce(schedule.time()+1, b);
                }
            }
        }

    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        
        // make new grids
        createGrids();
                
        int posx = trapGridWidth/2;
        int posy = trapGridHeight/2;
        if(modelBalls)
            {
            double x = (0.5+posx)*trapSizeX;
            double y = (0.5+posy)*trapSizeY;
            double z = computeFishTankCeiling();
            Ball b = new Ball(x,y,z, 0.0, 0.0, 0.0);
            ballSpace.setObjectLocation(b,new Double3D( x,y,z));
            schedule.scheduleOnce(Schedule.EPOCH, b);

            }
        else
            schedule.scheduleOnce(initialVelocity* GRAVITY_ACC, new MouseTrap(posx, posy));
        }
        
        
    public static void main(String[] args)
        {
        doLoop(MouseTraps.class, args);
        System.exit(0);
        }    
    }
    
    
    
    
    
