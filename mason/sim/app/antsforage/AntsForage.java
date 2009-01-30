/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;

import sim.engine.*;
import sim.field.grid.*;


public /*strictfp*/ class AntsForage extends SimState
    {

    public static final int HOME_XMIN = 75;
    public static final int HOME_XMAX = 75;
    public static final int HOME_YMIN = 75;
    public static final int HOME_YMAX = 75;

    public static final int FOOD_XMIN = 25;
    public static final int FOOD_XMAX = 25;
    public static final int FOOD_YMIN = 25;
    public static final int FOOD_YMAX = 25;

    public static final int MAX_ANTS_PER_LOCATION = 10;
    public static final int TIME_TO_LIVE = 1000;
    public static final int NEW_ANTS_PER_TIME_STEP = 2;
    public static final int MAX_ANTS = 1000;
    public static final int INITIALANTS = 2;
    public static final double MIN_PHEROMONE = 0.0;
    public static final double MAX_PHEROMONE = 1000.0;
    public static final double PHEROMONE_TO_LEAVE_BEHIND = 1;

    public static final double EVAPORATE_CONSTANT = 0.0001;
    public static final double DIFFUSION_CONSTANT = 0.0001;
    public static final int GRID_HEIGHT = 100;
    public static final int GRID_WIDTH = 100;

    public static final int NO_OBSTACLES = 0;
    public static final int ONE_OBSTACLE = 1;
    public static final int TWO_OBSTACLES = 2;
    public static final int ONE_LONG_OBSTACLE = 3;

    public static final int OBSTACLES = TWO_OBSTACLES;

/*
  public static final int HOME_XMIN = 24;
  public static final int HOME_XMAX = 26;
  public static final int HOME_YMIN = 24;
  public static final int HOME_YMAX = 26;

  public static final int FOOD_XMIN = 4;
  public static final int FOOD_XMAX = 6;
  public static final int FOOD_YMIN = 42;
  public static final int FOOD_YMAX = 44;

  public static final int MAX_ANTS_PER_LOCATION = 5;
  public static final int TIME_TO_LIVE = 200;
  public static final int NEW_ANTS_PER_TIME_STEP = 2;
  public static final int MAX_ANTS = 100;
  public static final int INITIALANTS = 2;
  public static final double MIN_PHEROMONE = 0.0;
  public static final double MAX_PHEROMONE = 110.0;
  public static final double PHEROMONE_TO_LEAVE_BEHIND = 1;

  public static final double EVAPORATE_CONSTANT = 0.01;
  public static final double DIFFUSION_CONSTANT = 0.05;
  public static final int GRID_HEIGHT = 50;
  public static final int GRID_WIDTH = 50;
  public static final double RANDOM_MOVEMENT_PROBABILITY = 0.1;
*/

/*
  public static final int HOME_XMIN = 52;
  public static final int HOME_XMAX = 54;
  public static final int HOME_YMIN = 52;
  public static final int HOME_YMAX = 54;

  public static final int FOOD_XMIN = 22;
  public static final int FOOD_XMAX = 24;
  public static final int FOOD_YMIN = 22;
  public static final int FOOD_YMAX = 24;

  public static final int MAX_ANTS_PER_LOCATION = 10;
  public static final int TIME_TO_LIVE = 200;
  public static final int newAntsPerTimeStep = 10;
  public static final int MAX_ANTS = 1000;
  public static final int INITIALANTS = 1;
  public static final double MIN_PHEROMONE = 0.0;
  public static final double MAX_PHEROMONE = 10.0;
  public static final double PHEROMONE_TO_LEAVE_BEHIND = 1;

  public static final double EVAPORATE_CONSTANT = 0.001;
  public static final double DIFFUSION_CONSTANT = 0.001;
  public static final int GRID_HEIGHT = 100;
  public static final int GRID_WIDTH = 100;
  public static final double RANDOM_MOVEMENT_PROBABILITY = 0.1;
*/

    public DoubleGrid2D sites = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
    public DoubleGrid2D toFoodGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
    public DoubleGrid2D toHomeGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
    public DoubleGrid2D valgrid2 = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT, 0);
    public SparseGrid2D buggrid = new SparseGrid2D(GRID_WIDTH, GRID_HEIGHT);
    public DoubleGrid2D obstacles = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);

    // a couple of objects to be shared by all ants in the simulation
    DecisionMaker decisionMaker = new DecisionMaker();
    DecisionInfo decisionInfo = new DecisionInfo();

    public AntsForage(long seed)
        { 
        super(seed);
        }
        
    public int foodCollected = 0;

    public void start()
        {
        super.start();  // clear out the schedule

        // make new grids
        sites = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
        toFoodGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
        toHomeGrid = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT,0);
        valgrid2 = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT, 0);
        buggrid = new SparseGrid2D(GRID_WIDTH, GRID_HEIGHT);
        obstacles = new DoubleGrid2D(GRID_WIDTH, GRID_HEIGHT, 0);

        foodCollected = 0;

        switch( OBSTACLES )
            {
            case NO_OBSTACLES:
                break;
            case ONE_OBSTACLE:
                for( int x = 0 ; x < GRID_WIDTH ; x++ )
                    for( int y = 0 ; y < GRID_HEIGHT ; y++ )
                        {
                        obstacles.field[x][y] = 0.0;
                        if( ((x-55)*0.707+(y-35)*0.707)*((x-55)*0.707+(y-35)*0.707)/36+
                            ((x-55)*0.707-(y-35)*0.707)*((x-55)*0.707-(y-35)*0.707)/1024 <= 1 )
                            obstacles.field[x][y] = 1.0;
                        }
                break;
            case TWO_OBSTACLES:
                for( int x = 0 ; x < GRID_WIDTH ; x++ )
                    for( int y = 0 ; y < GRID_HEIGHT ; y++ )
                        {
                        obstacles.field[x][y] = 0.0;
                        if( ((x-45)*0.707+(y-25)*0.707)*((x-45)*0.707+(y-25)*0.707)/36+
                            ((x-45)*0.707-(y-25)*0.707)*((x-45)*0.707-(y-25)*0.707)/1024 <= 1 )
                            obstacles.field[x][y] = 1.0;
                        if( ((x-35)*0.707+(y-70)*0.707)*((x-35)*0.707+(y-70)*0.707)/36+
                            ((x-35)*0.707-(y-70)*0.707)*((x-35)*0.707-(y-70)*0.707)/1024 <= 1 )
                            obstacles.field[x][y] = 1.0;
                        }
                break;
            case ONE_LONG_OBSTACLE:
                for( int x = 0 ; x < GRID_WIDTH ; x++ )
                    for( int y = 0 ; y < GRID_HEIGHT ; y++ )
                        {
                        obstacles.field[x][y] = 0.0;
                        if( (x-60)*(x-60)/1600+
                            (y-50)*(y-50)/25 <= 1 )
                            obstacles.field[x][y] = 1.0;
                        }
                break;
            }

        // initialize the grid with the home and food sites
        for( int x = HOME_XMIN ; x <= HOME_XMAX ; x++ )
            for( int y = HOME_YMIN ; y <= HOME_YMAX ; y++ )
                sites.field[x][y] = 1.0;
        for( int x = FOOD_XMIN ; x <= FOOD_XMAX ; x++ )
            for( int y = FOOD_YMIN ; y <= FOOD_YMAX ; y++ )
                sites.field[x][y] = 0.5;

        // make the ant farm 
        Steppable antFarm = new Steppable()
            {
            public void step(SimState state)
                {
                for(int x=0 ; x<NEW_ANTS_PER_TIME_STEP && numberOfAnts<MAX_ANTS ; x++)
                    {
                    Ant bug = new Ant( random.nextInt(8),
                        PHEROMONE_TO_LEAVE_BEHIND,
                        MIN_PHEROMONE,
                        MAX_PHEROMONE,
                        TIME_TO_LIVE ); 
                    buggrid.setObjectLocation(bug,(HOME_XMAX+HOME_XMIN)/2,(HOME_YMAX+HOME_YMIN)/2);
                    bug.toDiePointer = schedule.scheduleRepeating(bug);
                    numberOfAnts++;
                    }
                }
            };

        numberOfAnts = 0;

        // Schedule the heat bugs -- we could instead use a RandomSequence, which would be faster,
        // but this is a good test of the scheduler
        for(int x=0;x<INITIALANTS;x++)
            {
            Ant bug = new Ant(random.nextInt(8),
                PHEROMONE_TO_LEAVE_BEHIND,
                MIN_PHEROMONE,
                MAX_PHEROMONE,
                TIME_TO_LIVE ); 
            buggrid.setObjectLocation(bug,(HOME_XMAX+HOME_XMIN)/2,(HOME_YMAX+HOME_YMIN)/2);
            bug.toDiePointer = schedule.scheduleRepeating(bug);
            numberOfAnts++;
            }
                            
        // Schedule the decreaser to happen after the AntsForage
        schedule.scheduleRepeating(Schedule.EPOCH,1,new Diffuser(toHomeGrid,valgrid2,EVAPORATE_CONSTANT,DIFFUSION_CONSTANT),1);
        schedule.scheduleRepeating(Schedule.EPOCH,1,new Diffuser(toFoodGrid,valgrid2,EVAPORATE_CONSTANT,DIFFUSION_CONSTANT),1);
        // Schedule the ant farm to happen after the AntsForage
        schedule.scheduleRepeating(Schedule.EPOCH,1,antFarm,1);
        }

    public int numberOfAnts = 0;

    public static void main(String[] args)
        {
        doLoop(AntsForage.class, args);
        System.exit(0);
        }    
    }
    
    
    
    
    
