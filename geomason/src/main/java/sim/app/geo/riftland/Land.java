package sim.app.geo.riftland;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import sim.app.geo.riftland.parcel.*;
import sim.app.geo.riftland.util.Misc;
import sim.field.geo.GeomVectorField;
import sim.field.grid.ObjectGrid2D;
import sim.io.geo.ShapeFileImporter;
import sim.portrayal.grid.FastObjectGridPortrayal2D;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import sim.util.gui.SimpleColorMap;
import sim.app.geo.riftland.riftlandData.RiftlandData;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class for the grid containing the Parcels.  This requires that the
 * ThreadedGardener already have been initialized, so we can register
 * GrazableAreas with it.
 * 
 * @author Eric 'Siggy' Scott
 */
final public class Land
{    
    // Values for parcel types as used in the data files
    final static int FRESHWATER = -1; // parcel flagged as fresh water
    final static int ALSO_FRESHWATER = 0; // newer versions of data file have this as 0
    final static int OCEAN = -9999; // parcel flagged as ocean
    final static int NO_DATA = -9999; // Urban data flag indicating not urban
    
    private ObjectGrid2D landGrid;
    private GeomVectorField politicalBoundaries = new GeomVectorField();
    
    /**
     * An optional sub-area of interest as denoted by upper left and lower right
     * points
     *
     * Set in params file via "SubArea" parameter, which has format of "(X,Y),
     * (X,Y)", where the first coordinate pair indicates the upper left corner,
     * and the second the lower right. "SubArea" is loaded, if it exists in
     * params file, in loadParameters(), which then, in turn calls setSubArea(),
     * which will assign values to these variables.
     *
     */
    private java.awt.Point subAreaUpperLeft;
    private java.awt.Point subAreaLowerRight;
    
    private int width = 0;
    private int height = 0;
    
    private final Parameters params;
    
    // <editor-fold defaultstate="collapsed" desc="Accessors">
    public Parcel getParcel(int x, int y)
    {
        return (Parcel) landGrid.get(x,y);
    }
    
    public Bag allLand()
    {
        return landGrid.elements();
    }
    
    public java.awt.Point getSubAreaUpperLeft()
    {
        return subAreaUpperLeft;
    }

    public java.awt.Point getSubAreaLowerRight()
    {
        return subAreaLowerRight;
    }

    public int getWidth()
    {
        assert (width == landGrid.getWidth());
        return width;
    }

    public int getHeight()
    {
        assert (height == landGrid.getHeight());
        return height;
    }

    public ObjectGrid2D getLandGrid()
    {
        return landGrid;
    }

    public GeomVectorField getPoliticalBoundaries()
    {
        return politicalBoundaries;
    }
    
    /** @return true if user specified a SubArea in the params file */
    public boolean haveSubArea()
    {
        return this.subAreaLowerRight != null && this.subAreaUpperLeft != null;
    }
    // </editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Constructor">
    
    public Land(Parameters params)
    {
        this.params = params;
        setSubArea(params.world.getSubArea());
        assert(repOK());
    }

    /**
     * Defines s minimum bounding rectangle for a small area of interest
     *
     * Sets this.subAreaUpperLeft and this.subAreaLowerRight to points
     * demarcating a sub-region within the simulation on which to focus.
     *
     * @param MBR string coordinate in format "(x_u, y_u), (x_l, y_l), numWaterHoles"
     */
    public void setSubArea(String MBR)
    {
        // "SubArea" may not have been assigned.
        if (MBR == null)
        {
            return;
        }

        World.getLogger().log(Level.INFO, "SubArea: {0}", MBR);

        // First split out each coordinate

        // Regular expression for "(x_u, y_u), (x_l, y_l), "
        Pattern coordinateExp = Pattern.compile("[(](\\d+,\\s*\\d+)[)],\\s*[(](\\d+,\\s*\\d+)[)]\\s*,\\s*(\\d+)");
        Matcher coordinate = coordinateExp.matcher(MBR);

        if (coordinate.find())
        {
            String upperLeft = coordinate.group(1);
            String lowerRight = coordinate.group(2);
            String numWaterHoles = coordinate.group(3);

            World.getLogger().log(Level.INFO, "Upper left: {0}", upperLeft);
            World.getLogger().log(Level.INFO, "Lower right: {0}", lowerRight);
            World.getLogger().log(Level.INFO, "NumWaterHoles: {0}", numWaterHoles);

            this.subAreaUpperLeft = Misc.stringToPoint(upperLeft);
            this.subAreaLowerRight = Misc.stringToPoint(lowerRight);
            this.params.world.setNumInitialWateringHoles(Integer.parseInt(numWaterHoles));

            World.getLogger().log(Level.INFO, "Upper left: {0}", this.subAreaUpperLeft);
            World.getLogger().log(Level.INFO, "Lower right: {0}", this.subAreaLowerRight);
            
            if ((subAreaUpperLeft.x >= subAreaLowerRight.x) || (subAreaUpperLeft.y >= subAreaLowerRight.y))
            	throw new RuntimeException(String.format("Invalid SubArea bounds: %s", MBR));
        } else
        {
            World.getLogger().log(Level.SEVERE, "SubArea ''{0}'' is invalid.  Not setting subarea.", MBR);
            return;
        }
    }

    public void loadLandData(String datapath, ThreadedGardener gardener)
    {
        // We'll read the land use file first.  From that we'll get the
            // dimensions to create the various Grid containers.  Then we'll
            // initialize the Parcels from the read in data.
        BufferedReader landuse, urbanuse, parkland, forestland, ndviResidual, NDVI2001001;
        try
        {
            if ("".equals(datapath))
            { // Load data from a resource in our jar
                System.err.println("hey we got this? "  + params.world.getDatapath() + " " + params.world.getLanduseFile());
                landuse = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getLanduseFile())));
                urbanuse = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getUrbanuseFile())));
                parkland = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getParklandFile())));
                forestland = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getForestFile())));
                ndviResidual = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getNdviResidualFile())));
                NDVI2001001 = new BufferedReader(new InputStreamReader(RiftlandData.class.getResourceAsStream(params.world.getNdviDataFile())));
            }
            else
            { // Load data from an external file
                landuse = new BufferedReader(new FileReader(datapath + params.world.getLanduseFile()));
                urbanuse = new BufferedReader(new FileReader(datapath + params.world.getUrbanuseFile()));
                parkland = new BufferedReader(new FileReader(datapath + params.world.getParklandFile()));
                forestland = new BufferedReader(new FileReader(datapath + params.world.getForestFile()));
                ndviResidual = new BufferedReader(new FileReader(datapath + params.world.getNdviResidualFile()));
                NDVI2001001 = new BufferedReader(new FileReader(datapath + params.world.getNdviDataFile()));
            }
            
            setWidthAndHeight(landuse); 
            landGrid = new ObjectGrid2D(width, height);

            // If the user did not specify a subarea, go ahead and set the sub
            // area to that of the entire RiftLand extent.  That way the code
            // for compensating for subareas is generalizable.
            if (! haveSubArea() )
            {
                subAreaUpperLeft = new java.awt.Point(0, 0);
                subAreaLowerRight = new java.awt.Point(width, height);
            }

            // skip the next four lines as they contain irrelevant metadata
            for (int i = 0; i < 4; ++i)
                landuse.readLine();

            // Wind urban use, park land, and forest data to the start of data
            for (int i = 0; i < 6; i++)
            {
                urbanuse.readLine();
                parkland.readLine();
                forestland.readLine();
            }

            GeomVectorField politicalBoundaries = loadPoliticalBoundaries(datapath);
            assignParcels(politicalBoundaries, gardener, landuse, urbanuse, parkland, forestland, ndviResidual, NDVI2001001);
            
            landuse.close();
            urbanuse.close();
            parkland.close();
            forestland.close();
            ndviResidual.close();
            NDVI2001001.close();
        }
        catch(Exception ee)
        {
            World.getLogger().log(Level.SEVERE, ee.getMessage(), ee);
        }
    }

    /** Grind through the grid and add appropriate Parcels for each coordinate
     *
     * @param landuse land quality file
     * @param urbanuse
     * @param parkland state park data
     * @param forestland
     * @param ndviResidual 
     *
     * @throws IOException
     * @throws NumberFormatException
     */
    private void assignParcels(final GeomVectorField politicalBoundaries, final ThreadedGardener gardener, BufferedReader landuse, BufferedReader urbanuse, BufferedReader parkland, BufferedReader forestland, BufferedReader ndviResidual, BufferedReader NDVI2001001) throws IOException, NumberFormatException
    {
        class RowAssigner implements Runnable
        {
            private final int row;
            final String[] ndviTokens;
            final String[] landuseTokens; // current line of input tokenized by white space
            final String[] urbanLanduseTokens; // tokensized version of urbanLandUseLine
            final String[] parkLandTokens; // tokensized version of urbanLandUseLine
            final String[] forestLandTokens; // tokensized version of forestLandLine
            final String[] NDVIResTokens; // tokensized version of NDVIResLine
            
            
            RowAssigner(int row, String[] ndviTokens, String[] landuseTokens, String[] urbanLanduseTokens, String[] parkLandTokens, String[] forestLandTokens, String[] NDVIResTokens)
            {
                this.row = row;
                this.ndviTokens = ndviTokens;
                this.landuseTokens = landuseTokens;
                this.urbanLanduseTokens = urbanLanduseTokens;
                this.parkLandTokens = parkLandTokens;
                this.forestLandTokens = forestLandTokens;
                this.NDVIResTokens = NDVIResTokens;
            }
            
            @Override
            public void run() {
                for (int x = subAreaUpperLeft.x; x < subAreaLowerRight.x; x++)
                {
                        assignParcel(x, row, politicalBoundaries, gardener, ndviTokens[x], landuseTokens[x], NDVIResTokens[x], urbanLanduseTokens[x], parkLandTokens[x], forestLandTokens[x]);
                }
                Logger.getLogger(World.class.getName()).info(String.format("Row %d assigned", row));
            }
        }
        
       Logger.getLogger(World.class.getName()).info("Entering Land.assignParcels()");
       
        String ndviLine;
        String[] ndviTokens;

        for (int i = 0; i < 6; i++)
        {
            ndviLine = NDVI2001001.readLine();
        }

        String landuseLine; // current line of input
        String[] landuseTokens; // current line of input tokenized by white space

        String urbanLandUseLine; // current line of urban use input
        String[] urbanLanduseTokens; // tokensized version of urbanLandUseLine

        String parkLandUseLine; // current line of urban use input
        String[] parkLandTokens; // tokensized version of urbanLandUseLine

        String forestLandLine; // current line of forest land input
        String[] forestLandTokens; // tokensized version of forestLandLine

        String NDVIResLine; // current line of ndvi residual file
        String[] NDVIResTokens; // tokensized version of NDVIResLine

        ExecutorService executor = Executors.newFixedThreadPool(params.system.getNumthreads());
        
        // now we should be on first row of data; start grinding through
        // assigning parcel values based on what we're getting
        for (int curr_row = 0; curr_row < subAreaLowerRight.y; ++curr_row)
        {

            ndviLine = NDVI2001001.readLine();
            landuseLine = landuse.readLine();
            urbanLandUseLine = urbanuse.readLine();
            parkLandUseLine = parkland.readLine();
            forestLandLine = forestland.readLine();
            NDVIResLine = ndviResidual.readLine();

            // Why go through the trouble of processing the current row if it's
            // outside the desired subarea?  Again, by default the sub-area will
            // be the entire RiftLand region by default; i.e.,
            // world.subAreaUpperLeft.y will be 0 if no subarea was selected.
            if (curr_row >= subAreaUpperLeft.y)
            {
                ndviTokens = ndviLine.split("\\s+");
                NDVIResTokens = NDVIResLine.split(",\\s*");
                landuseTokens = landuseLine.split("\\s+");
                urbanLanduseTokens = urbanLandUseLine.split("\\s+");
                parkLandTokens = parkLandUseLine.split("\\s+");
                forestLandTokens = forestLandLine.split("\\s+");
                
                executor.execute(new RowAssigner(curr_row, ndviTokens, landuseTokens, urbanLanduseTokens, parkLandTokens, forestLandTokens, NDVIResTokens));
            }
        }
        
        executor.shutdown();
        while(!executor.isTerminated()) { }

        World.getLogger().info("Exiting Land.assignParcels()");
    }
    
    private void assignParcel(int x, int y, GeomVectorField politicalBoundaries, ThreadedGardener gardener, String ndviToken, String landuseToken, String NDVIResToken, String urbanLanduseToken, String parkLandToken, String forestLandToken)
    {
        // Current parcel
        Parcel parcel = null;

        double ndviValue = Double.parseDouble(ndviToken);

        // parcelValue is either the max vegetation for that parcel, -1
        // for fresh water, or -9999 for ocean
        double parcelValue = Double.parseDouble(landuseToken);

        // residual values from NDVI residual file
        double ndviResidualValue = Double.parseDouble(NDVIResToken);
        //  System.out.println("residual; " + ndviResidualValue);

        Integer urbanUseValue = Integer.parseInt(urbanLanduseToken);
        Integer parkLandValue = Integer.parseInt(parkLandToken);
        Integer forestLandValue = Integer.parseInt(forestLandToken);
        int country = lookUpCountry(politicalBoundaries, x, y);

        if (urbanUseValue == NO_DATA) // not an urban area
        {
        	if (parcelValue == OCEAN)  // ocean
                parcel = new SaltWater(x, y, country);

        	else if (parcelValue == FRESHWATER || parcelValue == ALSO_FRESHWATER || (ndviValue < 0)) // fresh water
                parcel = new FreshWater(x, y, country);
            
            else if (parkLandValue == 1) 
                parcel = new ParkLand(x, y, country);
            
            else if (forestLandValue == 1)
                parcel = new Forest(x, y, country);
            
            else if (ndviValue < 0) // this is a special case to handle parcels that should have been marked as water but aren't
                parcel = new FreshWater(x, y, country);
            
            else // grazable or urban area
                parcel = new GrazableArea(params, this, gardener, x, y, country, ndviValue, ndviResidualValue);
        }

        synchronized(landGrid)
        {
            landGrid.field[x][y] = parcel;
        }
    }
    
    

    /**
     * load political boundaries
     */
    private GeomVectorField loadPoliticalBoundaries(String datapath)
    {
        politicalBoundaries = new GeomVectorField();
        URL boundaryDB;
        try
        {
            URL boundaryFile;
            if ("".equals(datapath))
            {
                boundaryFile = RiftlandData.class.getResource(params.world.getDatapath() + params.world.getPoliticalBoundariesFile());
                boundaryDB = RiftlandData.class.getResource(params.world.getDatapath() + params.world.getPoliticalBoundariesFile().replace("shp", "dbf"));
            }
            else
            {
                File politicalBoundariesFile = new File(datapath + params.world.getPoliticalBoundariesFile());
                boundaryFile = politicalBoundariesFile.toURI().toURL();
                File db = new File(datapath + params.world.getPoliticalBoundariesFile().replace("shp", "dbf"));
                boundaryDB = db.toURI().toURL();

                if (boundaryFile == null)
                {
                    throw new FileNotFoundException(politicalBoundariesFile.getName());
                }
            }

            ShapeFileImporter.read(boundaryFile, boundaryDB, politicalBoundaries);

            World.getLogger().info("Loaded " + politicalBoundaries.getGeometries().numObjs
                    + " political regions");
        } catch (Exception ex)
        {
            World.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
        
        return politicalBoundaries;
    }
    
    /** Look up the country code for the given coordinates. This is expensive so only
     * do it once per parcel. */
    private static int lookUpCountry(GeomVectorField politicalBoundaries, int x, int y)
    {
        //Point p = world.populationGrid.toPoint(x, y); // The popgrid is the same dimensionality as the land, so I believe it is save to omit this call -- Siggy
        CoordinateSequence cs = ((new PackedCoordinateSequenceFactory()).create(new double[] {x,y}, 2));
        Point jtsPoint = new Point(cs, new GeometryFactory());
        Bag coveringObjects = politicalBoundaries.getCoveringObjects(jtsPoint);

        // If the coordinate falls outside all the country boundaries, then
        // coveringObjects will be empty.
        if (coveringObjects.isEmpty())
        {
            return 0;
        }

        // Can't have more than one country for this guy to be in, so error out.
        assert coveringObjects.size() == 1 : x + " " + y + " " + coveringObjects.size();

        MasonGeometry masonGeometry = (MasonGeometry) coveringObjects.objs[0];

        int country = masonGeometry.getIntegerAttribute("CID");

        return country;
    }
    
    private void setWidthAndHeight(BufferedReader landuse) throws IOException, NumberFormatException
    {
        String landuseLine = landuse.readLine(); // read line for width
        // the tokenized contents of 'line'
        String[] landuseTokens = landuseLine.split("\\s+");
        // first read the dimensions
        this.width = Integer.parseInt(landuseTokens[1]); // 2nd token contains #
        // The next line will have "nrows" and an integer separated
        // by whitespace
        landuseLine = landuse.readLine();
        landuseTokens = landuseLine.split("\\s+");
        // The line will have "ncols" and an integer separated
        // by whitespace
        this.height = Integer.parseInt(landuseTokens[1]); // 2nd token contains #
    }
    // </editor-fold>
     
    /**
     * fetches the nearest Grazable Areas <p> Intended as a replacement for
     * allLand.getNeighborsMaxDistance() as this is slow <em>and</em> returns
     * <em>everything</em>, including non-GrazableAreas <p> Note that this does
     * not return GrazableAreas within a circular distance, but returns the
     * rectangle of parcels instead, just like getNeighborsMaxDistance()
     *
     * XXX This should probably be cached -- Siggy.
     * 
     * @param x coordinate
     * @param y coordinate
     * @param dist is distance in parcels
     * @param closestGrazableAreas will contain GrazableaAreas within d
     */
    public final void getNearestGrazableAreas(int x, int y, int dist, Bag closestGrazableAreas)
    {
        // Cribbed from SparseGrid2D.getNeighborsMaxDistance()
        int xmin, xmax, ymin, ymax;

        if (haveSubArea())
        {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            xmin = ((x - dist >= subAreaUpperLeft.x) ? x - dist : subAreaUpperLeft.x);
            xmax = ((x + dist <= subAreaLowerRight.x - 1) ? x + dist : subAreaLowerRight.x - 1);

            // compute ymin and ymax for the neighborhood such that they are within boundaries
            ymin = ((y - dist >= subAreaUpperLeft.y) ? y - dist : subAreaUpperLeft.y);
            ymax = ((y + dist <= subAreaLowerRight.y - 1) ? y + dist : subAreaLowerRight.y - 1); // TODO check this
        } else
        {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            xmin = ((x - dist >= 0) ? x - dist : 0);
            xmax = ((x + dist <= getWidth() - 1) ? x + dist : getWidth() - 1);

            // compute ymin and ymax for the neighborhood such that they are within boundaries
            ymin = ((y - dist >= 0) ? y - dist : 0);
            ymax = ((y + dist <= getHeight() - 1) ? y + dist : getHeight() - 1);
        }

        for (int x0 = xmin; x0 <= xmax; x0++)
        {
            for (int y0 = ymin; y0 <= ymax; y0++)
            {
                if (this.landGrid.get(x0, y0) instanceof GrazableArea)
                {
                    closestGrazableAreas.add(this.landGrid.get(x0, y0));
                }
            }
        }
        assert(repOK());
    }

    /**
     * Returns the country ID corresponding to the parcel grid coordinate.
     *
     * @note A zero maps to an area outside of all countries such as the Indian
     * Ocean.
     *
     * @param x
     * @param y
     * @return CID as found in Riftland_Boundary shape file, or zero to indicate
     * no corresponding country found.
     *
     */
    public int getCountry(int x, int y)
    {
        assert(x >= 0);
        assert(x < landGrid.getWidth());
        assert(y >= 0);
        assert(y < landGrid.getHeight());
    	Parcel p = (Parcel)landGrid.get(x, y);
    	if (p == null)
        {
            assert(repOK());
            return 0;
        }
    	
        assert(repOK());
    	return p.getCountry();
    }
    
    public ObjectGridPortrayal2D getPortrayal()
    {
        assert(landGrid != null);
        FastObjectGridPortrayal2D landPortrayal = new FastObjectGridPortrayal2D();
        landPortrayal.setField(landGrid);
        
        // The color map goes as follows:
        //	0: Urban area	=>	Gray
        //	1: Ocean		=>	Ocean blue
        //	2: Fresh water	=>	Blue
        //	3: Parkland		=>	Khaki
        //	4: Forest		=>	Forest green
        //	5 and up: 		=>	Gradient from yellow to green

        Color oceanColor = new Color(28, 107, 160);	// Ocean blue
        Color forestColor = new Color(34, 139, 34); // Forest green from rgb.txt
        Color parkColor = new Color(161, 123, 93); // Color for parks
        java.awt.Color[] colors = new java.awt.Color[]
        {
            Color.LIGHT_GRAY, oceanColor, Color.BLUE, parkColor, forestColor
        };

        landPortrayal.setMap(new SimpleColorMap(colors, params.vegetation.getMinVegetationKgPerKm2(), 
                		params.vegetation.getTheoreticalMaxVegetationKgPerKm2(), Color.YELLOW, Color.GREEN));
        assert(repOK());
        return landPortrayal;
    }
    
    public boolean repOK()
    {
        return landGrid != null
                && width > 0
                && height > 0
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(landGrid.elements(), Parcel.class));
    }
}
