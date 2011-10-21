/**
 ** Polygon.java
 **
 ** Copyright 2011 by Andrew Crooks, Sarah Wise, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 **/
package sim.app.geo.schellingpolygon;

import java.util.ArrayList;

import sim.util.geo.AttributeField;
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
        ArrayList<AttributeField> attribs =
            (ArrayList<AttributeField>) geometry.getUserData();
        for (AttributeField af : attribs)
        {
            if (af.name.equals("ID_ID"))
            {
                Double d = (Double) af.value;
                id = (int) Math.floor(d);
            } else if (af.name.equals("SOC"))
            {
                soc = (String) af.value;
            }
        }
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