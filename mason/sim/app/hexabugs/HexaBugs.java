/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.hexabugs;
import sim.engine.*;
import sim.util.*;
import sim.field.grid.*;

public /*strictfp*/ class HexaBugs extends SimState
    {
    public double minIdealTemp = 17000;
    public double maxIdealTemp = 31000;
    public double minOutputHeat = 6000;
    public double maxOutputHeat = 10000;

    public double evaporationRate = 0.993;
    public double diffusionRate = 1.0;
    public static final double MAX_HEAT = 32000;
    public double randomMovementProbability = 0.1;

    public int gridHeight = 100;
    public int gridWidth = 100;
    public int bugCount = 100;
    HexaBug[] bugs;

    public double getMinimumIdealTemperature() { return minIdealTemp; }
    public void setMinimumIdealTemperature( double temp ) { if( temp <= maxIdealTemp ) minIdealTemp = temp; }
    public double getMaximumIdealTemperature() { return maxIdealTemp; }
    public void setMaximumIdealTemperature( double temp ) { if( temp >= minIdealTemp ) maxIdealTemp = temp; }
    public double getMinimumOutputHeat() { return minOutputHeat; }
    public void setMinimumOutputHeat( double temp ) { if( temp <= maxOutputHeat ) minOutputHeat = temp; }
    public double getMaximumOutputHeat() { return maxOutputHeat; }
    public void setMaximumOutputHeat( double temp ) { if( temp >= minOutputHeat ) maxOutputHeat = temp; }
    public Object domEvaporationConstant() { return new Interval(0.0,1.0); }
    public double getEvaporationConstant() { return evaporationRate; }
    public void setEvaporationConstant( double temp ) { if( temp >= 0 && temp <= 1 ) evaporationRate = temp; }
    public Object domDiffusionConstant() { return new Interval(0.0,1.0); }
    public double getDiffusionConstant() { return diffusionRate; }
    public void setDiffusionConstant( double temp ) { if( temp >= 0 && temp <= 1 ) diffusionRate = temp; }
    public Object domRandomMovementProbability() { return new Interval(0.0,1.0); }
    public double getRandomMovementProbability() { return randomMovementProbability; }
    public void setRandomMovementProbability( double t )
        {
        if (t >= 0 && t <= 1)
            {
            randomMovementProbability = t;
            for( int i = 0 ; i < bugCount ; i++ )
                if (bugs[i]!=null)
                    bugs[i].setRandomMovementProbability( randomMovementProbability );
            }
        }
    public double getMaximumHeat() { return MAX_HEAT; }
    public int getGridHeight() { return gridHeight; }
    public int getGridWidth() { return gridWidth; }
    public int getBugCount() { return bugCount; }
    public void setBugCount(int val) { if (val >= 0) bugCount = val; }


    public DoubleGrid2D valgrid = new DoubleGrid2D(gridWidth, gridHeight,0);
    public DoubleGrid2D valgrid2 = new DoubleGrid2D(gridWidth, gridHeight, 0);
    public SparseGrid2D buggrid = new SparseGrid2D(gridWidth, gridHeight);

    // some variables shared by all hexa bugs in the application
    DoubleBag neighVal = new DoubleBag();
    IntBag neighX = new IntBag();
    IntBag neighY = new IntBag();

    /** Creates a HexaBugs simulation with the given random number seed. */
    public HexaBugs(long seed)
        {
        // we build a schedule that has two orders per step: for the bugs, then the Hexa decreaser
        super(seed);
        bugs = new HexaBug[bugCount];
        }
        
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        
        // make new grids
        valgrid = new DoubleGrid2D(gridWidth, gridHeight,0);
        valgrid2 = new DoubleGrid2D(gridWidth, gridHeight, 0);
        buggrid = new SparseGrid2D(gridWidth, gridHeight);  // we're doing toroidal, so specify dimensions
        bugs = new HexaBug[bugCount];

        // Schedule the Hexa bugs -- we could instead use a RandomSequence, which would be faster
        // But we spend no more than 3% of our total runtime in the scheduler max, so it's not worthwhile
        for(int x=0;x<bugCount;x++)
            {
            bugs[x] = new HexaBug(random.nextDouble() * (maxIdealTemp - minIdealTemp) + minIdealTemp,
                random.nextDouble() * (maxOutputHeat - minOutputHeat) + minOutputHeat, 
                MAX_HEAT, randomMovementProbability);
            buggrid.setObjectLocation(bugs[x],random.nextInt(gridWidth),random.nextInt(gridHeight));
            schedule.scheduleRepeating(bugs[x]);
            }
                            
        // Schedule the decreaser to happen after the HexaBugs
        if (availableProcessors() >  1)  // yay, multi-processor!
            schedule.scheduleRepeating(Schedule.EPOCH,1,new ThreadedHexaDiffuser(valgrid,valgrid2,evaporationRate,diffusionRate),1);
        else
            schedule.scheduleRepeating(Schedule.EPOCH,1,new HexaDiffuser(valgrid,valgrid2,evaporationRate,diffusionRate),1);
        }
    
    public int availableProcessors()
        {
        Runtime runtime = Runtime.getRuntime();
        try { return ((Integer)runtime.getClass().getMethod("availableProcessors", (Class[])null).
                invoke(runtime,(Object[])null)).intValue(); }
        catch (Exception e) { return 1; }  // a safe but sometimes wrong assumption!
        }

    public static void main(String[] args)
        {
        doLoop(HexaBugs.class, args);
        System.exit(0);
        }    
    }
    
    
    
    
    
