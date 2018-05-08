/**
 ** SleuthWorld.java
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
package sleuth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import sim.engine.SimState;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;
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
    private static final String EXCLUDED_DATA_FILE_NAME = "data/excluded.txt.gz";
    private static final String HILLSIDE_DATA_FILE_NAME = "data/hillshade.txt.gz";
    private static final String LAND_USE_DATA_FILE_NAME = "data/landuse.txt.gz";
    private static final String SLOPE_DATA_FILE_NAME = "data/reclass_slope.txt.gz";
    private static final String TRANSPORT_DATA_FILE_NAME = "data/roads_0_1.txt.gz";
    private static final String URBAN_AREA_DATA_FILE_NAME = "data/urban.txt.gz";

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
    @Override
    public void start()
    {
        try
        {
            super.start();

            readSlopeData(); // Also create initial landscape from slope data

            readLandUseData();

            readExcludedAreaData();

            readUrbanAreaData();

            readTransportData();

            readHillShadeData();


            System.out.println("Successfully read in all data!");

            // Now count the initial total urban and non-urban areas.
            for (int i = 0; i < grid_width; i++)
            {
                for (int j = 0; j < grid_height; j++)
                {

                    Tile tile = (Tile) landscape.get(i, j);

                    if (tile != null && tile.urbanized)
                    {

                        numUrban++;

                        int numUrbanizedNeighbors = getUrbanNeighbors(tile).size();
                        if (numUrbanizedNeighbors > 1 && numUrbanizedNeighbors < 6)
                        {
                            spreadingCenters.add(tile);
                        }
                    } else
                    {
                        numNonUrban++;
                    }
                }
            }

            // schedule the growth cycle to happen every time step
            Grower grower = new Grower();

            schedule.scheduleRepeating(grower);
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(SleuthWorld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    private void readData(GeomGridField excludedGridField, final String fileName) throws FileNotFoundException
    {
        InputStream inputStream = SleuthWorld.class.getResourceAsStream(fileName);

        if (inputStream == null)
        {
           throw new FileNotFoundException(fileName);
        }

        try
        {
            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(compressedInputStream, GridDataType.INTEGER, excludedGridField);

        } catch (IOException ex)
        {
            Logger.getLogger(SleuthWorld.class.getName()).log(Level.SEVERE, null, ex);

            System.exit(-1);
        }
    }



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
                if (! neighboringRoads.isEmpty() )
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

                    if (potential.isEmpty())
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
    void setupLandscape(final GeomGridField slope)
    {
        grid_width = slope.getGridWidth();
        grid_height = slope.getGridHeight();

        landscape = new ObjectGrid2D(grid_width, grid_height);

        // populate the new landscape with new Tiles
        System.out.print("\nInitializing landscape...");
        
        for (int i = 0; i < grid_width; i++)
        {
            if (i % 500 == 0)
            {
                System.out.print(".");
            }

            for (int j = 0; j < grid_height; j++)
            {
                // -9999 means "no data"
                if ( ((IntGrid2D)slope.getGrid()).get(i, j) != -9999)
                {
                    Tile tile = new Tile(i,j);
                    tile.slope = ((IntGrid2D)slope.getGrid()).get(i, j);

                    landscape.set(i, j, tile);
                }
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


    
    /**
     * Main function, runs the simulation without any visualization.
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(SleuthWorld.class, args);
        System.exit(0);
    }





    private void readSlopeData() throws FileNotFoundException
    {
        System.out.println("Reading slope data ...");

        // Let's read in all the slope data first.  Not only will that give us
        // the dimensions, but we can use that to determine where to put new tiles.

        GeomGridField slopeField = new GeomGridField();

        readData(slopeField, SLOPE_DATA_FILE_NAME);

        // Now setup this.landscape
        setupLandscape(slopeField);
    }


    /**
     * The land use flags each tile with one of four classifications.
     *
     * (And what are these?)
     */
    private void readLandUseData() throws FileNotFoundException
    {
        System.out.println("Reading land use data ...");
        
        GeomGridField landuseGridField = new GeomGridField();

        readData(landuseGridField, LAND_USE_DATA_FILE_NAME);

        // Now set all the tiles' land use vales

        for (int y = 0; y < landscape.getHeight(); y++)
        {
            for (int x = 0; x < landscape.getWidth(); x++)
            {
                if (landscape.get(x, y) != null)
                {
                    Tile tile = (Tile) landscape.get(x, y);

                    tile.landuse = ((IntGrid2D) landuseGridField.getGrid()).get(x, y);
                }
            }
        }
    }



    /**
     * TODO: the excluded data area file appears to be either -9999 (no data)
     * or zeroes.  Is this right?
     */
    private void readExcludedAreaData() throws FileNotFoundException
    {
        System.out.println("Reading excluded area data ...");
        
        GeomGridField excludedGridField = new GeomGridField();
        
        readData(excludedGridField, EXCLUDED_DATA_FILE_NAME);

        // Now set all the tiles' land use vales

        for (int y = 0; y < landscape.getHeight(); y++)
        {
            for (int x = 0; x < landscape.getWidth(); x++)
            {
                if (landscape.get(x, y) != null)
                {
                    Tile tile = (Tile) landscape.get(x, y);

                    tile.excluded = ((IntGrid2D) excludedGridField.getGrid()).get(x, y) == 0;
                }
            }
        }
    }



    private void readUrbanAreaData() throws FileNotFoundException
    {
        System.out.println("Reading urban area data ...");
        
        GeomGridField urbanAreaGridField = new GeomGridField();

        readData(urbanAreaGridField,URBAN_AREA_DATA_FILE_NAME);

        // Now set all the tiles' land use vales

        for (int y = 0; y < landscape.getHeight(); y++)
        {
            for (int x = 0; x < landscape.getWidth(); x++)
            {
                if (landscape.get(x, y) != null)
                {
                    Tile tile = (Tile) landscape.get(x, y);
                    
                    int classification = ((IntGrid2D) urbanAreaGridField.getGrid()).get(x, y);

                    switch (classification)
                    {
                        case 1:
                            tile.urbanOriginally = false;
                            tile.urbanized = false;
                            break;
                        case 2:
                            tile.urbanOriginally = true;
                            tile.urbanized = false;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    
                }
            }
        }
    }



    private void readTransportData() throws FileNotFoundException
    {
        System.out.println("Reading transport data ...");
        
        GeomGridField transportGridField = new GeomGridField();

        readData(transportGridField, TRANSPORT_DATA_FILE_NAME);

        // Now set all the tiles' land use vales

        for (int y = 0; y < landscape.getHeight(); y++)
        {
            for (int x = 0; x < landscape.getWidth(); x++)
            {
                if (landscape.get(x, y) != null)
                {
                    Tile tile = (Tile) landscape.get(x, y);

                    int classification = ((IntGrid2D) transportGridField.getGrid()).get(x, y);

                    tile.transport = classification;
                }
            }
        }

    }



    private void readHillShadeData() throws FileNotFoundException
    {
        System.out.println("Reading hill shade data ...");
        
        GeomGridField hillshadeGridField = new GeomGridField();

        readData(hillshadeGridField, HILLSIDE_DATA_FILE_NAME);

        // Now set all the tiles' land use vales

        for (int y = 0; y < landscape.getHeight(); y++)
        {
            for (int x = 0; x < landscape.getWidth(); x++)
            {
                if (landscape.get(x, y) != null)
                {
                    Tile tile = (Tile) landscape.get(x, y);

                    int classification = ((IntGrid2D) hillshadeGridField.getGrid()).get(x, y);

                    tile.hillshade = classification;
                }
            }
        }

    }

}