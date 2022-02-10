package sim.app.geo.dsleuth;

import sim.engine.DObject;

public class DTile extends DObject{
	
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


    public DTile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

}
