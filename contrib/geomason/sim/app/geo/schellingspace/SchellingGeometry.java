//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//schellingspace is free software: you can redistribute it and/or modify
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