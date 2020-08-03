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
import sim.util.*;

public class LionOne extends Gazelle
    {
    public int number = 0;
    
    public String toString() { return "lion1"; }

    public void eval(final EvolutionState state,
        final int thread,
        final GPData input,
        final ADFStack stack,
        final GPIndividual individual,
        final Problem problem)
        {
        SerengetiData rd = ((SerengetiData)(input));
        SerengetiEC sec = ((SerengetiEC)problem);
        Serengeti s = sec.simstate;

        Double2D lpos = sec.simstate.field.getObjectLocation(sec.simstate.currentLion);		// me
        Double2D gpos = sec.simstate.field.getObjectLocation(sec.simstate.lions.get(number));

        Double2D best = nearestTo(lpos, gpos, s.width);
        rd.x = best.x;
        rd.y = best.y;
        }
    }



