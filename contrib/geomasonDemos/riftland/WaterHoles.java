package riftland;

import ec.util.MersenneTwisterFast;
import riftland.parcel.GrazableArea;
import riftland.parcel.Parcel;
import riftland.parcel.WaterHole;
import riftland.util.DiscreteVoronoi;
import riftland.util.Misc;
import riftland.util.SquareDiscreteVoronoi;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Int2D;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eric 'Siggy' Scott
 */
public class WaterHoles extends ArrayList
{
    /** locations of all the watering holes */
    private SparseGrid2D waterHolesGrid;
    final private Parameters params;
    private DiscreteVoronoi voronoi;

    public DiscreteVoronoi getVoronoi()
    {
        return voronoi;
    }
    
    public WaterHole getNearestWaterHole(int x, int y)
    {
        if (voronoi == null)
            return null;
        Int2D loc = voronoi.getNearestPoint(x, y);
        Bag waterHoles;
        synchronized(waterHolesGrid) // getObjectsAtLocation() is not threadsafe!
        {
            waterHoles = waterHolesGrid.getObjectsAtLocation(loc.x, loc.y);
        }
        assert(waterHoles != null);
        assert(waterHoles.size() == 1);
        return (WaterHole) waterHoles.get(0);
    }
    
    /** The constructor only initializes the grid.  placeWaterHoles() must be
     * called externally to population the data structures. */
    public WaterHoles(Parameters params, Land land)
    {
        this.params = params;
        waterHolesGrid = new SparseGrid2D(land.getWidth(), land.getHeight());
        assert(repOK());
    }

    public SparseGrid2D getWaterHolesGrid()
    {
        return waterHolesGrid;
    }
    
    /** Assigns watering holes to world grid
     * 
     * <p>
     *
     * placeWaterHoles follows these steps:
     *  <ol>
     *  <li> read river data that holds flow accumulation (estimate of amount of water)
     * a given waterhole can hold
     *  <li> allocate waterholes based on flow accumulation
     * </ol>
     *
     * The watering hole allocation algorithm uses Stochastic Uniform Selection,
     * and was originally taken from placeHouseholdsInRegionBiasedByDensity().
     *
     * @author Ates
     *
     * FIXME: this does not handle SubAreas; in fact, I think enabled SubAreas
     * breaks this.
     *
     * TODO: add reality check such that household size <= total number of
     * subarea parcels; same thing for number of water holes.
     * 
     */
    final public void placeWaterHoles(Land land, MersenneTwisterFast random, String datapath)
    {
        Logger.getLogger(World.class.getName()).info("Entering WaterHoles.assignWaterHoleBiasedLocator(World world)");

        // Reset for between run invocations
        waterHolesGrid.clear();

        // This will contain the water flow rate for each parcel, and is
        // populated from "RiverData/riverdatanew.txt".
        DoubleGrid2D riverGrid = new DoubleGrid2D(land.getWidth(), land.getHeight());

        double[][] roulette = new double[land.getWidth()][land.getHeight()];

        double totalAccumulationSoFar = 0.0; // Ongoing total flow accumulation

        // The river data file is a grid of real numbers.  Each number represents
        // a flow rate value in the range [0,615002624].  These values are used
        // to bias water hole placement; the higher the value, the likelier
        // a water hole will be placed there.  Note that we use Stochastic
        // Universal Selection (SUS) to ensure a smoother spread of water hole
        // placement.

        String riverDataFile = datapath + params.world.getRiverDataFile();

        try
        {
            BufferedReader riverData;
            if ("".equals(datapath))
            {
                riverData = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(params.world.getDatapath() + params.world.getRiverDataFile())));
            }
            else
            {
                riverData = new BufferedReader(new FileReader(riverDataFile));
            }

            String riverDataLine;
            String[] tokens;

            // Skip the header lines.
            for (int i = 0; i < 6; ++i)
            {
                riverDataLine = riverData.readLine();
            }

            for (int curr_row = 0; curr_row < land.getSubAreaLowerRight().y; ++curr_row)
            {
                riverDataLine = riverData.readLine();

                // As with assignParcels(), we only bother with processing the
                // current line iff it is within the selected subarea which,
                // again, will default to the entire RiftLand if the users has
                // not selected a subarea.
                if (curr_row >= land.getSubAreaUpperLeft().y)
                {
                    tokens = riverDataLine.split("\\s+");

                    for (int curr_col = land.getSubAreaUpperLeft().x; curr_col < land.getSubAreaLowerRight().x; ++curr_col)
                    {
                        Parcel p = (Parcel) land.getParcel(curr_col, curr_row);

                        if (p instanceof GrazableArea)
                        {
                            double parcelFlowRate = Double.parseDouble(tokens[curr_col]);
                            double parcelMaxFlow = java.lang.Math.min(parcelFlowRate, params.world.getPermanentWaterThreshold());

                            riverGrid.field[curr_col][curr_row] = parcelMaxFlow;

                            totalAccumulationSoFar += parcelMaxFlow;

                            // Uncomment if you want to do a log transform on
                            // the data to get more spread on watering holes.
//                            totalAccumulationSoFar += parcelFlowRate == 0.0 ? 0.0 : Math.log(parcelFlowRate); //riverGrid.get(curr_col, curr_row);

                            roulette[curr_col][curr_row] = totalAccumulationSoFar;
                        }

                    }
                }
            }
        } catch (IOException ex)
        {
            Logger.getLogger(Weather.class.getName()).log(Level.SEVERE, null, ex);
        }

        // assign waterhole based on flow accumulation
        // similar to population assignment in the parcel

        final double ticksize = totalAccumulationSoFar / params.world.getNumInitialWateringHoles();

        double r = random.nextDouble() * ticksize;

        int xx = land.getSubAreaUpperLeft().x;
        int yy = land.getSubAreaUpperLeft().y;

        // For all the water holes we wish to place
        for (int i = 0; i < params.world.getNumInitialWateringHoles(); i++)
        {

            // If we randomly happen to pick a location that already has a
            // water hole, try again until we pick a novel location.
            do
            {
                while (roulette[xx][yy] < r)
                {
                    // Increment to next coordinate
                    xx++;

                    if ( xx >= land.getSubAreaLowerRight().x )
                    {
                        // Time to scan the next row.
                        yy++;

                        // If we're out of parcels, reset to the whole thing,
                        // rejiggle the offset, and start over.
                        if ( yy >= land.getSubAreaLowerRight().y )
                        {
//                            System.out.println("Exhausted area for water holes; restarting.");

                            r = random.nextDouble() * ticksize;

                            yy = land.getSubAreaUpperLeft().y;
                        }
                        
                        xx = land.getSubAreaUpperLeft().x;
                    }
                }

                // Very large basins of attraction for water holes will try
                // to assign more than one water hole to the same location;
                // however, there can be at most one water hole per parcel.
                // So, if there is already a wataer hole here, just move along
                // and hope the next tick is at a different location.
                if (waterHolesGrid.getObjectsAtLocation(xx, yy) != null)
                {
                    r += ticksize;
                }


            } while (waterHolesGrid.getObjectsAtLocation(xx, yy) != null);

            // (x,y) contains a viable location, so place one
            Parcel location = land.getParcel(xx, yy);
            assert(location instanceof GrazableArea);
            int maxWaterHoleContent = 3000000; // max water hole content is 3,000,000 liters
            WaterHole newWaterHole = new WaterHole((GrazableArea)location, riverGrid.get(xx, yy), maxWaterHoleContent, this, params);
           
            waterHolesGrid.setObjectLocation(newWaterHole, xx, yy);
            this.add(newWaterHole);

            r += ticksize;
        }
        Logger.getLogger(World.class.getName()).info("Computing Voronoi diagram for waterHoles...");
        computeVoronoi(land);
        Logger.getLogger(World.class.getName()).info("Leaving WaterHoles.assignWaterHoleBiasedLocator(World world)");
        assert(repOK());
    }
    
    private void computeVoronoi(Land land)
    {
        if (waterHolesGrid.getAllObjects().isEmpty())
        {
            voronoi = null;
            return;
        }
        final Bag allWaterHoles = waterHolesGrid.getAllObjects();
        List<Int2D> waterHolePoints = new ArrayList<Int2D>(allWaterHoles.size()) {{
            for (Object o : allWaterHoles)
            {
                assert(o instanceof WaterHole);
                WaterHole wh = (WaterHole) o;
                add(new Int2D(wh.getX(), wh.getY()));
            }
        }};
        voronoi = new SquareDiscreteVoronoi(land.getWidth(), land.getHeight(), waterHolePoints);
    }
    
    public int countWaterHoles(int upperLeftX, int upperLeftY, int lowerRightX, int lowerRightY)
    {
        int counter = 0;

        for (Object o : waterHolesGrid.locationAndIndexHash.entrySet())
        {
            Map.Entry pairs = (Map.Entry) o;
            Int2D waterHole = ((WaterHole) pairs.getKey()).getLocation();
            int x = waterHole.getX();
            int y = waterHole.getY();

            if (x >= upperLeftX && x <= lowerRightX && y >= upperLeftY && y <= lowerRightY)
                counter++;
        }
        return counter;
    }
    
    final public boolean repOK()
    {
        return waterHolesGrid != null
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(waterHolesGrid.getAllObjects(), WaterHole.class))
                && params != null;
    }
}
