/*

Tile.java

Copyright 2011 by Andrew Crooks, Sarah Wise, Mark Coletti, and
George Mason University Mason University.

Licensed under the Academic Free License version 3.0

See the file "LICENSE" for more information

*/
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