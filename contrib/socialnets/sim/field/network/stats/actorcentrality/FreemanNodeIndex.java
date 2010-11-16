/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.network.stats.actorcentrality;

import sim.field.network.*;
/**
 * This centrality metric allows for a "maximum possible value difference" 
 * given the size of the net (only the number of nodes in the graph is fixed).
 * 
 * @author Gabriel Catalin Balan
 */
public abstract class FreemanNodeIndex extends NodeIndex {
    public FreemanNodeIndex(final Network network){super(network);}
    public abstract double getMaxCummulativeDifference();
    public double getStandarnizedMaxCummulativeDifference()
        {
        return getMaxCummulativeDifference()/getMaxValue();
        }
    }
