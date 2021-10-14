package sim.app.geo.dturkana;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import sim.app.dheatbugs.DHeatBug;
import sim.app.geo.dcampusworld.DCampusWorld;
import sim.app.geo.turkana.TurkanaSouthModel;
import sim.app.geo.turkana.Turkanian;
import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.util.Int2D;

public class DTurkanaSouthModel extends DSimState{
	
	static final long serialVersionUID = 1L;
	public static final int width = 108;
	public static final int height = 114;
	public static final int aoi = 1;// TODO what value???
	
	
    public GeomGridField populationDensityGrid;	// integer [0,inf] indicating relative density
    //DIntGrid2D popluationDensityGrid;
    
    public DoubleGrid2D rainGrid; 			// double [0,inf] indicating rain in mm/hr
    
    public GeomGridField[] monthlyRainGrids;	// array of rain grids
    //DIntGrid2D[] monthlyRainGrids;

    
    public GeomGridField NdviGrid;			// double [0,1] indicating level of vegetation
    //DDoubleGrid2D NdviGrid;
    
    public DDoubleGrid2D vegetationGrid;			// double [0,maxVegetationLevel]
    
    //SparseGrid2D agentGrid;
    public DDenseGrid2D agentGrid; //What type should this be?

    public ArrayList<DTurkanian> agents = new ArrayList<DTurkanian>();
    
    
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



    public DTurkanaSouthModel(long seed)
    {
        super(seed, width, height, aoi);
    }
    
	protected void startRoot()
	{
	    ArrayList<DTurkanian> sendAgents = new ArrayList<DTurkanian>();
	    
        // Read the raster GIS data
        populationDensityGrid = new GeomGridField();

        InputStream inputStream = DTurkanaSouthModel.class.getResourceAsStream("data/tspop2007.txt");
        System.out.println(inputStream);
        //System.exit(-1);
        ArcInfoASCGridImporter.read(inputStream, GridDataType.INTEGER, populationDensityGrid);
		
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

            DTurkanian t = new DTurkanian(x, y);
            t.energy = random.nextDouble() * birthEnergy;
            sendAgents.add(t);
            

        }

		
		
		sendRootInfoToAll("agents", sendAgents);


		
	}
	
    @Override
    public void start()
    {
        super.start();
        month = 0;
        
        // create the agent and vegetation grids to match the pop. grid's dimensions
        //agentGrid = new SparseGrid2D(populationDensityGrid.getGridWidth(), populationDensityGrid.getGridHeight());
        agentGrid = new DDenseGrid2D(this);
        
        ArrayList<DTurkanian> recAgents = (ArrayList<DTurkanian>) getRootInfo("agents");
        System.out.println(recAgents.size());
        
        System.out.println(partition.getLocalBounds());
        //System.exit(-1);
		for (DTurkanian t : recAgents)
		{

			if (partition.getLocalBounds().contains(new Int2D(t.x, t.y)))
				{
		          agents.add(t);
		          agentGrid.add(new Int2D(t.x, t.y), t);
		          //agentGrid.setObjectLocation(t, t.x, t.y);
				}

			
		}
        
	    //System.out.println("a "+this.getPID()+" : "+agents.size()+" : ");
		
        //System.exit(-1);

        try
        {


            // Example of how to use GDAL to read the same dataset
//            URL inputSource = TurkanaSouthModel.class.getResource("data/turkana/tspop2007.txt");
//            GDALImporter.read(inputSource, GridDataType.INTEGER, populationDensityGrid);

            NdviGrid = new GeomGridField();

            InputStream inputStream = DTurkanaSouthModel.class.getResourceAsStream("data/ts_ndvi.txt");
            System.out.println(inputStream);

            ArcInfoASCGridImporter.read(inputStream, GridDataType.DOUBLE, NdviGrid);

            // Read all 144 months of rainfall data into an array
            monthlyRainGrids = new GeomGridField[monthsOfWeather];
            for (int i = 0; i < monthsOfWeather; i++)
            {
                monthlyRainGrids[i] = new GeomGridField();
                
                inputStream = DTurkanaSouthModel.class.getResourceAsStream(String.format("data/%d.txt", i + 1));
                ArcInfoASCGridImporter.read(inputStream, GridDataType.DOUBLE, monthlyRainGrids[i]);
            }

            // rainGrid will hold the current month's rainfall data. Just need the dimensions for now.
            //this is 4x4, NOT the whole space so just pass this to all partitions
            rainGrid = (DoubleGrid2D) monthlyRainGrids[0].getGrid();
            
            System.out.println("rain "+rainGrid.field);

          
            
            
            
            
        } catch (Exception e)
        {
            e.printStackTrace();
        }


        
        
        vegetationGrid = new DDoubleGrid2D(this);
        if (initWithNDVI)
        {
        	DoubleGrid2D fullNdviGrid = (DoubleGrid2D)NdviGrid.getGrid();
        	vegetationGrid = new DDoubleGrid2D(this);
        	
            //for (Int2D p: vegetationGrid.storage.getShape().getPointList()) {
            for (Int2D p :	partition.getLocalBounds().getPointList()) {

            	vegetationGrid.set(p, fullNdviGrid.get(p.getX(), p.getY()));
            }
            
        	
            //vegetationGrid.setTo((DoubleGrid2D)NdviGrid.getGrid());
        }
        
        //System.out.println("veg "+vegetationGrid.storage.storage);


        //createInitialPopulation(); Done in startRoot
        
        
        for (int i = 0; i < agents.size(); i++)
        {
            schedule.scheduleOnce(agents.get(i));
    		//System.out.println(this.getPID()+" : "+agents.size()+" : ");

        }
        
	    //System.out.println("b "+this.getPID()+" : "+agents.size()+" : ");

        //System.exit(-1);

        schedule.scheduleRepeating(new DSteppable()
        {

            @Override
            public void step(SimState state)
            {
                System.out.format("Partition %d : Step: %d Population: %d State Pop: %d\n", ((DSimState)state).getPID(), schedule.getSteps(), agents.size(), ((DTurkanaSouthModel)state).agents.size());
                //System.exit(-1);
                // check to see if it's time to switch months and rain grids
                if (schedule.getSteps() % ticksPerMonth == 0)
                {
                    
                    rainGrid.setTo((DoubleGrid2D)monthlyRainGrids[month % monthlyRainGrids.length].getGrid());

                    

                    //rainGrid.setTo((DoubleGrid2D)monthlyRainGrids[month % monthlyRainGrids.length].getGrid());
                    month++;
                }

                // grow the grass
                //for (int j = 0; j < vegetationGrid.getHeight(); j++)
                //{
                  //  for (int i = 0; i < vegetationGrid.getWidth(); i++)
                    //{
                
                
                //Raj:  Start here
                //for (Int2D p: vegetationGrid.storage.getShape().getPointList()) {
                for (Int2D p :	partition.getLocalBounds().getPointList()) {

                        double rainfall = getRainfall(p.getX(), p.getY());
                        int i = p.getX();
                        int j = p.getY();
                        
                        double newVal = vegetationGrid.getLocal(p) + 1.057 * Math.pow((rainfall / ticksPerMonth), 1.001) * ((DoubleGrid2D)NdviGrid.getGrid()).field[i][j] * vegetationGrowthRate;
                        vegetationGrid.set(p, newVal);
                        newVal = clamp(newVal, 0 , maxVegetationLevel);
                        vegetationGrid.set(p, newVal);

                                           
                }
                
                System.out.format("Partition %d : Step: %d Population: %d State Pop: %d\n", ((DSimState)state).getPID(), schedule.getSteps(), agents.size(), ((DTurkanaSouthModel)state).agents.size());
                //System.exit(-1);

                if (printStats)
                {
                	
                    //System.out.format("Partition %d : Step: %d Population: %d\n", ((DSimState)state).getPID(), schedule.getSteps(), agents.size());
                }
            }

        });
    }
    
    
    /**
     * @return the current rainfall corresponding to the given coordinates in the vegetation grid.
     */
    public double getRainfall(int i, int j)
    {
        int vWidth = NdviGrid.getGrid().getWidth();
        int vHeight = NdviGrid.getGrid().getHeight();
        int rWidth = monthlyRainGrids[0].getGrid().getWidth();
        int rHeight = monthlyRainGrids[0].getGrid().getHeight();

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
        //return rainGrid.getLocal(new Int2D(rx, ry)); //What should be done here?
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
     * Create offspring of the current agent and add them to the grid in the same cell.
     * @param parent
     */
    public void createOffspring(DTurkanian parent)
    {
        if (parent.energy <= birthEnergy)
        {
            return;
        }

        DTurkanian offspring = new DTurkanian(parent.x, parent.y);
        parent.energy -= birthEnergy;
        offspring.energy = 0;
        agents.add(offspring);
        //agentGrid.setObjectLocation(offspring, , offspring.y);
        agentGrid.addLocal(new Int2D(offspring.x, offspring.y), offspring);
        schedule.scheduleOnce(offspring);
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
    
	public static void main(final String[] args)
	{
		doLoopDistributed(DTurkanaSouthModel.class, args);
		System.exit(0);
	}

}
