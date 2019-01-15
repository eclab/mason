/**
 ** Polygon.java
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
package sim.app.geo.schellingpolygon;

import java.util.ArrayList;
import sim.util.geo.MasonGeometry;



public class Polygon extends MasonGeometry
{
    int id = -1;
    String soc;

    ArrayList<Person> residents;
    ArrayList<Polygon> neighbors;



    public Polygon()
    {
        super();
        residents = new ArrayList<Person>();
        neighbors = new ArrayList<Polygon>();
    }



    public void init()
    {
        id = getDoubleAttribute("ID_ID").intValue();
        soc = getStringAttribute("SOC");
    }



    int getID()
    {
        if (id == -1)
        {
            init();
        }
        return id;
    }



    String getSoc()
    {
        if (soc == null)
        {
            init();
        }
        return soc;
    }

}
