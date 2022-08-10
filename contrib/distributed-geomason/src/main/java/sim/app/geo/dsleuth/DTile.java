package sim.app.geo.dsleuth;

import sim.engine.DObject;

public class DTile extends DObject{
	
    // position in landscape
    public int x;
    public int y;


    /**
     * In range [1,51].  I have no idea why.
     */
    public int slope;

    /**
     * One of four values to indicate how this tile is used.
     */
    public int landuse;

    public boolean excluded;

    public int transport;

    public int hillshade;

    public boolean urbanOriginally;

    public boolean urbanized;


    public DTile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

}
