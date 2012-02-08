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

import sim.util.geo.MasonGeometry;


public class SchellingGeometry extends MasonGeometry
{

    private int id = -1;

    private String soc;

    public int initRed = 0, initBlue = 0;

    public ArrayList<Person> residents;
    public ArrayList<SchellingGeometry> neighbors;



    public SchellingGeometry()
    {
        super();
        residents = new ArrayList<Person>();
        neighbors = new ArrayList<SchellingGeometry>();
    }



    public void init()
    {
//        id = getDoubleAttribute("ID_ID").intValue();
        initRed = getIntegerAttribute("RED");
        initBlue = getIntegerAttribute("BLUE");

//        soc = getStringAttribute("SOC");
//
//
//        ArrayList<AttributeValue> attribs =
//            (ArrayList<AttributeValue>) geometry.getUserData();
//
//        for (AttributeValue af : attribs)
//        {
//            if (af.getName().equals("ID_ID"))
//            {
//                Double d = (Double) af.getValue();
//                id = (int) Math.floor(d);
//            } else if (af.getName().equals("RED"))
//            {
//                initRed = (Integer) af.getValue();
//            } else if (af.getName().equals("BLUE"))
//            {
//                initBlue = (Integer) af.getValue();
//            }
//        }
    }



    int getID()
    {
        return getDoubleAttribute("ID_ID").intValue();
//
//        if (id == -1)
//        {
//            init();
//        }
//        return id;
    }



    String getSoc()
    {
        return getStringAttribute("SOC");
//        if (soc == null)
//        {
//            init();
//        }
//        return soc;
    }

}