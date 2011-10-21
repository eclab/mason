//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//schellingpolygon is free software: you can redistribute it and/or modify
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