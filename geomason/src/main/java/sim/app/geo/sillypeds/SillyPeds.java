/**
 ** SillyPeds.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.sillypeds;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import sim.engine.SimState;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.io.geo.ArcInfoASCGridImporter;



/**
 * The SillyPeds simulation core.
 */
public class SillyPeds extends SimState
{
    private static final long serialVersionUID = 1L;


    /** A land scape is comprised of more than one space for pedestrian movement
     * 
     */
    ArrayList<Space> landscape;
    
    /** The set of pedestrians
     * 
     */
    ArrayList<Pedestrian> peds;

    /** How many pedestrians we want
     *
     */
    int initialAgentsPerFloor = 1000;



    /**
     * Constructor function.
     * @param seed
     */
    public SillyPeds(long seed)
    {
        super(seed);
    }



    void scenarioSimpleRoom()
    {
        landscape = new ArrayList<Space>();

        // Read in the exit gradient corresponding to a single building floor
        Space floor = setupLandscapeSpace("data/first.txt.gz");

        landscape.add(floor);

        // set up the exits
        Space room = landscape.get(0);

        // Exits are denoted by a zero.  So scan through all the tiles and flag
        // those grid locations that have a base height of zero as exits.
        for (Tile t : room.validTiles)
        {
            if (t.baseheight == 0)
            {
                t.makeExit();
                room.exits.put(t, null);
            }
        }
    }



    /**
     * Starts a new run of the simulation. Sets up the landscape and adds
     * Pedestrians to every floor.
     */
    @Override
    public void start()
    {
        super.start();

        //
        // read landscape from file
        //

        // SIMPLE SCENARIO: Simple Rooms
        scenarioSimpleRoom();

        //
        // add Pedestrians to the landscape
        //

        peds = new ArrayList<Pedestrian>();
        for (Space s : landscape)
        {
            boolean addAll = peds.addAll(s.populate(this, initialAgentsPerFloor));
        }

    }




    /**
     * Read in a landscape from a file
     * 
     * @param filename - the file containing gradients denoting exit paths
     * @return newly created Space
     */
    Space setupLandscapeSpace(final String filename)
    {
        Space result = null;

        try
        {
            GeomGridField floorPlan = new GeomGridField();

            InputStream inputStream = SillyPeds.class.getResourceAsStream(filename);

            if (inputStream == null)
            {
                throw new FileNotFoundException(filename);
            }

            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(compressedInputStream, GridDataType.DOUBLE, floorPlan);

            result = new Space(floorPlan);
        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        return result;
    }



    /**
     * Main function, runs the simulation without any visualization.
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(SillyPeds.class, args);
        System.exit(0);
    }

}
