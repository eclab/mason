/**
 ** SchellingGeometry.java
 **
 ** Copyright 2011 by Andrew Crooks, Sarah Wise, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 **/
package sim.app.geo.schellingspace;

import java.util.ArrayList;

import sim.util.geo.AttributeField;
import sim.util.geo.MasonGeometry;




public class SchellingGeometry extends MasonGeometry
{

    int id = -1;
    String soc;
    int initRed = 0, initBlue = 0;
    ArrayList<Person> residents;
    ArrayList<SchellingGeometry> neighbors;



    public SchellingGeometry()
    {
        super();
        residents = new ArrayList<Person>();
        neighbors = new ArrayList<SchellingGeometry>();
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
            } else if (af.name.equals("RED"))
            {
                initRed = (Integer) af.value;
            } else if (af.name.equals("BLUE"))
            {
                initBlue = (Integer) af.value;
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