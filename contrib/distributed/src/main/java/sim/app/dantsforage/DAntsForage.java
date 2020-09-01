package sim.app.dantsforage;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.Steppable;
import sim.field.grid.*;
import sim.util.Interval;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.field.partitioning.IntPoint;
import sim.field.partitioning.NdPoint;
import sim.field.partitioning.QuadTreePartition;


import sim.util.Timing.*; 
import sim.util.MPITest.*;

import java.util.ArrayList;
import java.util.HashMap;

import mpi.*;

public class DAntsForage extends DSimState{


	
	
	    private static final long serialVersionUID = 1;

	    public static final int GRID_HEIGHT = 100;
	    public static final int GRID_WIDTH = 100;

	    public static final int HOME_XMIN = 25;
	    public static final int HOME_XMAX = 25;
	    public static final int HOME_YMIN = 25;
	    public static final int HOME_YMAX = 25;

	    public static final int FOOD_XMIN = 75;
	    public static final int FOOD_XMAX = 75;
	    public static final int FOOD_YMIN = 75;
	    public static final int FOOD_YMAX = 75;

	    public static final int NO_OBSTACLES = 0;
	    public static final int ONE_OBSTACLE = 1;
	    public static final int TWO_OBSTACLES = 2;
	    public static final int ONE_LONG_OBSTACLE = 3;

	    public static final int OBSTACLES = TWO_OBSTACLES;
	        
	    public static final int ALGORITHM_VALUE_ITERATION = 1;
	    public static final int ALGORITHM_TEMPORAL_DIFERENCE = 2;
	    public static final int ALGORITHM = ALGORITHM_VALUE_ITERATION;
	        
	    public static final double IMPOSSIBLY_BAD_PHEROMONE = -1;
	    public static final double LIKELY_MAX_PHEROMONE = 3;
	        
	    public static final int HOME = 1;
	    public static final int FOOD = 2;
	        
	        
	    public int numAnts = 100;
	    public double evaporationConstant = 0.999;
	    public double reward = 1.0;
	    public double updateCutDown = 0.9;
	    public double diagonalCutDown = computeDiagonalCutDown();
	    public double computeDiagonalCutDown() { return Math.pow(updateCutDown, Math.sqrt(2)); }
	    public double momentumProbability = 0.8;
	    public double randomActionProbability = 0.1;
	        
	        
	    // some properties
	    public int getNumAnts() { return numAnts; }
	    public void setNumAnts(int val) {if (val > 0) numAnts = val; }
	        
	    public double getEvaporationConstant() { return evaporationConstant; }
	    public void setEvaporationConstant(double val) {if (val >= 0 && val <= 1.0) evaporationConstant = val; }

	    public double getReward() { return reward; }
	    public void setReward(double val) {if (val >= 0) reward = val; }

	    public double getCutDown() { return updateCutDown; }
	    public void setCutDown(double val) {if (val >= 0 && val <= 1.0) updateCutDown = val;  diagonalCutDown = computeDiagonalCutDown(); }
	    public Object domCutDown() { return new Interval(0.0, 1.0); }

	    public double getMomentumProbability() { return momentumProbability; }
	    public void setMomentumProbability(double val) {if (val >= 0 && val <= 1.0) momentumProbability = val; }
	    public Object domMomentumProbability() { return new Interval(0.0, 1.0); }

	    public double getRandomActionProbability() { return randomActionProbability; }
	    public void setRandomActionProbability(double val) {if (val >= 0 && val <= 1.0) randomActionProbability = val; }
	    public Object domRandomActionProbability() { return new Interval(0.0, 1.0); }


	    public DIntGrid2D sites;
	    public DDoubleGrid2D toFoodGrid;
	    public DDoubleGrid2D toHomeGrid;
	    public DDenseGrid2D<DAnt> buggrid;
	    public DIntGrid2D obstacles;
	    
	    
	    


		public DAntsForage(long seed) {
			super(seed, GRID_WIDTH, GRID_HEIGHT, 5);

	        // make new grids
	        sites = new DIntGrid2D(getPartitioning(), this.aoi, 0, this);
	        toFoodGrid = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
	        toHomeGrid = new DDoubleGrid2D(getPartitioning(), this.aoi, 0, this);
	        //valgrid2 = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT, 0);
	        buggrid = new DDenseGrid2D<DAnt>(getPartitioning(), this.aoi, this);
	        obstacles = new DIntGrid2D(getPartitioning(), this.aoi, 0, this);
			// TODO Auto-generated constructor stub
		}	        

		
		protected void startRoot() {
			HashMap<IntPoint, ArrayList<DAnt>> agents = new HashMap<IntPoint, ArrayList<DAnt>>();
			
			IntPoint point = new IntPoint((HOME_XMAX+HOME_XMIN)/2,(HOME_YMAX+HOME_YMIN)/2); //same start for each ant
	        for(int x=0; x < numAnts; x++)
            {
	        	DAnt ant = new DAnt(reward);
	        	
				if (!agents.containsKey(point))
					agents.put(point, new ArrayList<DAnt>());
				agents.get(point).add(ant);

            }
	        
	        
	        // initialize the grid with the home and food sites
	        for( int x = HOME_XMIN ; x <= HOME_XMAX ; x++ )
	            for( int y = HOME_YMIN ; y <= HOME_YMAX ; y++ )
	            	sites.add(new IntPoint(x, y), HOME);

	        for( int x = FOOD_XMIN ; x <= FOOD_XMAX ; x++ )
	            for( int y = FOOD_YMIN ; y <= FOOD_YMAX ; y++ )
	            	sites.add(new IntPoint(x, y), FOOD);
	        
			sendRootInfoToAll("agents",agents);

			
		}
		
		
	    public void start()
	        {
	        super.start();  // clear out the schedule

	        
	        
	        int startx = getPartitioning().getPartition().ul().getArray()[0];  //ul and br
            int endx = getPartitioning().getPartition().br().getArray()[0];
	        int starty = getPartitioning().getPartition().ul().getArray()[1];  //ul and br
            int endy = getPartitioning().getPartition().br().getArray()[1];            
            
            System.out.println("Partion "+startx+", "+starty+"  "+endx+", "+endy);
            
	        switch( OBSTACLES )
	            {
	            case NO_OBSTACLES:
	                break;
	            case ONE_OBSTACLE:
	                for( int x = startx ; x < endx ; x++ )
	                    for( int y = starty ; y < endy ; y++ )
	                        {
	                        //obstacles.field[x][y] = 0;
	                    	obstacles.add(new IntPoint(x, y), 0);
	                    	
	                        if( ((x-55)*0.707+(y-35)*0.707)*((x-55)*0.707+(y-35)*0.707)/36+
	                            ((x-55)*0.707-(y-35)*0.707)*((x-55)*0.707-(y-35)*0.707)/1024 <= 1 )
	                            //obstacles.field[x][y] = 1;
	                    	    obstacles.add(new IntPoint(x, y), 1);

	                        }
	                break;
	            case TWO_OBSTACLES:
	                for( int x = startx ; x < endx ; x++ )
	                    for( int y = starty ; y < endy ; y++ )
	                        {
	                        //obstacles.field[x][y] = 0;
	                    	obstacles.add(new IntPoint(x, y), 0);
	                        if( ((x-45)*0.707+(y-25)*0.707)*((x-45)*0.707+(y-25)*0.707)/36+
	                            ((x-45)*0.707-(y-25)*0.707)*((x-45)*0.707-(y-25)*0.707)/1024 <= 1 )
	                            //obstacles.field[x][y] = 1;
		                    	obstacles.add(new IntPoint(x, y), 1);

	                        if( ((x-35)*0.707+(y-70)*0.707)*((x-35)*0.707+(y-70)*0.707)/36+
	                            ((x-35)*0.707-(y-70)*0.707)*((x-35)*0.707-(y-70)*0.707)/1024 <= 1 )
	                            //obstacles.field[x][y] = 1;
		                    	obstacles.add(new IntPoint(x, y), 1);

	                        }
	                break;
	            case ONE_LONG_OBSTACLE:
	                for( int x = startx ; x < endx ; x++ )
	                    for( int y = starty ; y < endy ; y++ )
	                        {
	                        //obstacles.field[x][y] = 0;
	                    	obstacles.add(new IntPoint(x, y), 0);
	                        if( (x-60)*(x-60)/1600+
	                            (y-50)*(y-50)/25 <= 1 )
	                            //obstacles.field[x][y] = 1;
     	                    	obstacles.add(new IntPoint(x, y), 1);

	                        }
	                break;
	            }





	        
			HashMap<IntPoint, ArrayList<DAnt>> agents = (HashMap<IntPoint, ArrayList<DAnt>>) getRootInfo("agents");
			
			//int tempX = 0;
			for (IntPoint p : agents.keySet()) {
				for (DAnt a : agents.get(p)) {
					if (partition.getPartition().contains(p)) {
						buggrid.addAgent(p, a); //this adds to schedule, look once vs repeat
						//buggrid.addRepeatingAgent(p, a, 0, 1);

		                //tempX = tempX + 1;
					}
				}

			}


	        // Schedule evaporation to happen after the ants move and update
			
			//here is the problem
			//DSteppable needs to be passed here?
	        schedule.scheduleRepeating(Schedule.EPOCH,1, new DSteppable()
	            {
	            public void step(SimState state) { toFoodGrid.multiply(evaporationConstant); toHomeGrid.multiply(evaporationConstant); }
	            }, 1);

	        }
	    
	    

	    


	    public static void main(String[] args) throws MPIException
	        {
	        doLoopDistributed(DAntsForage.class, args);
	        System.exit(0);
	        }    
	    }
	    
	    
	    
	    
	    



