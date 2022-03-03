package sim.app.geo.dsleuth;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;


import sim.app.geo.dsleuth.data.DSleuthData;
import sim.engine.DSimState;
import sim.field.geo.DGeomGridField;
import sim.field.geo.DGeomGridField.GridDataType;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.field.grid.DObjectGrid2D;
import sim.io.geo.DArcInfoASCGridImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntBag;
import sim.util.MPIUtil;


public class DSleuthWorld  extends DSimState{
	
    private static final String EXCLUDED_DATA_FILE_NAME = "excluded.txt.gz";
    private static final String HILLSIDE_DATA_FILE_NAME = "hillshade.txt.gz";
    private static final String LAND_USE_DATA_FILE_NAME = "landuse.txt.gz";
    private static final String SLOPE_DATA_FILE_NAME = "reclass_slope.txt.gz";
    private static final String TRANSPORT_DATA_FILE_NAME = "roads_0_1.txt.gz";
    private static final String URBAN_AREA_DATA_FILE_NAME = "urban.txt.gz";

    public DObjectGrid2D<DTile> landscape;
    ArrayList<DTile> spreadingCenters = new ArrayList<DTile>();
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
    int grid_width = 0; //5030;  //taken from data
    int grid_height = 0; //4192; //taken from data
    // cheap way to visualize
    public int numUrban = 0;
    public int numNonUrban = 0;
    private static final long serialVersionUID = 1L;



    /**
     * Constructor function.
     * @param seed
     */
    public DSleuthWorld(long seed)
    {
        //super(seed, grid_width, grid_height, 1);
    	super(seed, readDimensions(SLOPE_DATA_FILE_NAME)[0], readDimensions(SLOPE_DATA_FILE_NAME)[1], 1);
    	int[] width_height = readDimensions(SLOPE_DATA_FILE_NAME);
    	grid_width = width_height[0];
    	grid_height = width_height[1];

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
            
            //int[] aaa = readDimensions(SLOPE_DATA_FILE_NAME);
            //System.out.println( "width : "+aaa[0]);
            //System.out.println("height : "+aaa[1]);
            
           // System.exit(-1);

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
                	//only count if in bounds?
                	if (landscape.getLocalBounds().contains(new Int2D(i,j))){

                		//DTile tile = (DTile) landscape.get(i, j);
                		DTile tile = (DTile) landscape.getLocal(new Int2D(i,j));

                		if (tile != null && tile.urbanized)
                		{

                			numUrban++;

                			int numUrbanizedNeighbors = getUrbanNeighbors(tile).size();
                			if (numUrbanizedNeighbors > 1 && numUrbanizedNeighbors < 6)
                			{
                				spreadingCenters.add(tile);
                			}
                		} 
                		else
                		{
                        numNonUrban++;
                		}
                	}
                }
            }
            
            syncNonUrban_Urban();

            // schedule the growth cycle to happen every time step
            DGrower grower = new DGrower();

            schedule.scheduleRepeating(grower);
            
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(DSleuthWorld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    //in case it matters, we get the global count, using approach similar to gatherGlobals()
    //We only need to do this once, which is why we don't use gather/arbitrateGlobals
    private void syncNonUrban_Urban() {
    	
    	try {
    	Serializable[] o = new Serializable[2];
        o[0] = numUrban;
        o[1] = numNonUrban;   	
    	
		ArrayList<Serializable[]> gg = MPIUtil.gather(partition, o, 0);
		
		int newNonUrban = 0;
		int newUrban = 0;
		
		for (int i = 0; i < partition.getNumProcessors(); i++)
		{

			    newUrban =  newUrban + (int) gg.get(i)[0];
			    newNonUrban =  newNonUrban + (int) gg.get(i)[1];
			
		}
		
		Serializable[] newNums = new Serializable[2];
		newNums[0] = newUrban;
		newNums[1] = newNonUrban;
		
		newNums = MPIUtil.bcast(partition.getCommunicator(), newNums, 0);

		numUrban = (int) newNums[0];
		numNonUrban = (int) newNums[1];
		
    	}
    	
    	catch (Exception e) {
    		System.out.println(e);
    		System.exit(-1);
    	}

    }
    
    
    private void readData(DGeomGridField excludedGridField, final String fileName) throws FileNotFoundException
    {
        InputStream inputStream = DSleuthData.class.getResourceAsStream(fileName);

        if (inputStream == null)
        {
           throw new FileNotFoundException(fileName);
        }

        try
        {
            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);

            DArcInfoASCGridImporter.read(compressedInputStream, GridDataType.INTEGER, excludedGridField, this);

        } catch (IOException ex)
        {
            Logger.getLogger(DSleuthWorld.class.getName()).log(Level.SEVERE, null, ex);

            System.exit(-1);
        }
    }
    
    //to initially read file if necessary (perhaps I should put this in DSimState?)
    private static int[] readDimensions(final String fileName)
    {
    	try {
        InputStream inputStream = DSleuthData.class.getResourceAsStream(fileName);

        if (inputStream == null)
        {
           throw new FileNotFoundException(fileName);
        }


            GZIPInputStream compressedInputStream = new GZIPInputStream(inputStream);
            int width = 0;
            int height = 0;

            Scanner scanner = new Scanner(compressedInputStream);
            scanner.useLocale(Locale.US);

            scanner.next(); // skip "ncols"
            width = scanner.nextInt();

            scanner.next(); // skip "nrows"
            height = scanner.nextInt();
            
            scanner.close();
            
            int[] width_and_height = new int[2];
            width_and_height[0] = width;
            width_and_height[1] = height;
            
            return  width_and_height;


        } catch (IOException ex)
        {
            Logger.getLogger(DSleuthWorld.class.getName()).log(Level.SEVERE, null, ex);

            System.exit(-1);
        }

    	catch (Exception ex)
        {
            Logger.getLogger(DSleuthWorld.class.getName()).log(Level.SEVERE, null, ex);

            System.exit(-1);
        }
    	
        return null;
    }
    

    /**
     * any non-urbanized cell on the lattice has a certain (small) probability
     * of becoming urbanized in any time step.
     * @param dispersion_value - the dispersion value
     * @return a list of Tiles that have been spontaneously urbanized
     */
    ArrayList<DTile> spontaneousGrowth(double dispersion_value)
    {

        ArrayList<DTile> urbanized = new ArrayList<DTile>();
        
       	int minX = this.getPartition().getLocalBounds().ul().x;
    	int maxX = this.getPartition().getLocalBounds().br().x;
    	
    	int minY = this.getPartition().getLocalBounds().ul().y;
    	int maxY = this.getPartition().getLocalBounds().br().y;

        for (int i = 0; i < dispersion_value; i++)
        {
 
            // find a tile that is a part of the simulation
            //int x = random.nextInt(grid_width), y = random.nextInt(grid_height);
        	
        	int x = random.nextInt((maxX - minX)) + minX;
        	int y = random.nextInt((maxY - minY)) + minY;
        	
        	
            //while (landscape.get(x, y) == null)
            while (landscape.getLocal(new Int2D(x, y)) == null)
            {
            	x = random.nextInt((maxX - minX)) + minX;
            	y = random.nextInt((maxY - minY)) + minY;
            }

            DTile t = (DTile) landscape.getLocal(new Int2D(x, y));
            // try to urbanize t!
            if (t.urbanized)
            {
                continue; // already urbanized
            } else if (t.excluded)
            {
                continue; // excluded from urbanization
            } else
            { // urbanize the tile!
                DTile newlyUrbanized = (DTile) landscape.getLocal(new Int2D(x, y));
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
    ArrayList<DTile> newSpreadingCenters(ArrayList<DTile> newlyUrbanized)
    {

        ArrayList<DTile> spreadFromNewlyUrbanized = new ArrayList<DTile>();

        // go through all of the newly urbanized tiles
        for (DTile t : newlyUrbanized)
        {

            // with some probability, try to spread out from them
            if (random.nextInt(maxCoefficient) < breedCoefficient)
            {

                // assemble a list of unurbanized neighbors
                ArrayList<DTile> potential = getNeighborsAvailableForUrbanization(t);

                // if there are at least 2 unurbanized neighbors, urbanize 2 of them!
                if (potential.size() > 1)
                {
                    for (int i = 0; i < 2; i++)
                    {
                        DTile toUrbanize = potential.remove(
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
    ArrayList<DTile> edgeGrowth()
    {

        ArrayList<DTile> newlyUrbanized = new ArrayList<DTile>();

        ArrayList<DTile> centers = new ArrayList<DTile>(spreadingCenters);

        // go through urban centers, potentially grow their neighboring unurbanized cells
        for (DTile t : centers)
        {

            // with some probability, spread out from this
            if (random.nextInt(maxCoefficient) < spreadCoefficient)
            {

                // get neighbors that can be urbanized
                ArrayList<DTile> suitableForUrbanization =
                    getNeighborsAvailableForUrbanization(t);
                if (suitableForUrbanization.size() > 0)
                {

                    DTile toUrbanize = suitableForUrbanization.get(
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
                              ArrayList<DTile> recentlyUrbanized)
    {

        // go through all Tiles that were urbanized this turn and possibly spread along
        // nearby roads
        for (DTile t : recentlyUrbanized)
        {

            // do so with some probability
            if (random.nextInt(maxCoefficient) < breedCoefficient)
            {

                // check if there is a road within a given distance of the newly
                // urbanized Tile
                ArrayList<DTile> neighboringRoads =
                    getNeighborsTransport(t, (int) max_search_index);
                if (! neighboringRoads.isEmpty() )
                {

                    // if so, do a random walk along the road with number of steps
                    // depending on the weight of that road
                    DTile bordersRoad = neighboringRoads.get(0);

                    // calculate the number of steps the test will random walk
                    // along the road, based on the caliber of road on which
                    // it initially finds itself
                    double run_value = (dispersionCoefficient
                                        * (maxRoadValue - bordersRoad.transport + 1) / maxRoadValue);
                    DTile finalPoint = walkAlongRoad(bordersRoad, (int) run_value);

                    // at the place we finally end up, see if there are any neighbors
                    // available for urbanization. If so, urbanize one.
                    ArrayList<DTile> potential =
                        getNeighborsAvailableForUrbanization(finalPoint);

                    if (potential.isEmpty())
                    {
                        continue; // no neighbors available
                    }
                    DTile newUrbanized = potential.get(random.nextInt(potential.size()));
                    boolean successful = urbanizeTile(newUrbanized);

                    if (!successful)
                    {
                        continue; // it didn't take, so we don't check
                    }					// the neighbors of this failed urbanization attempts

                    // check and see if this newly urbanized Tile has neighbors to
                    // urbanize. If it has at least two, urbanize two randomly selected
                    // neighbors
                    ArrayList<DTile> neighbors =
                        getNeighborsAvailableForUrbanization(newUrbanized);
                    if (neighbors.size() > 1)
                    {
                        for (int i = 0; i < 2; i++)
                        {
                            DTile neighbor = neighbors.remove(
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
    DTile walkAlongRoad(DTile origin, int numSteps)
    {
        if (origin.transport < 1)
        {
            return null; // NOT A ROAD, SILLY
        }
        DTile result = origin;

        ArrayList<DTile> neighbors;

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
    boolean urbanizeTile(DTile t)
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
        ArrayList<DTile> urbanizedNeighbors = getUrbanNeighbors(t);
        if (urbanizedNeighbors.size() > 2)
        {
            spreadingCenters.add(t);
        }

        // check to see if the urbanization of this Tile has made any of its neighbors
        // into centers
        for (DTile n : urbanizedNeighbors)
        {

            // check to see if we've already found this Tile
            if (spreadingCenters.contains(n))
            {
                continue;
            }

            // otherwise, if this Tile now has enough urbanized neighbors to be an urban
            // center, make it so!
            ArrayList<DTile> neighborsUrbanizedNeighbors = getUrbanNeighbors(n);
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
    void setupLandscape(final DGeomGridField slope)
    {
        grid_width = slope.getGridWidth();
        grid_height = slope.getGridHeight();
        
        

        landscape = new DObjectGrid2D(this);
        
        int lowx = landscape.getLocalBounds().ul().x ;
        int highx = landscape.getLocalBounds().br().x ;
        int lowy = landscape.getLocalBounds().ul().y ;
        int highy = landscape.getLocalBounds().br().y ;

 

        // populate the new landscape with new Tiles
        System.out.print("\nInitializing landscape...");
        
        // Now set all the tiles' land use vales

        for (int y = lowy; y < highy; y++)
        {
            for (int x = lowx; x < highx; x++)
            {	
            	if (landscape.getLocalBounds().contains(new Int2D(x,y))) {
            		
            		// -9999 means "no data"
            		//if ( ((DIntGrid2D)slope.getGrid()).get(i, j) != -9999)
            		if ( ((DIntGrid2D)slope.getGrid()).getLocal(new Int2D(x, y)) != -9999)
            		{
            			DTile tile = new DTile(x,y);
            			tile.slope = ((DIntGrid2D)slope.getGrid()).getLocal(new Int2D(x, y));
            			//tile.slope = ((DDoubleGrid2D)slope.getGrid()).getLocal(new Int2D(i, j));

            			//landscape.set(i, j, tile);
            			landscape.setLocal(new Int2D(x, y), tile);

            		}
                
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
    ArrayList<DTile> getUrbanNeighbors(DTile t)
    {

        ArrayList<DTile> result = new ArrayList<DTile>();
        //Bag neighbors = new Bag();
        ArrayList<DTile> neighbors = new ArrayList<DTile>();
        
        
        
        landscape.getMooreNeighbors( t.x, t.y, 1, false, neighbors, null, null);

        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            }
            DTile n = (DTile) o;
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
    ArrayList<DTile> getNeighborsAvailableForUrbanization(DTile t)
    {

        ArrayList<DTile> result = new ArrayList<DTile>();
        ArrayList<DTile> neighbors = new ArrayList<DTile>();
        //landscape.getNeighborsMaxDistance(t.x, t.y, 1, false,neighbors, null, null);
        
        landscape.getMooreNeighbors( t.x, t.y, 1, false, neighbors, null, null);


        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            }
            DTile n = (DTile) o;

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
    ArrayList<DTile> getNeighborsTransport(DTile t, int dist)
    {
        ArrayList<DTile> result = new ArrayList<DTile>();
        ArrayList<DTile> neighbors = new ArrayList<DTile>();
        //landscape.getNeighborsMaxDistance(t.x, t.y, dist, false,neighbors, null, null);
        
        landscape.getMooreNeighbors( t.x, t.y, 1, false, neighbors, null, null);

        for (Object o : neighbors)
        {
            if (o == null)
            {
                continue;
            } else if (o == t)
            {
                continue;
            } else if (((DTile) o).transport > 0)
            {
                result.add((DTile) o);
            }
        }


        return result;
    }

	public static void main(final String[] args)
	{
		doLoopDistributed(DSleuthWorld.class, args);
		System.exit(0);
	}
	
	   private void readSlopeData() throws FileNotFoundException
	    {
	        System.out.println("Reading slope data ...");

	        // Let's read in all the slope data first.  Not only will that give us
	        // the dimensions, but we can use that to determine where to put new tiles.

	        DGeomGridField slopeField = new DGeomGridField();

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
	        
	        DGeomGridField landuseGridField = new DGeomGridField();

	        readData(landuseGridField, LAND_USE_DATA_FILE_NAME);

	        // Now set all the tiles' land use vales
	        
	        int lowx = landscape.getLocalBounds().ul().x ;
	        int highx = landscape.getLocalBounds().br().x ;
	        int lowy = landscape.getLocalBounds().ul().y ;
	        int highy = landscape.getLocalBounds().br().y ;

	        for (int y = lowy; y < highy; y++)
	        {
	            for (int x = lowx; x < highx; x++)
	            {
	                if (landscape.getLocal(new Int2D(x, y)) != null)
	                {
	                    DTile tile = (DTile) landscape.getLocal(new Int2D(x, y));

	                    tile.landuse = ((DIntGrid2D) landuseGridField.getGrid()).getLocal(new Int2D(x, y));
	                    //tile.landuse = ((DDoubleGrid2D) landuseGridField.getGrid()).get(x, y);

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
	        
	        DGeomGridField excludedGridField = new DGeomGridField();
	        
	        readData(excludedGridField, EXCLUDED_DATA_FILE_NAME);
	        
	        int lowx = landscape.getLocalBounds().ul().x ;
	        int highx = landscape.getLocalBounds().br().x ;
	        int lowy = landscape.getLocalBounds().ul().y ;
	        int highy = landscape.getLocalBounds().br().y ;

	        // Now set all the tiles' land use vales

	        for (int y = lowy; y < highy; y++)
	        {
	            for (int x = lowx; x < highx; x++)
	            {
	                if (landscape.getLocal(new Int2D(x, y)) != null)
	                {
	                    DTile tile = (DTile) landscape.getLocal(new Int2D(x, y));

	                    tile.excluded = ((DIntGrid2D) excludedGridField.getGrid()).getLocal(new Int2D(x, y)) == 0;
	                    //tile.excluded = ((DDoubleGrid2D) excludedGridField.getGrid()).getLocal(new Int2D(x, y)) == 0;

	                }
	            }
	        }
	    }



	    private void readUrbanAreaData() throws FileNotFoundException
	    {
	        System.out.println("Reading urban area data ...");
	        
	        DGeomGridField urbanAreaGridField = new DGeomGridField();

	        readData(urbanAreaGridField,URBAN_AREA_DATA_FILE_NAME);

	        int lowx = landscape.getLocalBounds().ul().x ;
	        int highx = landscape.getLocalBounds().br().x ;
	        int lowy = landscape.getLocalBounds().ul().y ;
	        int highy = landscape.getLocalBounds().br().y ;

	        // Now set all the tiles' land use vales

	        for (int y = lowy; y < highy; y++)
	        {
	            for (int x = lowx; x < highx; x++)
	            {
	                if (landscape.getLocal(new Int2D(x, y)) != null)
	                {
	                    DTile tile = (DTile) landscape.getLocal(new Int2D(x, y));
	                    
	                    int classification = ((DIntGrid2D) urbanAreaGridField.getGrid()).getLocal(new Int2D(x, y));
	                    //int classification = ((DDoubleGrid2D) urbanAreaGridField.getGrid()).get(x, y);

	                    
	                    
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
	        
	        DGeomGridField transportGridField = new DGeomGridField();

	        readData(transportGridField, TRANSPORT_DATA_FILE_NAME);

	        int lowx = landscape.getLocalBounds().ul().x ;
	        int highx = landscape.getLocalBounds().br().x ;
	        int lowy = landscape.getLocalBounds().ul().y ;
	        int highy = landscape.getLocalBounds().br().y ;

	        // Now set all the tiles' land use vales

	        for (int y = lowy; y < highy; y++)
	        {
	            for (int x = lowx; x < highx; x++)
	            {
	                if (landscape.getLocal(new Int2D(x, y)) != null)
	                {
	                    DTile tile = (DTile) landscape.getLocal(new Int2D(x, y));

	                    int classification = ((DIntGrid2D) transportGridField.getGrid()).getLocal(new Int2D(x, y));
	                    //int classification = ((DDoubleGrid2D) transportGridField.getGrid()).get(x, y);

	                    
	                    tile.transport = classification;
	                }
	            }
	        }

	    }



	    private void readHillShadeData() throws FileNotFoundException
	    {
	        System.out.println("Reading hill shade data ...");
	        
	        DGeomGridField hillshadeGridField = new DGeomGridField();

	        readData(hillshadeGridField, HILLSIDE_DATA_FILE_NAME);

	        int lowx = landscape.getLocalBounds().ul().x ;
	        int highx = landscape.getLocalBounds().br().x ;
	        int lowy = landscape.getLocalBounds().ul().y ;
	        int highy = landscape.getLocalBounds().br().y ;

	        // Now set all the tiles' land use vales

	        for (int y = lowy; y < highy; y++)
	        {
	            for (int x = lowx; x < highx; x++)
	            {
	                if (landscape.getLocal(new Int2D(x, y)) != null)
	                {
	                    DTile tile = (DTile) landscape.getLocal(new Int2D(x, y));

	                    int classification = ((DIntGrid2D) hillshadeGridField.getGrid()).getLocal(new Int2D(x, y));
	                    //int classification = ((DDoubleGrid2D) hillshadeGridField.getGrid()).get(x, y);

	                    
	                    tile.hillshade = classification;
	                }
	            }
	        }

	    }
}
