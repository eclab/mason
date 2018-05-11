package CDI.src.movement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import CDI.src.movement.data.BarChartFactor.Factor;
import CDI.src.environment.Cell;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.util.Double2D;

public class WorldAgent implements Steppable {

	private static final long serialVersionUID = 1L;

	public class WheelCell implements Comparable<WheelCell>
	{
		public double prob;
		public int index;
		
		public WheelCell(double prob, int index)
		{
			this.prob = prob;
			this.index = index;
		}

		

		@Override
		public int compareTo(WheelCell o) {
			if(this.prob>o.prob)
				return 1;
			else if(this.prob<o.prob)
				return -1;
			
			return 0;
		}
	}
	
	
	public NorthLandsMovement model;
    private FileWriter migrationWriter = null;
	public double[] urbanScores, ruralScores, urbanDesirability, ruralDesirability;
	private int counter = 0;
    private int oneYrCounter = 0;
	private double urbanTotalProb = 0;
	private double ruralTotalProb = 0;
	private double[] urbanCumulProb = null;
	private double[] ruralCumulProb = null;
	public double infrastructureExpenses=0;

	public ArrayList<Event> events = new ArrayList<Event>();
	public ArrayList<WheelCell> ruralWheel = new ArrayList<WheelCell>();
	public ArrayList<WheelCell> urbanWheel = new ArrayList<WheelCell>();
	
	public HouseholdTracker tracker;
	
	
	public WorldAgent(NorthLandsMovement model)
	{
		this.model = model;
		counter = 0;
		urbanScores = new double[model.map.canadaCells.size()];
		ruralScores = new double[model.map.canadaCells.size()];
		
		urbanDesirability = new double[model.map.canadaCells.size()];
		ruralDesirability = new double[model.map.canadaCells.size()];
		
    	this.updateRouletteWheel();
		this.updateCumulativeProbs();
		
		this.tracker = new HouseholdTracker(this.model);
		
		this.resetMigrationLogFile();
	}

	
    private void resetMigrationLogFile() {

        if (this.migrationWriter != null) {
            // first close the old file
            try { this.migrationWriter.close(); } 
            catch (IOException e) { e.printStackTrace(); }
        }

        // generate a new file name
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        // get current date time with Date()
        Date date = new Date();
        String suffix = dateFormat.format(date);

        try 
        {
            // Should we create a new parameter for this file?
            String p = "./" + model.parameters.censusMigrationFilePath + "-" + suffix + ".csv";
            System.out.println(p);
            File file = new File(p);

            if (!file.exists()) { file.createNewFile(); }
            migrationWriter = new FileWriter(file);
        } 
        catch (FileNotFoundException e) 
        {
            System.err.println("Could not open file: " + e.getMessage());
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        // write header
        try 
        {
            migrationWriter.append("Year");
            for(int i = 0; i < model.numProvinces; i++)
                migrationWriter.append("," + model.provinceNames[i]);
            migrationWriter.append("\n");

        } 
        catch (IOException e) 
        {
            System.err.println("IOException: " + e.getMessage());
        }        
    }

    public void closeMigrationLogFile()
    {
        try { migrationWriter.close(); }
        catch (IOException e)
        { System.err.println("IOException: " + e.getMessage()); }
    }
    

	public void updateDesirability()
	{
        double[] tempData = model.map.getTempDesData();
        double[] elevData = model.map.getElevDesData();
        double[] portData = model.map.getPortDesData();
        double[] riverData = model.map.getRiverDesData();
        DoubleGrid2D oppGrid = model.infrastructureAvailabilityGrid;
        DoubleGrid2D urbanSocGrid = model.urbanSocialWeightGrid;
        DoubleGrid2D urbanAdjGrid = model.urbanAdjacentSocialWeightGrid;

double fred = 0; 
double fredm = 0;
int fredx, fredy, fredmx, fredmy;

    int index = 0;
	for (Cell cell : model.map.canadaCells)
	{
            double urbanScore = 0.0;
            double temp = 0.0, max = 0.0;
            temp = model.parameters.urbanTempCoeff * tempData[index];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.TEMPERATURE;
            	max = Math.abs(temp);
            }
//System.out.printf("urban: temp: %.2f", temp);
            urbanScore += temp;
            
            temp = model.parameters.urbanElevCoeff * elevData[index];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.ELEVATION;
            	max = Math.abs(temp);
            }
//System.out.printf(" elevation: %.2f", temp);
            urbanScore += temp;
            
            temp = model.parameters.urbanPortCoeff * portData[index];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.PORT;
            	max = Math.abs(temp);
            }
//System.out.printf(" port: %.2f", temp);
            urbanScore += temp;
            
            temp = model.parameters.urbanRiverCoeff * riverData[index];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.RIVER;
            	max = Math.abs(temp);
            }
//System.out.printf(" river: %.2f", temp);
            urbanScore += temp;
            
            temp = model.parameters.urbanInfrastructureAvailabilityCoeff * oppGrid.field[cell.x][cell.y];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.OPPORTUNITY;
            	max = Math.abs(temp);
            }
//System.out.printf(" opport: %.2f", temp);
if (temp < fred) 
{
    fred = temp;
    fredx = cell.x;
    fredy = cell.y;
//    System.out.printf("updated min opp to: %6.3f ", temp);
//    System.out.println(" at urban cell (" + cell.x + ", " + cell.y + ")");
}
if (temp > fredm) 
{
    fredm = temp;
    fredmx = cell.x;
    fredmy = cell.y;
//    System.out.printf("updated max opp to: %6.3f ", temp);
//    System.out.println(" at urban cell (" + cell.x + ", " + cell.y + ")");
}
            urbanScore += temp;
            

            temp = urbanSocGrid.field[cell.x][cell.y] + urbanAdjGrid.field[cell.x][cell.y];
            if(Math.abs(temp)>max)
            {
            	cell.urbanMajorFactor = Factor.SOCIAL;
            	max = Math.abs(temp);
            }
//System.out.printf(" social: %.2f%n", temp);            
            urbanScore += temp;
            
            
            urbanDesirability[index] = urbanScore;

            
            DoubleGrid2D ruralSocGrid = model.ruralSocialWeightGrid;
            DoubleGrid2D ruralAdjGrid = model.ruralAdjacentSocialWeightGrid;

            double ruralScore = 0.0;
            temp = 0.0;
            max = 0.0;
            temp = model.parameters.ruralTempCoeff * tempData[index];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.TEMPERATURE;
            	max = Math.abs(temp);
            }
//System.out.printf("rural: temp: %5.2f", temp);
            ruralScore += temp;
            
            temp = model.parameters.ruralElevCoeff * elevData[index];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.ELEVATION;
            	max = Math.abs(temp);
            }
//System.out.printf(" elevation: %5.2f", temp);
            ruralScore += temp;
            
            temp = model.parameters.ruralPortCoeff * portData[index];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.PORT;
            	max = Math.abs(temp);
            }
//System.out.printf(" port: %5.2f", temp);
            ruralScore += temp;
            
            temp = model.parameters.ruralRiverCoeff * riverData[index];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.RIVER;
            	max = Math.abs(temp);
            }
//System.out.printf(" river: %5.2f", temp);
            ruralScore += temp;
            
            temp = model.parameters.ruralInfrastructureAvailabilityCoeff * oppGrid.field[cell.x][cell.y];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.OPPORTUNITY;
            	max = Math.abs(temp);
            }
//System.out.printf(" opport: %5.2f", temp);
if (temp < fred) 
{
    fred = temp;
    fredx = cell.x;
    fredy = cell.y;
    System.out.printf("updated min opp to: %6.3f ", temp);
    System.out.println(" at urban cell (" + cell.x + ", " + cell.y + ")");
}
if (temp > fredm) 
{
    fredm = temp;
    fredmx = cell.x;
    fredmy = cell.y;
    System.out.printf("updated max opp to: %6.3f ", temp);
    System.out.println(" at rural cell (" + cell.x + ", " + cell.y + ")");
}

            ruralScore += temp;
            

            temp = ruralSocGrid.field[cell.x][cell.y] + ruralAdjGrid.field[cell.x][cell.y];
            if(Math.abs(temp)>max)
            {
            	cell.ruralMajorFactor = Factor.SOCIAL;
            	max = Math.abs(temp);
            }
//System.out.printf(" social: %5.2f%n", temp);
            ruralScore += temp;


            ruralDesirability[index] = ruralScore;
            index++;
            //System.out.println(index);
		}
		
        //System.out.println("updateRouletteWheel: remap");
        // convert the value between 0 and 1
        Double2D minMax = this.calcDesirabilityBounds(urbanDesirability);
        double urbanMin = minMax.x;
        double urbanMax = minMax.y;
        double urbanRange = urbanMax - urbanMin;


        minMax = this.calcDesirabilityBounds(ruralDesirability);
        double ruralMin = minMax.x;
        double ruralMax = minMax.y;
        double ruralRange = ruralMax - ruralMin;
        
        //System.out.println("ruralMax:"+ruralMax+", ruralMin:"+ruralMin);
        //System.out.println("rural range: "+ruralRange);
        
        index = 0;
        for (Cell cell : model.map.canadaCells)
        {
            if(urbanRange!=0)
            	urbanDesirability[index] = (urbanDesirability[index]-urbanMin)/urbanRange;
            if(ruralRange!=0)
            	ruralDesirability[index] = (ruralDesirability[index]-ruralMin)/ruralRange;
            index++;
            //System.out.println(index);
        }
	}
	
	
	public void updateRouletteWheel() 
    {
	    updateDesirability();	
		
	    int index = 0;
	    for (Cell cell : model.map.canadaCells) 
        {
            urbanScores[index] = Math.pow(urbanDesirability[index], model.parameters.urbanDesExp);
            ruralScores[index] = Math.pow(ruralDesirability[index], model.parameters.ruralDesExp);
		    
			if(urbanScores[index]<0&&ruralScores[index]<0)
				System.out.println("less than 0 scores:"+urbanScores[index]+","+ruralScores[index]);
			
			
			model.urbanRouletteWheelGrid.field[cell.x][cell.y] = urbanScores[index];
			model.ruralRouletteWheelGrid.field[cell.x][cell.y] = ruralScores[index];
			
			model.updateBounds(model.rouletteBounds, urbanScores[index]);
			model.updateBounds(model.rouletteBounds, ruralScores[index]);
			
            //System.out.println(urbanScores[index]);
			index++;
            //System.out.println(index);
		}
		
	}

	
	public Double2D calcDesirabilityBounds(double[] scores)
	{
		// calculate bounds of desirability map
    	double min = Double.POSITIVE_INFINITY;
    	double max = Double.NEGATIVE_INFINITY;
    	for(int i = 0;i<scores.length;++i)
    	{
    		double des = scores[i];

			if (des < min)
				min = des;
			if (des > max)
				max = des;
    	}
    	
    	return new Double2D(min, max);
	}

	
	public Double2D calcDesirabilityBounds(DoubleGrid2D grid) 
    {
    	// calculate bounds of desirability map
    	double min = Double.POSITIVE_INFINITY;
    	double max = Double.NEGATIVE_INFINITY;
    	for (Cell cell : model.map.canadaCells)
        {
    			int x = cell.x, y = cell.y;
				double des = grid.field[x][y];

				if (des < min)
					min = des;
				if (des > max)
					max = des;
		}
		
    	return new Double2D(min, max);
    }
	

	@Override
    public void step(SimState state) 
    {

        // increase the counter
        counter++;
        
        // update the urbanDensityThreshold
        if ((counter > 0) && (counter % model.parameters.densityIncrementInterval == 0)) 
        { model.urbanDensity += model.parameters.densityIncrement; }

        eventProcess(counter);

        // update the residence grid
        model.updateResidenceGrid();
        model.updateSocialWeight();

        if (model.parameters.permafrostAffectsInfrastructure) 
        { this.updatePermafrost(); }
        
		// update the infrastructure
        this.updateInfrastructure();
        
        model.collector.setInfrastructureExpenses(this.infrastructureExpenses);
        //System.out.println("worldAgent>step: agent:" + this + ": " + this.infrastructureExpenses);

        int err = model.map.updateTemperatures(model, counter);
        if (err < 0) 
        {
            // If we make things asynchronous, we should schedule an agent to do this.
            model.kill();
        }

        model.updateDesirabilityMap(model.urbanDesirabilityGrid, model.parameters.urbanTempCoeff,
                model.parameters.urbanRiverCoeff, model.parameters.urbanPortCoeff,
                model.parameters.urbanElevCoeff);
        model.updateDesirabilityMap(model.ruralDesirabilityGrid, model.parameters.ruralTempCoeff,
                model.parameters.ruralRiverCoeff, model.parameters.ruralPortCoeff,
                model.parameters.ruralElevCoeff);

        model.updateInfrastructureGrid();
        model.updateInfrastructureAvailabilityGrid();
        model.updateMigrationInfo();
        if (model.censusHeld())
        {
            // Output the census migration results
            try 
            {
                Double time = model.schedule.getTime();
                migrationWriter.append(time.toString());
                for(int i = 0; i < model.numProvinces; i++)
                    migrationWriter.append("," + model.provinceNetMigration[i]);
                migrationWriter.append("\n");
                migrationWriter.flush();

            } catch (IOException e) 
            {
                System.err.println("IOException: " + e.getMessage());
            }        

        }

        // update the roulette wheel
        if ((counter > 0) && (counter % model.parameters.recalSkip == 0)) 
        {
            // System.out.println("update cumulative probs "+counter);
            this.updateRouletteWheel();
            this.updateCumulativeProbs();
        }

        // output to file
        if (model.parameters.recordData) 
        {
            model.collector.updateFile();
            if (model.parameters.trackHousehold) { tracker.track(); }
        }

        // reset some counter for some data
        model.collector.step(counter, model.schedule.getTime());        

        model.collector.setWealthGini(calculateGiniCoeff(0, "wealth"));
        model.collector.setUrbanWealthGini(calculateGiniCoeff(1, "wealth"));
        model.collector.setRuralWealthGini(calculateGiniCoeff(2, "wealth"));

        model.collector.setUrbanGiniCoeff(calculateGiniCoeff(1, "satisfaction"));
        model.collector.setRuralGiniCoeff(calculateGiniCoeff(2, "satisfaction"));

        double urbanMean = calculateDesirabilityMean(1);
        double ruralMean = calculateDesirabilityMean(2);

        double urbanStdev = calculateDesirabilityMoment(urbanMean, 2, 1);
        double ruralStdev = calculateDesirabilityMoment(ruralMean, 2, 2);

        double urbanKurtosis = (calculateDesirabilityMoment(urbanMean, 4, 1) / Math.pow(urbanStdev, 2)) - 3;
        double ruralKurtosis = (calculateDesirabilityMoment(ruralMean, 4, 1) / Math.pow(ruralStdev, 2)) - 3;

        model.collector.setUrbanDesireMean(urbanMean);
        model.collector.setRuralDesireMean(ruralMean);

        model.collector.setUrbanDesireStdev(urbanStdev);
        model.collector.setRuralDesireStdev(ruralStdev);

        model.collector.setUrbanDesireKurtosis(urbanKurtosis);
        model.collector.setRuralDesireKurtosis(ruralMean);

        model.collector.setFederalRevenues(model.federalGovAgent.getFederalRevenues());
        model.collector.setFederalAssets(model.federalGovAgent.getFederalAssets());
    }
	

	private void eventProcess(int time) 
    {
		for(Event event:this.events)
            { event.doEvent(time, this);}
	}
	
	
	private void updateInfrastructure() 
    {	
		this.infrastructureExpenses=0;  // "this" is the agent for the whole world, i.e., Canada
	
        // for each cell in Canada, decide infrastructure changes 
        // three units involved: "diff" = need = diff between households and existing infrastructure
        //                       "cost" of additional infrastructure
        //                       cost to maintain ("maintenanceCost") the new infrastructure
        // each calculate base on 4 cases: (permafrost affects costs or not) x (need for more infrastructure or not)
		for(Cell cell:model.map.canadaCells)
		{	// compare households to existing infrastruture
			double diff = cell.numHouseholds - cell.infrastructure;  
			double cost;
			double maintenanceCost;
            // 4 cases through 2 nested if statements
			if (model.parameters.permafrostAffectsInfrastructure) 
            {
				if (diff>0) // test need to build infrastructure
                {
					diff = diff * model.parameters.infrastructureIncreaseRate 
                           * cell.bearingCapacity;
					cost = diff * infrastructureCostPerUnit(cell); //calculate how much the increase costs
					maintenanceCost = cell.infrastructure * infrastructureCostPerUnit(cell) 
                                      * model.parameters.infrastructureMaintenanceCoefficient;
					cell.netAssets -= cost + maintenanceCost;   // sum cell's assets
					this.infrastructureExpenses += cost + maintenanceCost;  // sum for national costs
				}
				else // too much infrastructure for existing population, let it decay & only pay to maintain what's nec'y
                {
					diff = diff * model.parameters.infrastructureDecreaseRate 
                           * (1.0/cell.bearingCapacity);
					maintenanceCost = cell.numHouseholds * infrastructureCostPerUnit(cell) 
                                      * model.parameters.infrastructureMaintenanceCoefficient;
					cell.netAssets -= maintenanceCost; // no cost of new infrastrcuture
					this.infrastructureExpenses += maintenanceCost;
				}		
			}
			else // not considering permafrost effects
            {
				if (diff>0) // test need to build 
                {
					diff = diff * model.parameters.infrastructureIncreaseRate;
					cost = diff * infrastructureCostPerUnit(cell); //calculate how much it would cost for the given cell 
					maintenanceCost = cell.infrastructure * infrastructureCostPerUnit(cell) 
                                      * model.parameters.infrastructureMaintenanceCoefficient;
					cell.netAssets -= cost + maintenanceCost;
					this.infrastructureExpenses += cost + maintenanceCost;
				}
				else // too much infrastructure, allow it to decay & only pay to maintain what's nec'y
                {
					diff = diff * model.parameters.infrastructureDecreaseRate;
					maintenanceCost = cell.numHouseholds * infrastructureCostPerUnit(cell) 
                                      * model.parameters.infrastructureMaintenanceCoefficient;
					cell.netAssets -= maintenanceCost;
					this.infrastructureExpenses += maintenanceCost;
				}			
			}			
			
			cell.infrastructure += diff;  // update this cell's infrastructure wrt population 
			
			// infrastructure can not be less than zero
			if(cell.infrastructure<0) { cell.infrastructure = 0; }
		} // end loop over all cells	
	}
	
	private double infrastructureCostPerUnit(Cell cell) 
    {
		double totalCost = model.parameters.infrastructureBaseCost*Math.pow(cell.numHouseholds, model.parameters.infrastructureCostExponent);
		double costPerUnit=model.parameters.infrastructureBaseCost;
		if (cell.numHouseholds>0) { costPerUnit = totalCost/cell.numHouseholds; }
		return costPerUnit;
	}



	public int chooseLocation(int agentType)
	{
		switch(agentType)
		{
            case 0:
                return PeopleSprinkler.chooseStochasticallyFromCumulativeProbs(this.urbanCumulProb, this.urbanTotalProb, model.random);
            case 1:
                return PeopleSprinkler.chooseStochasticallyFromCumulativeProbs(this.ruralCumulProb, this.ruralTotalProb, model.random);
            case 2:
            	System.err.println("This should never happen");
			return -1;
		}
		return -1;
	}

	
	public void updateCumulativeProbs()
	{
		double[] cumulProbs = new double[urbanScores.length];
		this.urbanTotalProb = PeopleSprinkler.calcCumulativeProbs(urbanScores, cumulProbs);
		//this.urbanTotalProb = calcCumulativeProbs(urbanScores, urbanWheel);
		this.urbanCumulProb = cumulProbs;
		
		cumulProbs = new double[ruralScores.length];
		//this.ruralTotalProb = calcCumulativeProbs(ruralScores, ruralWheel);
		this.ruralTotalProb = PeopleSprinkler.calcCumulativeProbs(ruralScores, cumulProbs);
		this.ruralCumulProb = cumulProbs;
	}
	
	private void updatePermafrost()
	{
		for (Cell cell:model.map.canadaCells)
		{
			// function  inverse sigmoid
            cell.bearingCapacity = getTwoParamterLogistic(0.0 ,1.0, (1.0-model.map.tempDes.field[cell.x][cell.y]),0.8); 
        }
    }

    
	public double getTwoParamterLogistic(double c, double a, double b, double value)
    {
	    // a = the maximum slope
	    // b = the half-way point between c(min) and 1(max), also where the slope is maximized.  
	    // c = asymptotic minimum 
	    // return probability p(value)  
	    return c + (( 1.0-c)/( 1.0 + Math.exp(- a * (value -b))));      
	}


    public double calculateDesirabilityMean(int mode)
    {
		double[] satis = model.desirabilityArray(mode);
		
		double total=0;
		
		for (int i=0; i<satis.length;i++) 
        {
			total=total+satis[i]; //cost of agent's defection on the neighbor
		}
		double mean = total/satis.length;
		return mean;
	}


	public double calculateDesirabilityMoment(double mean, int moment, int mode) 
    {	
		double[] satis = model.desirabilityArray(mode);
		
		double devs=0;
		for (int i=0; i<satis.length;i++) 
        {
			double dev=Math.pow(satis[i]-mean,moment); //cost of agent's defection on the neighbor
			devs=devs+dev;
		}
		
		double stdev = Math.sqrt(devs/satis.length);
		return stdev;
	}
	
	
	
	//mode 0 = all, 1 = urban, 2 = rural
	public double calculateGiniCoeff(int mode, String attribute) 
    {	
		double[] attributeArray;
		
		if (attribute=="satisfaction") 
        {
			attributeArray=model.desirabilityArray(mode);
		}
		else 
        {
			attributeArray=model.getHouseholdWealthArray(mode);
		}
		
		double[] sortAttribute = attributeArray;
		Arrays.sort(sortAttribute);
		
		double[] cumulativeAttribute = new double[sortAttribute.length];
		
		cumulativeAttribute[0]=sortAttribute[0];
		
		double totalSatisfaction=0;
		
		double[] distancesFromIdeal = new double[sortAttribute.length];
		
		double sumDistancesFromIdeal=0;
		
		
		for (int i=0;i<sortAttribute.length;i++) 
        {
			totalSatisfaction+=sortAttribute[i];
			
			if (i>0) 
            {
				cumulativeAttribute[i]=cumulativeAttribute[i-1]+sortAttribute[i];	
			}
		}
		
		for (int i=0;i<sortAttribute.length;i++) 
        {	
			cumulativeAttribute[i]=cumulativeAttribute[i]/totalSatisfaction;
			double ii = (double) i;
			distancesFromIdeal[i]=(ii/sortAttribute.length)-cumulativeAttribute[i];	
		}
		
		for (int i=0;i<sortAttribute.length;i++) 
        {	
			sumDistancesFromIdeal+=distancesFromIdeal[i];	
		}
		
		double giniCoeff = 2*(sumDistancesFromIdeal/sortAttribute.length);
	
		return giniCoeff;
	}
	
	

	public boolean isUrban(Cell cell) {
		return cell.numHouseholds >= model.urbanDensity;
	}

	public void registerEvent(Event event) {
		this.events.add(event);
		
	}
	
	
}
