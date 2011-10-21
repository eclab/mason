//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//waterworld is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//It is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//
//***************************************************************
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