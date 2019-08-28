/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.hyperCycle;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.IntGrid2D;

/**
 * This class implements the basic automaton. An instance of this class will be
 * called every frame in the simulation. This automaton holds no information
 * except the current parameter set. In overall this class provides the
 * computational process only.
 * <p>
 * The information for computing is provided by an instance of the
 * {@link HyperCycleSimulation} class. This instance will be accessible in the
 * {@link #step(SimState)} method. The {@link #step(SimState)} will be called
 * every simulation frame and calls the {@link #cycle(IntGrid2D, IntGrid2D)}
 * method with the appropriate parameters.
 * <p>
 * This automaton uses only basic data structures of the MASON library and is
 * really straightforward. I did no profiling to optimize the execution speed
 * but I've got the feeling the Java compiler is doing a good job on optimizing
 * so there is no need to do it yourself. If you need more speed work on the
 * visualization process.
 * 
 * @author hoehne
 * 
 */
public class HyperCycleAutomaton implements Steppable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3082011624710483151L;
	/**
	 * The current parameter set.
	 */
	HyperCycleParameters p;

	/**
	 * Constructor that will be called with a set of parameters.
	 * 
	 * @param params
	 */
	public HyperCycleAutomaton(HyperCycleParameters params) {
		setParameters(params);
	}

	/**
	 * Set the current parameters set.
	 * 
	 * @param p
	 */
	public void setParameters(HyperCycleParameters p) {
		this.p = p;
	}

	/**
	 * The overwritten method that will be called every step. An instance of the
	 * class {@link HyperCycleSimulation} is provided for the computation
	 * process.
	 * 
	 * @param state
	 *            The {@link HyperCycleSimulation} object
	 */
	public void step(SimState state) {
		// do the casting
		HyperCycleSimulation hcs = (HyperCycleSimulation) state;

		// call the cycle to compute the next state of all cells
		cycle(hcs.grid, hcs.gridBuffer);
	}

	/**
	 * A public method for randomly setting up the cellular automaton. The
	 * parasite state will be set less often than the other states.
	 * 
	 * @param d
	 */

	public static final void randomSeed(IntGrid2D d, HyperCycleParameters p) {
		final int maxN = p.states;
		int x;
		int y;
		int w = d.getWidth();
		int h = d.getHeight();
		int cell = 0;

		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++) {
				cell = p.r.nextInt(maxN);
				if (cell == 1) {
					cell = p.r.nextInt(maxN);
					if (cell == 1) {
						cell = p.r.nextInt(maxN);
					}
				}
				d.set(x, y, cell);
			}
		}
	}

	/**
	 * A single cycle in the life of the cellular automaton. Compute for every
	 * cell the next state and also apply the Toffoli & Margolus diffusion.
	 * 
	 * @param d
	 * @param buffer
	 */
	final private void cycle(IntGrid2D d, IntGrid2D buffer) {

		buffer.setTo(d);

		int x;
		int y;
		int w = d.getWidth();
		int h = d.getHeight();

		for (y = 0; y < h; y++) {
			for (x = 0; x < w; x++)
				computeNextState(d, buffer, x, y);
		}

		for (y = 0; y < h; y += 2) {
			for (x = 0; x < w; x += 2)
				turn22(d, x, y);
		}
		for (y = 1; y < h; y += 2) {
			for (x = 1; x < w; x += 2)
				turn22(d, x, y);
		}
	}

	/**
	 * Compute the next state for the given cell. Place the next state in the
	 * provided destination array.
	 * 
	 * @param d
	 * @param s
	 * @param x
	 * @param y
	 */
	final private void computeNextState(IntGrid2D d, IntGrid2D s, int x, int y) {
		int cell = s.get(x, y);
		if (cell == 0) {
			// an empty cell, try to fill it

			// compute directions

			// compute all Moore neighbours
			int cNW = s.get(s.stx(x - 1), s.sty(y - 1));
			int cN = s.get(s.stx(x), s.sty(y - 1));
			int cNE = s.get(s.stx(x + 1), s.sty(y - 1));
			int cW = s.get(s.stx(x - 1), s.sty(y));
			int cE = s.get(s.stx(x + 1), s.sty(y));
			int cSW = s.get(s.stx(x - 1), s.sty(y + 1));
			int cS = s.get(s.stx(x), s.sty(y + 1));
			int cSE = s.get(s.stx(x + 1), s.sty(y + 1));

			// compute replication factor for the neighbours N, S, E, W

			int aN = p.replication[cN] + p.rSupport[cN][cNE]
					+ p.rSupport[cN][cNW] + p.rSupport[cN][cE]
					+ p.rSupport[cN][cW];
			int aS = p.replication[cS] + p.rSupport[cS][cSE]
					+ p.rSupport[cS][cSW] + p.rSupport[cS][cE]
					+ p.rSupport[cS][cW];
			int aE = p.replication[cE] + p.rSupport[cE][cNE]
					+ p.rSupport[cE][cSE] + p.rSupport[cE][cN]
					+ p.rSupport[cE][cS];
			int aW = p.replication[cW] + p.rSupport[cW][cNW]
					+ p.rSupport[cW][cSW] + p.rSupport[cW][cN]
					+ p.rSupport[cW][cS];

			// build the sum of all replication factors
			int a = p.aEmpty + aN + aS + aE + aW;

			// compute the relative probability for each state given by the
			// neighbours N, S, E, W
			float pEmpty = (float) p.aEmpty / (float) a;
			float pN = (float) aN / (float) a;
			float pS = (float) aS / (float) a;
			float pE = (float) aE / (float) a;
			float pW = (float) aW / (float) a;

			// build an interval [0..1[ for all states N, S, E, W including the
			// probability of staying empty
			pS += pN;
			pE += pS;
			pW += pE;
			pEmpty += pW;

			// choose a random number from [0..1[
			float r = p.r.nextFloat();
			// compare in which interval the chosen number the lies in; choose
			// the corresponding neighbour state
			// if no state is chosen the cell will remain empty
			if (r < pN)
				cell = cN;
			else if (r < pS)
				cell = cS;
			else if (r < pE)
				cell = cE;
			else if (r < pW)
				cell = cW;
		} else {
			// a filled cell, compute if it will become empty in the next step
			if (p.r.nextFloat() < p.decays[cell])
				cell = 0;
		}

		// set the next state of the current cell
		d.set(x, y, cell);
	}


	/**
	 * Diffuse a 2x2 block clockwise or counter-clockwise. This procedure was
	 * published by Toffoli and Margolus to simulate the diffusion in an ideal
	 * gas. The 2x2 blocks will be turned by random in one direction (clockwise
	 * or counter clockwise). Usually a second run will be done but this time
	 * with a offset of one to the previous run
	 * 
	 * @param grid
	 * @param x
	 * @param y
	 */
	final private void turn22(IntGrid2D grid, int x, int y) {
		int p1, p2, p3, p4;

		// retrieving the 2x2 block as
		// 12
		// 43
		// where every digit denotes a cell

		p1 = grid.get(grid.stx(x), grid.sty(y));
		p2 = grid.get(grid.stx(x + 1), grid.sty(y));
		p3 = grid.get(grid.stx(x + 1), grid.sty(y + 1));
		p4 = grid.get(grid.stx(x), grid.sty(y + 1));

		if (p.r.nextBoolean()) {
			// turn 2x2 block clockwise

			// storing the block as
			// 41
			// 32
			grid.set(grid.stx(x), grid.sty(y), p4);
			grid.set(grid.stx(x + 1), grid.sty(y), p1);
			grid.set(grid.stx(x + 1), grid.sty(y + 1), p2);
			grid.set(grid.stx(x), grid.sty(y + 1), p3);
		} else {
			// turn 2x2 block counter clockwise

			// storing the block as
			// 23
			// 14
			grid.set(grid.stx(x), grid.sty(y), p2);
			grid.set(grid.stx(x + 1), grid.sty(y), p3);
			grid.set(grid.stx(x + 1), grid.sty(y + 1), p4);
			grid.set(grid.stx(x), grid.sty(y + 1), p1);
		}
	}
}
