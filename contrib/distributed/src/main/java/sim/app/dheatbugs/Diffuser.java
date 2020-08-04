/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs;

import sim.engine.AbstractStopping;
import sim.engine.SimState;
import sim.field.grid.NDoubleGrid2D;

/**
 * This agent decreases evaporates and diffuses all the heat at each time step.
 * See the comments in Diffuser.java for a tutorial on how to speed up Java
 * many-fold in classes such as Diffuser.
 */

public class Diffuser extends AbstractStopping {
	private static final long serialVersionUID = 1;

	public void step(final SimState state) {
		final DHeatBugs heatbugs = (DHeatBugs) state;

		// locals are faster than instance variables
		final double[] _valgrid_field = heatbugs.valgrid.getStorageArray();
		final double[] _valgrid2_field = heatbugs.valgrid2.getStorageArray();
		final int _gridWidth = heatbugs.getPartition().getPartition().getSize()[0];
		final int _gridHeight = heatbugs.getPartition().getPartition().getSize()[1];
		final double _evaporationRate = heatbugs.evaporationRate;
		final double _diffusionRate = heatbugs.diffusionRate;
		final int aoi = heatbugs.aoi[0];
		final int offset = heatbugs.getPartition().getPartition().getSize()[1] + (2 * aoi);
		int past, curr, next;

		// for each x and y position
		for (int x = aoi; x < _gridWidth + aoi; x++) {
			past = (x - 1) * offset;
			curr = past + offset;
			next = curr + offset;

			for (int y = aoi; y < _gridHeight + aoi; y++) {
				final double average = (_valgrid_field[past + y - 1] + _valgrid_field[past + y]
						+ _valgrid_field[past + y + 1]
						+ _valgrid_field[curr + y - 1] + _valgrid_field[curr + y] + _valgrid_field[curr + y + 1]
						+ _valgrid_field[next + y - 1] + _valgrid_field[next + y] + _valgrid_field[next + y + 1]) / 9.0;

				// load the new value into HeatBugs.this.valgrid2
				_valgrid2_field[curr + y] = _evaporationRate
						* (_valgrid_field[curr + y] + _diffusionRate * (average - _valgrid_field[curr + y]));
			}

			// swap elements
			past = curr;
			curr = next;
			next += offset;
		}

		final NDoubleGrid2D temp = heatbugs.valgrid;
		heatbugs.valgrid = heatbugs.valgrid2;
		heatbugs.valgrid2 = temp;
	}
}
