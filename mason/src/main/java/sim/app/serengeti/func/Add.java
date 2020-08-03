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

public class Add extends GPNode
    {
    public String toString() { return "+"; }

    public int expectedChildren() { return 2; }

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

        children[0].eval(state,thread,input,stack,individual,problem);
        resultx = rd.x;
        resulty = rd.y;

        children[1].eval(state,thread,input,stack,individual,problem);
        rd.x = resultx + rd.x;
        rd.y = resulty + rd.y;
        }
    }



