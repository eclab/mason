/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;


/** Synapse is a simple synapse connection between two Cells.  Cells have a "from" (outgoing)
    Cell, and a "to" (incoming) Cell.  Synapses also have two types, type_chemical for chemical
    synapses and type_gap for gap junctions. These two types are closely correlated with the
    values returned by DisplaySynapses.java, so be careful when adding new types or changing
    their inherent values. Synapses also have a number, indicating the number of synapses
    from the one cell to the other (all such synapses are represented by the same Synapse
    object). */

public class Synapse extends Object
    {
    private static final long serialVersionUID = 1;

    static int type_chemical=0;
    static int type_gap=1;
    public Cell from;
    public Cell to;
    public int type;
    public int number=1;
    public String toString()
        {
        String s = (type == type_chemical ? "chemical" : "gap");
        if (number > 1) s = s + " (" + number + ")";
        return s;
        }
    }
