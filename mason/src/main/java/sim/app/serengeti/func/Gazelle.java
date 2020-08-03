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

public class Gazelle extends GPNode
    {
    public String toString() { return "gazelle"; }

    public int expectedChildren() { return 0; }

	public Double2D nearestTo(Double2D me, Double2D him, double width)
		{
        double bestx = 0;
        double besty = 0;
        double distance = java.lang.Double.POSITIVE_INFINITY;
        for(double x = -width; x <= width; x+= width)
        	{
        	for(double y = -width; y <= width; y+=width)
        		{
        		double gx = him.x + x;
        		double gy = him.y + y;
				double nx = gx - me.x;
				double ny = gy - me.y;
				double nd = nx * nx + ny * ny;
				if (nd < distance) { bestx = nx; besty = ny; distance = nd; }
        		}
        	}
        Double2D d2d = new Double2D(bestx, besty);
        return d2d.subtract(me);
		}

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
        Double2D gpos = sec.simstate.field.getObjectLocation(sec.simstate.gazelle);
        
        Double2D best = nearestTo(lpos, gpos, s.width);
        rd.x = best.x;
        rd.y = best.y;
        
        //System.err.println("" + lpos + " " + gpos + " --> " + rd.x + " " + rd.y);
        }
    }



