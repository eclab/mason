/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.sweep;

interface SimulationJob
    {
    double[] run();  // returns a double[] representing the resulting the dependent variable results
    }
