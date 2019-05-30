/**
 ** Basin.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.waterworld;

import java.util.ArrayList;



public class Basin
{

    int loc_x, loc_y; // location in the grid
    ArrayList<Raindrop> drops = new ArrayList<Raindrop>();
    double baseheight = 0; // the pure elevation of the tile
    double cumulativeHeight = 0; // the combined elevation and height from water of the tile
    double raindropFactor = 1;

    // initialize a Basin
    public Basin(int x, int y)
    {
        loc_x = x;
        loc_y = y;
    }

    // initialize a Basin at a certain altitude
    public Basin(int x, int y, int h)
    {
        loc_x = x;
        loc_y = y;
        baseheight = h;
        cumulativeHeight = baseheight;
    }

    //
    // MANAGING RAINDROPS AND HEIGHTS
    //

    // add a Raindrop and update the height accordingly
    public void addDrop(Raindrop r)
    {
        drops.add(r);
        cumulativeHeight += raindropFactor;
    }

    // add some Raindrops and update the height accordingly
    public void addDrops(ArrayList<Raindrop> rs)
    {
        drops.addAll(rs);
        cumulativeHeight += rs.size() * raindropFactor;
    }

    // remove a Raindrop and update the height accordingly
    public void removeDrop(Raindrop r)
    {
        drops.remove(r);
        cumulativeHeight -= raindropFactor;
    }

    // remove some Raindrops and update the height accordingly
    public void removeDrops(ArrayList<Raindrop> rs)
    {
        drops.removeAll(rs);
        cumulativeHeight -= rs.size() * raindropFactor;
    }

}
