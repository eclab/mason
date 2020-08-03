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

public class IfGEC extends GPNode
    {
    public String toString() { return "if>="; }

    public int expectedChildren() { return 4; }

    public void eval(final EvolutionState state,
        final int thread,
        final GPData input,
        final ADFStack stack,
        final GPIndividual individual,
        final Problem problem)
        {
        double mag1;
        double mag2;
        
        SerengetiData rd = ((SerengetiData)(input));

        children[0].eval(state,thread,input,stack,individual,problem);
        mag1 = rd.x * rd.x + rd.y * rd.y;

        children[1].eval(state,thread,input,stack,individual,problem);
        mag2 = rd.x * rd.x + rd.y * rd.y;
        
        if (mag1 >= mag2)
        	{
       		children[2].eval(state,thread,input,stack,individual,problem);
        	}
        else
        	{
       		children[3].eval(state,thread,input,stack,individual,problem);
        	}
        }
    }



