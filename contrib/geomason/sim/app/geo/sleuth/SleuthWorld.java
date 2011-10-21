/*

SleuthWorld.java

Copyright 2011 by Andrew Crooks, Sarah Wise, Mark Coletti, and
George Mason University Mason University.

Licensed under the Academic Free License version 3.0

See the file "LICENSE" for more information

*/
package sim.app.geo.sleuth;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;



/**
 * The SleuthWorld simulation core.
 * 
 * The simulation can require a LOT of memory, so make sure the virtual machine has enough.
 * Do this by adding the following to the command line, or by setting up your run 
 * configuration in Eclipse to include the VM argument:
 * 
 * 		-Xmx2048M
 * 
 * With smaller simulations this chunk of memory is obviously not necessary. You can 
 * take it down to -Xmx800M or some such. If you get an OutOfMemory error, push it up.
 */
public class SleuthWorld extends SimState
{

    ObjectGrid2D landscape;
    ArrayList<Tile> spreadingCenters = new ArrayList<Tile>();
    // model parameters
    double dispersionCoefficient = 5; // maximally 100?



    public double getDispersionCoefficient()
    {
        return dispersionCoefficient;
    }



    public void setDispersionCoefficient(double val)
    {
        dispersionCoefficient = val;
    }

    double breedCoefficient = 5;



    public double getBreedCoefficient()
    {
        return breedCoefficient;
    }



    public void setBreedCoefficient(double val)
    {
        breedCoefficient = val;
    }

    double spreadCoefficient = 5;



    public double getSpreadCoefficient()
    {
        return spreadCoefficient;
    }



    public void setSpreadCoefficient(double val)
    {
        spreadCoefficient = val;
    }

    double slopeCoefficient = 5;



    public double getSlopeCoefficient()
    {
        return slopeCoefficient;
    }



    public void setSlopeCoefficient(double val)
    {
        slopeCoefficient = val;
    }

    double roadGravityCoefficient = 1;



    public double getRoadGravityCoefficient()
    {
        return roadGravityCoefficient;
    }



    public void setRoadGravityCoefficient(double val)
    {
        roadGravityCoefficient = val;
    }

    int maxCoefficient = 100;
    double maxRoadValue = 4;
    // landscape parameters
    int grid_width = 100;
    int grid_height = 100;
    // cheap way to visualize
    int numUrban = 0;
    int numNonUrban = 0;
    private static final long serialVersionUID = 1L;



    /**
     * Constructor function.
     * @param seed
     */
    public SleuthWorld(long seed)
    {
        super(seed);
    }



    /**
     * Starts a new run of the simulation. Reads in the data and schedules
     * the growth rules to fire every turn.
     */
    public void start()
    {
        super.start();

        // --- read in the various layers of data ---

        // slope
        System.out.println("Reading in slope data...");
        setupLandscapeFromFileDoubles("../../data/sleuth/reclass_slope.txt", 1);

        // land use
        System.out.println("Reading in land use data...");
        setupLandscapeFromFileDoubles("../../data/sleuth/landuse.txt", 2);

        // excluded
        System.out.println("Reading in excluded area data...");
        setupLandscapeFromFileBoolean("../../data/sleuth/excluded.txt", 3);

        // urban
        System.out.println("Reading in urban area data...");
        setupLandscapeFromFileBoolean("../../data/sleuth/urban.txt", 4);

        // transport
        System.out.println("Reading in transport data...");
        setupLandscapeFromFileDoubles("../../data/sleuth/roads_0_1.txt", 5);

        // hillshade
        System.out.println("Reading in hillshade data...");
        setupLandscapeFromFileDoubles("../../data/sleuth/hillshade.txt", 6);

        System.out.println("Successfully read in all data!");

        // -- process the data for efficiency and convenience --
        for (int i = 0; i < grid_width; i++)
        {
            for (int j = 0; j < grid_height; j++)
            {

                Tile t = (Tile) landscape.get(i, j);

                // remove all tiles that are not part of the simulation
                if (t.slope == Integer.MIN_VALUE)
                {
                    landscape.set(i, j, null);
                    continue;
                } // compile a list of all "spreading urban center" tiles
                else if (t.urbanized)
                {

                    numUrban++;

                    int numUrbanizedNeighbors = getUrbanNeighbors(t).size();
                    if (numUrbanizedNeighbors > 1 && numUrbanizedNeighbors < 6)
                    {
                        spreadingCenters.add(t);
                    }
                } else
                {
                    numNonUrban++;
                }
            }
        }

        // schedule the growth cycle to happen every time step
        Steppable grower = new Grower();
        schedule.scheduleRepeating(grower);
    }



    /** Implements the Sleuth growth rules in the appropriate order */
    class Grower implements Steppable
    {

        @Override
        public void step(SimState state)
        {

            // calculate the coefficients
            double dispersion_value = ((dispersionCoefficient * 0.005)
                                       * Math.sqrt(grid_width * grid_width + grid_height * grid_height));

            double rg_value = (roadGravityCoefficient / maxCoefficient)
                * ((grid_width + grid_height) / 16.0);

            double max_search_index = 4 * (rg_value * (1 + rg_value));

            // spontaneously urbanizes cells with some probability
            ArrayList<Tile> spontaneouslyUrbanized = spontaneousGrowth(dispersion_value);

            // determines whether any of the new, spontaneously urbanized cells will
            // become new urban spreading centers. If the cell is allowed to become
            // a spreading center, two additional cells adjacent to the new spreading
            // center cell also have to be urbanized
            ArrayList<Tile> spreadFromUrbanized =
                newSpreadingCenters(spontaneouslyUrbanized);

            // growth propagates both the new centers generated in newSpreadingCenters
            // in this time step and the more established centers from earlier Steps
            ArrayList<Tile> growthAroundCenters = edgeGrowth();

            // compile a list of all cells urbanized this turn
            ArrayList<Tile> allGrowthThisTurn = new ArrayList<Tile>();
            allGrowthThisTurn.addAll(spontaneouslyUrbanized);
            allGrowthThisTurn.addAll(spreadFromUrbanized);
            allGrowthThisTurn.addAll(growthAroundCenters);

            // newly urbanized cells search for nearby roads. If they encounter them,
            // they build on this infrastructure by establishing a new urban area
            // some random walk along the road away from themselves. If this area is
            // prime for urbanization, two further neighbors of our new roadside cell
            // are urbanized.
            roadInfluencedGrowth(max_search_index, allGrowthThisTurn);

        }

    }

    //
    // --- GROWTH RULES ---
    //


    /**
     * any non-urbanized cell on the lattice has a certain (small) probability
     * of becoming urbanized in any time step.
     * @param dispersion_value - the dispersion value
     * @return a list of Tiles that have been spontaneously urbanized
     */
    ArrayList<Tile> spontaneousGrowth(double dispersion_value)
    {

        ArrayList<Tile> urbanized = new ArrayList<Tile>();

        for (int i = 0; i < dispersion_value; i++)
        {
            // find a tile that is a part of the simulation
            int x = random.nextInt(grid_width), y = random.nextInt(grid_height);
            while (landscape.get(x, y) == null)
            {
                x = random.nextInt(grid_width);
                y = random.nextInt(grid_height);
            }

            Tile t = (Tile) landscape.get(x, y);
            // try to urbanize t!
            if (t.urbanized)
            {
                continue; // already urbanized
            } else if (t.excluded)
            {
                continue; // excluded from urbanization
            } else
            { // urbanize the tile!
                Tile newlyUrbanized = (Tile) landscape.get(x, y);
                boolean successful = urbanizeTile(newlyUrbanized);
                if (successful)
                {
                    urbanized.add(newlyUrbanized);
                }
            }
        }

        return urbanized;

    }



    /**
     * Spontaneously urbanized cells can be new urban centers. With some probability,
     * these spontaneous cells will urbanize two of their unurbanized neighbors.
     * @param newlyUrbanized - the list of Tiles that were just spontaneously
     * urbanized
     * @return the list of all Tiles that grew up around these new centers
     */
    ArrayList<Tile> newSpreadingCenters(ArrayList<Tile> newlyUrbanized)
    {

        ArrayList<Tile> spreadFromNewlyUrbanized = new ArrayList<Tile>();

        // go through all of the newly urbanized tiles
        for (Tile t : newlyUrbanized)
        {

            // with some probability, try to spread out from them
            if (random.nextInt(maxCoefficient) < breedCoefficient)
            {

                // assemble a list of unurbanized neighbors
                ArrayList<Tile> potential = getNeighborsAvailableForUrbanization(t);

                // if there are at least 2 unurbanized neighbors, urbanize 2 of them!
                if (potential.size() > 1)
                {
                    for (int i = 0; i < 2; i++)
                    {
                        Tile toUrbanize = potential.remove(
                            random.nextInt(potential.size()));
                        boolean successful = urbanizeTile(toUrbanize);
                        if (successful)
                        {
                            spreadFromNewlyUrbanized.add(toUrbanize);
                        }
                    }

                    // the central Tile is now an urban center
                    spreadingCenters.add(t);
                }
            }
        }

        return spreadFromNewlyUrbanized;
    }



    /**
     * Urban centers tend to fill in. Consider the neighbors of urban centers, e.g.
     * cells that have at least 3 neighboring urbanized cells. With some probability,
     * urbanize these.
     * @return the list of newly urbanized Tiles
     */
    ArrayList<Tile> edgeGrowth()
    {

        ArrayList<Tile> newlyUrbanized = new ArrayList<Tile>();

        ArrayList<Tile> centers = new ArrayList<Tile>(spreadingCenters);

        // go through urban centers, potentially grow their neighboring unurbanized cells
        for (Tile t : centers)
        {

            // with some probability, spread out from this
            if (random.nextInt(maxCoefficient) < spreadCoefficient)
            {

                // get neighbors that can be urbanized
                ArrayList<Tile> suitableForUrbanization =
                    getNeighborsAvailableForUrbanization(t);
                if (suitableForUrbanization.size() > 0)
                {

                    Tile toUrbanize = suitableForUrbanization.get(
                        random.nextInt(suitableForUrbanization.size()));

                    // urbanize one such randomly selected Tile
                    boolean successful = urbanizeTile(toUrbanize);

                    if (successful)
                    {
                        newlyUrbanized.add(toUrbanize);
                    }
                }
            }
        }

        return newlyUrbanized;
    }



    /**
     * Growth often spreads along the transportation network. For all of the cells
     * which have just been urbanized, check if there is a road within a given
     * distance of them. If so, walk along that road and potentially establish a
     * new urban center. If the urban center can spread, do so this turn.
     * @param max_search_index - the maximum distance from the Tile within which
     * roads are considered
     * @param recentlyUrbanized - the list of all Tiles urbanized this turn
     */
    void roadInfluencedGrowth(double max_search_index,
                              ArrayList<Tile> recentlyUrbanized)
    {

        // go through all Tiles that were urbanized this turn and possibly spread along
        // nearby roads
        for (Tile t : recentlyUrbanized)
        {

            // do so with some probability
            if (random.nextInt(maxCoefficient) < breedCoefficient)
            {

                // check if there is a road within a given distance of the newly
                // urbanized Tile
                ArrayList<Tile> neighboringRoads =
                    getNeighborsTransport(t, (int) max_search_index);
                if (neighboringRoads.size() > 0)
                {

                    // if so, do a random walk along the road with number of steps
                    // depending on the weight of that road
                    Tile bordersRoad = neighboringRoads.get(0);

                    // calculate the number of steps the test will random walk
                    // along the road, based on the caliber of road on which
                    // it initially finds itself
                    double run_value = (dispersionCoefficient
                                        * (maxRoadValue - bordersRoad.transport + 1) / maxRoadValue);
                    Tile finalPoint = walkAlongRoad(bordersRoad, (int) run_value);

                    // at the place we finally end up, see if there are any neighbors
                    // available for urbanization. If so, urbanize one.
                    ArrayList<Tile> potential =
                        getNeighborsAvailableForUrbanization(finalPoint);

                    if (potential.size() == 0)
                    {
                        continue; // no neighbors available
                    }
                    Tile newUrbanized = potential.get(random.nextInt(potential.size()));
                    boolean successful = urbanizeTile(newUrbanized);

                    if (!successful)
                    {
                        continue; // it didn't take, so we don't check
                    }					// the neighbors of this failed urbanization attempts

                    // check and see if this newly urbanized Tile has neighbors to
                    // urbanize. If it has at least two, urbanize two randomly selected
                    // neighbors
                    ArrayList<Tile> neighbors =
                        getNeighborsAvailableForUrbanization(newUrbanized);
                    if (neighbors.size() > 1)
                    {
                        for (int i = 0; i < 2; i++)
                        {
                            Tile neighbor = neighbors.remove(
                                random.nextInt(neighbors.size()));
                            urbanizeTile(neighbor);
                        }
                    }
                }
            }
        }
    }

    //
    // --- END GROWTH RULES ---
    //

    //
    // --- HELPFUL UTILITIES ---
    //

    /**
     * Takes a point of origin and randomly walks along the connected roads for the given
     * number of steps
     * @param origin - the Tile from which the random walk begins
     * @param numSteps - the number of random steps taken
     * @return the Tile where the random walk terminates
     */
    Tile walkAlongRoad(Tile origin, int numSteps)
    {
        if (origin.transport < 1)
        {
            return null; // NOT A ROAD, SILLY
        }
        Tile result = origin;

        ArrayList<Tile> neighbors;

        // for the number of steps, move to random connected road segments
        for (int i = 0; i < numSteps; i++)
        {
            neighbors = getNeighborsTransport(result, 1);
            if (neighbors.isEmpty())
            {
                return result;
            }

            // move to a random neighboring road segment
            result = neighbors.get(random.nextInt(neighbors.size()));
        }

        return result;
    }



    /**
     * Tidily urbanizes a tile, updating the list of edge Tiles given the Tile's new
     * status and its new, potentially now edge-y neighbors.
     * @param t - the Tile to urbanize
     */
    boolean urbanizeTile(Tile t)
    {

        if (t.excluded || t.slope > 21)
        { // can't urbanize a Tile with slope > 21
            System.out.println("Error: can't urbanize a Tile with slope > 21");

            // unsuccessful urbanization
            return false;
        }

        t.urbanized = true;
        t.landuse = 1; // set the landuse to urban
        numUrban++;

        // if this Tile qualifies as a city center, make it so!
        ArrayList<Tile> urbanizedNeighbors = getUrbanNeighbors(t);
        if (urbanizedNeighbors.size() > 2)
        {
            spreadingCenters.add(t);
        }

        // check to see if the urbanization of this Tile has made any of its neighbors
        // into centers
        for (Tile n : urbanizedNeighbors)
        {

            // check to see if we've already found this Tile
            if (spreadingCenters.contains(n))
            {
                continue;
            }

            // otherwise, if this Tile now has enough urbanized neighbors to be an urban
            // center, make it so!
            ArrayList<Tile> neighborsUrbanizedNeighbors = getUrbanNeighbors(n);
            // need to be at least 2 and if it's completely surrounded that's not very
            // useful
            if (neighborsUrbanizedNeighbors.size() > 1
                && neighborsUrbanizedNeighbors.size() < 6)
            {
                spreadingCenters.add(n);
            }
        }

        // successfully urbanized cell
        return true;
    }



    /**
     * Sets up the landscape full of tiles
     * @param width - the width of the landscape
     * @param height - the height of the landscape
     */
    void setupLandscape(int width, int height)
    {
        grid_width = width;
        grid_height = height;

        landscape = new ObjectGrid2D(width, height);

        // populate the new landscape with new Tiles
        System.out.print("\nInitializing landscape...");
        for (int i = 0; i < width; i++)
        {
            if (i % 500 == 0)
            {
                System.out.print(".");
            }
            for (int j = 0; j < height; j++)
            {
                landscape.set(i, j, new Tile(i, j));
            }
        }
        System.out.println("completed!");

    }



    /**
     * Get the neighbors of the given Tile, specifically the urbanized or unurbanized
     * neighbors depending on the boolean value passed.
     * @param t - the Tile in question
     * @param urbanized - whether you want urbanized or unurbanized neighbors. E.g.
     * 		true means you want urbanized neighbors, false gives you unurbanized neighbors
     * @return returns the neighbors of the Tile t given the type passed to it
     */
    ArrayList<Tile> getUrbanNeighbors(Tile t)
    {

        ArrayList<Tile> result = new ArrayList<Tile>();
        Bag neighbors = new Bag();
        landscape.getNeighborsMaxDistance(t.x, t.y, 1, false,
                                          neighbors, null, null);

        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            }
            Tile n = (Tile) o;
            if (n.urbanized)
            {
                result.add(n);
            }
        }

        return result;
    }



    /**
     * Get the urbanizable neighbors of the given Tile, that is unurbanized neighbors
     * with acceptable slopes
     * @param t - the Tile in question
     * @return returns the appropriate neighbors of the Tile t
     */
    ArrayList<Tile> getNeighborsAvailableForUrbanization(Tile t)
    {

        ArrayList<Tile> result = new ArrayList<Tile>();
        Bag neighbors = new Bag();
        landscape.getNeighborsMaxDistance(t.x, t.y, 1, false,
                                          neighbors, null, null);

        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            }
            Tile n = (Tile) o;

            // if the tile hasn't been urbanized and its slope is within an acceptable
            // level, add it!
            if (n.urbanized == false && n.slope <= 21 && !n.excluded)
            {
                result.add(n);
            }
        }

        return result;
    }



    /**
     * Returns the neighbors of a given Tile where there are roads
     * @param t - the Tile around which we want to search
     * @param dist - the maximal distance from the Tile to search
     * @return the set of transport-enabled tiles within the search radius of the Tile t
     */
    ArrayList<Tile> getNeighborsTransport(Tile t, int dist)
    {
        ArrayList<Tile> result = new ArrayList<Tile>();
        Bag neighbors = new Bag();
        landscape.getNeighborsMaxDistance(t.x, t.y, dist, false,
                                          neighbors, null, null);
        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            } else if (((Tile) o).transport > 0)
            {
                result.add((Tile) o);
            }
        }


        return result;
    }


    //

    //
    // --- END HELPFUL UTILITIES ---
    //
    //
    // --- READING IN DATA FROM FILES ---
    //
    /**
     * Read in the boolean portions of the landscape from a file.
     * @param filename - the name of the file that holds the exclusion data
     */
    void setupLandscapeFromFileBoolean(String filename, int parameter)
    {

        try
        { // to read in a file

            // Open the file
            FileInputStream fstream = new FileInputStream(filename);

            // Convert our input stream to a BufferedReader
            BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

            // get the parameters from the file
            String s;
            int height = 0, width = 0;
            int nodata = -1;
            for (int i = 0; i < 6; i++)
            {

                s = d.readLine();

                // format the line appropriately
                String[] parts = s.split(" ", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if (i == 1)
                {
                    height = Integer.parseInt(trimmed);
                } else if (i == 0)
                {
                    width = Integer.parseInt(trimmed);
                } else if (i == 5)
                {
                    nodata = Integer.parseInt(trimmed);
                } else
                {
                    continue;
                }
            }

            // if the landscape has not already been set up, do so. Otherwise, ensure
            // that the data in this file are in the same format at the data we have
            // already read in
            if (landscape == null)
            {
                setupLandscape(width, height);
            } else if (landscape.getHeight() != height || landscape.getWidth() != width)
            {
                System.out.println("ERROR: incorrect grid height and width "
                    + "passed in " + filename);
                System.exit(0);
            }


            // read in the data from the file and store it in tiles
            int i = 0, j = 0;
            while ((s = d.readLine()) != null)
            {
                String[] parts = s.split(" ");

                for (String p : parts)
                {
                    int value = Integer.parseInt(p);
                    boolean tval = true;
                    if (value == nodata) // no positive match
                    {
                        tval = false;
                    }

                    Tile t = (Tile) landscape.get(j, i);

                    // update t's appropriate parameter
                    if (parameter == 3) // excluded
                    {
                        t.excluded = tval;
                    } else if (parameter == 4)
                    { // urban

                        // 1 is not urbanized, it is something else somehow? "not off screen"?
                        if (value == 1)
                        {
                            tval = false;
                        }
                        t.urbanized = tval;
                        t.urbanOriginally = tval;
                    }

                    j++; // increase the column count
                }

                j = 0; // reset the column count
                i++; // increase the row count
            }

        } // if it messes up, print out the error
        catch (Exception e)
        {
            System.out.println(e);
        }
    }



    /**
     * Read in the double values of the landscape from a file.
     * @param filename - the name of the file that holds the data
     */
    void setupLandscapeFromFileDoubles(String filename, int parameter)
    {

        try
        { // to read in a file

            // Open the file
            FileInputStream fstream = new FileInputStream(filename);

            // Convert our input stream to a BufferedReader
            BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

            // get the parameters from the file
            String s;
            int width = 0, height = 0;
            int nodata = -1;
            for (int i = 0; i < 6; i++)
            {

                s = d.readLine();

                // format the line appropriately
                String[] parts = s.split(" ", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if (i == 1)
                {
                    height = Integer.parseInt(trimmed);
                } else if (i == 0)
                {
                    width = Integer.parseInt(trimmed);
                } else if (i == 5)
                {
                    nodata = Integer.parseInt(trimmed);
                } else
                {
                    continue;
                }
            }

            // if the landscape has not already been set up, do so. Otherwise, ensure
            // that the data in this file are in the same format at the data we have
            // already read in
            if (landscape == null)
            {
                setupLandscape(width, height);
            } else if (landscape.getHeight() != height || landscape.getWidth() != width)
            {
                System.out.println("ERROR: incorrect grid height and width "
                    + "passed in " + filename);
                System.exit(0);
            }


            // read in the data from the file and store it in tiles
            int i = 0, j = 0;
            while ((s = d.readLine()) != null)
            {
                String[] parts = s.split(" ");

                for (String p : parts)
                {

                    int value = Integer.parseInt(p);
                    if (value == nodata) // mark the tile as having no value
                    {
                        value = Integer.MIN_VALUE;
                    }

                    Tile t = (Tile) landscape.get(j, i);

                    // update t's appropriate parameter
                    if (parameter == 1) // slope
                    {
                        t.slope = value;
                    } else if (parameter == 2) // landuse
                    {
                        t.landuse = value;
                    } else if (parameter == 5) // transport
                    {
                        t.transport = value;
                    } else if (parameter == 6) // hillshade
                    {
                        t.hillshade = value;
                    }

                    j++; // increase the column count
                }

                j = 0; // reset the column count
                i++; // increase the row count
            }

        } // if it messes up, print out the error
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    //
    // --- END READING IN DATA FROM FILES ---
    //


    /**
     * Main function, runs the simulation without any visualization.
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(SleuthWorld.class, args);
        System.exit(0);
    }

}