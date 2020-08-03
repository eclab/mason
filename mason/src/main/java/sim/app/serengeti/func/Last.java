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

public class Last extends GPNode
    {
    public String toString() { return "last"; }

    public int expectedChildren() { return 0; }

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
        Double2D d = s.field.getObjectLocation(s.currentLion).subtract(s.currentLion.last);
        rd.x = d.x;
        rd.y = d.y;
        }
    }



