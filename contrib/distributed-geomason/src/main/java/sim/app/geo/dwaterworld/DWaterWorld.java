package sim.app.geo.dwaterworld;

import java.io.FileNotFoundException;



import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import sim.app.geo.waterworld.data.WaterWorldData;
//import sim.app.geo.dwaterworld.data.DWaterWorldData;
import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.geo.GeomGridField;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.field.grid.DObjectGrid2D;
import sim.field.grid.IntGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.util.Int2D;

public class DWaterWorld extends DSimState{
	
    DObjectGrid2D landscape;
    ArrayList<DRaindrop> drops;
    
    // landscape parameters
    static int grid_width = 285;
    static int grid_height = 143;
    private static final long serialVersionUID = 1L;



    /**
     * Constructor function.
     * @param seed
     */
    public DWaterWorld(long seed)
    {
        super(seed, grid_width, grid_height, 20, false);
    }



    /**
     * Starts a new run of the simulation. Refreshes the landscape and
     * schedules the addition of new Raindrops to the system.
     */
    @Override
    public void start()
    {
        super.start();

        // various options for setting up the landscape
        //landscape = setupLandscape(); // uniform landscape, completely flat
        //landscape = setupLandscapeGradientIn(); // landscape that slopes in
        landscape = setupLandscapeReadIn("elevation.txt.gz"); // read landscape from file
        //System.out.println("landscape : "+landscape);
        //System.exit(-1);
        
        drops = new ArrayList<DRaindrop>();

        schedule.scheduleRepeating(new DRaincloud());
//		schedule.scheduleRepeating(new Printout()); // printout of depth data
    }



    /**
     * set up the landscape to be of uniform height.
     * @return new landscape
     */
    DObjectGrid2D setupLandscape()
    {

        DObjectGrid2D result = new DObjectGrid2D(this);
        
        // go over the landscape and set up a basin at every grid point
        for (int i = 0; i < grid_width; i++)
        {
            for (int j = 0; j < grid_height; j++)
            {
    			if (getPartition().getLocalBounds().contains(new Int2D(i,j))) {
    				// initialize a Basin with elevation 0
    				DBasin b = new DBasin(i, j);
    				result.set(new Int2D(i, j), b);
    			}
            }
        }

        return result;
    }



    /**
     * Read in a landscape from a file.
     * @param filename - the name of the file that holds the landscape data
     * @return new landscape
     */
    DObjectGrid2D setupLandscapeReadIn(final String filename)
    {
        DObjectGrid2D result = null;

        try
        {
            //DGeomGridField elevationsField = new DGeomGridField();
        	GeomGridField elevationsField = new GeomGridField(); //I think we can just use the regular here, as mainly for loading data?

            InputStream inputStream = WaterWorldData.class.getResourceAsStream(filename);

            if (inputStream == null)
            {
                throw new FileNotFoundException(filename);
            }

            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(compressedInputStream, GridDataType.INTEGER, elevationsField);
            
            grid_width = elevationsField.getGridWidth();
            grid_height = elevationsField.getGridHeight();
            
            result = new DObjectGrid2D<DBasin>(this);

            IntGrid2D elevations = (IntGrid2D) elevationsField.getGrid(); //again, can do this because only for setup from my understanding
            //DDoubleGrid2D elevations2 = (DDoubleGrid2D) elevationsField.getGrid();
            
            
            for (int x = 0; x < grid_width; x++)
            {
                for (int y = 0; y < grid_height; y++)
                {
                    DBasin b = new DBasin(x, y, (int)elevations.get(x,y));
                    //result.set(x, y, b);
                	result.set(new Int2D(x, y), b) ;

                }
            }

        } // if it messes up, print out the error
        catch (Exception e)
        {
            System.out.println(e);
        }

        return result;
    }



    /**
     *  set up the landscape to be shaped like a funnel, so that Raindrops move
     *  from the outside of the landscape to the center.
     * @return new landscape
     */
    DObjectGrid2D setupLandscapeGradientIn()
    {

        DObjectGrid2D result = new DObjectGrid2D(this);

        // saved to avoid recalculating the same numbers for every basin
        int hwidth = grid_width / 2;
        int hheight = grid_height / 2;

        // iterate over every Basin and set its height equal to its distance from the center
        for (int i = 0; i < grid_width; i++)
        {
            for (int j = 0; j < grid_height; j++)
            {

                // set its height to either the distance...
                int height = (int) Math.sqrt(Math.pow(i - hwidth, 2) + Math.pow(j - hheight, 2));
                // of the square of its distance to the center
                //int height = (int) ( Math.pow(i - hwidth, 2) + Math.pow (j - hheight, 2));

                DBasin b = new DBasin(i, j, height);
                result.set(new Int2D(i, j), b);
            }
        }

        return result;
    }



    /**
     *  a Steppable that adds new Raindrops to the simulation
     */
    class DRaincloud extends DSteppable
    {

        // the number of drops added to the simulation per tick
        int numDropsPerTurn = 500;



        public void step(SimState state)
        {

            // to dump all raindrops into the center of the graph, uncomment here
            //int w = grid_width / 2, h = grid_height / 2;

            // randomly select a tile and add a new raindrop to it
            for (int i = 0; i < numDropsPerTurn; i++)
            {

                // select a random tile on the landscape
                //int x = random.nextInt(grid_width), y = random.nextInt(grid_height);
                int x = random.nextInt(grid_width), y = random.nextInt(grid_height);

                // to dump all raindrops into the center of the graph, uncomment here
                //x = w; y = h;

                //DBasin b = (DBasin) landscape.get(new Int2D(x, y));
                DBasin b = (DBasin) landscape.getLocal(new Int2D(x, y));

                // set up a new raindrop and add it to the tile
                DRaindrop r = new DRaindrop((DWaterWorld) state, landscape, b);
                
                Stoppable stopper = schedule.scheduleRepeating(r);
                //Instead add this to a field? maybe?
                		
                r.stopper = stopper; // give the raindrop the ability to stop itself
                ((DWaterWorld) state).drops.add(r);
                b.addDrop(r);
            }

        }

    }



    /**
     *  prints out a report about either the depth of the water or the overall elevation
     */
    class Printout extends DSteppable
    {

        public void step(SimState state)
        {

            String output = "";

            for (int i = 0; i < grid_width; i++)
            {
                for (int j = 0; j < grid_height; j++)
                {

                    // setting for measuring water depth...
                    output += ((DBasin) landscape.get(new Int2D(i, j))).drops.size()
                        // setting for measuring overall elevation
                        //output += ((Basin)landscape.get(i, j)).baseheight
                        + "\t";
                }
                output += "\n";
            }

            System.out.println("STEP " + schedule.getSteps() + "\n" + output);
        }

    }
	
	public static void main(final String[] args)
	{
		doLoopDistributed(DWaterWorld.class, args);
		System.exit(0);
	}

}
