/**
 ** Tile.java
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
package sleuth;


/**
 * Stores information about the underlying qualities of the raster grid cells
 */
public class Tile
{

    // position in landscape
    int x;
    int y;


    /**
     * In range [1,51].  I have no idea why.
     */
    int slope;

    /**
     * One of four values to indicate how this tile is used.
     */
    int landuse;

    boolean excluded;

    int transport;

    int hillshade;

    boolean urbanOriginally;

    boolean urbanized;


    public Tile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

}