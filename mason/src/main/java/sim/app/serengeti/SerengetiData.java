/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package sim.app.serengeti;
import ec.gp.*;

public class SerengetiData extends GPData
    {
    public double x;
    public double y;
    public void copyTo(final GPData gpd) 
        {
        SerengetiData sd = (SerengetiData) gpd; 
        sd.x = x; 
        sd.y = y;
        }
    }
