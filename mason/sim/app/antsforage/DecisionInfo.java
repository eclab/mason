/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;
import java.awt.*;

public class DecisionInfo implements java.io.Serializable
    {

    public Point position;
    public int orientation;
    public double homePheromoneAmount;
    public double foodPheromoneAmount;

    // to be computed from homePheromoneAmount and foodPheromoneAmount based on goal (go to home or to food)
    public double profit;

    public DecisionInfo() { position = new Point(); }

    }
