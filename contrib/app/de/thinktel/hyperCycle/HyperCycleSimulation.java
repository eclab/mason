/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.hyperCycle;

import sim.engine.SimState;
import sim.field.grid.IntGrid2D;

/**
 * This class hold all information for the simulation. These structures are the
 * current parameters for computation and the grid with all the cells. A second
 * grid is stored as a 'shadow register' to store the old values of all the
 * cells. The old state has to be preserved because computing on the current
 * states and concurrently modifying these will result in corrupt computation.
 * 
 * @author hoehne
 * 
 */
public class HyperCycleSimulation extends SimState {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2187248402977949189L;

	/**
	 * The current parameter set for the simulation.
	 */
	HyperCycleParameters p;

	/**
	 * The grid that holds all cells.
	 */
	public IntGrid2D grid;

	/**
	 * A grid used for computation to temporary hold the values (shadow
	 * register)
	 */
	protected IntGrid2D gridBuffer;

	/**
	 * The public constructor. Takes a seed for initializing the random number
	 * generator and also a parameter set.
	 * 
	 * @param seed
	 * @param params
	 */
	public HyperCycleSimulation(long seed, HyperCycleParameters params) {
		super(seed);

		if (params == null)
			params = new HyperCycleParameters();
		this.p = params;
	}

	/**
	 * Return the current parameter set.
	 * 
	 * @return p the parameters
	 */
	public HyperCycleParameters getParameters() {
		return p;
	}

	/**
	 * Overwritten method for execution of this {@link SimState} object.
	 */
	public void start() {
		super.start();
		// initialize the grids for computing.
		grid = new IntGrid2D(p.getWidth(), p.getHeight());
		gridBuffer = new IntGrid2D(p.getWidth(), p.getHeight());

		// seed the grid with random values
		HyperCycleAutomaton.randomSeed(grid, p);

		// add this simulation to the scheduler
		schedule.scheduleRepeating(new HyperCycleAutomaton(p));
	}
}
