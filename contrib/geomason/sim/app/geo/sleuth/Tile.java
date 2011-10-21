//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//sleuth is free software: you can redistribute it and/or modify
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

package sim.app.geo.sleuth;


/**
 * Stores information about the underlying qualities of the raster grid cells
 */
public class Tile
{

    // position in landscape
    int x;
    int y;


    double slope;
    double landuse;

    boolean excluded;

    double transport;

    double hillshade;

    boolean urbanOriginally;

    boolean urbanized;


    public Tile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

}