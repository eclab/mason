/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;


public /*strictfp*/ class HeatBugs extends SimState  
/// See further below for information about these options
// implements sim.portrayal.inspector.Tabbable
// implements sim.portrayal.inspector.Groupable
// implements sim.portrayal.inspector.TabbableAndGroupable 
    {
    private static final long serialVersionUID = 1;

    public double minIdealTemp = 17000;
    public double maxIdealTemp = 31000;
    public double minOutputHeat = 6000;
    public double maxOutputHeat = 10000;

    public double evaporationRate = 0.993;
    public double diffusionRate = 1.0;
    public static final double MAX_HEAT = 32000;
    public double randomMovementProbability = 0.1;

    public int gridHeight;
    public int gridWidth;
    public int bugCount;
    HeatBug[] bugs;
    
    public double getMinimumIdealTemperature() { return minIdealTemp; }
    public void setMinimumIdealTemperature( double temp ) { if( temp <= maxIdealTemp ) minIdealTemp = temp; }
    public double getMaximumIdealTemperature() { return maxIdealTemp; }
    public void setMaximumIdealTemperature( double temp ) { if( temp >= minIdealTemp ) maxIdealTemp = temp; }
    public double getMinimumOutputHeat() { return minOutputHeat; }
    public void setMinimumOutputHeat( double temp ) { if( temp <= maxOutputHeat ) minOutputHeat = temp; }
    public double getMaximumOutputHeat() { return maxOutputHeat; }
    public void setMaximumOutputHeat( double temp ) { if( temp >= minOutputHeat ) maxOutputHeat = temp; }
    public double getEvaporationConstant() { return evaporationRate; }
    public void setEvaporationConstant( double temp ) { if( temp >= 0 && temp <= 1 ) evaporationRate = temp; }
    public Object domEvaporationConstant() { return new Interval(0.0,1.0); }
    public double getDiffusionConstant() { return diffusionRate; }
    public void setDiffusionConstant( double temp ) { if( temp >= 0 && temp <= 1 ) diffusionRate = temp; }
    public Object domDiffusionConstant() { return new Interval(0.0, 1.0); }
    public double getRandomMovementProbability() { return randomMovementProbability; }
        
    public double[] getBugXPos() {
        try
            {
            double[] d = new double[bugs.length];
            for(int x=0;x<bugs.length;x++)
                {
                d[x] = ((Int2D)(buggrid.getObjectLocation(bugs[x]))).x;
                }
            return d;
            }
        catch (Exception e) { return new double[0]; }
        }
    
    public double[] getBugYPos() {
        try
            {
            double[] d = new double[bugs.length];
            for(int x=0;x<bugs.length;x++)
                {
                d[x] = ((Int2D)(buggrid.getObjectLocation(bugs[x]))).y;
                }
            return d;
            }
        catch (Exception e) { return new double[0]; }
        }


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
    public Object domRandomMovementProbability() { return new Interval(0.0, 1.0); }
        
    public double getMaximumHeat() { return MAX_HEAT; }

    // we presume that no one relies on these DURING a simulation
    public int getGridHeight() { return gridHeight; }
    public void setGridHeight(int val) { if (val > 0) gridHeight = val; }
    public int getGridWidth() { return gridWidth; }
    public void setGridWidth(int val) { if (val > 0) gridWidth = val; }
    public int getBugCount() { return bugCount; }
    public void setBugCount(int val) { if (val >= 0) bugCount = val; }
    
    public DoubleGrid2D valgrid;
    public DoubleGrid2D valgrid2;
    public SparseGrid2D buggrid;

    /** Creates a HeatBugs simulation with the given random number seed. */
    public HeatBugs(long seed)
        {
        this(seed, 100, 100, 100);
        }
        
    public HeatBugs(long seed, int width, int height, int count)
        {
        super(seed);
        gridWidth = width; gridHeight = height; bugCount = count;
        createGrids();
        }

    protected void createGrids()
        {
        bugs = new HeatBug[bugCount];
        valgrid = new DoubleGrid2D(gridWidth, gridHeight,0);
        valgrid2 = new DoubleGrid2D(gridWidth, gridHeight, 0);
        buggrid = new SparseGrid2D(gridWidth, gridHeight);      
        }
    
    ThreadedDiffuser diffuser = null;
        
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        
        // make new grids
        createGrids();
    
        // Schedule the heat bugs -- we could instead use a RandomSequence, which would be faster
        // But we spend no more than 3% of our total runtime in the scheduler max, so it's not worthwhile
        
       
        for(int x=0;x<bugCount;x++)
            {
            bugs[x] = new HeatBug(random.nextDouble() * (maxIdealTemp - minIdealTemp) + minIdealTemp,
                random.nextDouble() * (maxOutputHeat - minOutputHeat) + minOutputHeat,
                randomMovementProbability);
            buggrid.setObjectLocation(bugs[x],random.nextInt(gridWidth),random.nextInt(gridHeight));
            schedule.scheduleRepeating(bugs[x]);
            }
                    
        // Here we're going to pick whether or not to use Diffuser (the default) or if
        // we're really going for the gusto and have multiple processors on our computer, we
        // can use our multithreaded super-neato ThreadedDiffuser! 
        
        // Beyond 3 threads we don't see any advantage on a small grid
        
        if (HeatBugs.availableProcessors() > 3)  // yay, multi-processor!
            {
            diffuser = new ThreadedDiffuser(3);
            schedule.scheduleRepeating(Schedule.EPOCH,1,diffuser,1);
            }
        else if (HeatBugs.availableProcessors() >= 2)
            {
            diffuser = new ThreadedDiffuser(2);
            schedule.scheduleRepeating(Schedule.EPOCH,1,diffuser,1);
            }
        else
            {
            schedule.scheduleRepeating(Schedule.EPOCH,1,new Diffuser(),1);
            }
        }
    
    public void stop()
        {
        if (diffuser != null) diffuser.cleanup();
        diffuser = null;
        }
    
    /** This little function calls Runtime.getRuntime().availableProcessors() if it's available,
        else returns 1.  That function is nonexistent in Java 1.3.1, but it exists in 1.4.x.
        So we're doing a little dance through the Reflection library to call the method tentatively!
        The value returned by Runtime is the number of available processors on the computer.  
        If you're only using 1.4.x, then all this is unnecessary -- you can just call
        Runtime.getRuntime().availableProcessors() instead. */
    public static int availableProcessors()
        {
        Runtime runtime = Runtime.getRuntime();
        try { return ((Integer)runtime.getClass().getMethod("availableProcessors", (Class[])null).
                invoke(runtime,(Object[])null)).intValue(); }
        catch (Exception e) { return 1; }  // a safe but sometimes wrong assumption!
        }

/*
// Would you like your model inspector to be broken into tabs?  Here's an example.
// To get this to work, uncomment the "implements sim.portrayal.inspector.Tabbable" at the top of this class
// See also the getInspector() method in HeatBugsWithUI 
public String[] provideTabNames()
{ return new String[] {"Temps", "Constants", "Pos"}; }
public String[][] provideTabProperties()
{ return new String[][] {{"MinimumIdealTemperature", "MaximumIdealTemperature", "MinimumOutputHeat", "MaximumOutputHeat", "MaximumHeat"},
{"EvaporationConstant", "DiffusionConstant", "RandomMovementProbability"},
{"BugXPos", "BugYPos"}}; }
public String provideExtraTab() { return "Extra"; }
*/ 
        
/*
// Would you like your model inspector instead to be broken into groups?  Here's an example.
// To get this to work, uncomment the "implements sim.portrayal.inspector.Groupable" at the top of this class
// and uncomment the following code.
// See also the getInspector() method in HeatBugsWithUI 
public String[] provideGroupNames()
{ return new String[] {"Temps", "Constants", "Pos"}; }
public String[][] provideGroupProperties()
{ return new String[][] {{"MinimumIdealTemperature", "MaximumIdealTemperature", "MinimumOutputHeat", "MaximumOutputHeat", "MaximumHeat"},
{"EvaporationConstant", "DiffusionConstant", "RandomMovementProbability"},
{"BugXPos", "BugYPos"}}; }
public boolean showExtraProperties() { return true; }
*/   

    
    /*
    // Would you like your model inspector instead to be broken into tabs AND gropus?  Here's an example.
    // To get this to work, uncomment the "implements sim.portrayal.inspector.TabbableAndGroupable" at the top of this class
    // See also the getInspector() method in HeatBugsWithUI 
    public String[] provideTabNames()
    { return new String[] {"Temps", "Constants", "Pos"}; }
    public String[][][] provideTabGroupProperties()
    { return new String[][][] {{{"MinimumIdealTemperature", "MaximumIdealTemperature"},
    {"MinimumOutputHeat", "MaximumOutputHeat"}, {"MaximumHeat"}},
    {{"EvaporationConstant", "DiffusionConstant", "RandomMovementProbability"}},
    {{"BugXPos", "BugYPos"}}}; }
    public String[][] provideTabGroups()
    { return new String[][] {{"Ideal Temperature",
    "Output Heat"},
    {},
    {"Bug Position"}}; }
                        
    public String provideExtraTab()
    { return "Misc"; }
    */

    
    public static void main(String[] args)
        {
        doLoop(HeatBugs.class, args);
        System.exit(0);
        }    



    public double[] assess(int numObjectives)
        {
        double[] assessment = new double[numObjectives];
        double[] xp = getBugXPos();
        double sum = 0;
        for(int i = 0; i < xp.length; i++)
            {
            sum += xp[i];
            }
        sum /= xp.length;
                
        double var = 0;
        for(int i = 0; i < xp.length; i++)
            {
            double d1 = Math.abs(sum - xp[i]);
            double d2 = Math.abs(100 - (sum - xp[i]));
            var += Math.min(d1, d2);
            }
        var /= xp.length;
                
        assessment[0] = 1 - var;
        System.err.println(var);
        return assessment;
        }

    }
    
    
    
    
    
