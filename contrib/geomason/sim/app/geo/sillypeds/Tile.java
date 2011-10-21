//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//sillypeds is free software: you can redistribute it and/or modify
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
package sim.app.geo.sillypeds;

import java.util.ArrayList;

//TODO: better note! basically: maintained as an object so can make it on fire or whatever
public class Tile
{

    int loc_x, loc_y; // location in the grid
    boolean exit = false;
    ArrayList<Pedestrian> peds = new ArrayList<Pedestrian>();
    double baseheight = 0; // the pure elevation of the tile
    double cumulativeHeight = 0; // the combined elevation and height from people of the tile
    double pedestrianFactor = 1;
    int trace = -1;

    // a basic Tile

    // initialize a Tile

    public Tile(int x, int y)
    {
        loc_x = x;
        loc_y = y;
    }

    // initialize a Tile at a certain altitude


    public Tile(int x, int y, double h)
    {
        loc_x = x;
        loc_y = y;
        baseheight = h;
        cumulativeHeight = baseheight;
    }



    public void makeExit()
    {
        exit = true;
    }

    //
    // MANAGING PEDESTIANS
    //

    // add a Pedestrian and update the height accordingly

    public void addPed(Pedestrian r)
    {
        peds.add(r);
        trace++;
    }

    // add some Pedestrians and update the height accordingly


    public void addPeds(ArrayList<Pedestrian> rs)
    {
        peds.addAll(rs);
        trace += rs.size();
    }

    // remove a Pedestrian and update the height accordingly


    public void removePed(Pedestrian r)
    {
        peds.remove(r);
    }

    // remove some Pedestrians and update the height accordingly


    public void removePeds(ArrayList<Pedestrian> rs)
    {
        peds.removeAll(rs);
    }

}