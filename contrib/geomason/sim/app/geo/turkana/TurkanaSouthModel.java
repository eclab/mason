/**
 ** TurkanaSouthModel.java
 **
 ** Copyright 2011 by Andrew Crooks, Joey Harrison, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package turkana;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.io.geo.ArcInfoASCGridExporter;
import sim.io.geo.ArcInfoASCGridImporter;


/*
 * TurkanaSouthModel
 * Main simulation class for the TurkanaSouth model.
 * 
 * Author: Joey Harrison & Mark Coletti
 * 
 */
public class TurkanaSouthModel extends SimState
{

    private static final long serialVersionUID = 1L;
    GeomGridField populationDensityGrid;	// integer [0,inf] indicating relative density
    DoubleGrid2D rainGrid; 			// double [0,inf] indicating rain in mm/hr
    GeomGridField[] monthlyRainGrids;	// array of rain grids
    GeomGridField NdviGrid;			// double [0,1] indicating level of vegetation
    DoubleGrid2D vegetationGrid;			// double [0,maxVegetationLevel]
    SparseGrid2D agentGrid;
    ArrayList<Turkanian> agents = new ArrayList<Turkanian>();
    
    
    public int ticksPerMonth = 1;
    public int getTicksPerMonth() { return ticksPerMonth; }
    public void setTicksPerMonth(int val) { ticksPerMonth = val; }

    public double vegetationGrowthRate = 0.1;	// for tweaking the vegetation growth
    public double getVegetationGrowthRate() { return vegetationGrowthRate; }
    public void setVegetationGrowthRate(double val) { vegetationGrowthRate = val; }

    public double vegetationConsumptionRate = 0.1; // how much vegetation a herd can eat in a month
    public double getVegetationConsumptionRate() { return vegetationConsumptionRate; }
    public void setVegetationConsumptionRate(double val) { vegetationConsumptionRate = val; }

    public double maxVegetationLevel = 1;
    public double getMaxVegetationLevel() { return maxVegetationLevel; }
    public void setMaxVegetationLevel(double val) { maxVegetationLevel = val; }

    public double energyPerUnitOfVegetation = 15;	// energy gained from eating one unit of vegetation
    public double getEnergyPerUnitOfVegetation() { return energyPerUnitOfVegetation; }
    public void setEnergyPerUnitOfVegetation(double val) { energyPerUnitOfVegetation = val; }

    public double birthEnergy = 20;	// new agents/herds begin with this much energy
    public double getBirthEnergy() { return birthEnergy; }
    public void setBirthEnergy(double val) { birthEnergy = val; }

    public double energyConsumptionRate = 1;	// energy used per month
    public double getEnergyConsumptionRate() { return energyConsumptionRate; }
    public void setEnergyConsumptionRate(double val) { energyConsumptionRate = val; }

    public double starvationLevel = -2;		// cows can survive for up to 60 days without food
    public double getStarvationLevel() { return starvationLevel; }
    public void setStarvationLevel(double val) { starvationLevel = val; }

    public boolean initWithNDVI = true;	// if false, the initial vegetaion will be zero
    public boolean getInitWithNDVI() { return initWithNDVI; }
    public void setInitWithNDVI(boolean val) { initWithNDVI = val; }

    public int numberOfAgents = 50;
    public int getNumberOfAgents() { return numberOfAgents; }
    public void setNumberOfAgents(int val) { numberOfAgents = val; }

    public int herderVision = 1;	// how far away herders look when considering where to go (not yet implemented)
//	public int getHerderVision() { return herderVision; }
//	public void setHerderVision(int val) { herderVision = val; }
    public int windowWidth = 400;
//	public int getWindowWidth() { return windowWidth; }
//	public void setWindowWidth(int val) { windowWidth = val; }
    public int windowHeight = 400;
//	public int getWindowHeight() { return windowHeight; }
//	public void setWindowHeight(int val) { windowHeight = val; }
    public boolean printStats = true;	// useful for printing the stats when running from the cmd line but not the gui

    public int monthsOfWeather = 144;	// there are 144 files of monthly rainfall data
    public int month = 0;	// current month



    public TurkanaSouthModel(long seed)
    {
        super(seed);
    }



    @Override
    public void finish()
    {
        super.finish();

        // This is an example of how to automatically write grid data when the 
        // simulation finishes.
        try
        {
            // Write out the population density grid field; it should be exactly
            // like "data/tspop2007.txt".
            BufferedWriter fos = new BufferedWriter( new FileWriter("newpop.asc") );

            ArcInfoASCGridExporter.write(this.populationDensityGrid, fos);

            fos.close();

        } catch (IOException ex)
        {
            Logger.getLogger(TurkanaSouthModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    @Override
    public void start()
    {
        super.start();
        month = 0;

        try
        {
            // Read the raster GIS data
            populationDensityGrid = new GeomGridField();

            InputStream inputStream = TurkanaSouthModel.class.getResourceAsStream("data/tspop2007.txt");
            ArcInfoASCGridImporter.read(inputStream, GridDataType.INTEGER, populationDensityGrid);

            // Example of how to use GDAL to read the same dataset
//            URL inputSource = TurkanaSouthModel.class.getResource("data/turkana/tspop2007.txt");
//            GDALImporter.read(inputSource, GridDataType.INTEGER, populationDensityGrid);

            NdviGrid = new GeomGridField();

            inputStream = TurkanaSouthModel.class.getResourceAsStream("data/ts_ndvi.txt");
            ArcInfoASCGridImporter.read(inputStream, GridDataType.DOUBLE, NdviGrid);

            // Read all 144 months of rainfall data into an array
            monthlyRainGrids = new GeomGridField[monthsOfWeather];
            for (int i = 0; i < monthsOfWeather; i++)
            {
                monthlyRainGrids[i] = new GeomGridField();
                
                inputStream = TurkanaSouthModel.class.getResourceAsStream(String.format("data/%d.txt", i + 1));
                ArcInfoASCGridImporter.read(inputStream, GridDataType.DOUBLE, monthlyRainGrids[i]);
            }

            // rainGrid will hold the current month's rainfall data. Just need the dimensions for now.
            rainGrid = (DoubleGrid2D) monthlyRainGrids[0].getGrid();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // create the agent and vegetation grids to match the pop. grid's dimensions
        agentGrid = new SparseGrid2D(populationDensityGrid.getGridWidth(), populationDensityGrid.getGridHeight());
        vegetationGrid = new DoubleGrid2D(populationDensityGrid.getGridWidth(), populationDensityGrid.getGridHeight());
        if (initWithNDVI)
        {
            vegetationGrid.setTo((DoubleGrid2D)NdviGrid.getGrid());
        }

        createInitialPopulation();
        for (int i = 0; i < numberOfAgents; i++)
        {
            schedule.scheduleOnce(agents.get(i));
        }

        schedule.scheduleRepeating(new Steppable()
        {

            @Override
            public void step(SimState state)
            {
                // check to see if it's time to switch months and rain grids
                if (schedule.getSteps() % ticksPerMonth == 0)
                {
                    rainGrid.setTo((DoubleGrid2D)monthlyRainGrids[month % monthlyRainGrids.length].getGrid());
                    month++;
                }

                // grow the grass
                for (int j = 0; j < vegetationGrid.getHeight(); j++)
                {
                    for (int i = 0; i < vegetationGrid.getWidth(); i++)
                    {
                        double rainfall = getRainfall(i, j);
                        vegetationGrid.field[i][j] += 1.057 * Math.pow((rainfall / ticksPerMonth), 1.001) * ((DoubleGrid2D)NdviGrid.getGrid()).field[i][j] * vegetationGrowthRate;
                        vegetationGrid.field[i][j] = clamp(vegetationGrid.field[i][j], 0, maxVegetationLevel);
                    }
                }

                if (printStats)
                {
                    System.out.format("Step: %d Population: %d\n", schedule.getSteps(), agents.size());
                }
            }

        });
    }



    /**
     * Clamp the given value to be between min and max.
     */
    private double clamp(double value, double min, double max)
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }



    /**
     * Create the initial population based on the prior population densities.
     */
    public void createInitialPopulation()
    {
        int width = populationDensityGrid.getGridWidth();
        int height = populationDensityGrid.getGridHeight();
        int length = width * height;
        double total = 0;
        double cumul[] = new double[length];
        int k = 0;

        // calculate a 1D array of cumulative probabilities
        for (int j = 0; j < height; j++)
        {
            for (int i = 0; i < width; i++)
            {
                total += ((IntGrid2D)populationDensityGrid.getGrid()).field[i][j];
                cumul[k++] = total;
            }
        }

        // create the agents and add them
        agents.clear();
        for (int i = 0; i < numberOfAgents; i++)
        {
            double val = random.nextDouble() * total; // [0,total)
            int index = linearSearch(cumul, val);
            if (index == -1)
            {	// this should never happen
                System.out.println("ERROR: population sampling range failure.");
                continue;
            }

            // calculate the x and y indices based on the linear index
            int x = index % width;
            int y = index / width;

            Turkanian t = new Turkanian(this, x, y);
            t.energy = random.nextDouble() * birthEnergy;
            agents.add(t);
            agentGrid.setObjectLocation(t, x, y);
        }
    }



    /**
     * @return the current rainfall corresponding to the given coordinates in the vegetation grid.
     */
    public double getRainfall(int i, int j)
    {

        int vWidth = vegetationGrid.getWidth();
        int vHeight = vegetationGrid.getHeight();
        int rWidth = rainGrid.getWidth();
        int rHeight = rainGrid.getHeight();

        // calculate the width and height ratios between the rain and veg grid.
        // Since we're using these to rescale the *index* and arrays are zero-based,
        // we need to subtract one. For example (in 1-d):
        // rWidth = 3 (indices: 0,1,2), vWidth = 4 (indices: 0,1,2,3)
        // r_per_v_width = (3-1) / (4-1) = 2/3 = 0.667
        //
        // i = 0: rx = round(0 * 0.667) = 0
        // i = 1: rx = round(1 * 0.667) = 1
        // i = 2: rx = round(2 * 0.667) = 1
        // i = 3: rx = round(3 * 0.667) = 2

        double r_per_v_width = (rWidth - 1.0) / (vWidth - 1.0);
        double r_per_v_height = (rHeight - 1.0) / (vHeight - 1.0);

        int rx = (int) Math.round(i * r_per_v_width);
        int ry = (int) Math.round(j * r_per_v_height);

        // this was crucial during debugging
        if ((rx >= rWidth) || (ry >= rHeight))
        {
            System.out.format("ERROR: getRainfall index calculation out of range.\n");
            return 0;
        }

        return rainGrid.field[rx][ry];
    }



    /**
     * Create offspring of the current agent and add them to the grid in the same cell.
     * @param parent
     */
    public void createOffspring(Turkanian parent)
    {
        if (parent.energy <= birthEnergy)
        {
            return;
        }

        Turkanian offspring = new Turkanian(this, parent.x, parent.y);
        parent.energy -= birthEnergy;
        offspring.energy = 0;
        agents.add(offspring);
        agentGrid.setObjectLocation(offspring, offspring.x, offspring.y);
        schedule.scheduleOnce(offspring);
    }



    /**
     * Remove an agent who has died.
     */
    public void removeAgent(Turkanian t)
    {
        agents.remove(t);
        agentGrid.remove(t);
    }



    /**
     * Find the index of the given value in the given array. If the value isn't
     * in the array, it returns the first one larger than the value.
     */
    static public int linearSearch(double[] array, double value)
    {
        for (int i = 0; i < array.length; i++)
        {
            if (value <= array[i])
            {
                return i;
            }
        }

        return -1;
    }





    /**
     * Main function, runs the simulation without any visualization.
     * @param args
     */
    public static void main(String[] args)
    {
        doLoop(TurkanaSouthModel.class, args);
        System.exit(0);
    }

}
