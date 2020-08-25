/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package sim.app.serengeti.func;
import ec.*;
import sim.app.serengeti.*;
import ec.gp.*;
import ec.util.*;

public class RandDir extends GPNode
    {
    public String toString() { return "rand-dir"; }

    public int expectedChildren() { return 0; }

    public void eval(final EvolutionState state,
        final int thread,
        final GPData input,
        final ADFStack stack,
        final GPIndividual individual,
        final Problem problem)
        {
        SerengetiData rd = ((SerengetiData)(input));
        double angle = Math.PI * 2 * state.random[thread].nextDouble();
        rd.x = Math.cos(angle);
        rd.y = Math.sin(angle);
        }
    }



