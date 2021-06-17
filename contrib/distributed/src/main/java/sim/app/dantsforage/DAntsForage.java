/*
  Copyright 2009 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dantsforage;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

public /* strictfp */ class DAntsForage extends DSimState
{
	private static final long serialVersionUID = 1;

	public static final int GRID_HEIGHT = 100;
	public static final int GRID_WIDTH = 100;

	public static final int HOME_XMIN = 75;
	public static final int HOME_XMAX = 75;
	public static final int HOME_YMIN = 75;
	public static final int HOME_YMAX = 75;

	public static final int FOOD_XMIN = 25;
	public static final int FOOD_XMAX = 25;
	public static final int FOOD_YMIN = 25;
	public static final int FOOD_YMAX = 25;

	public static final int NO_OBSTACLES = 0;
	public static final int ONE_OBSTACLE = 1;
	public static final int TWO_OBSTACLES = 2;
	public static final int ONE_LONG_OBSTACLE = 3;

	public static final int OBSTACLES = NO_OBSTACLES;

	public static final int ALGORITHM_VALUE_ITERATION = 1;
	public static final int ALGORITHM_TEMPORAL_DIFERENCE = 2;
	public static final int ALGORITHM = ALGORITHM_VALUE_ITERATION;

	public static final double IMPOSSIBLY_BAD_PHEROMONE = -1;
	public static final double LIKELY_MAX_PHEROMONE = 3;

	public static final int HOME = 1;
	public static final int FOOD = 2;

	public int numAnts = 1000;
	public double evaporationConstant = 1.0;
	public double reward = 1.0;
	public double updateCutDown = 0.9;
	public double diagonalCutDown = computeDiagonalCutDown();

	public double computeDiagonalCutDown()
	{
		return Math.pow(updateCutDown, Math.sqrt(2));
	}

	public double momentumProbability = 0.8;
	public double randomActionProbability = 0.1;

	// some properties
	public int getNumAnts()
	{
		return numAnts;
	}

	public void setNumAnts(int val)
	{
		if (val > 0)
			numAnts = val;
	}

	public double getEvaporationConstant()
	{
		return evaporationConstant;
	}

	public void setEvaporationConstant(double val)
	{
		if (val >= 0 && val <= 1.0)
			evaporationConstant = val;
	}

	public double getReward()
	{
		return reward;
	}

	public void setReward(double val)
	{
		if (val >= 0)
			reward = val;
	}

	public double getCutDown()
	{
		return updateCutDown;
	}

	public void setCutDown(double val)
	{
		if (val >= 0 && val <= 1.0)
			updateCutDown = val;
		diagonalCutDown = computeDiagonalCutDown();
	}

	public Object domCutDown()
	{
		return new Interval(0.0, 1.0);
	}

	public double getMomentumProbability()
	{
		return momentumProbability;
	}

	public void setMomentumProbability(double val)
	{
		if (val >= 0 && val <= 1.0)
			momentumProbability = val;
	}

	public Object domMomentumProbability()
	{
		return new Interval(0.0, 1.0);
	}

	public double getRandomActionProbability()
	{
		return randomActionProbability;
	}

	public void setRandomActionProbability(double val)
	{
		if (val >= 0 && val <= 1.0)
			randomActionProbability = val;
	}

	public Object domRandomActionProbability()
	{
		return new Interval(0.0, 1.0);
	}

	public DIntGrid2D sites;
	public DDoubleGrid2D toFoodGrid;
	public DDoubleGrid2D toHomeGrid;
	public DDenseGrid2D<DAnt> buggrid;
	public DIntGrid2D obstacles;
	

	public DAntsForage(long seed)
	{
		super(seed, GRID_WIDTH, GRID_HEIGHT, 10);
		sites = new DIntGrid2D(this);
		toFoodGrid = new DDoubleGrid2D(this);
		toHomeGrid = new DDoubleGrid2D(this);
		buggrid = new DDenseGrid2D<DAnt>(this);
		obstacles = new DIntGrid2D(this);
	}

	public void start()
	{
		super.start(); // clear out the schedule

		switch (OBSTACLES)
		{
		case NO_OBSTACLES:
			break;
		case ONE_OBSTACLE:
			for (int x = 0; x < GRID_WIDTH; x++)
				for (int y = 0; y < GRID_HEIGHT; y++)
				{
					obstacles.set(new Int2D(x, y), 0);
					if (((x - 55) * 0.707 + (y - 35) * 0.707) * ((x - 55) * 0.707 + (y - 35) * 0.707) / 36 +
							((x - 55) * 0.707 - (y - 35) * 0.707) * ((x - 55) * 0.707 - (y - 35) * 0.707) / 1024 <= 1)
						obstacles.set(new Int2D(x, y), 1);

				}
			break;
		case TWO_OBSTACLES:
			for (int x = 0; x < GRID_WIDTH; x++)
				for (int y = 0; y < GRID_HEIGHT; y++)
				{
					obstacles.set(new Int2D(x, y), 0);
					if (((x - 45) * 0.707 + (y - 25) * 0.707) * ((x - 45) * 0.707 + (y - 25) * 0.707) / 36 +
							((x - 45) * 0.707 - (y - 25) * 0.707) * ((x - 45) * 0.707 - (y - 25) * 0.707) / 1024 <= 1)
						obstacles.set(new Int2D(x, y), 1);

					if (((x - 35) * 0.707 + (y - 70) * 0.707) * ((x - 35) * 0.707 + (y - 70) * 0.707) / 36 +
							((x - 35) * 0.707 - (y - 70) * 0.707) * ((x - 35) * 0.707 - (y - 70) * 0.707) / 1024 <= 1)
						obstacles.set(new Int2D(x, y), 1);
				}
			break;
		case ONE_LONG_OBSTACLE:
			for (int x = 0; x < GRID_WIDTH; x++)
				for (int y = 0; y < GRID_HEIGHT; y++)
				{
					obstacles.set(new Int2D(x, y), 0);
					if ((x - 60) * (x - 60) / 1600 +
							(y - 50) * (y - 50) / 25 <= 1)
						obstacles.set(new Int2D(x, y), 1);
				}
			break;
		}

		// initialize the grid with the home and food sites
		for (int x = HOME_XMIN; x <= HOME_XMAX; x++)
			for (int y = HOME_YMIN; y <= HOME_YMAX; y++)
				if (getPartition().getLocalBounds().contains(new Int2D(x, y)))
					sites.set(new Int2D(x, y), 1);
		for (int x = FOOD_XMIN; x <= FOOD_XMAX; x++)
			for (int y = FOOD_YMIN; y <= FOOD_YMAX; y++)
				if (getPartition().getLocalBounds().contains(new Int2D(x, y)))
					sites.set(new Int2D(x, y), 2);

		for (int x = 0; x < numAnts; x++)
		{
			if (getPartition().getLocalBounds()
					.contains(new Int2D((HOME_XMAX + HOME_XMIN) / 2, (HOME_YMAX + HOME_YMIN) / 2)))
			{
				DAnt ant = new DAnt(reward, (HOME_XMAX + HOME_XMIN) / 2, (HOME_YMAX + HOME_YMIN) / 2);
				buggrid.add(new Int2D((HOME_XMAX + HOME_XMIN) / 2, (HOME_YMAX + HOME_YMIN) / 2), ant);
				schedule.scheduleRepeating(Schedule.EPOCH + x, 0, ant, 1);
			}
		}

		// Schedule evaporation to happen after the ants move and update
		schedule.scheduleRepeating(Schedule.EPOCH, 1, new DSteppable()
		{
			public void step(SimState state)
			{
				toFoodGrid.multiply(evaporationConstant);
				toHomeGrid.multiply(evaporationConstant);
			}
		}, 1);

	}

	public static void main(String[] args)
	{
		doLoopDistributed(DAntsForage.class, args);
		System.exit(0);
	}
}
