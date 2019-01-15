/**
 ** WaterWorld.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **
 **/
package sim.app.geo.waterworld;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;



/**
 * The WaterWorld simulation core.
 */
public class WaterWorld extends SimState
{

    ObjectGrid2D landscape;
    ArrayList<Raindrop> drops;
    
    // landscape parameters
    int grid_width = 100;
    int grid_height = 100;
    private static final long serialVersionUID = 1L;



    /**
     * Constructor function.
     * @param seed
     */
    public WaterWorld(long seed)
    {
        super(seed);
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
        landscape = setupLandscapeReadIn("data/elevation.txt.gz"); // read landscape from file

        drops = new ArrayList<Raindrop>();

        schedule.scheduleRepeating(new Raincloud());
//		schedule.scheduleRepeating(new Printout()); // printout of depth data
    }



    /**
     * set up the landscape to be of uniform height.
     * @return new landscape
     */
    ObjectGrid2D setupLandscape()
    {

        ObjectGrid2D result = new ObjectGrid2D(grid_width, grid_height);

        // go over the landscape and set up a basin at every grid point
        for (int i = 0; i < grid_width; i++)
        {
            for (int j = 0; j < grid_height; j++)
            {

                // initialize a Basin with elevation 0
                Basin b = new Basin(i, j);
                result.set(i, j, b);
            }
        }

        return result;
    }



    /**
     * Read in a landscape from a file.
     * @param filename - the name of the file that holds the landscape data
     * @return new landscape
     */
    ObjectGrid2D setupLandscapeReadIn(final String filename)
    {
        ObjectGrid2D result = null;

        try
        {
            GeomGridField elevationsField = new GeomGridField();

            InputStream inputStream = WaterWorld.class.getResourceAsStream(filename);

            if (inputStream == null)
            {
                throw new FileNotFoundException(filename);
            }

            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(compressedInputStream, GridDataType.INTEGER, elevationsField);
            
            grid_width = elevationsField.getGridWidth();
            grid_height = elevationsField.getGridHeight();
            
            result = new ObjectGrid2D(grid_width, grid_height);

            IntGrid2D elevations = (IntGrid2D) elevationsField.getGrid();
            
            
            for (int x = 0; x < grid_width; x++)
            {
                for (int y = 0; y < grid_height; y++)
                {
                    Basin b = new Basin(x, y, elevations.get(x, y));
                    result.set(x, y, b);
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
    ObjectGrid2D setupLandscapeGradientIn()
    {

        ObjectGrid2D result = new ObjectGrid2D(grid_width, grid_height);

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

                Basin b = new Basin(i, j, height);
                result.set(i, j, b);
            }
        }

        return result;
    }



    /**
     *  a Steppable that adds new Raindrops to the simulation
     */
    class Raincloud implements Steppable
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
                int x = random.nextInt(grid_width), y = random.nextInt(grid_height);

                // to dump all raindrops into the center of the graph, uncomment here
                //x = w; y = h;

                Basin b = (Basin) landscape.get(x, y);

                // set up a new raindrop and add it to the tile
                Raindrop r = new Raindrop((WaterWorld) state, landscape, b);
                Stoppable stopper = schedule.scheduleRepeating(r);
                r.stopper = stopper; // give the raindrop the ability to stop itself
                ((WaterWorld) state).drops.add(r);
                b.addDrop(r);
            }

        }

    }



    /**
     *  prints out a report about either the depth of the water or the overall elevation
     */
    class Printout implements Steppable
    {

        public void step(SimState state)
        {

            String output = "";

            for (int i = 0; i < grid_width; i++)
            {
                for (int j = 0; j < grid_height; j++)
                {

                    // setting for measuring water depth...
                    output += ((Basin) landscape.get(i, j)).drops.size()
                        // setting for measuring overall elevation
                        //output += ((Basin)landscape.get(i, j)).baseheight
                        + "\t";
                }
                output += "\n";
            }

            System.out.println("STEP " + schedule.getSteps() + "\n" + output);
        }

    }



    /**
     * Main function, runs the simulation without any visualization.
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(WaterWorld.class, args);
        System.exit(0);
    }

}
