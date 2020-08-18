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

public class Invert extends GPNode
    {
    public String toString() { return "inv"; }

    public int expectedChildren() { return 1; }

    public void eval(final EvolutionState state,
        final int thread,
        final GPData input,
        final ADFStack stack,
        final GPIndividual individual,
        final Problem problem)
        {
        double resultx;
        double resulty;
        
        SerengetiData rd = ((SerengetiData)(input));
		SerengetiEC sec = (SerengetiEC)problem;
		
        children[0].eval(state,thread,input,stack,individual,problem);
        double mag = Math.sqrt(rd.x * rd.x + rd.y * rd.y);
        if (mag != 0)  // dunno what we did when it's zero
        	{
        	double max = Math.sqrt(sec.simstate.width * sec.simstate.width + sec.simstate.height * sec.simstate.height);
        	rd.x *= (1.0 / mag * (max - mag));
        	rd.y *= (1.0 / mag * (max - mag));
        	}
        }
    }



